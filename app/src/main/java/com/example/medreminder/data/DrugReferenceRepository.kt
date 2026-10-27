package com.example.medreminder.data

import com.example.medreminder.data.remote.DrugSearchQueryExpander
import com.example.medreminder.data.remote.FuzzyStringMatch
import com.example.medreminder.data.remote.BadRecordsApi
import com.example.medreminder.data.remote.GrlsApi
import com.example.medreminder.data.remote.GrlsRecordDto
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
    private val badApi: BadRecordsApi,
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
        fetchOptionsFromBadRegistry(trimmed)
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
                error.code() == 429
            else -> false
        }

    private suspend fun fetchOptionsFromGrls(trimmed: String): DrugReferenceListResult {
        val queries = DrugSearchQueryExpander.expand(trimmed)
        val merged = linkedMapOf<String, GrlsRecordDto>()
        var lastError: Exception? = null
        for (q in queries) {
            try {
                val response = grlsApi.search(q, limit = 20)
                for (item in response.items.orEmpty()) {
                    val key = recordKey(item)
                    if (key != null) merged.putIfAbsent(key, item)
                }
            } catch (e: Exception) {
                lastError = when (e) {
                    is HttpException -> IOException("ГРЛС: ошибка сервера ${e.code()}", e)
                    is SocketTimeoutException -> IOException("ГРЛС: превышено время ожидания", e)
                    is UnknownHostException -> IOException("Нет доступа к интернету или DNS", e)
                    is IOException -> IOException(e.message ?: "ГРЛС: нет связи с сервером", e)
                    is JsonSyntaxException -> IOException("ГРЛС: неверный формат ответа", e)
                    else -> IOException(e.message ?: "ГРЛС: ошибка запроса", e)
                }
            }
        }
        if (merged.isEmpty()) {
            throw lastError ?: NoSuchElementException("По запросу «$trimmed» в ГРЛС ничего не найдено")
        }
        val ranked = merged.values
            .map { record -> record to scoreRecord(record, trimmed) }
            .filter { (_, score) -> score <= MAX_ACCEPT_RANK }
            .sortedBy { (_, score) -> score }
        if (ranked.isEmpty()) {
            throw NoSuchElementException("По запросу «$trimmed» нет подходящих совпадений в ГРЛС")
        }
        val options = ranked.take(10).map { (record, _) -> record.toOption() }
        val notice = if (queries.size > 1 && queries.first() != trimmed) {
            "Показаны результаты с учётом похожих вариантов написания."
        } else {
            null
        }
        return DrugReferenceListResult(
            query = trimmed,
            options = options,
            sourceLabel = GRLS_SOURCE_LABEL,
            notice = notice,
        )
    }

    private suspend fun fetchOptionsFromBadRegistry(trimmed: String): DrugReferenceListResult {
        val queries = DrugSearchQueryExpander.expand(trimmed)
        val merged = linkedMapOf<String, GrlsRecordDto>()
        var lastError: Exception? = null
        for (q in queries) {
            try {
                val response = badApi.search(q, limit = 20)
                for (item in response.items.orEmpty()) {
                    val key = recordKey(item)
                    if (key != null) merged.putIfAbsent(key, item)
                }
            } catch (e: Exception) {
                lastError = when (e) {
                    is HttpException -> IOException("Реестр БАД: ошибка сервера ${e.code()}", e)
                    is SocketTimeoutException -> IOException("Реестр БАД: превышено время ожидания", e)
                    is UnknownHostException -> IOException("Нет доступа к интернету или DNS", e)
                    is IOException -> IOException(e.message ?: "Реестр БАД: нет связи с сервером", e)
                    is JsonSyntaxException -> IOException("Реестр БАД: неверный формат ответа", e)
                    else -> IOException(e.message ?: "Реестр БАД: ошибка запроса", e)
                }
            }
        }
        if (merged.isEmpty()) {
            throw lastError ?: NoSuchElementException("По запросу «$trimmed» в реестре БАД ничего не найдено")
        }
        val ranked = merged.values
            .map { record -> record to scoreRecord(record, trimmed) }
            .filter { (_, score) -> score <= MAX_ACCEPT_RANK }
            .sortedBy { (_, score) -> score }
        if (ranked.isEmpty()) {
            throw NoSuchElementException("По запросу «$trimmed» нет подходящих совпадений в реестре БАД")
        }
        val options = ranked.take(10).map { (record, _) -> record.toBadOption() }
        val notice = if (queries.size > 1 && queries.first() != trimmed) {
            "Показаны результаты с учётом похожих вариантов написания."
        } else {
            null
        }
        return DrugReferenceListResult(
            query = trimmed,
            options = options,
            sourceLabel = BAD_SOURCE_LABEL,
            notice = notice,
        )
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

    private fun recordKey(record: GrlsRecordDto): String? {
        val id = record.id?.toString()
        if (!id.isNullOrBlank()) return "id:$id"
        val reg = record.registrationNumber
        if (!reg.isNullOrBlank()) return "reg:$reg"
        val trade = record.tradeName
        if (!trade.isNullOrBlank()) return "trade:$trade"
        return null
    }

    private fun GrlsRecordDto.toOption(): DrugReferenceOption = toRegistryOption("grls", GRLS_SOURCE_LABEL)

    private fun GrlsRecordDto.toBadOption(): DrugReferenceOption = toRegistryOption("bad", BAD_SOURCE_LABEL)

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
        val fuzzy = FuzzyStringMatch.bestScore(query, listOf(trade, inn))
        var score = ((1.0 - fuzzy) * 100).toInt()
        val status = record.status.orEmpty()
        if (status.startsWith("Действ")) score -= 8
        if (status.contains("Исключ") || status.contains("Истёк")) score += 12
        if (fuzzy < 0.42) score += 200
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
        private const val MAX_ACCEPT_RANK = 95
        const val GRLS_SOURCE_LABEL = "ГРЛС (Минздрав РФ), данные pharmcontrol.ru"
        const val BAD_SOURCE_LABEL = "Реестр зарегистрированных БАД, данные pharmcontrol.ru"
        const val WIKI_SOURCE_LABEL = "Википедия (рус.)"
    }
}
