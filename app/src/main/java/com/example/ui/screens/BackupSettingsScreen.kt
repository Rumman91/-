package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.TallyUiState
import com.example.ui.theme.LocalLedgerColors
import com.example.util.BengaliFormatter

@Composable
fun BackupSettingsScreen(
  uiState: TallyUiState,
  onEditShopProfile: () -> Unit,
  onExportBackupClick: () -> Unit,
  onRestoreBackupClick: () -> Unit,
  onShareShopSummaryWhatsApp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val ledgerColors = LocalLedgerColors.current
  val profile = uiState.shopProfile
  val summary = uiState.businessSummary

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item(key = "backup_header") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(text = "ব্যাকআপ ও দোকানের সেটিংস", style = MaterialTheme.typography.headlineSmall)
          Text(
            text = "আপনার দোকানের নাম পরিবর্তন করুন, JSON ব্যাকআপ রাখুন বা হোয়াটসঅ্যাপে হিসাব পাঠান।",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Shop Profile Card
      item(key = "shop_profile_card") {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(48.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Default.Storefront,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "দোকানের নাম",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(text = profile.shopName, style = MaterialTheme.typography.titleMedium)
              if (profile.ownerName.isNotBlank()) {
                Text(
                  text = "মালিক: ${profile.ownerName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            IconButton(
              onClick = onEditShopProfile,
              modifier = Modifier.testTag("settings_edit_shop_button"),
            ) {
              Icon(Icons.Default.Edit, contentDescription = "দোকানের তথ্য পরিবর্তন")
            }
          }
        }
      }

      // WhatsApp Summary Card
      item(key = "whatsapp_summary_card") {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "হোয়াটসঅ্যাপে দোকানের মোট হিসাব",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text =
                "মোট বাকি, মোট জমা, আজকের লেনদেন এবং বকেয়া কাস্টমারদের তালিকাসহ সম্পূর্ণ রিপোর্ট এক ক্লিকে হোয়াটসঅ্যাপে পাঠান।",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
              onClick = onShareShopSummaryWhatsApp,
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = ledgerColors.whatsapp,
                  contentColor = Color.White,
                ),
              shape = MaterialTheme.shapes.medium,
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("settings_whatsapp_summary_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text("হোয়াটসঅ্যাপে মোট হিসাব পাঠান")
            }
          }
        }
      }

      // JSON Backup Export & Restore Card
      item(key = "json_backup_card") {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "JSON ব্যাকআপ ও রিস্টোর",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text =
                "বর্তমান খাতায় ${BengaliFormatter.formatInt(summary.customerCount)} জন কাস্টমার ও ${BengaliFormatter.formatInt(summary.transactionCount)} টি লেনদেন সংরক্ষিত আছে।",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (profile.lastBackupTimestamp > 0L) {
              Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text =
                    "সর্বশেষ ব্যাকআপ: ${BengaliFormatter.formatBengaliDateTime(profile.lastBackupTimestamp)}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
              }
            }

            Button(
              onClick = onExportBackupClick,
              shape = MaterialTheme.shapes.medium,
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("export_json_backup_button"),
            ) {
              Icon(
                Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text("JSON ব্যাকআপ ডাউনলোড করুন")
            }

            OutlinedButton(
              onClick = onRestoreBackupClick,
              border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
              shape = MaterialTheme.shapes.medium,
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("restore_json_backup_button"),
            ) {
              Icon(
                Icons.Default.Restore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text("JSON ব্যাকআপ রিস্টোর করুন")
            }
          }
        }
      }

      // Local Storage Info Card
      item(key = "local_storage_info") {
        Card(
          shape = MaterialTheme.shapes.medium,
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "১০০% অফলাইন লোকাল স্টোরেজ",
                style = MaterialTheme.typography.titleSmall,
              )
            }
            Row(verticalAlignment = Alignment.Top) {
              Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text =
                  "আপনার সব কাস্টমার ও বাকি-জমার হিসাব সরাসরি এই ডিভাইসের নিরাপদ লোকাল স্টোরেজে সংরক্ষিত থাকে। ফোন পরিবর্তনের আগে JSON ব্যাকআপ ডাউনলোড করে রাখুন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}
