package com.example.butigersandbloompos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["sale_id"]),
        Index(value = ["product_id"])
    ]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    @ColumnInfo(name = "product_id") val productId: Long? = null,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "product_variant") val productVariant: String = "",
    val quantity: Double,
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    val subtotal: Double = 0.0
)
