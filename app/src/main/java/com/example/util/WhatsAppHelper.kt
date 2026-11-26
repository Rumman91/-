package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.CustomerWithSummary
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.repository.BusinessSummary

object WhatsAppHelper {

  fun normalizePhoneForWhatsApp(rawPhone: String): String? {
    val english = BengaliFormatter.toEnglishDigits(rawPhone)
    val digitsOnly = english.filter { it.isDigit() }
    if (digitsOnly.length < 10) return null
    return when {
      digitsOnly.startsWith("880") && digitsOnly.length >= 13 -> digitsOnly
      digitsOnly.startsWith("01") && digitsOnly.length == 11 -> "88$digitsOnly"
      digitsOnly.startsWith("1") && digitsOnly.length == 10 -> "880$digitsOnly"
      else -> digitsOnly
    }
  }

  fun buildShopSummaryMessage(
    shopName: String,
    summary: BusinessSummary,
    dueCustomers: List<CustomerWithSummary>,
    nowMillis: Long = System.currentTimeMillis(),
  ): String {
    val sb = StringBuilder()
    sb.appendLine("📒 *${shopName.ifBlank { "আমার দোকান" }} - টালি খাতা মোট হিসাব*")
    sb.appendLine("📅 তারিখ: ${BengaliFormatter.formatBengaliDateTime(nowMillis)}")
    sb.appendLine("━━━━━━━━━━━━━━━━━━")
    sb.appendLine("🔴 মোট বাকি দিয়েছি: ${BengaliFormatter.formatTaka(summary.totalBaki)}")
    sb.appendLine("🟢 মোট জমা পেয়েছি: ${BengaliFormatter.formatTaka(summary.totalJoma)}")
    sb.appendLine("💰 *মোট বকেয়া পাওনা: ${BengaliFormatter.formatTaka(summary.netReceivable)}*")
    if (summary.totalAdvance > 0.0) {
      sb.appendLine("🔵 কাস্টমারের অগ্রিম জমা: ${BengaliFormatter.formatTaka(summary.totalAdvance)}")
    }
    sb.appendLine()
    sb.appendLine("📆 *আজকের লেনদেন:*")
    sb.appendLine("• আজকের বাকি: ${BengaliFormatter.formatTaka(summary.todayBaki)}")
    sb.appendLine("• আজকের জমা: ${BengaliFormatter.formatTaka(summary.todayJoma)}")
    sb.appendLine()
    sb.appendLine(
      "👥 মোট কাস্টমার: ${BengaliFormatter.formatInt(summary.customerCount)} জন (বাকি আছে: ${BengaliFormatter.formatInt(summary.dueCustomerCount)} জন)"
    )

    val topDue = dueCustomers.filter { it.netDue > 0.0 }.sortedByDescending { it.netDue }.take(10)
    if (topDue.isNotEmpty()) {
      sb.appendLine()
      sb.appendLine("📋 *বকেয়া কাস্টমার তালিকা:*")
      topDue.forEachIndexed { index, customer ->
        sb.appendLine(
          "${BengaliFormatter.formatInt(index + 1)}. ${customer.name} — ${BengaliFormatter.formatTaka(customer.netDue)}"
        )
      }
      if (dueCustomers.count { it.netDue > 0.0 } > topDue.size) {
        val remaining = dueCustomers.count { it.netDue > 0.0 } - topDue.size
        sb.appendLine("...এবং আরও ${BengaliFormatter.formatInt(remaining)} জন।")
      }
    }
    sb.appendLine("━━━━━━━━━━━━━━━━━━")
    sb.appendLine("_টালি খাতা অ্যাপ থেকে পাঠানো_")
    return sb.toString().trim()
  }

