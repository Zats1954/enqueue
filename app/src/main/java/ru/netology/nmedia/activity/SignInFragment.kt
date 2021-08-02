package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.di.DependencyContainer
import ru.netology.nmedia.model.FeedState
import ru.netology.nmedia.util.AndroidUtils

import ru.netology.nmedia.viewmodel.AuthViewModel

class SignInFragment: Fragment() {

    private val viewModel: AuthViewModel by viewModels(
        ownerProducer = ::requireParentFragment,
        factoryProducer = {
            DependencyContainer.getInstance(requireContext().applicationContext).viewModelFactory
        })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSignInBinding.inflate(inflater, container, false)
        binding.button.setOnClickListener {
            viewModel.signUser(binding.login.text.toString(), binding.pass.text.toString())
            viewModel.save()
            AndroidUtils.hideKeyboard(requireView())
        }

        viewModel.authCreated.observe(viewLifecycleOwner) {
            if(viewModel.dataState.value == FeedState.Error){
                Snackbar.make(binding.root , "${R.string.authError}", Snackbar.LENGTH_LONG).show()
            }
            viewModel.loadPosts()
            findNavController().navigateUp()
        }

        return binding.root
    }
}
