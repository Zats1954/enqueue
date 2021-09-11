package ru.netology.nmedia.dto

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.netology.nmedia.enumeration.AttachmentType

@Parcelize
data class Post(
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val newPost: Boolean = true,
    val ownedByMe: Boolean = false,
    val serverId: Boolean = true,
    val attachment: Attachment? = null
) : Parcelable

@Parcelize
data class Attachment(
    val url: String,
    val type: AttachmentType,
) : Parcelable