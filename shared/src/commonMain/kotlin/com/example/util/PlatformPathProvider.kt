package com.example.util

interface PlatformPathProvider {
    fun getAppDataDirectory(): String
    fun getDatabasePath(): String
}

expect fun getPlatformPathProvider(): PlatformPathProvider
