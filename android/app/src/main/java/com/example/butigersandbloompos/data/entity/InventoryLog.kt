package com.example.butigersandbloompos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// No FK constraint — inventory_log is an append-only audit trail.
// Removing FK avoids SQLITE_CONSTRAINT (787) errors on product deletion.
@Entity(
    tableName = "inventory_log",
    indices = [Index(value = ["product_id"]), Index(value = ["created_at"])]
)
data class InventoryLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "product_id") val productId: Long? = null,
    @ColumnInfo(name = "product_name") val productName: String,
    val action: String, // ADDED, EDITED, SOLD, RESTOCKED, DELETED
    @ColumnInfo(name = "quantity_change") val quantityChange: Double = 0.0,
    @ColumnInfo(name = "stock_before") val stockBefore: Double = 0.0,
    @ColumnInfo(name = "stock_after") val stockAfter: Double = 0.0,
    val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: String = ""
)
