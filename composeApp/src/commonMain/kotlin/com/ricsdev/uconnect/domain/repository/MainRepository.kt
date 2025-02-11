package com.ricsdev.uconnect.domain.repository

interface MainRepository {
    suspend fun getData(): String
}