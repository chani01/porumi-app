package com.chani.porumi

import android.app.Application
import com.chani.porumi.di.initKoin

class PorumiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
