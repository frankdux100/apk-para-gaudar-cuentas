package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class VaultRepository(private val dao: VaultDao) {

    val allAccounts: Flow<List<AccountEntity>> = dao.getAllAccounts()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allSections: Flow<List<SectionEntity>> = dao.getAllSections()
    val allRoles: Flow<List<RoleEntity>> = dao.getAllRoles()

    suspend fun insertAccount(account: AccountEntity) {
        dao.insertAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        dao.deleteAccount(account)
    }

    suspend fun insertCategory(name: String) {
        dao.insertCategory(CategoryEntity(name.trim()))
    }

    suspend fun deleteCategory(name: String) {
        dao.deleteCategory(CategoryEntity(name))
    }

    suspend fun insertSection(name: String) {
        dao.insertSection(SectionEntity(name.trim()))
    }

    suspend fun deleteSection(name: String) {
        dao.deleteSection(SectionEntity(name))
    }

    suspend fun insertRole(name: String) {
        dao.insertRole(RoleEntity(name.trim()))
    }

    suspend fun deleteRole(name: String) {
        dao.deleteRole(RoleEntity(name))
    }

    /**
     * Prepopulate defaults if the tables are completely empty.
     */
    suspend fun prepopulateDefaultsIfEmpty() {
        val categories = allCategories.first()
        if (categories.isEmpty()) {
            listOf("Personal", "Trabajo", "Finanzas", "Social").forEach {
                dao.insertCategory(CategoryEntity(it))
            }
        }

        val sections = allSections.first()
        if (sections.isEmpty()) {
            listOf("Principal", "Seguridad", "Respaldos", "Pruebas").forEach {
                dao.insertSection(SectionEntity(it))
            }
        }

        val roles = allRoles.first()
        if (roles.isEmpty()) {
            listOf("Administrador", "Usuario", "Soporte", "Colaborador").forEach {
                dao.insertRole(RoleEntity(it))
            }
        }
    }
}
