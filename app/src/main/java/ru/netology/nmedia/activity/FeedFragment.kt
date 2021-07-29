package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedState
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = PostsAdapter(object : OnInteractionListener {

            override fun onEdit(post: Post) {
                viewModel.edit(post)
                val bundle = Bundle()
                bundle.putParcelable("post",post)
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment, bundle)
                viewModel.removeById(post.id)
                viewModel.refreshPosts()
            }

            override fun onLike(post: Post) {
                viewModel.likeById(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }


            override fun onShowImage(post:Post){
                    val bundle = Bundle()
                    bundle.putString("postImage", post.attachment?.url)
                    findNavController().navigate(R.id.action_feedFragment_to_showImageFragment, bundle)
            }
        })

        binding.list.adapter = adapter

        viewModel.dataState.observe(viewLifecycleOwner){state ->
            when(state){
                FeedState.Loading -> {
                    binding.progress.isVisible = true
                    binding.errorGroup.isVisible = false
                    binding.list.isVisible = false
                }
                FeedState.Error -> {
                    binding.progress.isVisible = false
                    binding.errorGroup.isVisible = true
                    binding.errorMessage.text = viewModel.errorMessage
                    binding.list.isVisible = false
                }
                FeedState.Refreshing, FeedState.Success ->{
                    binding.progress.isVisible = false
                    binding.errorGroup.isVisible = false
                    binding.list.isVisible = true
                    binding.newsButton.isVisible = false
                }
            }
        }

        viewModel.data.observe(viewLifecycleOwner) {state ->
            adapter.submitList(state.posts)
            binding.emptyText.isVisible = state.empty
        }

        binding.retryButton.setOnClickListener {
            viewModel.loadPosts()

        }

        binding.fab.setOnClickListener {
            val bundle = Bundle()
            bundle.putParcelable("post",viewModel.empty)
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment, bundle)
        }

        binding.newsButton.setOnClickListener {
            viewModel.showNews()
            binding.newsButton.isVisible = false
            viewModel.clearCountNews()
            viewModel.loadPosts()
        }

        viewModel.newer.observe(viewLifecycleOwner){
           if(viewModel.countNewPosts > 0){
               binding.newsButton.text ="${viewModel.countNewPosts} new posts"
               binding.newsButton.isVisible = true
           }
        }

        return binding.root
    }
}
