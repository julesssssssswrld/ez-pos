package com.example.butigersandbloompos

import android.app.Application
import com.example.butigersandbloompos.data.SoapDatabase
import com.example.butigersandbloompos.data.SoapRepository

class SoapApp : Application() {
    val database by lazy { SoapDatabase.getDatabase(this) }
    val repository by lazy {
        SoapRepository(database.productDao(), database.saleDao(), database.inventoryLogDao())
    }
}
