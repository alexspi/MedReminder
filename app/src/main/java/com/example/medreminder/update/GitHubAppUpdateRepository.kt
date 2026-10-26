package com.example.medreminder.update

import android.content.Context
import com.example.medreminder.data.remote.GitHubReleaseApi
import com.example.medreminder.data.remote.GitHubReleaseAssetDto
import com.example.medreminder.data.remote.ReleaseVersionManifest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitHubAppUpdateRepository(
    context: Context,
    private val githubOwner: String,
    private val githubRepo: String,
) {
    private val appContext = context.applicationContext
    private val gson = Gson()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MedReminder-Android")
                .build()
            chain.proceed(request)
        }
        .build()

    private val api: GitHubReleaseApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(GitHubReleaseApi::class.java)

    suspend fun checkForUpdate(installedVersionCode: Int): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (githubOwner.isBlank()) {
            return@withContext UpdateCheckResult.NotConfigured
        }
        try {
            val release = api.latestRelease(githubOwner, githubRepo)
            val assets = release.assets ?: emptyList()
            val manifest = loadVersionManifest(assets)
            val apkUrl = resolveApkDownloadUrl(assets, manifest)
                ?: return@withContext UpdateCheckResult.Error("В релизе нет APK-файла")

            val versionCode = manifest?.versionCode
            val versionName = manifest?.versionName?.takeIf { it.isNotBlank() }
                ?: release.tagName?.trimStart('v', 'V')?.takeIf { it.isNotBlank() }
                ?: return@withContext UpdateCheckResult.Error(
                    "Добавьте version.json в релиз на GitHub",
                )

            val remoteCode = versionCode
                ?: return@withContext UpdateCheckResult.Error(
                    "В version.json укажите versionCode (целое число)",
                )

            if (remoteCode <= installedVersionCode) {
                return@withContext UpdateCheckResult.UpToDate
            }

            UpdateCheckResult.Available(
                AvailableUpdate(
                    versionCode = remoteCode,
                    versionName = versionName,
                    apkDownloadUrl = apkUrl,
                    releaseNotes = release.body?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        } catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                404 -> "Релизов на GitHub пока нет"
                403 -> "Лимит GitHub API. Попробуйте позже"
                else -> "Ошибка GitHub: ${e.code()}"
            }
            UpdateCheckResult.Error(message)
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Не удалось проверить обновление")
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
        val destination = File(updatesDir, "medreminder-update.apk")
        if (destination.exists()) {
            destination.delete()
        }

        val request = Request.Builder()
            .url(downloadUrl)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "MedReminder-Android")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Скачивание не удалось: ${response.code}")
            }
            val body = response.body ?: throw IOException("Пустой ответ сервера")
            val totalBytes = body.contentLength().coerceAtLeast(0L)
            body.byteStream().use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0L) {
                            onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                        }
                    }
                }
            }
        }
        onProgress(1f)
        destination
    }

    private fun loadVersionManifest(assets: List<GitHubReleaseAssetDto>): ReleaseVersionManifest? {
        val manifestAsset = assets.firstOrNull { it.name.equals("version.json", ignoreCase = true) }
            ?: return null
        val request = Request.Builder()
            .url(manifestAsset.browserDownloadUrl)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "MedReminder-Android")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val json = response.body?.string() ?: return null
            return gson.fromJson(json, ReleaseVersionManifest::class.java)
        }
    }

    private fun resolveApkDownloadUrl(
        assets: List<GitHubReleaseAssetDto>,
        manifest: ReleaseVersionManifest?,
    ): String? {
        val preferredName = manifest?.apkFileName?.trim()?.takeIf { it.isNotEmpty() }
        if (preferredName != null) {
            val match = assets.firstOrNull { it.name == preferredName }
            if (match != null) return match.browserDownloadUrl
        }
        return assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }?.browserDownloadUrl
    }
}
