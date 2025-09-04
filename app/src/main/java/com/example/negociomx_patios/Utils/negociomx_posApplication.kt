package com.example.negociomx_patios.Utils

import android.app.Application

class negociomx_posApplication:Application() {
    companion object{
        lateinit var prefs:Preferencias
    }

    override fun onCreate() {
        super.onCreate()

        prefs=Preferencias(applicationContext)
    }
}