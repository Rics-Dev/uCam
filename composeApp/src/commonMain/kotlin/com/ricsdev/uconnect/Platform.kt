package com.ricsdev.uconnect

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform