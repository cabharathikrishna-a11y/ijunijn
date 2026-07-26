package com.example.util

class WasmPathProvider : PlatformPathProvider {
    override fun getAppDataDirectory(): String = "/localStorage/appData"
    override fun getDatabasePath(): String = "/localStorage/lifeos.db"
}

actual fun getPlatformPathProvider(): PlatformPathProvider = WasmPathProvider()
