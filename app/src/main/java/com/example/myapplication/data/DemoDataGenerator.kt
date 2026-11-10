package com.example.myapplication.data

import com.example.myapplication.data.db.TransactionEntity
import com.example.myapplication.data.model.Account
import com.example.myapplication.data.model.Category
import java.time.LocalDate
import kotlin.random.Random

/** Generates ~6 months of realistic sample data so forecasts can be tried immediately. */
object DemoDataGenerator {

    fun generate(today: LocalDate = LocalDate.now(), months: Int = 6, seed: Int = 42): List<TransactionEntity> {
        val random = Random(seed)
        val start = today.minusMonths(months.toLong()).withDayOfMonth(1)
        val result = mutableListOf<TransactionEntity>()
        val totalDays = today.toEpochDay() - start.toEpochDay()

        fun add(day: LocalDate, category: Category, amount: Long, account: Account = Account.CARD, note: String = "") {
            if (day.isAfter(today)) return
            result += TransactionEntity(
                amount = (amount / 1000).coerceAtLeast(1) * 1000,
                type = category.type,
                category = category,
                account = account,
                epochDay = day.toEpochDay(),
                note = note,
            )
        }

        var day = start
        while (!day.isAfter(today)) {
            val progress = (day.toEpochDay() - start.toEpochDay()).toDouble() / totalDays.coerceAtLeast(1)

            when (day.dayOfMonth) {
                5 -> add(day, Category.SALARY, 7_000_000, Account.CARD)
                1 -> add(day, Category.RENT, 2_500_000, Account.CARD)
                10 -> add(day, Category.UTILITIES, random.nextLong(350_000, 650_000), Account.CARD)
                15 -> if (random.nextInt(3) == 0) add(day, Category.EDUCATION, 400_000, Account.CARD)
                20 -> add(day, Category.LOANS, 800_000, Account.DEBT)
            }
            if (random.nextInt(10) == 0) add(day, Category.FREELANCE, random.nextLong(500_000, 2_000_000), Account.CARD)

            // Food: mix of card and cash
            if (random.nextInt(3) != 0) {
                val acc = if (random.nextBoolean()) Account.CARD else Account.CASH
                add(day, Category.FOOD, (random.nextLong(40_000, 160_000) * (1 + 0.35 * progress)).toLong(), acc)
            }
            if (random.nextInt(4) != 0) add(day, Category.TRANSPORT, random.nextLong(8_000, 40_000), Account.CASH)
            if (random.nextInt(7) == 0) add(day, Category.ENTERTAINMENT, random.nextLong(80_000, 300_000), Account.CARD)
            if (random.nextInt(9) == 0) add(day, Category.SHOPPING, random.nextLong(150_000, 700_000), Account.CARD)
            if (random.nextInt(20) == 0) add(day, Category.HEALTH, random.nextLong(60_000, 400_000), Account.CARD)

            day = day.plusDays(1)
        }
        return result
    }
}
