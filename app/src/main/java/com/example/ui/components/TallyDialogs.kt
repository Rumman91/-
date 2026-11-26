package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.backup.BackupPayload
import com.example.data.local.CustomerWithSummary
import com.example.data.local.ShopProfile
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.theme.LocalLedgerColors
import com.example.util.BengaliFormatter
import kotlinx.coroutines.delay

@Composable
fun CustomerFormDialog(
  initialCustomer: CustomerWithSummary? = null,
  onDismiss: () -> Unit,
  onSave: (name: String, phone: String, address: String) -> Unit,
) {
  var name by remember(initialCustomer) { mutableStateOf(initialCustomer?.name ?: "") }
  var phone by remember(initialCustomer) { mutableStateOf(initialCustomer?.phone ?: "") }
  var address by remember(initialCustomer) { mutableStateOf(initialCustomer?.address ?: "") }
  var showNameError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (initialCustomer == null) "নতুন কাস্টমার যোগ করুন" else "কাস্টমারের তথ্য সংশোধন",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            if (it.isNotBlank()) showNameError = false
          },
          label = { Text("কাস্টমারের নাম *") },
          placeholder = { Text("যেমন: করিম মিয়া") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          isError = showNameError,
          supportingText =
            if (showNameError) {
              { Text("অনুগ্রহ করে কাস্টমারের নাম লিখুন") }
            } else null,
          singleLine = true,
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
          modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("মোবাইল নম্বর (হোয়াটসঅ্যাপের জন্য)") },
          placeholder = { Text("যেমন: 017XXXXXXXX") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth().testTag("customer_phone_input"),
        )
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("ঠিকানা / বিবরণ (ঐচ্ছিক)") },
          placeholder = { Text("যেমন: বাজার রোড") },
          leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("customer_address_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank()) {
            showNameError = true
          } else {
            onSave(name.trim(), phone.trim(), address.trim())
          }
        },
        modifier = Modifier.heightIn(min = 48.dp).testTag("save_customer_button"),
      ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("সংরক্ষণ করুন")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("বাতিল") }
    },
  )
}

