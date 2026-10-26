package com.example.medreminder.data

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
    private val api: GrlsApi,
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
            if (grlsError is NoSuchElementException) {
                try {
                    fetchOptionsFromWikipedia(trimmed, grlsNotFound = true)
                } catch (_: Exception) {
                    throw grlsError
                }
            } else {
                try {
                    fetchOptionsFromWikipedia(trimmed, grlsNotFound = false, grlsCause = grlsError)
                } catch (_: Exception) {
                    throw mapGrlsError(grlsError)
                }
            }
        }
    }

    private suspend fun fetchOptionsFromGrls(trimmed: String): DrugReferenceListResult {
        val response = try {
            api.search(trimmed, limit = 10)
        } catch (e: HttpException) {
            throw IOException("ГРЛС: ошибка сервера ${e.code()}", e)
        } catch (e: SocketTimeoutException) {
            throw IOException("ГРЛС: превышено время ожидания", e)
        } catch (e: UnknownHostException) {
            throw IOException("Нет доступа к интернету или DNS", e)
        } catch (e: IOException) {
            throw IOException("ГРЛС: нет связи с сервером", e)
        } catch (e: JsonSyntaxException) {
            throw IOException("ГРЛС: неверный формат ответа", e)
        }
        val items = response.items.orEmpty()
        if (items.isEmpty()) {
            throw NoSuchElementException("По запросу «$trimmed» в ГРЛС ничего не найдено")
        }
        val sorted = items.sortedWith(compareBy<GrlsRecordDto> { scoreRecord(it, trimmed) })
        val options = sorted.map { it.toOption() }
        return DrugReferenceListResult(
            query = trimmed,
            options = options,
            sourceLabel = GRLS_SOURCE_LABEL,
        )
    }

    private suspend fun fetchOptionsFromWikipedia(
        trimmed: String,
        grlsNotFound: Boolean,
        grlsCause: Exception? = null,
    ): DrugReferenceListResult {
        val article = wikipedia.fetchArticle(trimmed)
        val notice = when {
            grlsNotFound -> "В ГРЛС записей нет. Показана статья из Википедии."
            else -> "ГРЛС недоступен (${grlsCause?.message ?: "сеть"}). Показана статья из Википедии."
        }
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

    private fun GrlsRecordDto.toOption(): DrugReferenceOption {
        val recordId = id ?: registrationNumber?.hashCode()?.toLong() ?: tradeName.hashCode().toLong()
        return DrugReferenceOption(
            id = "grls:$recordId",
            tradeName = tradeName.orEmpty().ifBlank { "—" },
            innName = innName.orEmpty().ifBlank { "—" },
            status = status.orEmpty().ifBlank { "—" },
            registrationNumber = registrationNumber,
            dosageFormsSummary = shortenDosageForms(dosageForms),
            pharmGroup = pharmGroup,
            sourceLabel = GRLS_SOURCE_LABEL,
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
        val q = query.lowercase()
        var score = 0
        val status = record.status.orEmpty()
        if (status.startsWith("Действ")) score -= 20
        if (status.contains("Исключ") || status.contains("Истёк")) score += 10
        val trade = record.tradeName.orEmpty().lowercase()
        val inn = record.innName.orEmpty().lowercase()
        if (trade == q) score -= 15
        else if (trade.contains(q)) score -= 8
        if (inn == q) score -= 12
        else if (inn.contains(q)) score -= 5
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
        const val GRLS_SOURCE_LABEL = "ГРЛС (Минздрав РФ), данные pharmcontrol.ru"
        const val WIKI_SOURCE_LABEL = "Википедия (рус.)"
    }
}
