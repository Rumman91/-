package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CustomerWithSummary
import com.example.data.local.TransactionType
import com.example.ui.CustomerFilter
import com.example.ui.TallyUiState
import com.example.ui.theme.LocalLedgerColors
import com.example.util.BengaliFormatter

@Composable
fun KhataHomeScreen(
  uiState: TallyUiState,
  onEditShopClick: () -> Unit,
  onAddCustomerClick: () -> Unit,
  onQuickTransactionClick: (customerId: Long?, type: TransactionType) -> Unit,
  onCustomerClick: (Long) -> Unit,
  onShareShopSummaryWhatsApp: () -> Unit,
  onShareCustomerReceiptWhatsApp: (CustomerWithSummary) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onFilterChange: (CustomerFilter) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(key = "hero_summary") {
        HeroSummaryCard(
          uiState = uiState,
          onEditShopClick = onEditShopClick,
          onShareShopSummaryWhatsApp = onShareShopSummaryWhatsApp,
        )
      }

      item(key = "quick_actions") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = onAddCustomerClick,
            modifier = Modifier.weight(1f).heightIn(min = 50.dp).testTag("add_customer_button"),
            shape = MaterialTheme.shapes.medium,
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("নতুন কাস্টমার", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }

          if (uiState.allCustomers.isNotEmpty()) {
            OutlinedButton(
              onClick = { onQuickTransactionClick(null, TransactionType.BAKI) },
              modifier =
                Modifier.weight(1f).heightIn(min = 50.dp).testTag("quick_entry_button"),
              shape = MaterialTheme.shapes.medium,
              border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("বাকি / জমা লিখুন", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }

      if (uiState.allCustomers.isNotEmpty()) {
        item(key = "search_and_filter") {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = uiState.searchQuery,
              onValueChange = onSearchQueryChange,
              placeholder = { Text("কাস্টমারের নাম বা মোবাইল খুঁজুন...") },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = "খুঁজুন") },
              trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                  IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "মুছুন")
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
              modifier = Modifier.fillMaxWidth().testTag("customer_search_input"),
            )

            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CustomerFilter.entries.forEach { filter ->
                val count =
                  when (filter) {
                    CustomerFilter.ALL -> uiState.allCustomers.size
                    CustomerFilter.DUE -> uiState.allCustomers.count { it.netDue > 0.005 }
                    CustomerFilter.SETTLED ->
                      uiState.allCustomers.count { it.netDue >= -0.005 && it.netDue <= 0.005 }
                    CustomerFilter.ADVANCE -> uiState.allCustomers.count { it.netDue < -0.005 }
                  }
                FilterChip(
                  selected = uiState.customerFilter == filter,
                  onClick = { onFilterChange(filter) },
                  label = {
                    Text("${filter.label} (${BengaliFormatter.formatInt(count)})")
                  },
                  modifier = Modifier.testTag("customer_filter_${filter.name.lowercase()}"),
                )
              }
            }
          }
        }
      }

      if (uiState.allCustomers.isEmpty() && !uiState.isLoading) {
        item(key = "empty_state") {
          EmptyKhataStateCard(onAddCustomerClick = onAddCustomerClick)
        }
      } else if (uiState.filteredCustomers.isEmpty() && uiState.allCustomers.isNotEmpty()) {
        item(key = "no_search_matches") {
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
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
              )
              Text("কোনো কাস্টমার পাওয়া যায়নি", style = MaterialTheme.typography.titleMedium)
              Text(
                "অন্য নাম দিয়ে খুঁজুন অথবা উপরের ফিল্টার পরিবর্তন করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      } else {
        items(items = uiState.filteredCustomers, key = { it.id }) { customer ->
          CustomerLedgerCard(
            customer = customer,
            onClick = { onCustomerClick(customer.id) },
            onAddBaki = { onQuickTransactionClick(customer.id, TransactionType.BAKI) },
            onAddJoma = { onQuickTransactionClick(customer.id, TransactionType.JOMA) },
            onShareWhatsApp = { onShareCustomerReceiptWhatsApp(customer) },
          )
        }
      }
    }
  }
}

