package ru.netology.nmedia.api


import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.*
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.dto.Post
import java.util.concurrent.TimeUnit

private const val BASE_URL = "http://10.0.2.2:9999"
//private const val BASE_URL = "http://192.168.0.136:9999"

private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BuildConfig.BASE_URL)
    .client(client)
    .build()

interface PostApiService {

    @GET("api/posts")
    suspend fun getAll():  List<Post>

    @POST("api/posts")
    suspend fun save(@Body post: Post): Post

    @DELETE("api/posts/{id}")
    suspend fun removeById(@Path("id") id: Long)

    @POST("api/posts/{id}/likes")
    suspend fun likeById(@Path("id") id: Long): Post
}

object PostsApi {
    val service by lazy <PostApiService> ( retrofit::create )
}
