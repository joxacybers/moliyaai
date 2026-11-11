package com.example.myapplication.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.myapplication.R
import com.example.myapplication.data.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

import androidx.compose.ui.text.intl.Locale as ComposeLocale

/** 1250000 -> "1 250 000" (non-breaking spaces). */
fun formatAmount(amount: Long): String =
    String.format(Locale.US, "%,d", amount).replace(',', '\u00A0')

@Composable
@ReadOnlyComposable
fun money(amount: Long): String = stringResource(R.string.money_format, formatAmount(amount))

@Composable
@ReadOnlyComposable
fun signedMoney(amount: Long, type: TransactionType): String {
    val sign = if (type == TransactionType.INCOME) "+" else "−"
    return sign + money(abs(amount))
}

fun formatPercent(value: Double): String {
    val rounded = value.roundToInt()
    return when {
        rounded > 0 -> "+$rounded%"
        rounded < 0 -> "−${abs(rounded)}%"
        else -> "0%"
    }
}

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale {
    val composeLocales = ComposeLocale.current
    return Locale.forLanguageTag(composeLocales.toLanguageTag())
}

@Composable
@ReadOnlyComposable
fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale()))

@Composable
@ReadOnlyComposable
fun formatShortMonth(date: LocalDate): String =
    date.month.getDisplayName(TextStyle.SHORT_STANDALONE, currentLocale())

fun formatDayMonth(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd.MM"))

@Composable
@ReadOnlyComposable
fun formatFullMonthYear(date: LocalDate): String {
    val monthName = date.month.getDisplayName(TextStyle.FULL_STANDALONE, currentLocale())
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(currentLocale()) else it.toString() }
    return "$monthName ${date.year}"
}

@Composable
@ReadOnlyComposable
fun formatRelativeDay(date: LocalDate): String {
    val today = LocalDate.now()
    val locale = currentLocale()
    val monthShort = date.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
    return when (date) {
        today -> "${stringResource(R.string.today)}, ${date.dayOfMonth}-$monthShort"
        today.minusDays(1) -> "${stringResource(R.string.yesterday)}, ${date.dayOfMonth}-$monthShort"
        else -> formatDate(date)
    }
}

/**
 * Automatically groups digits into groups of 3 separated by spaces
 * (e.g. "1250000" visually displays as "1 250 000") without altering the raw numeric value.
 */
class ThousandsSeparatorVisualTransformation(
    private val delimiter: Char = ' ',
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val out = StringBuilder()
        val originalLength = originalText.length
        for (i in 0 until originalLength) {
            out.append(originalText[i])
            val remaining = originalLength - (i + 1)
            if (remaining > 0 && remaining % 3 == 0) {
                out.append(delimiter)
            }
        }

        val formattedText = out.toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val safeOffset = offset.coerceAtMost(originalLength)
                var spacesBefore = 0
                for (k in 0 until safeOffset) {
                    val remaining = originalLength - (k + 1)
                    if (remaining > 0 && remaining % 3 == 0) {
                        spacesBefore++
                    }
                }
                return (safeOffset + spacesBefore).coerceAtMost(formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val safeOffset = offset.coerceAtMost(formattedText.length)
                var originalOffset = 0
                for (i in 0 until safeOffset) {
                    if (formattedText[i] != delimiter) {
                        originalOffset++
                    }
                }
                return originalOffset.coerceAtMost(originalLength)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
