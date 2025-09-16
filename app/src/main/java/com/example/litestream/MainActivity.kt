package com.example.litestream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jsoup.Jsoup
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiteStreamApp()
        }
    }
}

@Composable
fun LiteStreamApp() {
    var searchQuery by remember { mutableStateOf("") }
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LiteStream", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                backgroundColor = Color.Black,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari film...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    if (searchQuery.isBlank()) return@Button
                    isLoading = true
                    loadMovies(searchQuery) { results ->
                        movies = results
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Cari")
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            LazyColumn {
                items(movies) { movie ->
                    MovieItem(movie.title, movie.url, movie.source)
                }
            }
        }
    }
}

data class Movie(val title: String, val url: String, val source: String)

@Composable
fun MovieItem(title: String, url: String, source: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Gray)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = source[0].toString().uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(text = title, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(text = "($source)", color = Color.LightGray, fontSize = 12.sp)
            }
        }
    }
}

suspend fun loadMovies(query: String, onSuccess: (List<Movie>) -> Unit) {
    val encodedQuery = URLEncoder.encode(query, "UTF-8")
    val sources = listOf(
        "https://123movie.so",
        "https://fmovies.to",
        "https://putlocker.is",
        "https://gostream.is"
    )

    val results = mutableListOf<Movie>()

    for (source in sources) {
        try {
            val doc = Jsoup.connect("$source/search?q=$encodedQuery")
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .get()

            doc.select("div.movie-item a").forEach { link ->
                val title = link.text().trim()
                val href = link.attr("abs:href")
                if (title.isNotEmpty() && href.contains("/movie/") && !href.contains("trailer")) {
                    results.add(Movie(title, href, source.substringAfterLast("/")))
                }
            }
        } catch (e: Exception) {}
    }

    onSuccess(results.distinctBy { it.title }.take(20))
}
