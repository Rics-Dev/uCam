package com.ricsdev.ucam

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform