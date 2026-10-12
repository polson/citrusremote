package com.reggiesoft.citrusremote.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.reggiesoft.citrusremote.data.model.MockAppleTv
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAppleTvPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val serverAddress: String
        get() = preferences.getString(SERVER_ADDRESS_KEY, DEFAULT_SERVER_ADDRESS)
            ?: DEFAULT_SERVER_ADDRESS

    fun setServerAddress(address: String) {
        preferences.edit().putString(SERVER_ADDRESS_KEY, address).apply()
    }
}

private const val PREFERENCES_NAME = "AppleTVPrefs"
private const val SERVER_ADDRESS_KEY = "mock_server_address"
private const val DEFAULT_SERVER_ADDRESS = MockAppleTv.DEVICE_ID
