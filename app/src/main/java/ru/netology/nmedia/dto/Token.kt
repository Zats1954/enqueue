package ru.netology.nmedia.dto

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Token(
    val id: Long,
    val token: String
) : Parcelable


