package com.example.medreminder.data

import com.example.medreminder.data.remote.DrugSearchQueryExpander
import com.example.medreminder.data.remote.FuzzyStringMatch
import com.example.medreminder.data.remote.GrlsApi
import com.example.medreminder.data.remote.GrlsRecordDto
import com.example.medreminder.data.remote.VidalParapharmReferenceDataSource
import com.example.medreminder.data.remote.WikipediaReferenceDataSource
import com.example.medreminder.domain.model.DrugReferenceOption
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class DrugReferenceListResult(
    val query: String,
    val options: List<DrugReferenceOption>,
    val sourceLabel: String,
    val notice: String? = null,
)

class DrugReferenceRepository(
    private val grlsApi: GrlsApi,
    private val vidalParapharm: VidalParapharmReferenceDataSource,
    private val wikipedia: WikipediaReferenceDataSource,
) {
    suspend fun fetchDrugReferenceOptions(query: String): DrugReferenceListResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            throw IllegalArgumentException("Введите название лекарства для поиска")
        }
        try {
            fetchOptionsFromGrls(trimmed)
        } catch (grlsError: Exception) {
            if (!isGrlsServiceUnavailable(grlsError)) {
                throw grlsError
            }
            try {
                fetchOptionsFromWikipedia(trimmed, grlsCause = grlsError)
            } catch (_: Exception) {
                throw mapGrlsError(grlsError)
            }
        }
    }

    suspend fun fetchSupplementReferenceOptions(query: String): DrugReferenceListResult = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            throw IllegalArgumentException("Введите название БАД для поиска")
        }
        val hits = try {
            vidalParapharm.searchParapharm(trimmed)
        } catch (e: IOException) {
            throw IOException("Vidal.ru: ${e.message ?: "нет связи"}", e)
        }
        if (hits.isEmpty()) {
            throw NoSuchElementException("По запросу «$trimmed» в справочнике Vidal (БАД) ничего не найдено")
        }
        val options = hits.take(10).map { hit -> vidalParapharm.toOption(hit) }
        DrugReferenceListResult(
            query = trimmed,
            options = options,
            sourceLabel = VidalParapharmReferenceDataSource.VIDAL_SOURCE_LABEL,
            notice = "Поиск по разделу «Парафармацевтика / БАД» на vidal.ru.",
        )
    }

    /** Википедия — только если медсправочник (ГРЛС) недоступен, не при «ничего не найдено». */
    private fun isGrlsServiceUnavailable(error: Exception): Boolean =
        when (error) {
            is NoSuchElementException,
            is IllegalArgumentException,
            -> false
            is IOException -> true
            is HttpException -> error.code() in 500..599 ||
                error.code() == 408 ||
                error.code() == 429 ||
                error.code() == 502 ||
                error.code() == 503 ||
                error.code() == 504
            else -> false
        }

    private suspend fun fetchOptionsFromGrls(trimmed: String): DrugReferenceListResult {
        val queries = buildGrlsSearchQueries(trimmed)
        val merged = linkedMapOf<String, GrlsRecordDto>()
        var lastError: Exception? = null
        for (q in queries) {
            try {
                val response = grlsApi.search(q, limit = GRLS_PAGE_LIMIT)
                for (item in response.items.orEmpty()) {
                    val key = recordKey(item)
                    if (key != null) merged.putIfAbsent(key, item)
                }
            } catch (e: Exception) {
                lastError = mapGrlsIOException(e)
            }
        }
        if (merged.isEmpty()) {
            throw lastError ?: NoSuchElementException("По запросу «$trimmed» в ГРЛС ничего не найдено")
        }
        var ranked = rankRecords(merged.values, trimmed, MAX_ACCEPT_RANK)
        var relaxedNotice: String? = null
        if (ranked.isEmpty()) {
            ranked = rankRecords(merged.values, trimmed, MAX_ACCEPT_RANK_RELAXED)
            if (ranked.isNotEmpty()) {
                relaxedNotice = "Показаны близкие совпадения — проверьте название в списке."
            }
        }
        if (ranked.isEmpty()) {
            ranked = merged.values
                .map { record -> record to scoreRecord(record, trimmed) }
                .sortedBy { (_, score) -> score }
                .take(10)
            relaxedNotice = "Точного совпадения нет — показаны ближайшие записи из ГРЛС."
        }
        val options = ranked.take(10).map { (record, _) -> record.toOption() }
        val expandNotice = if (queries.size > 1 && queries.first() != trimmed) {
            "Показаны результаты с учётом похожих вариантов написания."
        } else {
            null
        }
        val notice = listOfNotNull(expandNotice, relaxedNotice).joinToString(" ").ifBlank { null }
        return DrugReferenceListResult(
            query = trimmed,
            options = options,
            sourceLabel = GRLS_SOURCE_LABEL,
            notice = notice,
        )
    }

    private suspend fun buildGrlsSearchQueries(trimmed: String): List<String> {
        val queries = linkedSetOf<String>()
        queries.addAll(DrugSearchQueryExpander.expand(trimmed))
        try {
            val suggestions = grlsApi.suggestions(trimmed.take(60))
            for (item in suggestions.items.orEmpty()) {
                val text = item.trim()
                if (text.length >= 2) queries += text
            }
        } catch (_: Exception) {
            // подсказки необязательны
        }
        return queries.take(12).toList()
    }

    private suspend fun fetchOptionsFromWikipedia(
        trimmed: String,
        grlsCause: Exception,
    ): DrugReferenceListResult {
        val article = wikipedia.fetchArticle(trimmed)
        if (!FuzzyStringMatch.isAcceptable(trimmed, article.title) &&
            !FuzzyStringMatch.isAcceptable(trimmed, article.body.take(120))
        ) {
            throw NoSuchElementException(
                "По запросу «$trimmed» не найдено подходящей справки о лекарстве",
            )
        }
        val notice = "Медсправочник ГРЛС недоступен (${grlsCause.message ?: "сеть"}). " +
            "Показана статья из Википедии."
        val option = DrugReferenceOption(
            id = "wiki:${article.title}",
            tradeName = article.title,
            innName = "—",
            status = "Справочная статья",
            registrationNumber = null,
            dosageFormsSummary = article.body.take(800),
            pharmGroup = null,
            sourceLabel = WIKI_SOURCE_LABEL,
        )
        return DrugReferenceListResult(
            query = trimmed,
            options = listOf(option),
            sourceLabel = WIKI_SOURCE_LABEL,
            notice = notice,
        )
    }

    private fun rankRecords(
        records: Collection<GrlsRecordDto>,
        query: String,
        maxRank: Int,
    ): List<Pair<GrlsRecordDto, Int>> =
        records
            .map { record -> record to scoreRecord(record, query) }
            .filter { (_, score) -> score <= maxRank }
            .sortedBy { (_, score) -> score }

    private fun recordKey(record: GrlsRecordDto): String? {
        val id = record.id?.toString()
        if (!id.isNullOrBlank()) return "id:$id"
        val reg = record.registrationNumber
        if (!reg.isNullOrBlank()) return "reg:$reg"
        val trade = record.tradeName
        if (!trade.isNullOrBlank()) return "trade:$trade"
        return null
    }

    private fun GrlsRecordDto.toOption(): DrugReferenceOption =
        toRegistryOption("grls", GRLS_SOURCE_LABEL)

    private fun GrlsRecordDto.toRegistryOption(idPrefix: String, sourceLabel: String): DrugReferenceOption {
        val recordId = id ?: registrationNumber?.hashCode()?.toLong() ?: tradeName.hashCode().toLong()
        return DrugReferenceOption(
            id = "$idPrefix:$recordId",
            tradeName = tradeName.orEmpty().ifBlank { "—" },
            innName = innName.orEmpty().ifBlank { "—" },
            status = status.orEmpty().ifBlank { "—" },
            registrationNumber = registrationNumber,
            dosageFormsSummary = shortenDosageForms(dosageForms),
            pharmGroup = pharmGroup,
            sourceLabel = sourceLabel,
        )
    }

    private fun mapGrlsIOException(e: Exception): IOException =
        when (e) {
            is HttpException -> IOException("ГРЛС: ошибка сервера ${e.code()}", e)
            is SocketTimeoutException -> IOException("ГРЛС: превышено время ожидания", e)
            is UnknownHostException -> IOException("Нет доступа к интернету или DNS", e)
            is IOException -> IOException(e.message ?: "ГРЛС: нет связи с сервером", e)
            is JsonSyntaxException -> IOException("ГРЛС: неверный формат ответа", e)
            else -> IOException(e.message ?: "ГРЛС: ошибка запроса", e)
        }

    private fun mapGrlsError(e: Exception): IOException {
        val message = when (e) {
            is IOException -> e.message ?: "Ошибка сети"
            else -> e.message ?: "Не удалось загрузить справку"
        }
        return IOException("$message. Запасной источник тоже недоступен.")
    }

    private fun scoreRecord(record: GrlsRecordDto, query: String): Int {
        val trade = record.tradeName.orEmpty()
        val inn = record.innName.orEmpty()
        val reg = record.registrationNumber.orEmpty()
        val fuzzy = FuzzyStringMatch.bestScore(query, listOf(trade, inn, reg))
        var score = ((1.0 - fuzzy) * 100).toInt()
        val status = record.status.orEmpty()
        if (status.startsWith("Действ")) score -= 8
        if (status.contains("Исключ") || status.contains("Истёк")) score += 12
        if (fuzzy < 0.35) score += 160
        return score
    }

    private fun shortenDosageForms(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val cleaned = raw.replace("_x000D_", " ").replace(Regex("\\s+"), " ").trim()
        val parts = cleaned.split(';').map { it.trim() }.filter { it.isNotEmpty() }
        val short = parts.take(2).joinToString("; ")
        return if (parts.size > 2) "$short; …" else short
    }

    companion object {
        private const val GRLS_PAGE_LIMIT = 50
        private const val MAX_ACCEPT_RANK = 95
        private const val MAX_ACCEPT_RANK_RELAXED = 120
        const val GRLS_SOURCE_LABEL = "ГРЛС (Минздрав РФ), API pharmcontrol.ru"
        const val WIKI_SOURCE_LABEL = "Википедия (рус.)"
    }
}
