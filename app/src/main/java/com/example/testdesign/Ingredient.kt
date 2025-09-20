package com.example.testdesign

data class Ingredient(
    val id: Int,
    val name: String,
    val imageName: String,
    val confidence: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis()
)