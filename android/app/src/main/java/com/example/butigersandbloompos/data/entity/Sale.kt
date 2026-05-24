package com.example.butigersandbloompos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "total_amount") val totalAmount: Double = 0.0,
    @ColumnInfo(name = "cash_tendered") val cashTendered: Double = 0.0,
    @ColumnInfo(name = "change_amount") val changeAmount: Double = 0.0,
    @ColumnInfo(name = "item_count") val itemCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: String = ""
)
