package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TallyDao {

  @Query(
    """
    SELECT
      c.id,
      c.name,
      c.phone,
      c.address,
      c.createdAt,
      COALESCE(SUM(CASE WHEN t.type = 'BAKI' THEN t.amount ELSE 0.0 END), 0.0) AS totalBaki,
      COALESCE(SUM(CASE WHEN t.type = 'JOMA' THEN t.amount ELSE 0.0 END), 0.0) AS totalJoma,
      COUNT(t.id) AS transactionCount,
      MAX(t.timestamp) AS lastTransactionAt
    FROM customers c
    LEFT JOIN transactions t ON c.id = t.customerId
    GROUP BY c.id
    ORDER BY COALESCE(MAX(t.timestamp), c.createdAt) DESC, c.id DESC
    """
  )
  fun observeCustomerSummaries(): Flow<List<CustomerWithSummary>>

  @Query(
    """
    SELECT
      c.id,
      c.name,
      c.phone,
      c.address,
      c.createdAt,
      COALESCE(SUM(CASE WHEN t.type = 'BAKI' THEN t.amount ELSE 0.0 END), 0.0) AS totalBaki,
      COALESCE(SUM(CASE WHEN t.type = 'JOMA' THEN t.amount ELSE 0.0 END), 0.0) AS totalJoma,
      COUNT(t.id) AS transactionCount,
      MAX(t.timestamp) AS lastTransactionAt
    FROM customers c
    LEFT JOIN transactions t ON c.id = t.customerId
    WHERE c.id = :customerId
    GROUP BY c.id
    """
  )
  fun observeCustomerSummary(customerId: Long): Flow<CustomerWithSummary?>

  @Query(
    """
    SELECT
      t.id,
      t.customerId,
      c.name AS customerName,
      c.phone AS customerPhone,
      t.type,
      t.amount,
      t.note,
      t.timestamp
    FROM transactions t
    INNER JOIN customers c ON t.customerId = c.id
    ORDER BY t.timestamp DESC, t.id DESC
    """
  )
  fun observeAllTransactionsWithCustomer(): Flow<List<TransactionWithCustomer>>

  @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC, id DESC")
  fun observeCustomerTransactions(customerId: Long): Flow<List<TransactionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomer(customer: CustomerEntity): Long

  @Update suspend fun updateCustomer(customer: CustomerEntity)

  @Query("DELETE FROM customers WHERE id = :customerId")
  suspend fun deleteCustomerById(customerId: Long)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransaction(transaction: TransactionEntity): Long

  @Update suspend fun updateTransaction(transaction: TransactionEntity)

  @Query("DELETE FROM transactions WHERE id = :transactionId")
  suspend fun deleteTransactionById(transactionId: Long)

  @Query("SELECT * FROM customers ORDER BY id ASC")
  suspend fun getAllCustomersOnce(): List<CustomerEntity>

  @Query("SELECT * FROM transactions ORDER BY id ASC")
  suspend fun getAllTransactionsOnce(): List<TransactionEntity>

  @Query("DELETE FROM transactions") suspend fun deleteAllTransactions()

  @Query("DELETE FROM customers") suspend fun deleteAllCustomers()

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomers(customers: List<CustomerEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTransactions(transactions: List<TransactionEntity>)

  @Transaction
  suspend fun replaceAllData(
    customers: List<CustomerEntity>,
    transactions: List<TransactionEntity>,
  ) {
    deleteAllTransactions()
    deleteAllCustomers()
    if (customers.isNotEmpty()) {
      insertCustomers(customers)
    }
    if (transactions.isNotEmpty()) {
      insertTransactions(transactions)
    }
  }
}
