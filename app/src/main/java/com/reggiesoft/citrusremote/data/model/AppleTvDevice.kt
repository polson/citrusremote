package com.reggiesoft.citrusremote.data.model

data class AppleTvDevice(
    val name: String,
    val address: String,
    val model: String = "Unknown"
) {
    val isAudioDevice: Boolean
        get() = model.contains("HomePod", ignoreCase = true) ||
            model.contains("Speaker", ignoreCase = true) ||
            model.contains("AirPort", ignoreCase = true)
}
