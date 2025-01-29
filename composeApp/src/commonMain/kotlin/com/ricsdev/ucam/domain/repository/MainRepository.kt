package com.ricsdev.ucam.domain.repository

interface MainRepository {
    suspend fun getData(): String
}