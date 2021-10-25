package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardAdBinding
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.model.AdModel
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.PostModel
import ru.netology.nmedia.view.load
import java.text.SimpleDateFormat

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
    fun onShowImage(post: Post) {}
    fun onAdClick(adModel: AdModel) {}
}

class PostsAdapter(private val onInteractionListener: OnInteractionListener) :
    PagingDataAdapter<FeedModel, RecyclerView.ViewHolder>(PostDiffCallback()) {

    override fun getItemViewType(position: Int): Int =
        when(getItem(position)){
            is AdModel   -> R.layout.card_ad
            is PostModel -> R.layout.card_post
            null -> error("Unknown tyoe at $position")
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when(viewType){
            R.layout.card_ad -> {
                val binding = CardAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AdViewHolder(binding, onInteractionListener)
            }
            R.layout.card_post -> {
                val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PostViewHolder(binding, onInteractionListener)
            }
            else -> error("Unknown viewType  $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
            is AdViewHolder -> {
                val item = getItem(position) as AdModel
                holder.bind(item)}
            is PostViewHolder -> {
                val item = getItem(position) as PostModel
                holder.bind(item.post)}
        }
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {
        binding.apply {
            menu.isVisible = post.ownedByMe
            author.text = "${post.author} + ${post.id}"
            val pattern = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
            published.text = pattern.format(post.published)
            content.text = post.content
            card.isVisible = !post.newPost
            imageConnect.isVisible = post.serverId
            when(post.attachment?.type){
                AttachmentType.IMAGE ->
                    Glide.with(imageView)
                    .load(BuildConfig.BASE_URL + "/media/" + post.attachment.url)
                    .placeholder(R.drawable.ic_camera_24dp)
                    .error(R.drawable.ic_error_100dp)
                    .override(300, 200)
                    .timeout(10_000)
                    .into(imageView)
            }

            imageView.isVisible = post.attachment?.type == AttachmentType.IMAGE

            Glide.with(avatar)
                .load(BuildConfig.BASE_URL + "/avatars/" + post.authorAvatar)
                .circleCrop()
                .placeholder(R.drawable.ic_loading_100dp)
                .error(R.drawable.ic_error_100dp)
                .timeout(10_000)
                .into(avatar)
            // в адаптере
            like.isActivated = post.serverId
            like.isChecked = post.likedByMe
            like.text = "${post.likes}"

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }

            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }

            imageView.setOnClickListener {
                onInteractionListener.onShowImage(post)
            }
        }
    }
}

class AdViewHolder(
    private val binding: CardAdBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(ad: AdModel) {
        binding.apply {
//            image.load("${BuildConfig.BASE_URL}/media/${ad.picture}")
            Glide.with(image)
                .load("${BuildConfig.BASE_URL}/media/${ad.picture}")
                .placeholder(R.drawable.ic_loading_100dp)
                .error(R.drawable.ic_error_100dp)
                .timeout(10_000)
                .into(image)
            image.setOnClickListener {
                onInteractionListener.onAdClick(ad)
            }
        }
    }
}


class PostDiffCallback : DiffUtil.ItemCallback<FeedModel>() {
    override fun areItemsTheSame(oldItem: FeedModel, newItem: FeedModel): Boolean {
        if(oldItem.javaClass != newItem.javaClass){
            return false
        }
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeedModel, newItem: FeedModel): Boolean {
        return oldItem == newItem
    }
}
