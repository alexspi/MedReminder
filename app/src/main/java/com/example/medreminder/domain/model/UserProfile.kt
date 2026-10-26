package com.example.medreminder.domain.model

import com.example.medreminder.data.local.entity.UserProfileEntity

data class UserProfile(
    val fullName: String = "",
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
) {
    fun toEntity(): UserProfileEntity =
        UserProfileEntity(
            fullName = fullName.trim(),
            age = age.trim(),
            heightCm = heightCm.trim(),
            weightKg = weightKg.trim(),
        )

    companion object {
        fun fromEntity(entity: UserProfileEntity): UserProfile =
            UserProfile(
                fullName = entity.fullName,
                age = entity.age,
                heightCm = entity.heightCm,
                weightKg = entity.weightKg,
            )
    }
}