  fun buildCustomerReceiptMessage(
    shopName: String,
    customer: CustomerWithSummary,
    transactions: List<TransactionEntity>,
    nowMillis: Long = System.currentTimeMillis(),
  ): String {
    val sb = StringBuilder()
    sb.appendLine("🧾 *বকেয়া হিসাবের রসিদ*")
    sb.appendLine("🏪 দোকান: *${shopName.ifBlank { "আমার দোকান" }}*")
    sb.appendLine("👤 কাস্টমার: *${customer.name}*")
    if (customer.phone.isNotBlank()) {
      sb.appendLine("📞 মোবাইল: ${customer.phone}")
    }
    sb.appendLine("📅 রসিদের তারিখ: ${BengaliFormatter.formatBengaliDateTime(nowMillis)}")
    sb.appendLine("━━━━━━━━━━━━━━━━━━")
    sb.appendLine("🔴 মোট বাকি: ${BengaliFormatter.formatTaka(customer.totalBaki)}")
    sb.appendLine("🟢 মোট জমা: ${BengaliFormatter.formatTaka(customer.totalJoma)}")

    val chronological = transactions.sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.id })
    var runningBalance = 0.0
    val txWithRunningBalance =
      chronological.map { tx ->
        if (tx.type == TransactionType.BAKI.name) {
          runningBalance += tx.amount
        } else {
          runningBalance -= tx.amount
        }
        tx to runningBalance
      }

    val latestJoma = chronological.lastOrNull { it.type == TransactionType.JOMA.name }
    if (latestJoma != null) {
      sb.appendLine(
        "💵 সর্বশেষ জমা: ${BengaliFormatter.formatTaka(latestJoma.amount)} (${BengaliFormatter.formatBengaliDate(latestJoma.timestamp)})"
      )
    }

    val balanceLine =
      when {
        customer.netDue > 0.01 ->
          if (latestJoma != null) {
            "💰 *টাকা জমা দেওয়ার পর অবশিষ্ট বকেয়া: ${BengaliFormatter.formatTaka(customer.netDue)}*"
          } else {
            "💰 *বর্তমান বকেয়া: ${BengaliFormatter.formatTaka(customer.netDue)}*"
          }
        customer.netDue < -0.01 ->
          "🔵 *জমা দেওয়ার পর অগ্রিম আছে: ${BengaliFormatter.formatTaka(-customer.netDue)}*"
        else -> "✅ *জমা দেওয়ার পর অবশিষ্ট বকেয়া: ৳ ০ (সম্পূর্ণ পরিশোধিত)*"
      }
    sb.appendLine(balanceLine)

    val recent = txWithRunningBalance.asReversed().take(5)
    if (recent.isNotEmpty()) {
      sb.appendLine()
      sb.appendLine("🕒 *সাম্প্রতিক লেনদেন:*")
      recent.forEach { (tx, balanceAfter) ->
        val typeTag = if (tx.type == TransactionType.BAKI.name) "বাকি" else "জমা"
        val dateStr = BengaliFormatter.formatBengaliDateTime(tx.timestamp)
        val noteSuffix = if (tx.note.isNotBlank()) " [${tx.note}]" else ""
        val balanceSuffix =
          when {
            balanceAfter > 0.01 -> " → বকেয়া: ${BengaliFormatter.formatTaka(balanceAfter)}"
            balanceAfter < -0.01 -> " → অগ্রিম: ${BengaliFormatter.formatTaka(-balanceAfter)}"
            else -> " → পরিশোধিত (৳ ০)"
          }
        sb.appendLine(
          "• $dateStr — $typeTag: ${BengaliFormatter.formatTaka(tx.amount)}$noteSuffix$balanceSuffix"
        )
      }
    }
    sb.appendLine("━━━━━━━━━━━━━━━━━━")
    if (customer.netDue > 0.01) {
      sb.appendLine("অনুগ্রহ করে সুবিধামতো অবশিষ্ট বকেয়া ${BengaliFormatter.formatTaka(customer.netDue)} পরিশোধ করবেন। ধন্যবাদ!")
    } else {
      sb.appendLine("আমাদের সাথে থাকার জন্য ধন্যবাদ!")
    }
    return sb.toString().trim()
  }

  fun shareToWhatsApp(context: Context, message: String, rawPhone: String? = null) {
    val normalizedPhone = rawPhone?.let { normalizePhoneForWhatsApp(it) }
    val encodedMessage = Uri.encode(message)
    val waUri =
      if (normalizedPhone != null) {
        Uri.parse("https://wa.me/$normalizedPhone?text=$encodedMessage")
      } else {
        Uri.parse("https://wa.me/?text=$encodedMessage")
      }

    // Try standard WhatsApp and WhatsApp Business explicitly first
    for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
      val directIntent =
        Intent(Intent.ACTION_VIEW, waUri).apply {
          setPackage(pkg)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      if (directIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(directIntent)
        return
      }
    }

    // Try generic ACTION_VIEW for wa.me link (works if browser or another WA handler is present)
    val viewIntent = Intent(Intent.ACTION_VIEW, waUri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    if (viewIntent.resolveActivity(context.packageManager) != null) {
      context.startActivity(viewIntent)
      return
    }

    // Graceful fallback to Android Share Sheet
    val shareIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
      }
    val chooser =
      Intent.createChooser(shareIntent, "হোয়াটসঅ্যাপ বা অন্য মাধ্যমে পাঠান").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }
}
