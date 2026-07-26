package com.example.util

import java.io.File

class WindowsPathProvider : PlatformPathProvider {
    override fun getAppDataDirectory(): String {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "LifeOS")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun getDatabasePath(): String {
        val dir = File(getAppDataDirectory(), "databases")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "lifeos.db").absolutePath
    }
}

actual fun getPlatformPathProvider(): PlatformPathProvider = WindowsPathProvider()
