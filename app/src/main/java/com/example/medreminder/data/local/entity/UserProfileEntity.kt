package com.example.medreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val fullName: String = "",
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
