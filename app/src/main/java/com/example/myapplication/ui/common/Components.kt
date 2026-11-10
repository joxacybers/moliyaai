package com.example.myapplication.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.db.date
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import com.example.myapplication.domain.CategoryAmount
import com.example.myapplication.domain.PeriodSummary
import com.example.myapplication.domain.PeriodType
import java.util.Locale

@Composable
fun incomeColor(): Color = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)

@Composable
fun expenseColor(): Color = if (isSystemInDarkTheme()) Color(0xFFE57373) else Color(0xFFC62828)

fun formatSharePercent(share: Float): String {
    val pct = share * 100f
    return when {
        pct <= 0f -> "0%"
        pct < 1f -> String.format(Locale.US, "%.1f%%", pct)
        pct % 1f < 0.1f || pct % 1f > 0.9f -> "${ kotlin.math.round(pct).toInt() }%"
        else -> String.format(Locale.US, "%.1f%%", pct)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
fun CategoryBadge(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(category.containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(category.iconRes),
            contentDescription = stringResource(category.labelRes),
            tint = category.tintColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun AccountBadge(
    account: Account,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    iconSize: Dp = 14.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(account.containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(account.iconRes),
            contentDescription = stringResource(account.labelRes),
            tint = account.tintColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(transaction.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(transaction.category.labelRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(transaction.account.iconRes),
                    contentDescription = null,
                    tint = transaction.account.tintColor,
                    modifier = Modifier.size(13.dp),
                )
                val secondary = listOfNotNull(
                    stringResource(transaction.account.labelRes),
                    if (showDate) formatDate(transaction.date) else null,
                    transaction.note.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = signedMoney(transaction.amount, transaction.type),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (transaction.type == TransactionType.INCOME) incomeColor() else expenseColor(),
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Donut chart representing category shares in percentages, with total amount in the center
 * and a compact legend on the right.
 */
@Composable
fun CategoryDonutChart(
    items: List<CategoryAmount>,
    totalAmount: Long,
    totalLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokeWidth = 28.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                if (items.isEmpty()) return@Canvas

                val gapAngle = if (items.size > 1) 2.5f else 0f
                val totalGap = gapAngle * items.size
                val availableSweep = (360f - totalGap).coerceAtLeast(1f)

                var startAngle = -90f
                items.forEach { item ->
                    val sweep = (item.share * availableSweep).coerceAtLeast(2f)
                    drawArc(
                        color = item.category.tintColor,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    )
                    startAngle += sweep + gapAngle
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                Text(
                    text = totalLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = money(totalAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "100%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Horizontal stacked percentage bar for quick visual comparison
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(item.share.coerceAtLeast(0.02f))
                        .height(10.dp)
                        .background(item.category.tintColor),
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownList(items: List<CategoryAmount>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            val pctText = formatSharePercent(item.share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryBadge(
                    category = item.category,
                    size = 40.dp,
                    iconSize = 20.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            Text(
                                text = stringResource(item.category.labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(item.category.containerColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = pctText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = item.category.tintColor,
                                    maxLines = 1,
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = money(item.amount),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { item.share },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = item.category.tintColor,
                        trackColor = item.category.containerColor,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}

@Composable
fun ChartLegend(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(incomeColor(), stringResource(R.string.income))
        LegendItem(expenseColor(), stringResource(R.string.expense))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/** Grouped bar chart: income and expense per period. */
@Composable
fun IncomeExpenseBarChart(
    summaries: List<PeriodSummary>,
    periodType: PeriodType,
    modifier: Modifier = Modifier,
) {
    val income = incomeColor()
    val expense = expenseColor()
    val grid = MaterialTheme.colorScheme.outlineVariant
    val maxValue = (summaries.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0L).coerceAtLeast(1L)

    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            if (summaries.isEmpty()) return@Canvas
            drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            val slot = size.width / summaries.size
            val barWidth = slot * 0.3f
            val radius = CornerRadius(4.dp.toPx())
            summaries.forEachIndexed { index, summary ->
                val x = index * slot + slot * 0.15f
                val incomeHeight = size.height * summary.income / maxValue
                val expenseHeight = size.height * summary.expense / maxValue
                drawRoundRect(
                    color = income,
                    topLeft = Offset(x, size.height - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = expense,
                    topLeft = Offset(x + barWidth + slot * 0.1f, size.height - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = radius,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            summaries.forEach { summary ->
                Text(
                    text = if (periodType == PeriodType.WEEK) formatDayMonth(summary.start) else formatShortMonth(summary.start),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(24.dp),
    )
}
