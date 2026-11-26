package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerWithSummary
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.CustomerDetailUiState
import com.example.ui.theme.LocalLedgerColors
import com.example.util.BengaliFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
  detailState: CustomerDetailUiState,
  onBack: () -> Unit,
  onEditCustomer: (CustomerWithSummary) -> Unit,
  onDeleteCustomer: (CustomerWithSummary) -> Unit,
  onAddTransaction: (TransactionType) -> Unit,
  onEditTransaction: (TransactionEntity) -> Unit,
  onDeleteTransaction: (TransactionEntity) -> Unit,
  onShareWhatsAppReceipt: (CustomerWithSummary, List<TransactionEntity>) -> Unit,
) {
  val customer = detailState.customer
  val ledgerColors = LocalLedgerColors.current

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = customer?.name ?: "কাস্টমার খাতা",
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (customer != null && customer.phone.isNotBlank()) {
              Text(
                text = customer.phone,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("customer_detail_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "পেছনে যান")
          }
        },
        actions = {
          if (customer != null) {
            IconButton(
              onClick = { onEditCustomer(customer) },
              modifier = Modifier.testTag("edit_customer_button"),
            ) {
              Icon(Icons.Default.Edit, contentDescription = "কাস্টমার সংশোধন")
            }
            IconButton(
              onClick = { onDeleteCustomer(customer) },
              modifier = Modifier.testTag("delete_customer_button"),
            ) {
              Icon(
                Icons.Default.Delete,
                contentDescription = "কাস্টমার মুছুন",
                tint = ledgerColors.baki,
              )
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          ),
      )
    },
  ) { innerPadding ->
    if (customer == null) {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        if (detailState.isLoaded) {
          Text("কাস্টমারের তথ্য পাওয়া যায়নি", style = MaterialTheme.typography.titleMedium)
        }
      }
      return@Scaffold
    }

    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item(key = "customer_summary") {
          CustomerAccountSummaryCard(customer = customer)
        }

        item(key = "customer_actions") {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = { onAddTransaction(TransactionType.BAKI) },
                colors = ButtonDefaults.buttonColors(containerColor = ledgerColors.baki),
                shape = MaterialTheme.shapes.medium,
                modifier =
                  Modifier.weight(1f).heightIn(min = 50.dp).testTag("detail_add_baki_button"),
              ) {
                Icon(
                  Icons.Default.ArrowUpward,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("বাকি দিন")
              }

              Button(
                onClick = { onAddTransaction(TransactionType.JOMA) },
                colors = ButtonDefaults.buttonColors(containerColor = ledgerColors.joma),
                shape = MaterialTheme.shapes.medium,
                modifier =
                  Modifier.weight(1f).heightIn(min = 50.dp).testTag("detail_add_joma_button"),
              ) {
                Icon(
                  Icons.Default.ArrowDownward,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("জমা নিন")
              }
            }

            Button(
              onClick = { onShareWhatsAppReceipt(customer, detailState.transactions) },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = ledgerColors.whatsapp,
                  contentColor = Color.White,
                ),
              shape = MaterialTheme.shapes.medium,
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 50.dp)
                  .testTag("detail_whatsapp_receipt_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text("হোয়াটসঅ্যাপে বকেয়া রসিদ পাঠান")
            }
          }
        }

        item(key = "tx_section_header") {
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "লেনদেন হিস্ট্রি (${BengaliFormatter.formatInt(detailState.transactions.size)})",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "অটোমেটিক তারিখসহ",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (detailState.transactionsWithBalance.isEmpty()) {
          item(key = "empty_customer_tx") {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Default.ReceiptLong,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(38.dp),
                )
                Text("এখনো কোনো লেনদেন হয়নি", style = MaterialTheme.typography.titleMedium)
                Text(
                  "উপরের 'বাকি দিন' বা 'জমা নিন' বাটনে চাপ দিয়ে প্রথম হিসাব লিখুন।",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(items = detailState.transactionsWithBalance, key = { it.transaction.id }) { item ->
            CustomerTransactionItemCard(
              tx = item.transaction,
              balanceAfter = item.balanceAfter,
              onEdit = { onEditTransaction(item.transaction) },
              onDelete = { onDeleteTransaction(item.transaction) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CustomerAccountSummaryCard(customer: CustomerWithSummary) {
  val ledgerColors = LocalLedgerColors.current
  val hasDue = customer.netDue > 0.005
  val hasAdvance = customer.netDue < -0.005

  val statusColor =
    when {
      hasDue -> ledgerColors.baki
      hasAdvance -> MaterialTheme.colorScheme.tertiary
      else -> ledgerColors.joma
    }
  val statusBg =
    when {
      hasDue -> ledgerColors.bakiContainer
      hasAdvance -> MaterialTheme.colorScheme.tertiaryContainer
      else -> ledgerColors.jomaContainer
    }
  val balanceTitle =
    when {
      hasDue -> "বর্তমান বকেয়া"
      hasAdvance -> "অগ্রিম জমা আছে"
      else -> "হিসাব পরিশোধিত"
    }

  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (customer.phone.isNotBlank() || customer.address.isNotBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          if (customer.phone.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(customer.phone, style = MaterialTheme.typography.bodyMedium)
            }
          }
          if (customer.address.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                customer.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = statusBg,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = balanceTitle,
            style = MaterialTheme.typography.labelLarge,
            color = statusColor,
          )
          Text(
            text =
              when {
                hasDue -> BengaliFormatter.formatTaka(customer.netDue)
                hasAdvance -> BengaliFormatter.formatTaka(-customer.netDue)
                else -> "৳ ০"
              },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            modifier = Modifier.testTag("customer_net_due_text"),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = ledgerColors.bakiContainer.copy(alpha = 0.55f),
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "মোট বাকি",
              style = MaterialTheme.typography.labelSmall,
              color = ledgerColors.onBakiContainer,
            )
            Text(
              text = BengaliFormatter.formatTaka(customer.totalBaki),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = ledgerColors.baki,
            )
          }
        }

        Surface(
          shape = MaterialTheme.shapes.small,
          color = ledgerColors.jomaContainer.copy(alpha = 0.55f),
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "মোট জমা",
              style = MaterialTheme.typography.labelSmall,
              color = ledgerColors.onJomaContainer,
            )
            Text(
              text = BengaliFormatter.formatTaka(customer.totalJoma),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = ledgerColors.joma,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CustomerTransactionItemCard(
  tx: TransactionEntity,
  balanceAfter: Double,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val ledgerColors = LocalLedgerColors.current
  val isBaki = tx.type == TransactionType.BAKI.name
  val accentColor = if (isBaki) ledgerColors.baki else ledgerColors.joma
  val bgColor = if (isBaki) ledgerColors.bakiContainer else ledgerColors.jomaContainer
  var menuExpanded by remember { mutableStateOf(false) }

  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().testTag("customer_tx_${tx.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(shape = CircleShape, color = bgColor, modifier = Modifier.size(40.dp)) {
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
          Surface(shape = RoundedCornerShape(6.dp), color = bgColor) {
            Text(
              text = if (isBaki) "বাকি" else "জমা",
              style = MaterialTheme.typography.labelSmall,
              color = accentColor,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
          if (tx.note.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
              text = tx.note,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Text(
          text = BengaliFormatter.formatBengaliDateTimeWithSeconds(tx.timestamp),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp),
        )
        val balanceAfterLabel =
          when {
            balanceAfter > 0.01 ->
              "${if (!isBaki) "জমার পর " else ""}বকেয়া: ${BengaliFormatter.formatTaka(balanceAfter)}"
            balanceAfter < -0.01 -> "অগ্রিম জমা: ${BengaliFormatter.formatTaka(-balanceAfter)}"
            else -> "পরিশোধিত (বকেয়া ৳ ০)"
          }
        Text(
          text = balanceAfterLabel,
          style = MaterialTheme.typography.labelSmall,
          color =
            when {
              balanceAfter > 0.01 -> ledgerColors.baki
              balanceAfter < -0.01 -> MaterialTheme.colorScheme.tertiary
              else -> ledgerColors.joma
            },
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(top = 1.dp),
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
          Icon(Icons.Default.MoreVert, contentDescription = "লেনদেন অপশন")
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
