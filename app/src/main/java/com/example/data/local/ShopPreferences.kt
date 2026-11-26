package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.shopDataStore: DataStore<Preferences> by preferencesDataStore(name = "tally_shop_prefs")

data class ShopProfile(
  val shopName: String = "আমার দোকান",
  val ownerName: String = "",
  val lastBackupTimestamp: Long = 0L,
)

class ShopPreferences(private val context: Context) {

  private object Keys {
    val SHOP_NAME = stringPreferencesKey("shop_name")
    val OWNER_NAME = stringPreferencesKey("owner_name")
    val LAST_BACKUP_TS = longPreferencesKey("last_backup_timestamp")
  }

  val shopProfileFlow: Flow<ShopProfile> =
    context.shopDataStore.data.map { prefs ->
      ShopProfile(
        shopName = prefs[Keys.SHOP_NAME]?.takeIf { it.isNotBlank() } ?: "আমার দোকান",
        ownerName = prefs[Keys.OWNER_NAME] ?: "",
        lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TS] ?: 0L,
      )
    }

  suspend fun getShopProfileOnce(): ShopProfile = shopProfileFlow.first()

  suspend fun updateShopProfile(shopName: String, ownerName: String) {
    context.shopDataStore.edit { prefs ->
      prefs[Keys.SHOP_NAME] = shopName.trim().ifBlank { "আমার দোকান" }
      prefs[Keys.OWNER_NAME] = ownerName.trim()
    }
  }

  suspend fun updateLastBackupTimestamp(timestamp: Long) {
    context.shopDataStore.edit { prefs -> prefs[Keys.LAST_BACKUP_TS] = timestamp }
  }
}
