package com.example.medreminder.data.remote

import com.example.medreminder.domain.model.DrugReferenceOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder

data class VidalParapharmHit(
    val slug: String,
    val tradeName: String,
    val registrationNumber: String?,
    val formsSummary: String,
    val holderSummary: String,
    val inactiveListing: Boolean,
)

class VidalParapharmReferenceDataSource(
    private val client: OkHttpClient,
) {
    suspend fun searchParapharm(query: String): List<VidalParapharmHit> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        val merged = linkedMapOf<String, VidalParapharmHit>()
        for (q in DrugSearchQueryExpander.expand(trimmed).take(4)) {
            try {
                for (hit in fetchSearchPage(q)) {
                    merged.putIfAbsent(hit.slug, hit)
                }
            } catch (_: IOException) {
                // пробуем другие варианты запроса
            }
        }
        if (merged.isEmpty()) {
            throw IOException("Не удалось загрузить результаты с vidal.ru")
        }
        var ranked = merged.values
            .map { hit -> hit to scoreHit(hit, trimmed) }
            .filter { (_, score) -> score <= MAX_ACCEPT_RANK }
            .sortedBy { (_, score) -> score }
            .map { (hit, _) -> hit }
        if (ranked.isEmpty()) {
            ranked = merged.values
                .map { hit -> hit to scoreHit(hit, trimmed) }
                .sortedBy { (_, score) -> score }
                .map { (hit, _) -> hit }
        }
        ranked.take(12)
    }

    fun toOption(hit: VidalParapharmHit): DrugReferenceOption {
        val summaryParts = listOf(hit.formsSummary, hit.holderSummary)
            .filter { it.isNotBlank() }
        return DrugReferenceOption(
            id = "vidal:${hit.slug}",
            tradeName = hit.tradeName,
            innName = "—",
            status = if (hit.inactiveListing) {
                "БАД (в справочнике как не поставляемый)"
            } else {
                "БАД (парафармацевтика)"
            },
            registrationNumber = hit.registrationNumber,
            dosageFormsSummary = summaryParts.joinToString(" · "),
            pharmGroup = null,
            sourceLabel = VIDAL_SOURCE_LABEL,
        )
    }

    private fun fetchSearchPage(query: String): List<VidalParapharmHit> {
        val url = "https://www.vidal.ru/search".toHttpUrl().newBuilder()
            .addQueryParameter("t", "all")
            .addQueryParameter("bad", "on")
            .addQueryParameter("q", query)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("User-Agent", "MedReminder/1.0 (Android; reference lookup)")
            .header("Accept-Language", "ru-RU,ru;q=0.9")
            .build()
        val html = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Vidal: HTTP ${response.code}")
            }
            response.body?.string() ?: throw IOException("Vidal: пустой ответ")
        }
        return parseBadRows(html)
    }

    private fun parseBadRows(html: String): List<VidalParapharmHit> {
        val badSectionIndex = html.indexOf("id=\"search_bads\"")
        if (badSectionIndex < 0) return emptyList()
        val section = html.substring(badSectionIndex)
        val rowRegex = Regex("""<tr[^>]*>[\s\S]*?<span class="bads"[\s\S]*?</tr>""", RegexOption.IGNORE_CASE)
        return rowRegex.findAll(section).mapNotNull { match -> parseRow(match.value) }.toList()
    }

    private fun parseRow(rowHtml: String): VidalParapharmHit? {
        val href = Regex("""products-table-name[\s\S]*?<a[^>]+href="(/drugs/[^"]+)"""")
            .find(rowHtml)?.groupValues?.get(1) ?: return null
        val rawName = Regex("""products-table-name[\s\S]*?<a[^>]+>([\s\S]*?)</a>""")
            .find(rowHtml)?.groupValues?.get(1) ?: return null
        val tradeName = stripHtml(rawName)
        if (tradeName.isBlank()) return null
        val slug = href.removePrefix("/drugs/").trim('/')
        val forms = Regex("""<div class="hyphenate">([\s\S]*?)</div>""")
            .find(rowHtml)?.groupValues?.get(1)?.let(::stripHtml).orEmpty()
        val regNumber = Regex("""РУ:\s*([^\s<]+)""")
            .find(rowHtml)?.groupValues?.get(1)?.trim()
        val company = Regex("""products-table-company[\s\S]*?<a[^>]+>([\s\S]*?)</a>""")
            .find(rowHtml)?.groupValues?.get(1)?.let(::stripHtml).orEmpty()
        val country = Regex("""<span class="small">\(([^)]+)\)</span>""")
            .find(rowHtml)?.groupValues?.get(1)?.trim()
        val holderSummary = when {
            company.isBlank() -> ""
            country.isNullOrBlank() -> company
            else -> "$company ($country)"
        }
        val inactive = rowHtml.contains("not-valid-status", ignoreCase = true)
        return VidalParapharmHit(
            slug = slug,
            tradeName = tradeName,
            registrationNumber = regNumber,
            formsSummary = forms,
            holderSummary = holderSummary,
            inactiveListing = inactive,
        )
    }

    private fun stripHtml(raw: String): String =
        raw.replace(Regex("<[^>]+>"), " ")
            .replace("&reg;", "®")
            .replace("&trade;", "™")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun scoreHit(hit: VidalParapharmHit, query: String): Int {
        val fuzzy = FuzzyStringMatch.bestScore(query, listOf(hit.tradeName, hit.registrationNumber.orEmpty()))
        var score = ((1.0 - fuzzy) * 100).toInt()
        if (hit.inactiveListing) score += 10
        if (fuzzy < 0.38) score += 180
        return score
    }

    companion object {
        const val VIDAL_SOURCE_LABEL = "Справочник Vidal.ru (парафармацевтика / БАД)"
        private const val MAX_ACCEPT_RANK = 100
    }
}
