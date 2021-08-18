package ru.netology.nmedia.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.util.AndroidUtils
import ru.netology.nmedia.util.PostArg
import ru.netology.nmedia.viewmodel.PostViewModel
import java.io.File

class NewPostFragment : Fragment() {
    companion object {
        var Bundle.postArg: Post? by PostArg
    }

    private val photoRequestCode = 1
    private val cameraRequestCode = 2

    private val viewModel: PostViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    private var fragmentBinding: FragmentNewPostBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_new_post, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {
                fragmentBinding?.let {
                    arguments?.getParcelable<Post>("post")?.let { post ->
                        viewModel.changePost(post.copy(content = it.edit.text.toString()))
                    }
                    viewModel.save()
                    AndroidUtils.hideKeyboard(requireView())
                }
                true
            }
            R.id.photoM -> {
                ImagePicker.with(this)
                    .crop()
                    .compress(1024)
                    .maxResultSize(1080, 1080)
                    .galleryOnly()
                    .galleryMimeTypes(
                        arrayOf(
                            "image/png",
                            "image/jpeg",
                        )
                    )
                    .start(photoRequestCode)
                true
            }
            R.id.cameraM -> {
                ImagePicker.with(this)
                    .crop()
                    .compress(1024)
                    .cameraOnly()
                    .maxResultSize(1080, 1080)
                    .start(cameraRequestCode)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(
            inflater,
            container,
            false
        )
// заполнение полей формы для редактирования
        arguments?.getParcelable<Post>("post")?.let { post ->
            if (post.id != 0L) {
                binding.edit.setText(post.content)
                post.attachment?.let {
                    val imagePath = "${BuildConfig.BASE_URL}/media/${post.attachment?.url}"
                    viewModel.changePhoto(Uri.parse(imagePath), File(imagePath))
                } ?: viewModel.changePhoto(null, null)
            }
        }
//-------------------------------------------------------
        fragmentBinding = binding


        binding.ok.setOnClickListener {
            arguments?.getParcelable<Post>("post")?.let {
                viewModel.changePost(it.copy(content = binding.edit.text.toString()))
            }
            viewModel.save()
            AndroidUtils.hideKeyboard(requireView())
        }

        viewModel.postCreated.observe(viewLifecycleOwner) {
            viewModel.loadPosts()
            findNavController().navigateUp()
        }

        binding.removePhoto.setOnClickListener {
            viewModel.changePhoto(null, null)
            arguments?.getParcelable<Post>("post")?.let {
                viewModel.changePost(it.copy(attachment = null))
            }
        }

        viewModel.photo.observe(viewLifecycleOwner) {
            if (it.uri == null) {
                binding.photoContainer.visibility = View.GONE
                return@observe
            }
            binding.photoContainer.visibility = View.VISIBLE
            binding.photo.setImageURI(it.uri)

        }
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == ImagePicker.RESULT_ERROR) {
            fragmentBinding?.let {
                Snackbar.make(it.root, ImagePicker.getError(data), Snackbar.LENGTH_LONG).show()
            }
            return
        }
        if (resultCode == Activity.RESULT_OK && requestCode == photoRequestCode) {
            val uri: Uri? = data?.data
            val file: File? = ImagePicker.getFile(data)
            viewModel.changePhoto(uri, file)
            return
        }
        if (resultCode == Activity.RESULT_OK && requestCode == cameraRequestCode) {
            val uri: Uri? = data?.data
            val file: File? = ImagePicker.getFile(data)
            viewModel.changePhoto(uri, file)
            return
        }
    }

    override fun onDestroyView() {
        fragmentBinding = null
        super.onDestroyView()
    }
}