@Composable
fun TransactionFormDialog(
  customers: List<CustomerWithSummary>,
  preselectedCustomerId: Long? = null,
  initialType: TransactionType = TransactionType.BAKI,
  editingTransaction: TransactionEntity? = null,
  onDismiss: () -> Unit,
  onSave: (customerId: Long, type: TransactionType, amount: Double, note: String, timestamp: Long) -> Unit,
) {
  val ledgerColors = LocalLedgerColors.current
  val isEditing = editingTransaction != null

  var selectedCustomerId by
    remember(preselectedCustomerId, editingTransaction, customers) {
      mutableStateOf(
        editingTransaction?.customerId
          ?: preselectedCustomerId
          ?: customers.firstOrNull()?.id
      )
    }
  var customerMenuExpanded by remember { mutableStateOf(false) }

  var selectedType by
    remember(initialType, editingTransaction) {
      mutableStateOf(
        if (editingTransaction != null) {
          if (editingTransaction.type == TransactionType.JOMA.name) TransactionType.JOMA
          else TransactionType.BAKI
        } else {
          initialType
        }
      )
    }
  var amountText by
    remember(editingTransaction) {
      mutableStateOf(
        editingTransaction?.amount?.let { amt ->
          if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
        } ?: ""
      )
    }
  var noteText by remember(editingTransaction) { mutableStateOf(editingTransaction?.note ?: "") }
  var showAmountError by remember { mutableStateOf(false) }

  var liveTimestamp by
    remember(editingTransaction) {
      mutableLongStateOf(editingTransaction?.timestamp ?: System.currentTimeMillis())
    }

  LaunchedEffect(isEditing) {
    if (!isEditing) {
      while (true) {
        liveTimestamp = System.currentTimeMillis()
        delay(1000L)
      }
    }
  }

  val selectedCustomer = customers.find { it.id == selectedCustomerId }
  val parsedAmount = BengaliFormatter.parseAmountInput(amountText)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isEditing) "লেনদেন সংশোধন করুন" else "বাকি / জমা হিসাব লিখুন",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Customer selector (locked when editing or preselected from detail)
        if (preselectedCustomerId == null && !isEditing) {
          Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
              shape = MaterialTheme.shapes.medium,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(MaterialTheme.shapes.medium)
                  .clickable { customerMenuExpanded = true }
                  .testTag("select_customer_dropdown"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "কাস্টমার নির্বাচন করুন",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = selectedCustomer?.name ?: "কোনো কাস্টমার নেই",
                    style = MaterialTheme.typography.titleMedium,
                  )
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "কাস্টমার তালিকা")
              }
            }
            DropdownMenu(
              expanded = customerMenuExpanded,
              onDismissRequest = { customerMenuExpanded = false },
            ) {
              customers.forEach { customer ->
                DropdownMenuItem(
                  text = {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(customer.name)
                      if (customer.netDue > 0.01) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                          text = "বাকি ${BengaliFormatter.formatTaka(customer.netDue)}",
                          style = MaterialTheme.typography.labelMedium,
                          color = ledgerColors.baki,
                        )
                      }
                    }
                  },
                  onClick = {
                    selectedCustomerId = customer.id
                    customerMenuExpanded = false
                  },
                )
              }
            }
          }
        } else if (selectedCustomer != null) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Person,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(selectedCustomer.name, style = MaterialTheme.typography.titleSmall)
              }
              if (selectedCustomer.netDue > 0.01) {
                Text(
                  text = "বকেয়া: ${BengaliFormatter.formatTaka(selectedCustomer.netDue)}",
                  style = MaterialTheme.typography.labelMedium,
                  color = ledgerColors.baki,
                )
              }
            }
          }
        }

        // BAKI vs JOMA toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          val bakiSelected = selectedType == TransactionType.BAKI
          Surface(
            shape = MaterialTheme.shapes.medium,
            color =
              if (bakiSelected) ledgerColors.bakiContainer
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border =
              BorderStroke(
                width = if (bakiSelected) 2.dp else 1.dp,
                color = if (bakiSelected) ledgerColors.baki else MaterialTheme.colorScheme.outline,
              ),
            modifier =
              Modifier.weight(1f)
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable { selectedType = TransactionType.BAKI }
                .testTag("type_baki_toggle"),
          ) {
            Row(
              modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Default.ArrowUpward,
                contentDescription = null,
                tint =
                  if (bakiSelected) ledgerColors.onBakiContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = "বাকি দিলাম",
                style = MaterialTheme.typography.labelLarge,
                color =
                  if (bakiSelected) ledgerColors.onBakiContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          val jomaSelected = selectedType == TransactionType.JOMA
          Surface(
            shape = MaterialTheme.shapes.medium,
            color =
              if (jomaSelected) ledgerColors.jomaContainer
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border =
              BorderStroke(
                width = if (jomaSelected) 2.dp else 1.dp,
                color = if (jomaSelected) ledgerColors.joma else MaterialTheme.colorScheme.outline,
              ),
            modifier =
              Modifier.weight(1f)
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable { selectedType = TransactionType.JOMA }
                .testTag("type_joma_toggle"),
          ) {
            Row(
              modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Default.ArrowDownward,
                contentDescription = null,
                tint =
                  if (jomaSelected) ledgerColors.onJomaContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = "জমা নিলাম",
                style = MaterialTheme.typography.labelLarge,
                color =
                  if (jomaSelected) ledgerColors.onJomaContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // Amount input
        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            if (BengaliFormatter.parseAmountInput(it) != null) showAmountError = false
          },
          label = { Text("টাকার পরিমাণ (৳) *") },
          placeholder = { Text("যেমন: ৫০০ বা 500") },
          isError = showAmountError,
          supportingText =
            if (showAmountError) {
              { Text("০ এর চেয়ে বেশি সঠিক টাকা লিখুন") }
            } else null,
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("transaction_amount_input"),
        )

        // Live preview of remaining due after this transaction
        if (selectedCustomer != null && parsedAmount != null && !isEditing) {
          val projectedDue =
            if (selectedType == TransactionType.BAKI) {
              selectedCustomer.netDue + parsedAmount
            } else {
              selectedCustomer.netDue - parsedAmount
            }
          val previewColor =
            when {
              projectedDue > 0.01 -> ledgerColors.baki
              projectedDue < -0.01 -> MaterialTheme.colorScheme.tertiary
              else -> ledgerColors.joma
            }
          val previewBg =
            when {
              projectedDue > 0.01 -> ledgerColors.bakiContainer.copy(alpha = 0.65f)
              projectedDue < -0.01 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
              else -> ledgerColors.jomaContainer.copy(alpha = 0.65f)
            }
          val previewText =
            when {
              projectedDue > 0.01 ->
                "${if (selectedType == TransactionType.JOMA) "জমা দেওয়ার পর " else ""}অবশিষ্ট বকেয়া হবে: ${BengaliFormatter.formatTaka(projectedDue)}"
              projectedDue < -0.01 ->
                "হিসাবের পর অগ্রিম জমা থাকবে: ${BengaliFormatter.formatTaka(-projectedDue)}"
              else -> "জমা দেওয়ার পর সম্পূর্ণ পরিশোধিত হবে (৳ ০)"
            }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = previewBg,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = previewText,
              style = MaterialTheme.typography.labelMedium,
              color = previewColor,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
          }
        }

        // Quick preset chips
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (
            selectedType == TransactionType.JOMA &&
              selectedCustomer != null &&
              selectedCustomer.netDue > 0.01 &&
              !isEditing
          ) {
            FilterChip(
              selected = false,
              onClick = {
                val due = selectedCustomer.netDue
                amountText = if (due % 1.0 == 0.0) due.toLong().toString() else String.format("%.2f", due)
                showAmountError = false
              },
              label = { Text("পুরো বকেয়া (${BengaliFormatter.formatTaka(selectedCustomer.netDue)})") },
            )
          }
          listOf(100, 200, 500, 1000).forEach { preset ->
            FilterChip(
              selected = false,
              onClick = {
                amountText = preset.toString()
                showAmountError = false
              },
              label = { Text("৳${BengaliFormatter.formatInt(preset)}") },
            )
          }
        }

        // Note field
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("বিবরণ / পণ্যের নাম (ঐচ্ছিক)") },
          placeholder = {
            Text(
              if (selectedType == TransactionType.BAKI) "যেমন: চাল, ডাল ও তেল"
              else "যেমন: নগদ পরিশোধ / বিকাশ"
            )
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("transaction_note_input"),
        )

        // Live automatic date & time badge
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Default.CalendarToday,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text =
                if (isEditing)
                  "তারিখ ও সময়: ${BengaliFormatter.formatBengaliDateTimeWithSeconds(liveTimestamp)}"
                else
                  "অটোমেটিক তারিখ ও সময়: ${BengaliFormatter.formatBengaliDateTimeWithSeconds(liveTimestamp)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
    },
    confirmButton = {
      val btnColor =
        if (selectedType == TransactionType.BAKI) ledgerColors.baki else ledgerColors.joma
      Button(
        onClick = {
          val parsed = BengaliFormatter.parseAmountInput(amountText)
          val custId = selectedCustomerId
          if (parsed == null || custId == null) {
            showAmountError = true
          } else {
            val saveTimestamp = editingTransaction?.timestamp ?: System.currentTimeMillis()
            onSave(custId, selectedType, parsed, noteText.trim(), saveTimestamp)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = btnColor),
        modifier = Modifier.heightIn(min = 48.dp).testTag("save_transaction_button"),
      ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
          if (isEditing) "আপডেট করুন"
          else if (selectedType == TransactionType.BAKI) "বাকি যোগ করুন"
          else "জমা সংরক্ষণ করুন"
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("বাতিল") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDateRangePickerDialog(
  initialStartLocalMillis: Long?,
  initialEndLocalMillis: Long?,
  onDismiss: () -> Unit,
  onClear: () -> Unit,
  onConfirmRange: (startLocalMillis: Long?, endLocalMillis: Long?) -> Unit,
) {
  val dateRangePickerState = rememberDateRangePickerState()

  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (initialStartLocalMillis != null || initialEndLocalMillis != null) {
          TextButton(
            onClick = {
              onClear()
              onDismiss()
            },
            modifier = Modifier.heightIn(min = 48.dp),
          ) {
            Text("রিসেট")
          }
        }
        Button(
          onClick = {
            val startUtc = dateRangePickerState.selectedStartDateMillis
            val endUtc = dateRangePickerState.selectedEndDateMillis ?: startUtc
            val startLocal = startUtc?.let { BengaliFormatter.utcPickerMillisToLocalDayStart(it) }
            val endLocal = endUtc?.let { BengaliFormatter.utcPickerMillisToLocalDayEnd(it) }
            onConfirmRange(startLocal, endLocal)
          },
          enabled = dateRangePickerState.selectedStartDateMillis != null,
          modifier = Modifier.heightIn(min = 48.dp).testTag("apply_date_range_button"),
        ) {
          Text("প্রয়োগ করুন")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("বাতিল") }
    },
  ) {
    DateRangePicker(
      state = dateRangePickerState,
      title = {
        Text(
          text = "নির্দিষ্ট তারিখের রেঞ্জ বেছে নিন",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
        )
      },
      headline = {
        val startLocal =
          dateRangePickerState.selectedStartDateMillis?.let {
            BengaliFormatter.utcPickerMillisToLocalDayStart(it)
          }
        val endLocal =
          dateRangePickerState.selectedEndDateMillis?.let {
            BengaliFormatter.utcPickerMillisToLocalDayEnd(it)
          }
        Text(
          text = BengaliFormatter.formatDateRangeLabel(startLocal, endLocal),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
      },
      showModeToggle = true,
      modifier = Modifier.height(480.dp),
    )
  }
}

@Composable
fun ShopProfileDialog(
  currentProfile: ShopProfile,
  onDismiss: () -> Unit,
  onSave: (shopName: String, ownerName: String) -> Unit,
) {
  var shopName by remember(currentProfile) { mutableStateOf(currentProfile.shopName) }
  var ownerName by remember(currentProfile) { mutableStateOf(currentProfile.ownerName) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("দোকানের তথ্য পরিবর্তন", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = shopName,
          onValueChange = { shopName = it },
          label = { Text("দোকানের নাম") },
          placeholder = { Text("যেমন: মায়ের দোয়া স্টোর") },
          leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("shop_name_input"),
        )
        OutlinedTextField(
          value = ownerName,
          onValueChange = { ownerName = it },
          label = { Text("দোকানদারের নাম (ঐচ্ছিক)") },
          placeholder = { Text("যেমন: মোঃ রফিকুল ইসলাম") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("owner_name_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(shopName.ifBlank { "আমার দোকান" }, ownerName) },
        modifier = Modifier.heightIn(min = 48.dp).testTag("save_shop_profile_button"),
      ) {
        Text("সংরক্ষণ করুন")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("বাতিল") }
    },
  )
}

@Composable
fun RestorePreviewDialog(
  payload: BackupPayload,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        Icons.Default.Restore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp),
      )
    },
    title = { Text("ব্যাকআপ রিস্টোর নিশ্চিত করুন", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "এই JSON ব্যাকআপ ফাইলটি রিস্টোর করলে বর্তমান খাতার সব ডেটা পরিবর্তন হয়ে নিচের তথ্য যোগ হবে:",
          style = MaterialTheme.typography.bodyMedium,
        )
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "🏪 দোকান: ${payload.shopName}",
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = "📅 ব্যাকআপ তারিখ: ${BengaliFormatter.formatBengaliDateTime(payload.exportedAt)}",
              style = MaterialTheme.typography.bodySmall,
            )
            Text(
              text = "👥 মোট কাস্টমার: ${BengaliFormatter.formatInt(payload.customers.size)} জন",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "🧾 মোট লেনদেন: ${BengaliFormatter.formatInt(payload.transactions.size)} টি",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_restore_button"),
      ) {
        Text("হ্যাঁ, রিস্টোর করুন")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("বাতিল") }
    },
  )
}

@Composable
fun ConfirmDeleteDialog(
  title: String,
  message: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  val ledgerColors = LocalLedgerColors.current
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        Icons.Default.Warning,
        contentDescription = null,
        tint = ledgerColors.baki,
        modifier = Modifier.size(30.dp),
      )
    },
    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
    text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = ledgerColors.baki),
        modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_delete_button"),
      ) {
        Text("মুছে ফেলুন")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("না, থাক") }
    },
  )
}
