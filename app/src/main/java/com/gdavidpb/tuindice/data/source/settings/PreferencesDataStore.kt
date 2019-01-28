package com.gdavidpb.tuindice.data.source.settings

import android.content.SharedPreferences
import com.gdavidpb.tuindice.domain.repository.SettingsRepository
import com.gdavidpb.tuindice.utils.*
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
            putBoolean(KEY_IS_AWAITING_FOR_RESET, true)
        }
    }

    override suspend fun setIsAwaitingForVerify(email: String) {
        preferences.edit {
            putString(KEY_AWAITING_EMAIL, email)
            putBoolean(KEY_IS_AWAITING_FOR_VERIFY, true)
        }
    }

    override suspend fun isAwaitingForReset(): Boolean {
        return preferences.getBoolean(KEY_IS_AWAITING_FOR_RESET, false)
    }

    override suspend fun isAwaitingForVerify(): Boolean {
        return preferences.getBoolean(KEY_IS_AWAITING_FOR_VERIFY, false)
    }

    override suspend fun clearIsAwaitingForReset() {
        preferences.edit {
            remove(KEY_AWAITING_EMAIL)
            remove(KEY_IS_AWAITING_FOR_RESET)
        }
    }

    override suspend fun clearIsAwaitingForVerify() {
        preferences.edit {
            remove(KEY_AWAITING_EMAIL)
            remove(KEY_IS_AWAITING_FOR_VERIFY)
        }
    }

    override suspend fun setCooldown(key: String) {
        val calendar = Calendar.getInstance(DEFAULT_LOCALE)

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        calendar.add(Calendar.DATE, 1)

        preferences.edit {
            putLong("$key$KEY_COOL_DOWN", calendar.timeInMillis)
        }
    }

    override suspend fun isCooldown(key: String): Boolean {
        val now = Calendar.getInstance(DEFAULT_LOCALE)
        val cooldown = Calendar.getInstance(DEFAULT_LOCALE)

        cooldown.timeInMillis = preferences.getLong("$key$KEY_COOL_DOWN", now.timeInMillis)

        return now.before(cooldown)
    }

    override suspend fun isFirstRun(): Boolean {
        return preferences.getBoolean(KEY_FIRST_RUN, true)
    }

    override suspend fun setFirstRun() {
        return preferences.edit {
            putBoolean(KEY_FIRST_RUN, true)
        }
    }

    override suspend fun getCountdown(): Long {
        return preferences.getLong(KEY_COUNT_DOWN, 0)
    }

    override suspend fun startCountdown() {
        preferences.edit {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.MILLISECOND, TIME_COUNT_DOWN)
            }

            putLong(KEY_COUNT_DOWN, calendar.timeInMillis)
        }
    }

    override suspend fun clearCountdown() {
        preferences.edit {
            remove(KEY_COUNT_DOWN)
        }
    }

    override suspend fun clear() {
        preferences.edit {
            clear()
        }
    }
}