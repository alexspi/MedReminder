package com.example.medreminder.data.remote

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

data class WikipediaArticle(
    val title: String,
    val body: String,
    val pageUrl: String,
)

class WikipediaReferenceDataSource(
    private val client: OkHttpClient,
) {
    suspend fun fetchArticle(query: String): WikipediaArticle = withContext(Dispatchers.IO) {
        val searchUrl = "https://ru.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "opensearch")
            .addQueryParameter("search", query)
            .addQueryParameter("limit", "1")
            .addQueryParameter("namespace", "0")
            .addQueryParameter("format", "json")
            .build()
        val searchJson = executeGet(searchUrl.toString())
        val titles = JsonParser.parseString(searchJson).asJsonArray
        if (titles.size() < 2 || titles[1].asJsonArray.size() == 0) {
            throw NoSuchElementException("В русской Википедии по запросу «$query» ничего не найдено")
        }
        val title = titles[1].asJsonArray[0].asString
        val pageUrl = if (titles.size() >= 4 && titles[3].asJsonArray.size() > 0) {
            titles[3].asJsonArray[0].asString
        } else {
            "https://ru.wikipedia.org/wiki/${URLEncoder.encode(title, Charsets.UTF_8.name())}"
        }
        val extractUrl = "https://ru.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("format", "json")
            .addQueryParameter("prop", "extracts")
            .addQueryParameter("exintro", "1")
            .addQueryParameter("explaintext", "1")
            .addQueryParameter("titles", title)
            .build()
        val extractJson = executeGet(extractUrl.toString())
        val pages = JsonParser.parseString(extractJson)
            .asJsonObject["query"]?.asJsonObject?.get("pages")?.asJsonObject
            ?: throw IOException("Не удалось разобрать ответ Википедии")
        val page = pages.entrySet().firstOrNull()?.value?.asJsonObject
            ?: throw IOException("Статья не найдена")
        if (page.has("missing")) {
            throw NoSuchElementException("Статья «$title» не найдена")
        }
        val extract = page["extract"]?.asString?.trim()
            ?: throw IOException("Пустой текст статьи")
        val body = buildString {
            append(extract.take(2500))
            if (extract.length > 2500) append("…")
        }
        WikipediaArticle(title = title, body = body.trim(), pageUrl = pageUrl)
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "MedReminder/1.0 (Android; drug-info)")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Википедия: код ${response.code}")
            }
            return response.body?.string() ?: throw IOException("Пустой ответ")
        }
    }
}
