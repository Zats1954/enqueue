package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.AttachmentType

@Entity
data class PostWorkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
    var newPost: Boolean = true,
    val ownedByMe: Boolean = false,
    val serverId: Boolean = false,
    @Embedded
    var attachment: AttachmentEmbeddable?,
    var uri: String? = null,
) {
    fun toDto() = Post(
        id, authorId, author, authorAvatar, content, published, likedByMe, likes, newPost,
        ownedByMe, serverId, attachment?.toDto(),
    )

    companion object {
        fun fromDto(dto: Post) =
            PostWorkEntity(
                0L,
                dto.authorId,
                dto.author,
                dto.authorAvatar,
                dto.content,
                dto.published,
                dto.likedByMe,
                dto.likes,
                dto.newPost,
                dto.ownedByMe,
                dto.serverId,
                AttachmentEmbeddable.fromDto(dto.attachment)
            )
    }
}
