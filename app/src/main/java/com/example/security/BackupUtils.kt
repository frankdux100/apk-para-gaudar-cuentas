package com.example.security

import com.example.data.AccountEntity
import com.example.data.CategoryEntity
import com.example.data.SectionEntity
import com.example.data.RoleEntity
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

data class DecryptedBackup(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val sections: List<SectionEntity>,
    val roles: List<RoleEntity>
)

object BackupUtils {

    /**
     * Converts full database state into a single JSON, then encrypts it with AES-256 using the derived key.
     */
    fun exportBackup(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        sections: List<SectionEntity>,
        roles: List<RoleEntity>,
        password: String
    ): String {
        return try {
            val root = JSONObject()

            // Accounts array
            val accountsArray = JSONArray()
            accounts.forEach { acc ->
                val accJson = JSONObject()
                accJson.put("email", acc.email)
                accJson.put("encryptedPassword", acc.encryptedPassword) // Already encrypted locally
                accJson.put("provider", acc.provider)
                accJson.put("categoryName", acc.categoryName)
                accJson.put("sectionName", acc.sectionName)
                accJson.put("roleName", acc.roleName)
                accJson.put("notes", acc.notes)
                accJson.put("updatedAt", acc.updatedAt)
                accountsArray.put(accJson)
            }
            root.put("accounts", accountsArray)

            // Categories
            val categoriesArray = JSONArray()
            categories.forEach { categoriesArray.put(it.name) }
            root.put("categories", categoriesArray)

            // Sections
            val sectionsArray = JSONArray()
            sections.forEach { sectionsArray.put(it.name) }
            root.put("sections", sectionsArray)

            // Roles
            val rolesArray = JSONArray()
            roles.forEach { rolesArray.put(it.name) }
            root.put("roles", rolesArray)

            val jsonString = root.toString()
            val derivedKey = CryptoUtils.deriveKey(password)
            CryptoUtils.encrypt(jsonString, derivedKey)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Decrypts an AES-256 backup payload and parses the JSON back into list entities.
     */
    fun importBackup(
        encryptedPayload: String,
        password: String
    ): DecryptedBackup? {
        return try {
            val derivedKey = CryptoUtils.deriveKey(password)
            val jsonString = CryptoUtils.decrypt(encryptedPayload, derivedKey)
            
            // If decryption failed, verify JSON syntax validity
            if (!jsonString.startsWith("{") || !jsonString.endsWith("}")) {
                return null
            }

            val root = JSONObject(jsonString)

            // Parse categories
            val categoriesList = mutableListOf<CategoryEntity>()
            if (root.has("categories")) {
                val catsArray = root.getJSONArray("categories")
                for (i in 0 until catsArray.length()) {
                    categoriesList.add(CategoryEntity(catsArray.getString(i)))
                }
            }

            // Parse sections
            val sectionsList = mutableListOf<SectionEntity>()
            if (root.has("sections")) {
                val secsArray = root.getJSONArray("sections")
                for (i in 0 until secsArray.length()) {
                    sectionsList.add(SectionEntity(secsArray.getString(i)))
                }
            }

            // Parse roles
            val rolesList = mutableListOf<RoleEntity>()
            if (root.has("roles")) {
                val rolesArray = root.getJSONArray("roles")
                for (i in 0 until rolesArray.length()) {
                    rolesList.add(RoleEntity(rolesArray.getString(i)))
                }
            }

            // Parse accounts
            val accountsList = mutableListOf<AccountEntity>()
            if (root.has("accounts")) {
                val accsArray = root.getJSONArray("accounts")
                for (i in 0 until accsArray.length()) {
                    val obj = accsArray.getJSONObject(i)
                    accountsList.add(
                        AccountEntity(
                            id = 0, // Generated automatically on room insert
                            email = obj.optString("email", ""),
                            encryptedPassword = obj.optString("encryptedPassword", ""),
                            provider = obj.optString("provider", "Custom"),
                            categoryName = obj.optString("categoryName", "General"),
                            sectionName = obj.optString("sectionName", "General"),
                            roleName = obj.optString("roleName", "Usuario"),
                            notes = obj.optString("notes", ""),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            DecryptedBackup(
                accounts = accountsList,
                categories = categoriesList,
                sections = sectionsList,
                roles = rolesList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
