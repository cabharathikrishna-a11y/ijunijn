package com.example.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun createHttpClient(): HttpClient = HttpClient(Js)
