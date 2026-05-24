package com.iandevs.ezpos

import android.app.Application
import com.iandevs.ezpos.data.SoapDatabase
import com.iandevs.ezpos.data.SoapRepository

class SoapApp : Application() {
    val database by lazy { SoapDatabase.getDatabase(this) }
    val repository by lazy {
        SoapRepository(database.productDao(), database.saleDao(), database.inventoryLogDao())
    }
}
