package ru.netology.nmedia.api

import android.content.SharedPreferences
import androidx.core.content.edit

private val KEY_TOKEN = "token"
private val KEY_ID = "id"

var SharedPreferences.token: String?
get() = getString(KEY_TOKEN, null)
set(value) {
    edit {
        putString(KEY_TOKEN, value)
    }
}

var SharedPreferences.id: Long
    get() = getLong(KEY_ID, 0L)
    set(value) {
        edit {
            putLong(KEY_ID, value)
        }
    }