package com.tv.terminal

import android.app.Application
import android.content.Context

/**
 * 应用入口类
 */
class TvApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }

    companion object {
        lateinit var context: Context
            private set
    }
}