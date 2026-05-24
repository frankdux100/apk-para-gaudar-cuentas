package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val encryptedPassword: String,
    val provider: String, // e.g., Gmail, Outlook, Yahoo, Custom
    val categoryName: String, // e.g., Personal, Trabajo, Social
    val sectionName: String, // e.g., Principal, Secundario, Respaldos
    val roleName: String, // e.g., Admin, Usuario, Moderador
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val name: String
)

@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey val name: String
)
