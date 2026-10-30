package com.example.medreminder

import android.app.Application
import com.example.medreminder.data.DrugReferenceRepository
import com.example.medreminder.data.MedReminderRepository
import com.example.medreminder.data.UserProfileRepository
import com.example.medreminder.data.local.AppDatabase
import com.example.medreminder.data.remote.GrlsApi
import com.example.medreminder.data.remote.VidalParapharmReferenceDataSource
import com.example.medreminder.data.remote.WikipediaReferenceDataSource
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.medreminder.update.GitHubAppUpdateRepository
import com.example.medreminder.notifications.NotificationChannels
import com.example.medreminder.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedReminderApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val scheduler: ReminderScheduler by lazy { ReminderScheduler(this) }

    private val database by lazy { AppDatabase.get(this) }

    val repository: MedReminderRepository by lazy {
        MedReminderRepository(database, scheduler)
    }

    val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepository(database)
    }

    val appUpdateRepository: GitHubAppUpdateRepository by lazy {
        GitHubAppUpdateRepository(
            context = this,
            githubOwner = BuildConfig.GITHUB_OWNER,
            githubRepo = BuildConfig.GITHUB_REPO,
        )
    }

    val drugReferenceRepository: DrugReferenceRepository by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "MedReminder/1.0 (Android)")
                    .header("Accept-Language", "ru-RU,ru;q=0.9")
                    .build()
                chain.proceed(request)
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.pharmcontrol.ru/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
        DrugReferenceRepository(
            grlsApi = retrofit.create(GrlsApi::class.java),
            vidalParapharm = VidalParapharmReferenceDataSource(client),
            wikipedia = WikipediaReferenceDataSource(client),
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        appScope.launch {
            try {
                scheduler.rescheduleAll(repository)
            } catch (_: Exception) {
                // Не роняем запуск, если БД/будильники недоступны
            }
        }
    }
}
