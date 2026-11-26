package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.data.local.TransactionWithCustomer
import com.example.ui.HistoryFilter
import com.example.ui.TallyUiState
import com.example.ui.components.HistoryDateRangePickerDialog
import com.example.ui.theme.LocalLedgerColors
import com.example.util.BengaliFormatter

@Composable
fun HistoryScreen(
  uiState: TallyUiState,
  onFilterChange: (HistoryFilter) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onDateRangeSelected: (startLocalMillis: Long?, endLocalMillis: Long?) -> Unit,
  onClearDateRange: () -> Unit,
  onCustomerClick: (Long) -> Unit,
  onEditTransaction: (TransactionEntity) -> Unit,
  onDeleteTransaction: (TransactionWithCustomer) -> Unit,
  modifier: Modifier = Modifier,
) {
  val ledgerColors = LocalLedgerColors.current
  val transactions = uiState.filteredTransactions

  var showDateRangeDialog by remember { mutableStateOf(false) }
  var showCustomerQuickMenu by remember { mutableStateOf(false) }

  val hasDateRange =
    uiState.historyStartDateMillis != null || uiState.historyEndDateMillis != null

  val filteredBaki =
    remember(transactions) {
      transactions.filter { it.type == TransactionType.BAKI.name }.sumOf { it.amount }
    }
  val filteredJoma =
    remember(transactions) {
      transactions.filter { it.type == TransactionType.JOMA.name }.sumOf { it.amount }
    }

  val groupedByDay =
    remember(transactions) {
      transactions.groupBy { BengaliFormatter.startOfDayMillis(it.timestamp) }
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item(key = "history_header") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "ট্রানজেকশন হিস্ট্রি",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "কাস্টমারের নাম বা নির্দিষ্ট তারিখের রেঞ্জ অনুযায়ী সব লেনদেন ফিল্টার করুন।",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          // Search bar for customer name, phone, or transaction note
          OutlinedTextField(
            value = uiState.historySearchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("কাস্টমারের নাম, মোবাইল বা বিবরণ লিখুন...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "খুঁজুন") },
            trailingIcon = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.historySearchQuery.isNotEmpty()) {
                  IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.testTag("clear_history_search_button"),
                  ) {
                    Icon(Icons.Default.Clear, contentDescription = "সার্চ মুছুন")
                  }
                }
                if (uiState.allCustomers.isNotEmpty()) {
                  Box {
                    IconButton(
                      onClick = { showCustomerQuickMenu = true },
                      modifier = Modifier.testTag("history_pick_customer_button"),
                    ) {
                      Icon(
                        Icons.Default.PersonSearch,
                        contentDescription = "কাস্টমার নির্বাচন করুন",
                        tint = MaterialTheme.colorScheme.primary,
                      )
                    }
                    DropdownMenu(
                      expanded = showCustomerQuickMenu,
                      onDismissRequest = { showCustomerQuickMenu = false },
                    ) {
                      DropdownMenuItem(
                        text = { Text("সকল কাস্টমার দেখুন") },
                        onClick = {
                          onSearchQueryChange("")
                          showCustomerQuickMenu = false
                        },
                      )
                      uiState.allCustomers.forEach { customer ->
                        DropdownMenuItem(
                          text = { Text(customer.name) },
                          onClick = {
                            onSearchQueryChange(customer.name)
                            showCustomerQuickMenu = false
                          },
                        )
                      }
                    }
                  }
                }
              }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              ),
            modifier = Modifier.fillMaxWidth().testTag("history_search_input"),
          )

          // Date-Range Picker Control Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = { showDateRangeDialog = true },
              shape = MaterialTheme.shapes.medium,
              border =
                BorderStroke(
                  width = if (hasDateRange) 1.5.dp else 1.dp,
                  color =
                    if (hasDateRange) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                ),
              modifier =
                Modifier.weight(1f).heightIn(min = 48.dp).testTag("history_date_range_button"),
            ) {
              Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text =
                  if (hasDateRange) {
                    BengaliFormatter.formatDateRangeLabel(
                      uiState.historyStartDateMillis,
                      uiState.historyEndDateMillis,
                    )
                  } else {
                    "তারিখ-রেঞ্জ পিকার (তারিখ বাছাই)"
                  },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            if (hasDateRange) {
              Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier =
                  Modifier.heightIn(min = 48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onClearDateRange)
                    .testTag("clear_date_range_button"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "তারিখ ফিল্টার মুছুন",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            HistoryFilter.entries.forEach { filter ->
              FilterChip(
                selected = uiState.historyFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.label) },
                modifier = Modifier.testTag("history_filter_${filter.name.lowercase()}"),
              )
            }
          }

          // Totals strip for active filter
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "মোট লেনদেন",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "${BengaliFormatter.formatInt(transactions.size)} টি",
                  style = MaterialTheme.typography.titleMedium,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "বাকি: ${BengaliFormatter.formatTaka(filteredBaki)}",
                  style = MaterialTheme.typography.labelLarge,
                  color = ledgerColors.baki,
                )
                Text(
                  text = "জমা: ${BengaliFormatter.formatTaka(filteredJoma)}",
                  style = MaterialTheme.typography.labelLarge,
                  color = ledgerColors.joma,
                )
              }
            }
          }
        }
      }

      if (transactions.isEmpty()) {
        item(key = "empty_history") {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp),
              )
              Text("কোনো লেনদেন পাওয়া যায়নি", style = MaterialTheme.typography.titleMedium)
              Text(
                if (uiState.historySearchQuery.isNotBlank() || hasDateRange) {
                  "নির্বাচিত তারিখ বা কাস্টমারের নামে কোনো লেনদেন পাওয়া যায়নি। ফিল্টার পরিবর্তন করে দেখুন।"
                } else {
                  "কাস্টমারের খাতায় বাকি বা জমা যোগ করলে অটোমেটিক তারিখসহ হিস্ট্রি এখানে দেখা যাবে।"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      } else {
        groupedByDay.forEach { (dayMillis, dayItems) ->
          item(key = "day_header_$dayMillis") {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Default.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = BengaliFormatter.formatRelativeDayLabel(dayMillis),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          items(items = dayItems, key = { it.id }) { tx ->
            HistoryTransactionCard(
              tx = tx,
              onCustomerClick = { onCustomerClick(tx.customerId) },
              onEdit = {
                onEditTransaction(
                  TransactionEntity(
                    id = tx.id,
                    customerId = tx.customerId,
                    type = tx.type,
                    amount = tx.amount,
                    note = tx.note,
                    timestamp = tx.timestamp,
                  )
                )
              },
              onDelete = { onDeleteTransaction(tx) },
            )
          }
        }
      }
    }
  }

  if (showDateRangeDialog) {
    HistoryDateRangePickerDialog(
      initialStartLocalMillis = uiState.historyStartDateMillis,
      initialEndLocalMillis = uiState.historyEndDateMillis,
      onDismiss = { showDateRangeDialog = false },
      onClear = onClearDateRange,
      onConfirmRange = { startLocal, endLocal ->
        onDateRangeSelected(startLocal, endLocal)
        showDateRangeDialog = false
      },
    )
  }
}

