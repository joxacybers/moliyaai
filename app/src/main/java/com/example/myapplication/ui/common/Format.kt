package com.example.myapplication.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
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
