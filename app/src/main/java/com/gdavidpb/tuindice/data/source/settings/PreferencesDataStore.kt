package com.gdavidpb.tuindice.data.source.settings

import android.content.SharedPreferences
import com.gdavidpb.tuindice.R
import com.gdavidpb.tuindice.domain.model.service.DstCredentials
import com.gdavidpb.tuindice.domain.repository.SettingsRepository
import com.gdavidpb.tuindice.utils.*
import com.gdavidpb.tuindice.utils.extensions.edit
import java.util.*

open class PreferencesDataStore(
        private val preferences: SharedPreferences
) : SettingsRepository {

    override suspend fun awaitingEmail(): String {
        return preferences.getString(KEY_AWAITING_EMAIL, "") ?: ""
    }

    override suspend fun awaitingPassword(): String {
        return preferences.getString(KEY_AWAITING_PASSWORD, "") ?: ""
    }

    override suspend fun setIsAwaitingForReset(email: String, password: String) {
        preferences.edit {
            putString(KEY_AWAITING_EMAIL, email)
            putString(KEY_AWAITING_PASSWORD, password)
        }
    }

    override suspend fun isAwaitingForReset(): Boolean {
        val email = preferences.getString(KEY_AWAITING_EMAIL, "") ?: ""
        val password = preferences.getString(KEY_AWAITING_PASSWORD, "") ?: ""

        return email.isNotEmpty() && password.isNotEmpty()
    }

    override suspend fun clearIsAwaitingForReset() {
        preferences.edit {
            remove(KEY_AWAITING_EMAIL)
            remove(KEY_AWAITING_PASSWORD)
        }
    }

    override suspend fun getCountdown(): Long {
        return preferences.getLong(KEY_COUNT_DOWN, 0)
    }

    override suspend fun startCountdown(): Long {
        return Calendar.getInstance().run {
            add(Calendar.MILLISECOND, TIME_COUNT_DOWN)

            preferences.edit {
                putLong(KEY_COUNT_DOWN, timeInMillis)
            }

            timeInMillis
        }
    }

    override suspend fun clearCountdown() {
        preferences.edit {
            remove(KEY_COUNT_DOWN)
        }
    }

    override suspend fun storeCredentials(credentials: DstCredentials) {
        preferences.edit {
            putString(KEY_USB_ID, credentials.usbId)
            putString(KEY_PASSWORD, credentials.password)
        }
    }

    override suspend fun getCredentials(): DstCredentials {
        return DstCredentials(
                usbId = preferences.getString(KEY_USB_ID, "") ?: "",
                password = preferences.getString(KEY_PASSWORD, "") ?: ""
        )
    }

    override suspend fun getLastScreen(): Int {
        return preferences.getInt(KEY_LAST_SCREEN, R.id.nav_summary)
    }

    override suspend fun setLastScreen(screen: Int) {
        preferences.edit {
            putInt(KEY_LAST_SCREEN, screen)
        }
    }

    override suspend fun clear() {
        preferences.edit {
            clear()
        }
    }
}