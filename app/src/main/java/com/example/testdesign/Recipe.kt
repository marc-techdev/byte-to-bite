package com.example.testdesign

data class Recipe(
    val id: Int,
    val title: String,
    val subtitle: String,
    val cookingTime: String,
    val servings: String,
    val difficulty: String,
    val imageUrl: String? = null,
    var isFavorite: Boolean = false,
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val description: String = "",
    val region: String? = null
)