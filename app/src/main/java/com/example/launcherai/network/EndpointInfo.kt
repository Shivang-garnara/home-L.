package com.example.launcherai.network

data class EndpointInfo(
    val category: String,
    val method: String,
    val url: String,
    val description: String,
    val details: String,
    val status: String = "Active (Verified)"
)
