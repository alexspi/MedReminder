package com.example.medreminder.data

import com.example.medreminder.data.local.AppDatabase
import com.example.medreminder.data.local.entity.UserProfileEntity
import com.example.medreminder.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserProfileRepository(
    database: AppDatabase,
) {
    private val dao = database.userProfileDao()

    fun observeProfile(): Flow<UserProfile> =
        dao.observeProfile().map { entity ->
            entity?.let(UserProfile::fromEntity) ?: UserProfile()
        }

    suspend fun saveProfile(profile: UserProfile) {
        dao.upsert(profile.toEntity())
    }
}
