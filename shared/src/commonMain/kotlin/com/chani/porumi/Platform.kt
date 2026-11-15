package com.chani.porumi

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform