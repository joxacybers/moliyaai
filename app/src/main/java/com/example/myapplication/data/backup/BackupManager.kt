package com.example.myapplication.data.backup

import android.content.Context
import android.net.Uri
import com.example.myapplication.data.SettingsRepository
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.db.date
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class BackupFile(
    val app: String = "moliya-ai",
    val version: Int = BackupManager.CURRENT_VERSION,
    val exportedAt: Long,
    val monthlyBudget: Long = 0,
    val transactions: List<BackupTransaction>,
)

@Serializable
data class BackupTransaction(
    val date: String,
    val type: TransactionType,
    val category: Category,
    val amount: Long,
    val account: Account = Account.CARD,
    val note: String = "",
    val createdAt: Long = 0,
)

/** JSON backup/restore and CSV export through the Storage Access Framework (no permissions needed). */
class BackupManager(
    context: Context,
    private val transactions: TransactionRepository,
    private val settings: SettingsRepository,
) {
    private val resolver = context.contentResolver
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    /** @return number of exported transactions. */
    suspend fun exportJson(uri: Uri): Int = withContext(Dispatchers.IO) {
        val all = transactions.getAll()
        val file = BackupFile(
            exportedAt = System.currentTimeMillis(),
            monthlyBudget = settings.monthlyBudget.first(),
            transactions = all.map { it.toBackup() },
        )
        write(uri, json.encodeToString(BackupFile.serializer(), file).toByteArray())
        all.size
    }

    /** Replaces all current data with the backup content. @return number of restored transactions. */
    suspend fun importJson(uri: Uri): Int = withContext(Dispatchers.IO) {
        val text = resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Cannot open $uri")
        val file = json.decodeFromString(BackupFile.serializer(), text)
        require(file.version <= CURRENT_VERSION) { "Unsupported backup version ${file.version}" }
        val entities = file.transactions.map { it.toEntity() }
        transactions.replaceAll(entities)
        settings.setMonthlyBudget(file.monthlyBudget)
        settings.clearAdvice()
        entities.size
    }

    /** @return number of exported rows. */
    suspend fun exportCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val all = transactions.getAll().sortedBy { it.epochDay }
        val csv = buildString {
            append('\uFEFF') // BOM so Excel opens UTF-8 correctly
            append("date,type,category,account,amount,note\n")
            all.forEach { t ->
                append(t.date).append(',')
                append(t.type.name).append(',')
                append(t.category.name).append(',')
                append(t.account.name).append(',')
                append(t.amount).append(',')
                append(t.note.csvEscape()).append('\n')
            }
        }
        write(uri, csv.toByteArray())
        all.size
    }

    private fun write(uri: Uri, bytes: ByteArray) {
        resolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: error("Cannot open $uri")
    }

    private fun TransactionEntity.toBackup() = BackupTransaction(
        date = date.toString(),
        type = type,
        category = category,
        amount = amount,
        account = account,
        note = note,
        createdAt = createdAt,
    )

    private fun BackupTransaction.toEntity(): TransactionEntity {
        require(amount > 0) { "Invalid amount" }
        require(category.type == type) { "Category $category does not match $type" }
        return TransactionEntity(
            amount = amount,
            type = type,
            category = category,
            account = account,
            epochDay = LocalDate.parse(date).toEpochDay(),
            note = note,
            createdAt = createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
        )
    }

    private fun String.csvEscape(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + replace("\"", "\"\"") + "\""
        } else {
            this
        }

    companion object {
        const val CURRENT_VERSION = 2
    }
}
