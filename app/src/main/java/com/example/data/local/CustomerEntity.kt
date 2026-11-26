package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val phone: String = "",
  val address: String = "",
  val createdAt: Long = System.currentTimeMillis(),
)

data class CustomerWithSummary(
  val id: Long,
  val name: String,
  val phone: String,
  val address: String,
  val createdAt: Long,
  val totalBaki: Double,
  val totalJoma: Double,
  val transactionCount: Int,
  val lastTransactionAt: Long?,
) {
  val netDue: Double
    get() = totalBaki - totalJoma
}
