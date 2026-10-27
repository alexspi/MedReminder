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
        val candidates = searchCandidates(query)
        candidates.firstOrNull()
            ?: throw NoSuchElementException("В русской Википедии по запросу «$query» ничего подходящего не найдено")
    }

    private fun searchCandidates(query: String): List<WikipediaArticle> {
        val queries = DrugSearchQueryExpander.expand(query)
        val titles = linkedSetOf<String>()
        for (q in queries) {
            titles.addAll(opensearchTitles(q, limit = 8))
        }
        if (titles.isEmpty()) return emptyList()
        val articles = fetchExtracts(titles.take(12))
        return articles
            .map { article -> article to scoreArticle(query, article) }
            .filter { (article, score) -> score >= 0.45 && isLikelyDrugArticle(article) }
            .sortedByDescending { (_, score) -> score }
            .map { (article, _) -> article }
    }

    private fun opensearchTitles(search: String, limit: Int): List<String> {
        val searchUrl = "https://ru.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "opensearch")
            .addQueryParameter("search", search)
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("namespace", "0")
            .addQueryParameter("format", "json")
            .build()
        val searchJson = executeGet(searchUrl.toString())
        val root = JsonParser.parseString(searchJson).asJsonArray
        if (root.size() < 2) return emptyList()
        val array = root[1].asJsonArray
        return array.map { it.asString }
    }

    private fun fetchExtracts(titles: Collection<String>): List<WikipediaArticle> {
        if (titles.isEmpty()) return emptyList()
        val extractUrl = "https://ru.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("format", "json")
            .addQueryParameter("prop", "extracts")
            .addQueryParameter("exintro", "1")
            .addQueryParameter("explaintext", "1")
            .addQueryParameter("titles", titles.joinToString("|"))
            .build()
        val extractJson = executeGet(extractUrl.toString())
        val pages = JsonParser.parseString(extractJson)
            .asJsonObject["query"]?.asJsonObject?.get("pages")?.asJsonObject
            ?: return emptyList()
        return pages.entrySet().mapNotNull { entry ->
            val page = entry.value.asJsonObject
            if (page.has("missing")) return@mapNotNull null
            val title = page["title"]?.asString ?: return@mapNotNull null
            val extract = page["extract"]?.asString?.trim() ?: return@mapNotNull null
            val body = buildString {
                append(extract.take(2500))
                if (extract.length > 2500) append("…")
            }
            val pageUrl = "https://ru.wikipedia.org/wiki/${URLEncoder.encode(title, Charsets.UTF_8.name())}"
            WikipediaArticle(title = title, body = body.trim(), pageUrl = pageUrl)
        }
    }

    private fun scoreArticle(query: String, article: WikipediaArticle): Double {
        val titleScore = FuzzyStringMatch.bestScore(
            query,
            listOf(article.title, article.title.substringBefore('(').trim()),
        )
        val bodyScore = FuzzyStringMatch.score(query, article.body.take(200))
        var score = maxOf(titleScore, titleScore * 0.7 + bodyScore * 0.3)
        if (article.body.lowercase().contains("препарат") ||
            article.body.lowercase().contains("лекарств")
        ) {
            score += 0.08
        }
        return score.coerceAtMost(1.0)
    }

    private fun isLikelyDrugArticle(article: WikipediaArticle): Boolean {
        val title = article.title.lowercase()
        val body = article.body.lowercase()
        val rejectInTitle = listOf(
            "футбольный клуб",
            "хоккейный клуб",
            "баскетбольный",
            "фильм",
            "сериал",
            "альбом",
            "песня",
            "музыкальный",
            "телепередач",
            "видеоигр",
            "компьютерн",
        )
        if (rejectInTitle.any { title.contains(it) }) return false
        val medicalMarkers = listOf(
            "препарат",
            "лекарств",
            "медикамент",
            "действующее вещество",
            "мг",
            "таблет",
            "капсул",
            "раствор",
            "фармакол",
            "показани",
            "дозиров",
        )
        return medicalMarkers.any { body.contains(it) } ||
            medicalMarkers.any { title.contains(it) }
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
