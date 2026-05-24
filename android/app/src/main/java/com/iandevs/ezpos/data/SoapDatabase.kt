package com.iandevs.ezpos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.iandevs.ezpos.data.dao.InventoryLogDao
import com.iandevs.ezpos.data.dao.ProductDao
import com.iandevs.ezpos.data.dao.SaleDao
import com.iandevs.ezpos.data.entity.InventoryLog
import com.iandevs.ezpos.data.entity.Product
import com.iandevs.ezpos.data.entity.Sale
import com.iandevs.ezpos.data.entity.SaleItem

@Database(
    entities = [Product::class, Sale::class, SaleItem::class, InventoryLog::class],
    version = 5,
    exportSchema = false
)
abstract class SoapDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun inventoryLogDao(): InventoryLogDao

    companion object {
        @Volatile
        private var INSTANCE: SoapDatabase? = null

        fun getDatabase(context: Context): SoapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoapDatabase::class.java,
                    "soap.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
