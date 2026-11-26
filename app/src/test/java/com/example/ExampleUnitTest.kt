package com.example

import com.example.data.local.CustomerWithSummary
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.TransactionWithCustomer
import com.example.data.repository.TallyRepository
import com.example.ui.HistoryFilter
import com.example.ui.TallyViewModel
import com.example.util.BengaliFormatter
import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun `bengali formatter formats taka, seconds, and parses both digit scripts`() {
    assertEquals("৳ ১,২৫,০০০", BengaliFormatter.formatTaka(125000.0))
    assertEquals("৳ ৪৫০.৫০", BengaliFormatter.formatTaka(450.5))
    assertEquals(1500.0, BengaliFormatter.parseAmountInput("১৫০০")!!, 0.01)
    assertEquals(750.25, BengaliFormatter.parseAmountInput("750.25")!!, 0.01)
    assertNull(BengaliFormatter.parseAmountInput("0"))

    val formattedWithSec = BengaliFormatter.formatBengaliDateTimeWithSeconds(1764100000000L)
    assertTrue(formattedWithSec.contains(":"))
  }

  @Test
  fun `whatsapp receipt explicitly includes remaining due after payment and running balances`() {
    assertEquals("8801712345678", WhatsAppHelper.normalizePhoneForWhatsApp("01712-345678"))
    assertEquals("8801812345678", WhatsAppHelper.normalizePhoneForWhatsApp("+8801812345678"))
    assertEquals("8801912345678", WhatsAppHelper.normalizePhoneForWhatsApp("০১৯১২৩৪৫৬৭৮"))

    val now = 1764100000000L
    val customer =
      CustomerWithSummary(
        id = 1L,
        name = "করিম মিয়া",
        phone = "01712345678",
        address = "বাজার রোড",
        createdAt = now - 86400000L,
        totalBaki = 1200.0,
        totalJoma = 400.0,
        transactionCount = 2,
        lastTransactionAt = now,
      )
    val receipt =
      WhatsAppHelper.buildCustomerReceiptMessage(
        shopName = "মায়ের দোয়া স্টোর",
        customer = customer,
        transactions =
          listOf(
            TransactionEntity(1L, 1L, TransactionType.BAKI.name, 1200.0, "চাল", now - 3600000L),
            TransactionEntity(2L, 1L, TransactionType.JOMA.name, 400.0, "নগদ", now),
          ),
        nowMillis = now,
      )
    assertTrue(receipt.contains("মায়ের দোয়া স্টোর"))
    assertTrue(receipt.contains("করিম মিয়া"))
    assertTrue(receipt.contains("সর্বশেষ জমা"))
    assertTrue(receipt.contains("টাকা জমা দেওয়ার পর অবশিষ্ট বকেয়া: ৳ ৮০০"))
    assertTrue(receipt.contains("→ বকেয়া: ৳ ৮০০"))

    val summary =
      TallyRepository.computeBusinessSummary(
        customers = listOf(customer),
        transactions =
          listOf(
            TransactionWithCustomer(
              id = 1L,
              customerId = 1L,
              customerName = "করিম মিয়া",
              customerPhone = "01712345678",
              type = TransactionType.BAKI.name,
              amount = 1200.0,
              note = "চাল",
              timestamp = now,
            ),
            TransactionWithCustomer(
              id = 2L,
              customerId = 1L,
              customerName = "করিম মিয়া",
              customerPhone = "01712345678",
              type = TransactionType.JOMA.name,
              amount = 400.0,
              note = "নগদ",
              timestamp = now,
            ),
          ),
        nowMillis = now,
      )
    assertEquals(800.0, summary.netReceivable, 0.01)
    assertEquals(1200.0, summary.todayBaki, 0.01)
    assertEquals(400.0, summary.todayJoma, 0.01)

    val shopMsg =
      WhatsAppHelper.buildShopSummaryMessage("মায়ের দোয়া স্টোর", summary, listOf(customer), now)
    assertTrue(shopMsg.contains("মায়ের দোয়া স্টোর"))
    assertTrue(shopMsg.contains("৳ ৮০০"))
    assertTrue(shopMsg.contains("করিম মিয়া"))
  }

  @Test
  fun `history filter supports customer name search and date range`() {
    val day1 = 1763900000000L
    val day2 = day1 + 2 * 86400000L
    val day3 = day1 + 5 * 86400000L

    val list =
      listOf(
        TransactionWithCustomer(1L, 1L, "করিম মিয়া", "01711", TransactionType.BAKI.name, 500.0, "চাল", day1),
        TransactionWithCustomer(2L, 2L, "রহিম শেখ", "01811", TransactionType.JOMA.name, 300.0, "নগদ", day2),
        TransactionWithCustomer(3L, 1L, "করিম মিয়া", "01711", TransactionType.JOMA.name, 200.0, "বিকাশ", day3),
      )

    val byCustomer =
      TallyViewModel.filterTransactions(
        transactions = list,
        filter = HistoryFilter.ALL,
        searchQuery = "করিম",
      )
    assertEquals(2, byCustomer.size)

    val byDateRange =
      TallyViewModel.filterTransactions(
        transactions = list,
        filter = HistoryFilter.ALL,
        startDateMillis = BengaliFormatter.startOfDayMillis(day2),
        endDateMillis = BengaliFormatter.endOfDayMillis(day3),
      )
    assertEquals(2, byDateRange.size)

    val byCustomerAndDate =
      TallyViewModel.filterTransactions(
        transactions = list,
        filter = HistoryFilter.ALL,
        searchQuery = "করিম",
        startDateMillis = BengaliFormatter.startOfDayMillis(day2),
        endDateMillis = BengaliFormatter.endOfDayMillis(day3),
      )
    assertEquals(1, byCustomerAndDate.size)
    assertEquals(3L, byCustomerAndDate.first().id)
  }
}
