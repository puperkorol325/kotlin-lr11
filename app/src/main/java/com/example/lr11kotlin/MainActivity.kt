package com.example.lr11kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lr11kotlin.ui.theme.Lr11kotlinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lr11kotlinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PostListScreen(Modifier.padding(innerPadding))
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

object RetrofitClient {
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/";

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val postApi: PostApi = retrofit.create(PostApi::class.java)
}

@Composable
fun PostListScreen(modifier: Modifier) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val list = withContext(Dispatchers.IO) {
                RetrofitClient.postApi.getPosts(page = 1, limit = 20)
            }
            posts = list
        } catch (e: Exception) {
            errorMessage = e.message ?: "Ошибка загрузки"
        } finally {
            isLoading = false
        }
    }
    when {
        isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
            {
                CircularProgressIndicator()
            }
        }
        errorMessage != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
            {
                Text(text = errorMessage!!, color =
                    MaterialTheme.colorScheme.error)
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    PostItem(post = post)
                }
            }
        }
    }
}
@Composable
private fun PostItem(post: Post) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor =
            MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text = post.title, style =
                MaterialTheme.typography.titleMedium)
            Text(text = post.body, style =
                MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}