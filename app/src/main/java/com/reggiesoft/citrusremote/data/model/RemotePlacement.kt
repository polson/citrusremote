package com.reggiesoft.citrusremote.data.model

data class RemotePlacement(
    val horizontalPosition: Float = 0.5f,
    val verticalPosition: Float = 0f,
    val width: Float = DEFAULT_REMOTE_WIDTH
)

internal const val DEFAULT_REMOTE_WIDTH = 228f
internal const val MINIMUM_REMOTE_WIDTH = 208f
