package ru.netology.nmedia.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {
    private val channelId = "remote"
    private val gson = Gson()

    @Inject
    lateinit var auth: AppAuth

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name)
            val descriptionText = getString(R.string.channel_remote_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

    }

    override fun onMessageReceived(message: RemoteMessage) {
        message.data["content"]?.let {
            val info = gson.fromJson(it, Info::class.java)
            info.recipientId?.let { id ->
                if (id != auth.authStateFlow.value.id) {
                    auth.sendPushToken()
                } else {
                    handleMessage(gson.fromJson(message.data["content"], Info::class.java))
                }
            }
                ?: handleMessage(gson.fromJson(message.data["content"], Info::class.java))
        }
    }

    private fun handleMessage(info: Info?) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("NMedia server notification")
            .setContentText(info?.content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(100_000), notification)
    }

    override fun onNewToken(token: String) {
        println("FCM token")
        println(token)
        auth.sendPushToken(token)
    }
}

data class Info(
    val recipientId: Long?,
    val content: String
)

