package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.google.firebase.iid.FirebaseInstanceId
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.postArg
import ru.netology.nmedia.di.DependencyContainer
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.dto.Token
import ru.netology.nmedia.viewmodel.AuthViewModel

class AppActivity : AppCompatActivity(R.layout.activity_app) {
    private val container by lazy {DependencyContainer.getInstance(application)}
    private val viewModel: AuthViewModel by viewModels(factoryProducer = {
                                container.viewModelFactory
                          })

    private val auth by lazy {container.auth}
    private var myToken: Token? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val post = it.getParcelableExtra<Post>(Intent.EXTRA_TEXT)
            if (post?.content?.isNotBlank() != true) {
                return@let
            }

            intent.removeExtra(Intent.EXTRA_TEXT)
            findNavController(R.id.nav_host_fragment)
                .navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        postArg = post
                    }
                )
        }

        viewModel.data.observe(this){
            invalidateOptionsMenu()
        }
        checkGoogleApiAvailability()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        menu?.let {
            it.setGroupVisible(R.id.unauthenticated, !viewModel.authenticated)
            it.setGroupVisible(R.id.authenticated, viewModel.authenticated)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.signin -> {
                val token = bundleOf("token" to myToken)
                findNavController(R.id.nav_host_fragment)
                    .navigate(R.id.action_feedFragment_to_signInFragment, token)
                myToken?.let{auth.setAuth(it.id, it.token)}
                true
            }
            R.id.signup -> {
                val tokenUp = Bundle()
                tokenUp.putParcelable("token", myToken)
                findNavController(R.id.nav_host_fragment).navigate(R.id.action_feedFragment_to_signUpFragment)
                myToken?.let{auth.setAuth( it.id, it.token)}
                true
            }
            R.id.signout -> {
                auth.removeAuth()
                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }


        private fun checkGoogleApiAvailability() {
        with(GoogleApiAvailability.getInstance()) {
            val code = isGooglePlayServicesAvailable(this@AppActivity)
            if (code == ConnectionResult.SUCCESS) {
                return@with
            }
            if (isUserResolvableError(code)) {
                getErrorDialog(this@AppActivity, code, 9000).show()
                return
            }
            Toast.makeText(this@AppActivity, R.string.google_play_unavailable, Toast.LENGTH_LONG)
                .show()
        }

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
            println(it.token)
        }
    }
}