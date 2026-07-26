package com.example.util

import android.content.Context

class AndroidPathProvider(private val context: Context) : PlatformPathProvider {
    override fun getAppDataDirectory(): String = context.filesDir.absolutePath
    override fun getDatabasePath(): String = context.getDatabasePath("lifeos.db").absolutePath
}

private var appContext: Context? = null

fun initAndroidPathProvider(context: Context) {
    appContext = context.applicationContext
}

actual fun getPlatformPathProvider(): PlatformPathProvider {
    val ctx = appContext ?: throw IllegalStateException("Context not initialized for AndroidPathProvider")
    return AndroidPathProvider(ctx)
}
