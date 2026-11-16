package com.senghol.knotx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions_entity ORDER BY createdAt DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions_entity WHERE paymentId = :paymentId LIMIT 1")
    suspend fun getByPaymentId(paymentId: String): TransactionEntity?

    @Query("SELECT SUM(amount) FROM transactions_entity WHERE status = 'SUCCESS'")
    suspend fun getTotalSuccessfulAmount(): Long?

    @Query("DELETE FROM transactions_entity")
    suspend fun clearAll()
    }