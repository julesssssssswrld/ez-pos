package com.iandevs.ezpos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val variant: String = "",
    val stock: Double = 0.0,
    val price: Double = 0.0,
    @ColumnInfo(name = "min_warning") val minWarning: Double = 0.0,
    val barcode: String = "",
    @ColumnInfo(name = "created_at") val createdAt: String = "",
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0
)
