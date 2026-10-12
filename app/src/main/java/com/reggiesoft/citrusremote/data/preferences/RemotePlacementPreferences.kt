package com.reggiesoft.citrusremote.data.preferences

import android.content.Context
import com.reggiesoft.citrusremote.data.model.DEFAULT_REMOTE_WIDTH
import com.reggiesoft.citrusremote.data.model.RemotePlacement

class RemotePlacementPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): RemotePlacement {
        return RemotePlacement(
            horizontalPosition = preferences.getFloat(HORIZONTAL_POSITION_KEY, 0.5f),
            verticalPosition = preferences.getFloat(VERTICAL_POSITION_KEY, 0f),
            width = preferences.getFloat(WIDTH_KEY, DEFAULT_REMOTE_WIDTH)
        )
    }

    fun save(placement: RemotePlacement) {
        preferences.edit()
            .putFloat(HORIZONTAL_POSITION_KEY, placement.horizontalPosition)
            .putFloat(VERTICAL_POSITION_KEY, placement.verticalPosition)
            .putFloat(WIDTH_KEY, placement.width)
            .apply()
    }
}

private const val PREFERENCES_NAME = "RemotePlacement"
private const val HORIZONTAL_POSITION_KEY = "horizontal_position"
private const val VERTICAL_POSITION_KEY = "vertical_position"
private const val WIDTH_KEY = "width"
