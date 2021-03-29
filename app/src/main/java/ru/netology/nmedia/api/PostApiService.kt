package ru.netology.nmedia.api


import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.*
import ru.netology.nmedia.BuildConfig
//import okhttp3.Interceptor
//import ru.netology.nmedia.api.PostApi.codeResponse
//import retrofit2.Response
import ru.netology.nmedia.dto.Post
import java.util.concurrent.TimeUnit

//private const val BASE_URL = "http://10.0.2.2:9999"
private const val BASE_URL = "http://192.168.0.136:9999"

private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
// !!!         Хотел перехватить ошибки сервера здесь, но что-то не получилось
//    .addInterceptor { chain ->
//         chain.proceed(chain.request().newBuilder().build()).let{
//             if (it.code != 200)
//                 codeResponse = it.code
//             it
//         }
//    }
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BuildConfig.BASE_URL)
    .client(client)
    .build()

interface PostApiService {
    @GET("api/posts")
    fun getAll(): Call <List<Post>>

    @POST("api/posts")
    fun save(@Body post: Post): Call <Post>

    @DELETE("api/posts/{id}")
    fun removeById(@Path("id") id: Long): Call<Unit>

    @POST("api/posts/{id}/likes")
    fun likeById(@Path("id") id: Long): Call<Post>
}

object PostApi {
    val service by lazy <PostApiService> ( retrofit::create )
//    var codeResponse = 0
}
