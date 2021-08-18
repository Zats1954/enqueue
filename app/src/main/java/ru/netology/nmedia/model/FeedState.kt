package ru.netology.nmedia.model

import ru.netology.nmedia.dto.Post

sealed class FeedState {
    object Loading : FeedState()
    object Error : FeedState()
    object Refreshing : FeedState()
    object Success : FeedState()
}

data class FeedModel(
    val posts: List<Post> = emptyList(),
    val empty: Boolean = false,
    val message: String = ""
)