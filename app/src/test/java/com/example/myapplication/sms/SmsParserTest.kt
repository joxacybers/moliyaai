package com.example.myapplication.sms

import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    @Test
    fun parseHumoSms_extractsAmountAndMerchantAndFoodCategory() {
        val sms = "HUMO: 55 000.00 UZS to'landi. KORZINKA. Balans: 1 200 000.00 UZS"
        val parsed = SmsParser.parse("HUMO", sms)

        assertNotNull(parsed)
        assertEquals(55000L, parsed!!.amount)
        assertEquals("KORZINKA", parsed.merchant)
        assertEquals(Category.FOOD, parsed.category)
        assertEquals(TransactionType.EXPENSE, parsed.type)
    }

    @Test
    fun parseClickSms_extractsAmountAndFoodCategory() {
        val sms = "Click: To'lov 50000 so'm. Xizmat: Oqtepa Lavash. Balans: 500 000 so'm"
        val parsed = SmsParser.parse("CLICK", sms)

        assertNotNull(parsed)
        assertEquals(50000L, parsed!!.amount)
        assertEquals("Oqtepa Lavash", parsed.merchant)
        assertEquals(Category.FOOD, parsed.category)
    }

    @Test
    fun parseYandexTransportSms_extractsTransportCategory() {
        val sms = "Oplata tovarov i uslug: 22000 UZS, Karta: *1234, Mesto: YANDEX GO, Ostatok: 100 000 UZS"
        val parsed = SmsParser.parse("UZCARD", sms)

        assertNotNull(parsed)
        assertEquals(22000L, parsed!!.amount)
        assertEquals("YANDEX GO", parsed.merchant)
        assertEquals(Category.TRANSPORT, parsed.category)
    }

    @Test
    fun parsePharmacySms_extractsHealthCategory() {
        val sms = "Xarid: 85000 so'm, Karta: *9876, Joy: OXYMED APTEKA"
        val parsed = SmsParser.parse("KAPITALBANK", sms)

        assertNotNull(parsed)
        assertEquals(85000L, parsed!!.amount)
        assertEquals("OXYMED APTEKA", parsed.merchant)
        assertEquals(Category.HEALTH, parsed.category)
    }

    @Test
    fun parseNonFinancialSms_returnsNull() {
        val sms = "Sizning tasdiqlash kodingiz: 123456. Hech kimga bermang."
        val parsed = SmsParser.parse("TELECOM", sms)

        assertNull(parsed)
    }
}
