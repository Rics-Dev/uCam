package com.ricsdev.uconnect.data.repository

import com.ricsdev.uconnect.domain.repository.MainRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay

class MainRepositoryImpl(
    private val httpClient:  HttpClient
) : MainRepository {
    override suspend fun getData(): String {
        delay(2000)

        return "Real data"
    }

}