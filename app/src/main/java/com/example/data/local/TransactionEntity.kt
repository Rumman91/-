package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
  BAKI,
  JOMA,
}

@Entity(
  tableName = "transactions",
  foreignKeys =
    [
      ForeignKey(
        entity = CustomerEntity::class,
        parentColumns = ["id"],
        childColumns = ["customerId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index(value = ["customerId"]), Index(value = ["timestamp"])],
)
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val customerId: Long,
  val type: String,
  val amount: Double,
  val note: String = "",
  val timestamp: Long = System.currentTimeMillis(),
)

data class TransactionWithCustomer(
  val id: Long,
  val customerId: Long,
  val customerName: String,
  val customerPhone: String,
  val type: String,
  val amount: Double,
  val note: String,
  val timestamp: Long,
)
