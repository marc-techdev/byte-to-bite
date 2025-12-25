package com.example.testdesign.tutorial

import androidx.annotation.RawRes

sealed class VideoSource {
    data class RawResSource(
        @RawRes val videoResId: Int,
        @RawRes val subtitleResId: Int? = null
    ) : VideoSource()

    data class UrlSource(
        val url: String,
        val subtitleUrl: String? = null
    ) : VideoSource()

    object None : VideoSource()
}
