package com.example.data.repository

import com.example.data.backup.BackupCodec
import com.example.data.backup.BackupPayload
import com.example.data.local.CustomerEntity
import com.example.data.local.CustomerWithSummary
import com.example.data.local.ShopPreferences
import com.example.data.local.ShopProfile
import com.example.data.local.TallyDao
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.TransactionWithCustomer
import com.example.util.BengaliFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class BusinessSummary(
  val totalBaki: Double = 0.0,
  val totalJoma: Double = 0.0,
  val netReceivable: Double = 0.0,
  val totalAdvance: Double = 0.0,
  val todayBaki: Double = 0.0,
  val todayJoma: Double = 0.0,
  val customerCount: Int = 0,
  val dueCustomerCount: Int = 0,
  val transactionCount: Int = 0,
)

class TallyRepository(
  private val dao: TallyDao,
  private val shopPreferences: ShopPreferences,
) {
  val customerSummariesFlow: Flow<List<CustomerWithSummary>> = dao.observeCustomerSummaries()

  val allTransactionsFlow: Flow<List<TransactionWithCustomer>> =
    dao.observeAllTransactionsWithCustomer()

  val shopProfileFlow: Flow<ShopProfile> = shopPreferences.shopProfileFlow

  val businessSummaryFlow: Flow<BusinessSummary> =
    combine(customerSummariesFlow, allTransactionsFlow) { customers, transactions ->
      computeBusinessSummary(customers, transactions)
    }

  fun observeCustomerSummary(customerId: Long): Flow<CustomerWithSummary?> =
    dao.observeCustomerSummary(customerId)

  fun observeCustomerTransactions(customerId: Long): Flow<List<TransactionEntity>> =
    dao.observeCustomerTransactions(customerId)

  suspend fun addCustomer(name: String, phone: String, address: String): Long {
    return dao.insertCustomer(
      CustomerEntity(
        name = name.trim(),
        phone = phone.trim(),
        address = address.trim(),
        createdAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun updateCustomer(customer: CustomerEntity) {
    dao.updateCustomer(
      customer.copy(
        name = customer.name.trim(),
        phone = customer.phone.trim(),
        address = customer.address.trim(),
      )
    )
  }

  suspend fun deleteCustomer(customerId: Long) {
    dao.deleteCustomerById(customerId)
  }

  suspend fun addTransaction(
    customerId: Long,
    type: TransactionType,
    amount: Double,
    note: String,
    timestamp: Long = System.currentTimeMillis(),
  ): Long {
    return dao.insertTransaction(
      TransactionEntity(
        customerId = customerId,
        type = type.name,
        amount = amount,
        note = note.trim(),
        timestamp = timestamp,
      )
    )
  }

  suspend fun updateTransaction(transaction: TransactionEntity) {
    dao.updateTransaction(transaction.copy(note = transaction.note.trim()))
  }

  suspend fun deleteTransaction(transactionId: Long) {
    dao.deleteTransactionById(transactionId)
  }

  suspend fun updateShopProfile(shopName: String, ownerName: String) {
    shopPreferences.updateShopProfile(shopName, ownerName)
  }

  suspend fun exportBackupJson(): String {
    val profile = shopPreferences.getShopProfileOnce()
    val customers = dao.getAllCustomersOnce()
    val transactions = dao.getAllTransactionsOnce()
    val now = System.currentTimeMillis()
    val json = BackupCodec.encode(profile, customers, transactions, exportedAt = now)
    shopPreferences.updateLastBackupTimestamp(now)
    return json
  }

  fun parseBackupJson(rawJson: String): Result<BackupPayload> = BackupCodec.decode(rawJson)

  suspend fun restoreBackup(payload: BackupPayload) {
    dao.replaceAllData(payload.customers, payload.transactions)
    shopPreferences.updateShopProfile(payload.shopName, payload.ownerName)
  }

  companion object {
    fun computeBusinessSummary(
      customers: List<CustomerWithSummary>,
      transactions: List<TransactionWithCustomer>,
      nowMillis: Long = System.currentTimeMillis(),
    ): BusinessSummary {
      var totalBaki = 0.0
      var totalJoma = 0.0
      var netReceivable = 0.0
      var totalAdvance = 0.0
      var dueCustomerCount = 0

      for (c in customers) {
        totalBaki += c.totalBaki
        totalJoma += c.totalJoma
        if (c.netDue > 0.005) {
          netReceivable += c.netDue
          dueCustomerCount++
        } else if (c.netDue < -0.005) {
          totalAdvance += -c.netDue
        }
      }

      var todayBaki = 0.0
      var todayJoma = 0.0
      for (tx in transactions) {
        if (BengaliFormatter.isToday(tx.timestamp, nowMillis)) {
          if (tx.type == TransactionType.BAKI.name) {
            todayBaki += tx.amount
          } else if (tx.type == TransactionType.JOMA.name) {
            todayJoma += tx.amount
          }
        }
      }

      return BusinessSummary(
        totalBaki = totalBaki,
        totalJoma = totalJoma,
        netReceivable = netReceivable,
        totalAdvance = totalAdvance,
        todayBaki = todayBaki,
        todayJoma = todayJoma,
        customerCount = customers.size,
        dueCustomerCount = dueCustomerCount,
        transactionCount = transactions.size,
      )
    }
  }
}
