package com.senghol.knotx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "transactions_entity")
data class TransactionEntity(

    @PrimaryKey
    val paymentId: String,

    val orderId: String?,
    val signature: String?,

    val amount: Long, // in paise
    val currency: String = "INR",

    val method: String?, // ✅ ADD THIS

    val status: String = "SUCCESS", // ✅ default safe

    val userId: String?,

    val createdAt: Long // store as epoch millis
)