@Composable
private fun HeroSummaryCard(
  uiState: TallyUiState,
  onEditShopClick: () -> Unit,
  onShareShopSummaryWhatsApp: () -> Unit,
) {
  val ledgerColors = LocalLedgerColors.current
  val summary = uiState.businessSummary
  val collectionRatio =
    if (summary.totalBaki > 0.0) {
      (summary.totalJoma / summary.totalBaki).toFloat().coerceIn(0f, 1f)
    } else {
      0f
    }

  Card(
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors =
                listOf(
                  Color(0xFF0B4228),
                  Color(0xFF13603B),
                  Color(0xFF197549),
                )
            )
          )
          .padding(18.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Header row: Shop name + date + edit button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Surface(
              shape = CircleShape,
              color = Color.White.copy(alpha = 0.16f),
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Default.Storefront,
                  contentDescription = null,
                  tint = Color(0xFFFCD34D),
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Spacer(Modifier.width(10.dp))
            Column {
              Text(
                text = uiState.shopProfile.shopName,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = BengaliFormatter.formatBengaliDateWithWeekday(System.currentTimeMillis()),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
              )
            }
          }
          IconButton(
            onClick = onEditShopClick,
            modifier = Modifier.testTag("edit_shop_name_button"),
          ) {
            Icon(
              Icons.Default.Edit,
              contentDescription = "দোকানের নাম পরিবর্তন",
              tint = Color.White.copy(alpha = 0.9f),
            )
          }
        }

        // Main Net Receivable Highlight
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = Color.White.copy(alpha = 0.13f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "মোট বকেয়া পাওনা",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFDE68A),
              )
              Text(
                text =
                  "কাস্টমার: ${BengaliFormatter.formatInt(summary.customerCount)} জন (বাকি ${BengaliFormatter.formatInt(summary.dueCustomerCount)})",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.88f),
              )
            }
            Text(
              text = BengaliFormatter.formatTaka(summary.netReceivable),
              style = MaterialTheme.typography.displaySmall,
              color = Color.White,
              modifier = Modifier.testTag("hero_net_due_text"),
            )

            if (summary.totalBaki > 0.0) {
              Spacer(Modifier.height(2.dp))
              LinearProgressIndicator(
                progress = { collectionRatio },
                color = Color(0xFF6EE7B7),
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
              )
              val pct = (collectionRatio * 100).toInt()
              Text(
                text = "মোট আদায়ের হার: ${BengaliFormatter.formatInt(pct)}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
              )
            }
          }
        }

        // Total Baki vs Total Joma + Today's summary
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = Color(0xFFFEF2F2),
            modifier = Modifier.weight(1f),
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
              Text(
                text = "মোট বাকি দিয়েছি",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF991B1B),
              )
              Text(
                text = BengaliFormatter.formatTaka(summary.totalBaki),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB91C1C),
              )
              Text(
                text = "আজ: ${BengaliFormatter.formatTaka(summary.todayBaki)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7F1D1D).copy(alpha = 0.8f),
              )
            }
          }

          Surface(
            shape = MaterialTheme.shapes.small,
            color = Color(0xFFF0FDF4),
            modifier = Modifier.weight(1f),
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
              Text(
                text = "মোট জমা পেয়েছি",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF166534),
              )
              Text(
                text = BengaliFormatter.formatTaka(summary.totalJoma),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D),
              )
              Text(
                text = "আজ: ${BengaliFormatter.formatTaka(summary.todayJoma)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF14532D).copy(alpha = 0.8f),
              )
            }
          }
        }

        // One-Click WhatsApp Shop Summary Button
        Button(
          onClick = onShareShopSummaryWhatsApp,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = ledgerColors.whatsapp,
              contentColor = Color.White,
            ),
          shape = MaterialTheme.shapes.medium,
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("whatsapp_shop_summary_button"),
        ) {
          Icon(
            Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = "এক ক্লিকে হোয়াটসঅ্যাপে দোকানের মোট হিসাব পাঠান",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun CustomerLedgerCard(
  customer: CustomerWithSummary,
  onClick: () -> Unit,
  onAddBaki: () -> Unit,
  onAddJoma: () -> Unit,
  onShareWhatsApp: () -> Unit,
) {
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
  val statusLabel =
    when {
      hasDue -> "বাকি আছে"
      hasAdvance -> "অগ্রিম জমা"
      else -> "পরিশোধিত"
    }

  Card(
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .testTag("customer_card_${customer.id}"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Initials Avatar
        Surface(
          shape = CircleShape,
          color = statusBg,
          modifier = Modifier.size(46.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = customer.name.trim().take(1).uppercase(),
              style = MaterialTheme.typography.titleLarge,
              color = statusColor,
            )
          }
        }
        Spacer(Modifier.width(12.dp))

        // Customer details
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = customer.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (customer.phone.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text(
                text = customer.phone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          val timestampText =
            customer.lastTransactionAt?.let {
              "শেষ লেনদেন: ${BengaliFormatter.formatBengaliDateTime(it)}"
            } ?: "যোগ হয়েছে: ${BengaliFormatter.formatBengaliDate(customer.createdAt)}"
          Text(
            text = timestampText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(Modifier.width(8.dp))

        // Net balance column
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text =
              when {
                hasDue -> BengaliFormatter.formatTaka(customer.netDue)
                hasAdvance -> BengaliFormatter.formatTaka(-customer.netDue)
                else -> "৳ ০"
              },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = statusColor,
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = statusBg,
          ) {
            Text(
              text = statusLabel,
              style = MaterialTheme.typography.labelSmall,
              color = statusColor,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
        }
      }

      // Quick actions bar per customer
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = onAddBaki,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          border = BorderStroke(1.dp, ledgerColors.baki.copy(alpha = 0.5f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = ledgerColors.baki),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.weight(1f).heightIn(min = 42.dp),
        ) {
          Text("+ বাকি", style = MaterialTheme.typography.labelMedium)
        }

        OutlinedButton(
          onClick = onAddJoma,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          border = BorderStroke(1.dp, ledgerColors.joma.copy(alpha = 0.5f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = ledgerColors.joma),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.weight(1f).heightIn(min = 42.dp),
        ) {
          Text("+ জমা", style = MaterialTheme.typography.labelMedium)
        }

        Surface(
          shape = MaterialTheme.shapes.small,
          color = ledgerColors.whatsapp.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, ledgerColors.whatsapp.copy(alpha = 0.45f)),
          modifier =
            Modifier.heightIn(min = 42.dp)
              .clip(MaterialTheme.shapes.small)
              .clickable(onClick = onShareWhatsApp)
              .testTag("whatsapp_customer_receipt_${customer.id}"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Default.ReceiptLong,
              contentDescription = "হোয়াটসঅ্যাপে বকেয়া রসিদ পাঠান",
              tint = ledgerColors.whatsapp,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
              text = "রসিদ",
              style = MaterialTheme.typography.labelMedium,
              color = ledgerColors.whatsapp,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyKhataStateCard(onAddCustomerClick: () -> Unit) {
  Card(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_khata_1795672879506),
        contentDescription = "টালি খাতা দোকানের ছবি",
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.fillMaxWidth().height(190.dp).clip(MaterialTheme.shapes.medium),
      )
      Text(
        text = "আপনার ডিজিটাল টালি খাতায় স্বাগতম!",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
      )
      Text(
        text =
          "প্রথম কাস্টমার যোগ করে বাকি ও জমার হিসাব রাখা শুরু করুন। সব হিসাব অটোমেটিক তারিখসহ আপনার ফোনেই সুরক্ষিত থাকবে।",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Button(
        onClick = onAddCustomerClick,
        modifier = Modifier.heightIn(min = 48.dp).testTag("empty_add_customer_button"),
      ) {
        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("প্রথম কাস্টমার যোগ করুন")
      }
    }
  }
}
