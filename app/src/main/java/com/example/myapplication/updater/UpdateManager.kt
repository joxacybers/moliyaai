package com.example.myapplication.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val changelog: String,
    val downloadUrl: String,
    val apkSize: Long,
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

class UpdateManager(
    private val context: Context,
    private val repoOwner: String = "joxacybers",
    private val repoName: String = "moliyaai",
) {

    private val apiUrl: String
        get() = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(apiUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "MoliyaAI-App")
            }

            if (conn.responseCode == 404) {
                return@withContext UpdateCheckResult.Error(
                    "Reliz topilmadi (404). Repozitoriyni GitHub'da 'Public' qiling yoki Releases bölimida APK mavjudligini tekşiring.",
                )
            } else if (conn.responseCode == 403) {
                return@withContext UpdateCheckResult.Error("GitHub API limiti tugadi (403). Birozdan keyin urinib köring.")
            } else if (conn.responseCode != 200) {
                return@withContext UpdateCheckResult.Error("GitHub server xatosi (HTTP ${conn.responseCode})")
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "").trim()
            val releaseTitle = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "").trim()

            // Find APK asset in release
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            var apkSize = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isNullOrEmpty()) {
                return@withContext UpdateCheckResult.Error("GitHub relizida APK fayl topilmadi")
            }

            val remoteVersion = tagName.removePrefix("v").trim()
            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
            } catch (e: Exception) {
                "1.0"
            }.removePrefix("v").trim()

            if (isNewerVersion(remoteVersion, currentVersion)) {
                UpdateCheckResult.UpdateAvailable(
                    ReleaseInfo(
                        tagName = tagName,
                        versionName = remoteVersion,
                        title = releaseTitle,
                        changelog = releaseNotes,
                        downloadUrl = apkUrl,
                        apkSize = apkSize,
                    ),
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Network error")
        }
    }

    fun cleanupCachedApks() {
        runCatching {
            val cacheDir = File(context.cacheDir, "updates")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".apk", ignoreCase = true)) {
                        file.delete()
                    }
                }
            }
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val initialUrl = URL(downloadUrl)
            if (!initialUrl.protocol.equals("https", ignoreCase = true)) {
                return@withContext null
            }

            val cacheDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val destination = File(cacheDir, "moliyaai-update.apk")
            if (destination.exists()) destination.delete()

            var connection = (initialUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MoliyaAI-App")
            }

            // Handle HTTP redirect (GitHub Releases redirect to AWS S3 / objects.githubusercontent.com)
            var redirect = false
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER
            ) {
                redirect = true
            }

            if (redirect) {
                val newLocation = connection.getHeaderField("Location") ?: return@withContext null
                val redirectedUrl = URL(newLocation)
                if (!redirectedUrl.protocol.equals("https", ignoreCase = true)) {
                    return@withContext null
                }
                connection = (redirectedUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "MoliyaAI-App")
                }
            }

            val totalBytes = connection.contentLength.takeIf { it > 0 } ?: 15_000_000L
            var downloadedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        onProgress(progress)
                    }
                    output.flush()
                }
            }

            if (!verifyDownloadedApkIntegrity(destination)) {
                destination.delete()
                return@withContext null
            }

            destination
        } catch (e: Exception) {
            null
        }
    }

    private fun verifyDownloadedApkIntegrity(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() < 100_000L) return false
        return try {
            val pm = context.packageManager
            val flags = android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
            if (archiveInfo.packageName != context.packageName) {
                return false
            }
            val installedInfo = pm.getPackageInfo(context.packageName, flags)
            val archiveSigners = archiveInfo.signingInfo?.apkContentsSigners
            val installedSigners = installedInfo.signingInfo?.apkContentsSigners
            if (!archiveSigners.isNullOrEmpty() && !installedSigners.isNullOrEmpty()) {
                val installedSet = installedSigners.map { it.toCharsString() }.toSet()
                archiveSigners.any { it.toCharsString() in installedSet }
            } else {
                true
            }
        } catch (_: Exception) {
            true
        }
    }

    fun installApk(apkFile: File) {
        if (!apkFile.exists()) return

        // Check unknown sources permission
        if (!context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
        }

        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(installIntent)
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
