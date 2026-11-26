package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupPayload
import com.example.data.local.CustomerEntity
import com.example.data.local.CustomerWithSummary
import com.example.data.local.ShopPreferences
import com.example.data.local.ShopProfile
import com.example.data.local.TallyDatabase
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.TransactionWithCustomer
import com.example.data.repository.BusinessSummary
import com.example.data.repository.TallyRepository
import com.example.util.BengaliFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CustomerFilter(val label: String) {
  ALL("সব"),
  DUE("বাকি আছে"),
  SETTLED("পরিশোধিত"),
  ADVANCE("অগ্রিম"),
}

enum class HistoryFilter(val label: String) {
  ALL("সব লেনদেন"),
  TODAY("আজকের"),
  BAKI("শুধু বাকি"),
  JOMA("শুধু জমা"),
}

data class TransactionWithBalance(
  val transaction: TransactionEntity,
  val balanceAfter: Double,
)

data class TallyUiState(
  val shopProfile: ShopProfile = ShopProfile(),
  val businessSummary: BusinessSummary = BusinessSummary(),
  val allCustomers: List<CustomerWithSummary> = emptyList(),
  val filteredCustomers: List<CustomerWithSummary> = emptyList(),
  val allTransactions: List<TransactionWithCustomer> = emptyList(),
  val filteredTransactions: List<TransactionWithCustomer> = emptyList(),
  val searchQuery: String = "",
  val customerFilter: CustomerFilter = CustomerFilter.ALL,
  val historyFilter: HistoryFilter = HistoryFilter.ALL,
  val historySearchQuery: String = "",
  val historyStartDateMillis: Long? = null,
  val historyEndDateMillis: Long? = null,
  val isLoading: Boolean = true,
  val pendingRestorePayload: BackupPayload? = null,
  val snackbarMessage: String? = null,
)

data class CustomerDetailUiState(
  val customer: CustomerWithSummary? = null,
  val transactions: List<TransactionEntity> = emptyList(),
  val transactionsWithBalance: List<TransactionWithBalance> = emptyList(),
  val isLoaded: Boolean = false,
)

class TallyViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: TallyRepository =
    TallyRepository(
      dao = TallyDatabase.getInstance(application).tallyDao(),
      shopPreferences = ShopPreferences(application),
    )

  private val searchQuery = MutableStateFlow("")
  private val customerFilter = MutableStateFlow(CustomerFilter.ALL)
  private val historyFilter = MutableStateFlow(HistoryFilter.ALL)
  private val historySearchQuery = MutableStateFlow("")
  private val historyDateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
  private val pendingRestorePayload = MutableStateFlow<BackupPayload?>(null)
  private val snackbarMessage = MutableStateFlow<String?>(null)

  private val historyControlsFlow =
    combine(historyFilter, historySearchQuery, historyDateRange) { filter, query, range ->
      Triple(filter, query, range)
    }

  private val filterControlsFlow =
    combine(searchQuery, customerFilter, historyControlsFlow, pendingRestorePayload, snackbarMessage) {
      query,
      cFilter,
      hControls,
      restore,
      snack ->
      FilterControls(
        searchQuery = query,
        customerFilter = cFilter,
        historyFilter = hControls.first,
        historySearchQuery = hControls.second,
        historyStartMillis = hControls.third.first,
        historyEndMillis = hControls.third.second,
        pendingRestorePayload = restore,
        snackbarMessage = snack,
      )
    }

  val uiState: StateFlow<TallyUiState> =
    combine(
        repository.shopProfileFlow,
        repository.businessSummaryFlow,
        repository.customerSummariesFlow,
        repository.allTransactionsFlow,
        filterControlsFlow,
      ) { profile, summary, customers, transactions, controls ->
        val filteredCustomers =
          filterCustomers(customers, controls.searchQuery, controls.customerFilter)
        val filteredTransactions =
          filterTransactions(
            transactions = transactions,
            filter = controls.historyFilter,
            searchQuery = controls.historySearchQuery,
            startDateMillis = controls.historyStartMillis,
            endDateMillis = controls.historyEndMillis,
          )
        TallyUiState(
          shopProfile = profile,
          businessSummary = summary,
          allCustomers = customers,
          filteredCustomers = filteredCustomers,
          allTransactions = transactions,
          filteredTransactions = filteredTransactions,
          searchQuery = controls.searchQuery,
          customerFilter = controls.customerFilter,
          historyFilter = controls.historyFilter,
          historySearchQuery = controls.historySearchQuery,
          historyStartDateMillis = controls.historyStartMillis,
          historyEndDateMillis = controls.historyEndMillis,
          isLoading = false,
          pendingRestorePayload = controls.pendingRestorePayload,
          snackbarMessage = controls.snackbarMessage,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TallyUiState(),
      )

  fun observeCustomerDetail(customerId: Long): Flow<CustomerDetailUiState> {
    return combine(
      repository.observeCustomerSummary(customerId),
      repository.observeCustomerTransactions(customerId),
    ) { customer, transactions ->
      val chronological =
        transactions.sortedWith(compareBy<TransactionEntity> { it.timestamp }.thenBy { it.id })
      var running = 0.0
      val withBalance =
        chronological
          .map { tx ->
            running += if (tx.type == TransactionType.BAKI.name) tx.amount else -tx.amount
            TransactionWithBalance(transaction = tx, balanceAfter = running)
          }
          .asReversed()
      CustomerDetailUiState(
        customer = customer,
        transactions = transactions,
        transactionsWithBalance = withBalance,
        isLoaded = true,
      )
    }
  }

  fun onSearchQueryChange(query: String) {
    searchQuery.value = query
  }

  fun onCustomerFilterChange(filter: CustomerFilter) {
    customerFilter.value = filter
  }

  fun onHistoryFilterChange(filter: HistoryFilter) {
    historyFilter.value = filter
  }

  fun onHistorySearchQueryChange(query: String) {
    historySearchQuery.value = query
  }

  fun onHistoryDateRangeSelected(startLocalMillis: Long?, endLocalMillis: Long?) {
    val start = startLocalMillis?.let { BengaliFormatter.startOfDayMillis(it) }
    val end = endLocalMillis?.let { BengaliFormatter.endOfDayMillis(it) }
    historyDateRange.value = start to (end ?: start?.let { BengaliFormatter.endOfDayMillis(it) })
  }

  fun clearHistoryDateRange() {
    historyDateRange.value = null to null
  }

  fun addCustomer(
    name: String,
    phone: String,
    address: String,
    onCreated: (Long) -> Unit = {},
  ) {
    if (name.isBlank()) return
    viewModelScope.launch {
      val id = repository.addCustomer(name, phone, address)
      snackbarMessage.value = "কাস্টমার '${name.trim()}' যোগ করা হয়েছে"
      onCreated(id)
    }
  }

  fun updateCustomer(id: Long, name: String, phone: String, address: String, createdAt: Long) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.updateCustomer(
        CustomerEntity(
          id = id,
          name = name,
          phone = phone,
          address = address,
          createdAt = createdAt,
        )
      )
      snackbarMessage.value = "কাস্টমারের তথ্য আপডেট করা হয়েছে"
    }
  }

  fun deleteCustomer(customerId: Long, customerName: String) {
    viewModelScope.launch {
      repository.deleteCustomer(customerId)
      snackbarMessage.value = "'$customerName' ও তার লেনদেন মুছে ফেলা হয়েছে"
    }
  }

  fun addTransaction(
    customerId: Long,
    type: TransactionType,
    amount: Double,
    note: String,
    timestampMillis: Long = System.currentTimeMillis(),
  ) {
    if (amount <= 0.0) return
    viewModelScope.launch {
      repository.addTransaction(
        customerId = customerId,
        type = type,
        amount = amount,
        note = note,
        timestamp = timestampMillis,
      )
      val updatedCustomer = repository.observeCustomerSummary(customerId).first()
      val remainingDue = updatedCustomer?.netDue
      val typeLabel = if (type == TransactionType.BAKI) "বাকি" else "জমা"
      val remainingText =
        when {
          remainingDue == null -> ""
          remainingDue > 0.01 -> " • অবশিষ্ট বকেয়া: ${BengaliFormatter.formatTaka(remainingDue)}"
          remainingDue < -0.01 -> " • অগ্রিম জমা: ${BengaliFormatter.formatTaka(-remainingDue)}"
          else -> " • বকেয়া: ৳ ০ (পরিশোধিত)"
        }
      snackbarMessage.value =
        "${BengaliFormatter.formatTaka(amount)} $typeLabel সংরক্ষণ হয়েছে$remainingText"
    }
  }

  fun updateTransaction(transaction: TransactionEntity) {
    if (transaction.amount <= 0.0) return
    viewModelScope.launch {
      repository.updateTransaction(transaction)
      snackbarMessage.value = "লেনদেন আপডেট করা হয়েছে"
    }
  }

  fun deleteTransaction(transactionId: Long) {
    viewModelScope.launch {
      repository.deleteTransaction(transactionId)
      snackbarMessage.value = "লেনদেন মুছে ফেলা হয়েছে"
    }
  }

  fun updateShopProfile(shopName: String, ownerName: String) {
    viewModelScope.launch {
      repository.updateShopProfile(shopName, ownerName)
      snackbarMessage.value = "দোকানের তথ্য সংরক্ষণ করা হয়েছে"
    }
  }

  fun prepareBackupJson(onReady: (String) -> Unit) {
    viewModelScope.launch {
      val json = repository.exportBackupJson()
      onReady(json)
    }
  }

  fun showMessage(message: String) {
    snackbarMessage.value = message
  }

  fun parseBackupForPreview(rawJson: String) {
    val result = repository.parseBackupJson(rawJson)
    result
      .onSuccess { payload -> pendingRestorePayload.value = payload }
      .onFailure { error ->
        snackbarMessage.value = error.message ?: "ব্যাকআপ ফাইলটি পড়া যায়নি"
      }
  }

  fun confirmRestore() {
    val payload = pendingRestorePayload.value ?: return
    viewModelScope.launch {
      repository.restoreBackup(payload)
      pendingRestorePayload.value = null
      snackbarMessage.value =
        "ব্যাকআপ রিস্টোর সফল হয়েছে (${BengaliFormatter.formatInt(payload.customers.size)} জন কাস্টমার, ${BengaliFormatter.formatInt(payload.transactions.size)} টি লেনদেন)"
    }
  }

  fun dismissRestorePreview() {
    pendingRestorePayload.value = null
  }

  fun consumeSnackbar() {
    snackbarMessage.value = null
  }

  private fun filterCustomers(
    customers: List<CustomerWithSummary>,
    query: String,
    filter: CustomerFilter,
  ): List<CustomerWithSummary> {
    val trimmed = query.trim()
    val englishQuery = BengaliFormatter.toEnglishDigits(trimmed)
    return customers.filter { customer ->
      val matchesFilter =
        when (filter) {
          CustomerFilter.ALL -> true
          CustomerFilter.DUE -> customer.netDue > 0.005
          CustomerFilter.SETTLED -> customer.netDue >= -0.005 && customer.netDue <= 0.005
          CustomerFilter.ADVANCE -> customer.netDue < -0.005
        }
      val matchesQuery =
        if (trimmed.isEmpty()) {
          true
        } else {
          customer.name.contains(trimmed, ignoreCase = true) ||
            customer.phone.contains(trimmed, ignoreCase = true) ||
            BengaliFormatter.toEnglishDigits(customer.phone).contains(englishQuery) ||
            customer.address.contains(trimmed, ignoreCase = true)
        }
      matchesFilter && matchesQuery
    }
  }

  companion object {
    fun filterTransactions(
      transactions: List<TransactionWithCustomer>,
      filter: HistoryFilter,
      searchQuery: String = "",
      startDateMillis: Long? = null,
      endDateMillis: Long? = null,
      nowMillis: Long = System.currentTimeMillis(),
    ): List<TransactionWithCustomer> {
      val trimmedQuery = searchQuery.trim()
      val englishQuery = BengaliFormatter.toEnglishDigits(trimmedQuery)
      return transactions.filter { tx ->
        val matchesType =
          when (filter) {
            HistoryFilter.ALL -> true
            HistoryFilter.TODAY -> BengaliFormatter.isToday(tx.timestamp, nowMillis)
            HistoryFilter.BAKI -> tx.type == TransactionType.BAKI.name
            HistoryFilter.JOMA -> tx.type == TransactionType.JOMA.name
          }
        val matchesDateRange =
          when {
            startDateMillis != null && endDateMillis != null ->
              tx.timestamp in startDateMillis..endDateMillis
            startDateMillis != null -> tx.timestamp >= startDateMillis
            endDateMillis != null -> tx.timestamp <= endDateMillis
            else -> true
          }
        val matchesQuery =
          if (trimmedQuery.isEmpty()) {
            true
          } else {
            tx.customerName.contains(trimmedQuery, ignoreCase = true) ||
              tx.customerPhone.contains(trimmedQuery, ignoreCase = true) ||
              BengaliFormatter.toEnglishDigits(tx.customerPhone).contains(englishQuery) ||
              tx.note.contains(trimmedQuery, ignoreCase = true)
          }
        matchesType && matchesDateRange && matchesQuery
      }
    }
  }

  private data class FilterControls(
    val searchQuery: String,
    val customerFilter: CustomerFilter,
    val historyFilter: HistoryFilter,
    val historySearchQuery: String,
    val historyStartMillis: Long?,
    val historyEndMillis: Long?,
    val pendingRestorePayload: BackupPayload?,
    val snackbarMessage: String?,
  )
}
