package com.example.myapplication.ui.notifications

import android.content.Context
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ai.AiAdvisor
import com.example.myapplication.data.BudgetRepository
import com.example.myapplication.data.NotificationRepository
import com.example.myapplication.data.SavingsRepository
import com.example.myapplication.data.SettingsRepository
import com.example.myapplication.data.TransactionRepository
import com.example.myapplication.data.db.AppNotificationEntity
import com.example.myapplication.data.db.AppNotificationType
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.db.date
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.CustomCategory
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.notification.AppNotificationHelper
import com.example.myapplication.sms.SmsParser
import com.example.myapplication.ui.common.formatAmount
import com.example.myapplication.ui.common.labelRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class NotificationFilter {
    ALL,
    SMS_DRAFTS,
}

data class NotificationsUiState(
    val notifications: List<AppNotificationEntity> = emptyList(),
    val unreadCount: Int = 0,
    val pendingDraftsCount: Int = 0,
    val filter: NotificationFilter = NotificationFilter.ALL,
    val customCategories: List<CustomCategory> = emptyList(),
    val isScanningSms: Boolean = false,
)

class NotificationsViewModel(
    private val notificationRepo: NotificationRepository,
    private val transactionRepo: TransactionRepository,
    private val budgetRepo: BudgetRepository,
    private val savingsRepo: SavingsRepository,
    private val settingsRepo: SettingsRepository,
    private val aiAdvisor: AiAdvisor,
    private val appContext: Context,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(NotificationFilter.ALL)
    private val isScanningFlow = MutableStateFlow(false)

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationRepo.observeAll(),
        filterFlow,
        settingsRepo.customCategories,
        isScanningFlow,
    ) { allNotifications, filter, customCategories, isScanning ->
        val filtered = when (filter) {
            NotificationFilter.ALL -> allNotifications
            NotificationFilter.SMS_DRAFTS -> allNotifications.filter { it.type == AppNotificationType.SMS_DRAFT }
        }
        val unreadCount = allNotifications.count { !it.isRead }
        val pendingDraftsCount = allNotifications.count {
            it.type == AppNotificationType.SMS_DRAFT && !it.isActionCompleted
        }
        NotificationsUiState(
            notifications = filtered,
            unreadCount = unreadCount,
            pendingDraftsCount = pendingDraftsCount,
            filter = filter,
            customCategories = customCategories,
            isScanningSms = isScanning,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationsUiState(),
    )

    fun setFilter(filter: NotificationFilter) {
        filterFlow.value = filter
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            notificationRepo.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepo.markAllAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            notificationRepo.deleteById(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            notificationRepo.deleteAll()
            _events.emit("Barça bildirişnomalar tozalandi")
        }
    }

    fun confirmSmsDraft(
        notification: AppNotificationEntity,
        overrideAmount: Long? = null,
        overrideType: TransactionType? = null,
        overrideCategory: Category? = null,
        overrideCustomCategory: CustomCategory? = null,
        clearCustomCategory: Boolean = false,
        overrideAccount: Account? = null,
        overrideNote: String? = null,
    ) {
        viewModelScope.launch {
            val amount = overrideAmount ?: notification.draftAmount
            if (amount <= 0L) return@launch

            val type = overrideType ?: notification.draftTransactionType
            val category = overrideCategory ?: notification.draftCategory
            val account = overrideAccount ?: notification.draftAccount
            val note = (overrideNote ?: notification.draftMerchantOrNote).trim().ifBlank { "SMS amal" }

            val customName = when {
                clearCustomCategory -> ""
                overrideCustomCategory != null -> overrideCustomCategory.name
                else -> notification.draftCustomCategoryName
            }
            val customIcon = when {
                clearCustomCategory -> ""
                overrideCustomCategory != null -> overrideCustomCategory.iconKey
                else -> notification.draftCustomCategoryIcon
            }
            val customColor = when {
                clearCustomCategory -> 0L
                overrideCustomCategory != null -> overrideCustomCategory.colorHex
                else -> notification.draftCustomCategoryColor
            }

            val txDate = Instant.ofEpochMilli(notification.createdAtEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val tx = TransactionEntity(
                amount = amount,
                type = type,
                category = category,
                account = account,
                epochDay = txDate.toEpochDay(),
                note = note,
                customCategoryName = customName,
                customCategoryIcon = customIcon,
                customCategoryColor = customColor,
            )

            transactionRepo.save(tx)

            // Update draft record & mark completed
            notificationRepo.update(
                notification.copy(
                    isRead = true,
                    isActionCompleted = true,
                    draftAmount = amount,
                    draftTransactionType = type,
                    draftCategory = category,
                    draftCustomCategoryName = customName,
                    draftCustomCategoryIcon = customIcon,
                    draftCustomCategoryColor = customColor,
                    draftAccount = account,
                    draftMerchantOrNote = note,
                ),
            )

            // Check budget alert if expense
            if (type == TransactionType.EXPENSE) {
                try {
                    val budget = budgetRepo.getForCategory(category)
                    if (budget != null && budget.limitAmount > 0) {
                        val allTx = transactionRepo.getAll()
                        val now = LocalDate.now()
                        val monthSpent = allTx.filter {
                            it.type == TransactionType.EXPENSE &&
                                it.category == category &&
                                it.date.year == now.year &&
                                it.date.monthValue == now.monthValue
                        }.sumOf { it.amount }

                        val percent = (monthSpent * 100 / budget.limitAmount).toInt()
                        if (percent >= 80) {
                            val catLabel = customName.ifBlank { appContext.getString(category.labelRes) }
                            AppNotificationHelper.showBudgetAlert(
                                context = appContext,
                                categoryName = catLabel,
                                percent = percent,
                                spent = monthSpent,
                                limit = budget.limitAmount,
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            _events.emit("✅ $note (${formatAmount(amount)} söm) amallarga qöşildi!")
        }
    }

    fun updateSmsDraftOnly(
        notification: AppNotificationEntity,
        amount: Long,
        type: TransactionType,
        category: Category,
        customCategory: CustomCategory?,
        account: Account,
        note: String,
    ) {
        viewModelScope.launch {
            val catLabel = customCategory?.name ?: appContext.getString(category.labelRes)
            val title = if (type == TransactionType.INCOME) {
                "💰 Yangi sotuv / tuşum: ${formatAmount(amount)} söm"
            } else {
                "💳 Yangi xarid: ${formatAmount(amount)} söm"
            }
            val message = "$note • Toifa: $catLabel (Qoralama tahrirlandi — tasdiqlaşga tayyor)"

            notificationRepo.update(
                notification.copy(
                    title = title,
                    message = message,
                    isRead = true,
                    draftAmount = amount,
                    draftTransactionType = type,
                    draftCategory = category,
                    draftCustomCategoryName = customCategory?.name.orEmpty(),
                    draftCustomCategoryIcon = customCategory?.iconKey.orEmpty(),
                    draftCustomCategoryColor = customCategory?.colorHex ?: 0L,
                    draftAccount = account,
                    draftMerchantOrNote = note,
                ),
            )
            _events.emit("✏️ Qoralama saqlandi")
        }
    }

    fun confirmEmergencyFundDeposit(notification: AppNotificationEntity) {
        val amount = notification.draftAmount
        if (amount <= 0L) return
        viewModelScope.launch {
            savingsRepo.addEmergencyFunds(amount)
            notificationRepo.markActionCompleted(notification.id)
            _events.emit("🛡️ ${formatAmount(amount)} söm moliyaviy yostiqçaga qöşildi!")
        }
    }

    fun analyzeCustomSmsText(sender: String, smsText: String) {
        if (smsText.isBlank()) return
        viewModelScope.launch {
            isScanningFlow.value = true
            try {
                val customCategories = settingsRepo.customCategories.first()
                val customApiKey = settingsRepo.customOpenAiKey.first()
                val initial = SmsParser.parse(sender.ifBlank { "BANK" }, smsText, customCategories)
                if (initial == null) {
                    _events.emit("⚠️ Uşbu SMS matnida xarid yoki sotuv summasi (UZS/söm) topilmadi")
                    return@launch
                }
                val enriched = aiAdvisor.analyzeSmsForDraft(
                    sender = sender.ifBlank { "BANK" },
                    rawSms = smsText,
                    initialParsed = initial,
                    customCategories = customCategories,
                    customApiKey = customApiKey,
                )
                AppNotificationHelper.saveAndShowSmsDraft(appContext, enriched)
                _events.emit("🤖 AI SMS xabarni tahlil qildi va qoralama sifatida saqladi!")
            } finally {
                isScanningFlow.value = false
            }
        }
    }

    fun scanInboxSmsMessages() {
        viewModelScope.launch {
            isScanningFlow.value = true
            try {
                val existingTexts = notificationRepo.observeAll().first()
                    .mapNotNull { it.rawSmsText.takeIf { raw -> raw.isNotBlank() } }
                    .toSet()
                val customCategories = settingsRepo.customCategories.first()
                val customApiKey = settingsRepo.customOpenAiKey.first()

                val foundCandidates = withContext(Dispatchers.IO) {
                    val list = mutableListOf<Triple<String, String, Long>>()
                    val cutoffMillis = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000 // last 14 days
                    val cursor = appContext.contentResolver.query(
                        Telephony.Sms.Inbox.CONTENT_URI,
                        arrayOf(
                            Telephony.Sms.ADDRESS,
                            Telephony.Sms.BODY,
                            Telephony.Sms.DATE,
                        ),
                        "${Telephony.Sms.DATE} >= ?",
                        arrayOf(cutoffMillis.toString()),
                        "${Telephony.Sms.DATE} DESC",
                    )
                    cursor?.use { c ->
                        val addrIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                        val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                        val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
                        var count = 0
                        while (c.moveToNext() && count < 35) {
                            val sender = if (addrIdx >= 0) c.getString(addrIdx).orEmpty() else ""
                            val body = if (bodyIdx >= 0) c.getString(bodyIdx).orEmpty() else ""
                            val date = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()
                            if (body.isNotBlank() && body.trim() !in existingTexts) {
                                list.add(Triple(sender, body.trim(), date))
                            }
                            count++
                        }
                    }
                    list
                }

                var addedCount = 0
                for ((sender, body, dateMillis) in foundCandidates) {
                    val initial = SmsParser.parse(sender, body, customCategories) ?: continue
                    val enriched = aiAdvisor.analyzeSmsForDraft(
                        sender = sender,
                        rawSms = body,
                        initialParsed = initial,
                        customCategories = customCategories,
                        customApiKey = customApiKey,
                    )
                    val catLabel = enriched.customCategoryName.ifBlank {
                        appContext.getString(enriched.category.labelRes)
                    }
                    val isIncome = enriched.type == TransactionType.INCOME
                    val title = if (isIncome) {
                        "💰 Yangi sotuv / tuşum: ${formatAmount(enriched.amount)} söm"
                    } else {
                        "💳 Yangi xarid: ${formatAmount(enriched.amount)} söm"
                    }
                    val message = "${enriched.merchant} • Toifa: $catLabel (AI qoralama tayyor — tekşirib tasdiqlang)"

                    notificationRepo.insert(
                        AppNotificationEntity(
                            type = AppNotificationType.SMS_DRAFT,
                            title = title,
                            message = message,
                            createdAtEpochMillis = dateMillis,
                            draftAmount = enriched.amount,
                            draftTransactionType = enriched.type,
                            draftCategory = enriched.category,
                            draftCustomCategoryName = enriched.customCategoryName,
                            draftCustomCategoryIcon = enriched.customCategoryIcon,
                            draftCustomCategoryColor = enriched.customCategoryColor,
                            draftAccount = enriched.account,
                            draftMerchantOrNote = enriched.merchant,
                            rawSmsText = enriched.rawText,
                        ),
                    )
                    addedCount++
                    if (addedCount >= 10) break
                }

                if (addedCount > 0) {
                    _events.emit("🤖 $addedCount ta yangi xarid/sotuv SMS topildi va qoralama sifatida saqlandi!")
                } else {
                    _events.emit("Yangi xarid yoki sotuv SMS xabarlari topilmadi")
                }
            } catch (_: SecurityException) {
                _events.emit("SMS xabarlarni öqiş uçun ruxsat kerak")
            } catch (_: Exception) {
                _events.emit("SMS xabarlarni skanerlaşda xatolik yuz berdi")
            } finally {
                isScanningFlow.value = false
            }
        }
    }
}
