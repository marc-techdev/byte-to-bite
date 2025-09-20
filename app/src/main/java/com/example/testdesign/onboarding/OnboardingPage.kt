package com.example.testdesign.onboarding

import androidx.annotation.DrawableRes
import java.io.Serializable

/** What kind of page this is (drives which sections show). */
enum class OnbType : Serializable {
    WELCOME, HOW, TIPS, INGREDIENTS, PRIVACY
}

/** Model for one onboarding screen. */
data class OnboardingPage(
    val type: OnbType,
    @DrawableRes val imageRes: Int = 0,
    val title: String,
    val body: String,
    val bullets: List<Bullet> = emptyList(),
    val showTipsCta: Boolean = false,
    val isIngredientList: Boolean = false
) : Serializable {

    data class Bullet(
        @DrawableRes val iconRes: Int,
        val text: String
    ) : Serializable
}