@Composable
private fun HistoryTransactionCard(
  tx: TransactionWithCustomer,
  onCustomerClick: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val ledgerColors = LocalLedgerColors.current
  val isBaki = tx.type == TransactionType.BAKI.name
  val accentColor = if (isBaki) ledgerColors.baki else ledgerColors.joma
  val containerColor = if (isBaki) ledgerColors.bakiContainer else ledgerColors.jomaContainer
  var menuExpanded by remember { mutableStateOf(false) }

  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onCustomerClick)
        .testTag("history_tx_${tx.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(shape = CircleShape, color = containerColor, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isBaki) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = if (isBaki) "বাকি" else "জমা",
            tint = accentColor,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Spacer(Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = tx.customerName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Spacer(Modifier.width(6.dp))
          Surface(shape = RoundedCornerShape(6.dp), color = containerColor) {
            Text(
              text = if (isBaki) "বাকি" else "জমা",
              style = MaterialTheme.typography.labelSmall,
              color = accentColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
          }
        }

        if (tx.note.isNotBlank()) {
          Text(
            text = tx.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = BengaliFormatter.formatBengaliDateTimeWithSeconds(tx.timestamp),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(Modifier.width(6.dp))

      Text(
        text = "${if (isBaki) "+" else "-"} ${BengaliFormatter.formatTaka(tx.amount)}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = accentColor,
      )

      Box {
        IconButton(onClick = { menuExpanded = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = "অপশন")
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
          DropdownMenuItem(
            text = { Text("সংশোধন করুন") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = {
              menuExpanded = false
              onEdit()
            },
          )
          DropdownMenuItem(
            text = { Text("মুছে ফেলুন", color = ledgerColors.baki) },
            leadingIcon = {
              Icon(Icons.Default.Delete, contentDescription = null, tint = ledgerColors.baki)
            },
            onClick = {
              menuExpanded = false
              onDelete()
            },
          )
        }
      }
    }
  }
}
