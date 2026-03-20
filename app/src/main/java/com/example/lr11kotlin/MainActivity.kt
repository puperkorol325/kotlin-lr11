package com.example.lr11kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.lr11kotlin.ui.theme.Lr11kotlinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// SRP: MainActivity теперь отвечает ТОЛЬКО за жизненный цикл
class MainActivity : ComponentActivity() {

    // DIP: Зависимость от абстракции
    private val dependencies: PostRepository by lazy {
        PostDependencies().repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lr11kotlinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PostListPagingScreen(
                        postsFlow = dependencies.getPostsFlow(),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

interface PostApi {
    @GET("posts")
    suspend fun getPosts(
        @Query("_page") page: Int = 1,
        @Query("_limit") limit: Int = 20
    ): List<Post>
}

interface PostDataSource {
    suspend fun getPosts(page: Int, limit: Int): List<Post>
}

class RemotePostDataSource(
    private val api: PostApi
) : PostDataSource {
    override suspend fun getPosts(page: Int, limit: Int): List<Post> {
        return api.getPosts(page = page, limit = limit)
    }
}

interface PostRepository {
    fun getPostsFlow(): Flow<PagingData<Post>>
}

class RemotePostRepository(
    private val pagingSourceFactory: () -> PagingSource<Int, Post>
) : PostRepository {
    override fun getPostsFlow(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow
    }
}

class PostPagingSource(
    private val dataSource: PostDataSource
) : PagingSource<Int, Post>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        return try {
            val page = params.key ?: 1
            val response = dataSource.getPosts(page = page, limit = params.loadSize)
            LoadResult.Page(
                data = response,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.size == params.loadSize) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { pos ->
            state.closestPageToPosition(pos)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
        }
    }
}

@Composable
fun PostListPagingScreen(
    postsFlow: Flow<PagingData<Post>>,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    val posts = postsFlow.collectAsLazyPagingItems()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        when (val refresh = posts.loadState.refresh) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is LoadState.Error -> {
                item {
                    ErrorItem(
                        message = (refresh as LoadState.Error).error.message ?: "Ошибка загрузки",
                        onRetry = { posts.retry() }
                    )
                }
            }
            else -> {}
        }

        items(
            count = posts.itemCount,
            key = { index -> posts[index]?.id ?: index }
        ) { index ->
            val post = posts[index]
            if (post != null) {
                PostItem(post = post)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }


        when (val append = posts.loadState.append) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is LoadState.Error -> {
                item {
                    ErrorItem(
                        message = (append as LoadState.Error).error.message ?: "Ошибка загрузки",
                        onRetry = { posts.retry() }
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun PostItem(post: Post) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ErrorItem(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}