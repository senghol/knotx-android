package com.senghol.knotx.data.sqldelight

import android.content.Context

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.senghol.knotx.data.sqldelight.SengholDB

class DatabaseDriverFactory(private val context: Context) {
    fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = SengholDB.Schema,
            context = context,
            name = "sqldelight_dootawas.db"
        )
    }

}