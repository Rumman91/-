package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.CustomerWithSummary
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CustomerFormDialog
import com.example.ui.components.RestorePreviewDialog
import com.example.ui.components.ShopProfileDialog
import com.example.ui.components.TransactionFormDialog
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.KhataHomeScreen
import com.example.util.BengaliFormatter
import com.example.util.WhatsAppHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ROUTE_KHATA = "khata"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_BACKUP = "backup"
private const val ROUTE_CUSTOMER_DETAIL = "customer/{customerId}"

private data class TopLevelTab(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

private val topLevelTabs =
  listOf(
    TopLevelTab(
      route = ROUTE_KHATA,
      label = "খাতা",
      selectedIcon = Icons.Filled.Book,
      unselectedIcon = Icons.Outlined.Book,
    ),
    TopLevelTab(
      route = ROUTE_HISTORY,
      label = "হিস্ট্রি",
      selectedIcon = Icons.Filled.History,
      unselectedIcon = Icons.Outlined.History,
    ),
    TopLevelTab(
      route = ROUTE_BACKUP,
      label = "ব্যাকআপ",
      selectedIcon = Icons.Filled.Backup,
      unselectedIcon = Icons.Outlined.Backup,
    ),
  )

private data class TransactionModalConfig(
  val customerId: Long?,
  val initialType: TransactionType,
  val editingTransaction: TransactionEntity? = null,
)

private sealed interface DeleteConfirmation {
  data class Customer(val customer: CustomerWithSummary) : DeleteConfirmation

  data class Transaction(val transactionId: Long, val amount: Double, val type: String) :
    DeleteConfirmation
}

@Composable
fun TallyKhataApp(viewModel: TallyViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val currentRoute = currentDestination?.route

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  // Modal states
  var showCustomerForm by remember { mutableStateOf(false) }
  var editingCustomer by remember { mutableStateOf<CustomerWithSummary?>(null) }
  var txModalConfig by remember { mutableStateOf<TransactionModalConfig?>(null) }
  var showShopProfileDialog by remember { mutableStateOf(false) }
  var deleteConfirmation by remember { mutableStateOf<DeleteConfirmation?>(null) }
  var pendingBackupJson by remember { mutableStateOf<String?>(null) }

  // SAF JSON Export Launcher
  val exportJsonLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
      val jsonToWrite = pendingBackupJson
      pendingBackupJson = null
      if (uri != null && jsonToWrite != null) {
        coroutineScope.launch {
          val success =
            withContext(Dispatchers.IO) {
              try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                  out.write(jsonToWrite.toByteArray(Charsets.UTF_8))
                }
                true
              } catch (_: Exception) {
                false
              }
            }
          if (success) {
            viewModel.showMessage("JSON ব্যাকআপ ফাইল সফলভাবে সংরক্ষণ হয়েছে")
          } else {
            viewModel.showMessage("ব্যাকআপ ফাইল সংরক্ষণ করা যায়নি")
          }
        }
      }
    }

  // SAF JSON Import Launcher
  val importJsonLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
      if (uri != null) {
        coroutineScope.launch {
          val rawText =
            withContext(Dispatchers.IO) {
              try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                  BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                }
              } catch (_: Exception) {
                null
              }
            }
          if (rawText != null) {
            viewModel.parseBackupForPreview(rawText)
          } else {
            viewModel.showMessage("ফাইলটি পড়া যায়নি")
          }
        }
      }
    }

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeSnackbar()
    }
  }

  val isTopLevelRoute = currentRoute in topLevelTabs.map { it.route }

  val shareShopSummary: () -> Unit = {
    val message =
      WhatsAppHelper.buildShopSummaryMessage(
        shopName = uiState.shopProfile.shopName,
        summary = uiState.businessSummary,
        dueCustomers = uiState.allCustomers,
      )
    WhatsAppHelper.shareToWhatsApp(context, message)
  }

  val startBackupExport: () -> Unit = {
    viewModel.prepareBackupJson { json ->
      pendingBackupJson = json
      val filename = "tally_khata_${BengaliFormatter.formatBackupFileTimestamp()}.json"
      exportJsonLauncher.launch(filename)
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useRail && isTopLevelRoute) {
          NavigationBar {
            topLevelTabs.forEach { tab ->
              val selected =
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
              NavigationBarItem(
                selected = selected,
                onClick = {
                  navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("nav_tab_${tab.route}"),
              )
            }
          }
        }
      },
      floatingActionButton = {
        if (currentRoute == ROUTE_KHATA || currentRoute == ROUTE_HISTORY) {
          FloatingActionButton(
            onClick = {
              if (uiState.allCustomers.isNotEmpty()) {
                txModalConfig = TransactionModalConfig(null, TransactionType.BAKI)
              } else {
                editingCustomer = null
                showCustomerForm = true
              }
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("main_fab"),
          ) {
            Icon(
              Icons.Default.Add,
              contentDescription =
                if (uiState.allCustomers.isNotEmpty()) "বাকি বা জমা লিখুন" else "নতুন কাস্টমার",
            )
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useRail && isTopLevelRoute) {
          NavigationRail(modifier = Modifier.statusBarsPadding()) {
            topLevelTabs.forEach { tab ->
              val selected =
                currentDestination?.hierarchy?.any { it.route == tab.route } == true
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label) },
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          NavHost(navController = navController, startDestination = ROUTE_KHATA) {
            composable(ROUTE_KHATA) {
              KhataHomeScreen(
                uiState = uiState,
                onEditShopClick = { showShopProfileDialog = true },
                onAddCustomerClick = {
                  editingCustomer = null
                  showCustomerForm = true
                },
                onQuickTransactionClick = { customerId, type ->
                  txModalConfig = TransactionModalConfig(customerId, type)
                },
                onCustomerClick = { customerId -> navController.navigate("customer/$customerId") },
                onShareShopSummaryWhatsApp = shareShopSummary,
                onShareCustomerReceiptWhatsApp = { customer ->
                  val customerTx =
                    uiState.allTransactions
                      .filter { it.customerId == customer.id }
                      .map {
                        TransactionEntity(
                          id = it.id,
                          customerId = it.customerId,
                          type = it.type,
                          amount = it.amount,
                          note = it.note,
                          timestamp = it.timestamp,
                        )
                      }
                  val receipt =
                    WhatsAppHelper.buildCustomerReceiptMessage(
                      shopName = uiState.shopProfile.shopName,
                      customer = customer,
                      transactions = customerTx,
                    )
                  WhatsAppHelper.shareToWhatsApp(
                    context = context,
                    message = receipt,
                    rawPhone = customer.phone,
                  )
                },
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onFilterChange = viewModel::onCustomerFilterChange,
              )
            }

            composable(ROUTE_HISTORY) {
              HistoryScreen(
                uiState = uiState,
                onFilterChange = viewModel::onHistoryFilterChange,
                onSearchQueryChange = viewModel::onHistorySearchQueryChange,
                onDateRangeSelected = viewModel::onHistoryDateRangeSelected,
                onClearDateRange = viewModel::clearHistoryDateRange,
                onCustomerClick = { customerId -> navController.navigate("customer/$customerId") },
                onEditTransaction = { tx ->
                  val txType =
                    if (tx.type == TransactionType.JOMA.name) TransactionType.JOMA
                    else TransactionType.BAKI
                  txModalConfig = TransactionModalConfig(tx.customerId, txType, tx)
                },
                onDeleteTransaction = { tx ->
                  deleteConfirmation =
                    DeleteConfirmation.Transaction(tx.id, tx.amount, tx.type)
                },
              )
            }

            composable(ROUTE_BACKUP) {
              BackupSettingsScreen(
                uiState = uiState,
                onEditShopProfile = { showShopProfileDialog = true },
                onExportBackupClick = startBackupExport,
                onRestoreBackupClick = {
                  importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                onShareShopSummaryWhatsApp = shareShopSummary,
              )
            }

            composable(
              route = ROUTE_CUSTOMER_DETAIL,
              arguments = listOf(navArgument("customerId") { type = NavType.LongType }),
            ) { backStackEntry ->
              val customerId = backStackEntry.arguments?.getLong("customerId") ?: -1L
              val detailFlow = remember(customerId) { viewModel.observeCustomerDetail(customerId) }
              val detailState by
                detailFlow.collectAsStateWithLifecycle(initialValue = CustomerDetailUiState())

              CustomerDetailScreen(
                detailState = detailState,
                onBack = { navController.popBackStack() },
                onEditCustomer = { cust ->
                  editingCustomer = cust
                  showCustomerForm = true
                },
                onDeleteCustomer = { cust ->
                  deleteConfirmation = DeleteConfirmation.Customer(cust)
                },
                onAddTransaction = { type ->
                  txModalConfig = TransactionModalConfig(customerId, type)
                },
                onEditTransaction = { tx ->
                  val txType =
                    if (tx.type == TransactionType.JOMA.name) TransactionType.JOMA
                    else TransactionType.BAKI
                  txModalConfig = TransactionModalConfig(customerId, txType, tx)
                },
                onDeleteTransaction = { tx ->
                  deleteConfirmation =
                    DeleteConfirmation.Transaction(tx.id, tx.amount, tx.type)
                },
                onShareWhatsAppReceipt = { customer, txs ->
                  val receipt =
                    WhatsAppHelper.buildCustomerReceiptMessage(
                      shopName = uiState.shopProfile.shopName,
                      customer = customer,
                      transactions = txs,
                    )
                  WhatsAppHelper.shareToWhatsApp(
                    context = context,
                    message = receipt,
                    rawPhone = customer.phone,
                  )
                },
              )
            }
          }
        }
      }
    }
  }

  // Dialogs
  if (showCustomerForm) {
    CustomerFormDialog(
      initialCustomer = editingCustomer,
      onDismiss = {
        showCustomerForm = false
        editingCustomer = null
      },
      onSave = { name, phone, address ->
        val existing = editingCustomer
        if (existing == null) {
          viewModel.addCustomer(name, phone, address)
        } else {
          viewModel.updateCustomer(existing.id, name, phone, address, existing.createdAt)
        }
        showCustomerForm = false
        editingCustomer = null
      },
    )
  }

  val activeTxConfig = txModalConfig
  if (activeTxConfig != null) {
    TransactionFormDialog(
      customers = uiState.allCustomers,
      preselectedCustomerId = activeTxConfig.customerId,
      initialType = activeTxConfig.initialType,
      editingTransaction = activeTxConfig.editingTransaction,
      onDismiss = { txModalConfig = null },
      onSave = { customerId, type, amount, note, timestamp ->
        val editing = activeTxConfig.editingTransaction
        if (editing == null) {
          viewModel.addTransaction(customerId, type, amount, note, timestamp)
        } else {
          viewModel.updateTransaction(
            editing.copy(type = type.name, amount = amount, note = note)
          )
        }
        txModalConfig = null
      },
    )
  }

  if (showShopProfileDialog) {
    ShopProfileDialog(
      currentProfile = uiState.shopProfile,
      onDismiss = { showShopProfileDialog = false },
      onSave = { shopName, ownerName ->
        viewModel.updateShopProfile(shopName, ownerName)
        showShopProfileDialog = false
      },
    )
  }

  val restorePayload = uiState.pendingRestorePayload
  if (restorePayload != null) {
    RestorePreviewDialog(
      payload = restorePayload,
      onDismiss = viewModel::dismissRestorePreview,
      onConfirm = viewModel::confirmRestore,
    )
  }

  val deleteItem = deleteConfirmation
  if (deleteItem != null) {
    when (deleteItem) {
      is DeleteConfirmation.Customer -> {
        ConfirmDeleteDialog(
          title = "কাস্টমার মুছে ফেলবেন?",
          message =
            "'${deleteItem.customer.name}'-কে মুছে ফেললে তার সব বাকি ও জমার লেনদেনও মুছে যাবে। আপনি কি নিশ্চিত?",
          onDismiss = { deleteConfirmation = null },
          onConfirm = {
            viewModel.deleteCustomer(deleteItem.customer.id, deleteItem.customer.name)
            deleteConfirmation = null
            if (currentRoute == ROUTE_CUSTOMER_DETAIL) {
              navController.popBackStack()
            }
          },
        )
      }
      is DeleteConfirmation.Transaction -> {
        val label = if (deleteItem.type == TransactionType.BAKI.name) "বাকি" else "জমা"
        ConfirmDeleteDialog(
          title = "লেনদেন মুছে ফেলবেন?",
          message =
            "${BengaliFormatter.formatTaka(deleteItem.amount)} ($label) লেনদেনটি খাতা থেকে মুছে ফেলতে চান?",
          onDismiss = { deleteConfirmation = null },
          onConfirm = {
            viewModel.deleteTransaction(deleteItem.transactionId)
            deleteConfirmation = null
          },
        )
      }
    }
  }
}
