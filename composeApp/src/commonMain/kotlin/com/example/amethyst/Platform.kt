package com.example.amethyst

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform