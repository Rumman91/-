package com.example.data.backup

import com.example.data.local.CustomerEntity
import com.example.data.local.ShopProfile
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BackupPayload(
  val schemaVersion: Int,
  val exportedAt: Long,
  val shopName: String,
  val ownerName: String,
  val customers: List<CustomerEntity>,
  val transactions: List<TransactionEntity>,
)

object BackupCodec {

  private const val CURRENT_SCHEMA_VERSION = 1

  fun encode(
    shopProfile: ShopProfile,
    customers: List<CustomerEntity>,
    transactions: List<TransactionEntity>,
    exportedAt: Long = System.currentTimeMillis(),
  ): String {
    val root = JSONObject()
    root.put("app", "TallyKhata")
    root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
    root.put("exportedAt", exportedAt)
    root.put("shopName", shopProfile.shopName)
    root.put("ownerName", shopProfile.ownerName)

    val customersJson = JSONArray()
    customers.forEach { customer ->
      customersJson.put(
        JSONObject().apply {
          put("id", customer.id)
          put("name", customer.name)
          put("phone", customer.phone)
          put("address", customer.address)
          put("createdAt", customer.createdAt)
        }
      )
    }
    root.put("customers", customersJson)

    val transactionsJson = JSONArray()
    transactions.forEach { tx ->
      transactionsJson.put(
        JSONObject().apply {
          put("id", tx.id)
          put("customerId", tx.customerId)
          put("type", tx.type)
          put("amount", tx.amount)
          put("note", tx.note)
          put("timestamp", tx.timestamp)
        }
      )
    }
    root.put("transactions", transactionsJson)

    return root.toString(2)
  }

  fun decode(rawJson: String): Result<BackupPayload> {
    if (rawJson.isBlank()) {
      return Result.failure(IllegalArgumentException("ফাইলটি খালি। একটি সঠিক টালি খাতা JSON ব্যাকআপ ফাইল নির্বাচন করুন।"))
    }
    return try {
      val root = JSONObject(rawJson)
      if (!root.has("customers") || !root.has("transactions")) {
        return Result.failure(
          IllegalArgumentException("এটি সঠিক টালি খাতা ব্যাকআপ ফাইল নয় (customers বা transactions পাওয়া যায়নি)।")
        )
      }

      val schemaVersion = root.optInt("schemaVersion", 1)
      val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
      val shopName = root.optString("shopName", "আমার দোকান").ifBlank { "আমার দোকান" }
      val ownerName = root.optString("ownerName", "")

      val customersArray = root.getJSONArray("customers")
      val customers = mutableListOf<CustomerEntity>()
      val customerIds = mutableSetOf<Long>()

      for (i in 0 until customersArray.length()) {
        val obj = customersArray.getJSONObject(i)
        val id = obj.optLong("id", (i + 1).toLong())
        val name = obj.optString("name", "").trim()
        if (id > 0L && name.isNotEmpty() && customerIds.add(id)) {
          customers.add(
            CustomerEntity(
              id = id,
              name = name,
              phone = obj.optString("phone", "").trim(),
              address = obj.optString("address", "").trim(),
              createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            )
          )
        }
      }

      val transactionsArray = root.getJSONArray("transactions")
      val transactions = mutableListOf<TransactionEntity>()
      val txIds = mutableSetOf<Long>()
      var fallbackTxId = 1L

      for (i in 0 until transactionsArray.length()) {
        val obj = transactionsArray.getJSONObject(i)
        val customerId = obj.optLong("customerId", -1L)
        if (!customerIds.contains(customerId)) continue

        val rawType = obj.optString("type", "").uppercase()
        val validType =
          when (rawType) {
            TransactionType.BAKI.name,
            "DUE" -> TransactionType.BAKI.name
            TransactionType.JOMA.name,
            "PAID",
            "PAYMENT" -> TransactionType.JOMA.name
            else -> continue
          }

        val amount = obj.optDouble("amount", 0.0)
        if (!amount.isFinite() || amount <= 0.0) continue

        var id = obj.optLong("id", 0L)
        if (id <= 0L || !txIds.add(id)) {
          while (!txIds.add(fallbackTxId)) {
            fallbackTxId++
          }
          id = fallbackTxId
        }

        transactions.add(
          TransactionEntity(
            id = id,
            customerId = customerId,
            type = validType,
            amount = amount,
            note = obj.optString("note", "").trim(),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
          )
        )
      }

      Result.success(
        BackupPayload(
          schemaVersion = schemaVersion,
          exportedAt = exportedAt,
          shopName = shopName,
          ownerName = ownerName,
          customers = customers,
          transactions = transactions,
        )
      )
    } catch (e: JSONException) {
      Result.failure(IllegalArgumentException("JSON ফাইলটি পড়া যায়নি। ফাইলটি সঠিক ফরম্যাটে নেই।", e))
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("ব্যাকআপ ফাইল রিস্টোর করা যায়নি: ${e.message ?: "অজানা ত্রুটি"}", e))
    }
  }
}
