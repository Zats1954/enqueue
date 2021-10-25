package ru.netology.nmedia.activity

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PagingLoadStateAdapter
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedState
import ru.netology.nmedia.util.SingleLiveEvent
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class FeedFragment : Fragment() {
    private val viewModel: PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment,
    )

    private val _needRefresh = SingleLiveEvent<Unit>()
    val needRefresh: LiveData<Unit>
        get() = _needRefresh

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
                bundle.putParcelable("post", post)
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment, bundle)
                viewModel.removeById(post.id)
                viewModel.refreshPosts()
                _needRefresh.value = Unit
            }

            override fun onLike(post: Post) {
                viewModel.likeById(post.id)
                viewModel.refreshPosts()
                _needRefresh.value = Unit
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
                _needRefresh.value = Unit
                viewModel.refreshPosts()
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


            override fun onShowImage(post: Post) {
                val bundle = Bundle()
                bundle.putString("postImage", post.attachment?.url)
                findNavController().navigate(R.id.action_feedFragment_to_showImageFragment, bundle)
            }
        })

        binding.list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PagingLoadStateAdapter(adapter::retry),
            footer = PagingLoadStateAdapter(adapter::retry),
        )

        val offsetHorizontal = resources.getDimensionPixelSize(R.dimen.common_spacing)
        binding.list.addItemDecoration(
            object: RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ){
                    outRect.left = offsetHorizontal
                    outRect.right = offsetHorizontal
                }
            })
        binding.list.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

//         val lastPost = {adapter.snapshot().items.maxOf{ it.published }}


        needRefresh.observe(viewLifecycleOwner) {
            adapter.refresh()
            binding.list.smoothScrollToPosition(0)
        }

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            when (state) {
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
                FeedState.Refreshing, FeedState.Success -> {
                    binding.progress.isVisible = false
                    binding.errorGroup.isVisible = false
                    binding.list.isVisible = true
                    binding.newsButton.isVisible = false
                }
            }
        }

        viewModel.authChanged.observe(viewLifecycleOwner) {
            _needRefresh.value = Unit
        }

        lifecycleScope.launchWhenCreated {
            viewModel.data.collectLatest { posts ->
                adapter.submitData(posts)
            }
        }
        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest { state ->
                binding.swiperefresh.isRefreshing = state.append is LoadState.Loading
                        || state.prepend is LoadState.Loading
                        || state.refresh is LoadState.Loading
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            adapter.refresh()
            binding.list.smoothScrollToPosition(0)
        }

        binding.retryButton.setOnClickListener {
            viewModel.loadPosts()

        }

        binding.fab.setOnClickListener {
            val bundle = Bundle()
            bundle.putParcelable("post", viewModel.empty)
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment, bundle)
        }

        binding.newsButton.setOnClickListener {
            viewModel.showNews()
            binding.newsButton.isVisible = false
            viewModel.loadPosts()
            adapter.refresh()
        }

//        viewModel.newer.observe(viewLifecycleOwner) {
//            viewModel.newer.value?.let {
//                if (it > 0) {
//                    binding.newsButton.text = "${it} new posts"
//                    binding.newsButton.isVisible = true
//                }
//            }
//        }
        return binding.root
    }
}
