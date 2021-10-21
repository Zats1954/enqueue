package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentShowImageBinding


class ShowImageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentShowImageBinding.inflate(
            inflater,
            container,
            false
        )
        Glide.with(binding.imagePost)
            .load(BuildConfig.BASE_URL + "/media/" + arguments?.getString("postImage"))
            .placeholder(R.drawable.ic_camera_24dp)
            .error(R.drawable.ic_error_100dp)
            .override(2000, 2000)
            .timeout(10_000)
            .into(binding.imagePost)
        return binding.root
    }
}