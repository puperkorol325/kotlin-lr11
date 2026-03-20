package com.example.lr11kotlin

import androidx.paging.PagingSource
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// DIP: Зависим от абстракций, а не от реализаций
// SRP: PostDependencies отвечает только за создание графа зависимостей
class PostDependencies {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/todos/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val postApi: PostApi by lazy { retrofit.create(PostApi::class.java) }

    // DIP: Возвращаем интерфейс, а не конкретный класс
    val dataSource: PostDataSource by lazy { RemotePostDataSource(postApi) }

    val pagingSourceFactory: () -> PagingSource<Int, Post> by lazy {
        { PostPagingSource(dataSource) }
    }

    // DIP: Возвращаем интерфейс репозитория
    val repository: PostRepository by lazy { RemotePostRepository(pagingSourceFactory) }
}