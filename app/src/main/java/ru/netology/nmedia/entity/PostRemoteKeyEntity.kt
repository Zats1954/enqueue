package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PostRemoteKeyEntity(
    @PrimaryKey
    val type:Type,
    val postId: Long,

) {

    enum class Type{
        PREPEND,
        APPEND,
    }

}