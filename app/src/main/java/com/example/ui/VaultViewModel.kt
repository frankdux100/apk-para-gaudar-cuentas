package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccountEntity
import com.example.data.CategoryEntity
import com.example.data.SectionEntity
import com.example.data.RoleEntity
import com.example.data.VaultDatabase
import com.example.data.VaultRepository
import com.example.security.BackupUtils
import com.example.security.CryptoUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VaultRepository
    private val sharedPrefs = application.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)

    // Unlocking / Security States
    private val _isFirstRun = MutableStateFlow(true)
    val isFirstRun: StateFlow<Boolean> = _isFirstRun.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _unlockError = MutableStateFlow<String?>(null)
    val unlockError: StateFlow<String?> = _unlockError.asStateFlow()

    // Temporary Memory decrypted SecretKeySpec
    private var _masterKey: SecretKeySpec? = null
    val masterKey: SecretKeySpec? get() = _masterKey

    // Central Data Streams
    val categories: StateFlow<List<CategoryEntity>>
    val sections: StateFlow<List<SectionEntity>>
    val roles: StateFlow<List<RoleEntity>>
    val accounts: StateFlow<List<AccountEntity>>

    // Search and Filter States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("Todos")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _selectedSectionFilter = MutableStateFlow("Todos")
    val selectedSectionFilter: StateFlow<String> = _selectedSectionFilter.asStateFlow()

    private val _selectedRoleFilter = MutableStateFlow("Todos")
    val selectedRoleFilter: StateFlow<String> = _selectedRoleFilter.asStateFlow()

    // Fully Filtered Accounts Stream
    val filteredAccounts: StateFlow<List<AccountEntity>>

    // Event Messages (Snackbars / Toasts feedback inside compose)
    private val _eventMessage = MutableStateFlow<String?>(null)
    val eventMessage: StateFlow<String?> = _eventMessage.asStateFlow()

    init {
        val database = VaultDatabase.getDatabase(application)
        repository = VaultRepository(database.vaultDao())

        categories = repository.allCategories.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        sections = repository.allSections.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        roles = repository.allRoles.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        accounts = repository.allAccounts.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        // Reactively filter accounts based on query text and custom attributes
        filteredAccounts = combine(
            accounts, searchQuery, _selectedCategoryFilter, _selectedSectionFilter, _selectedRoleFilter
        ) { list, query, cat, sec, role ->
            list.filter { account ->
                val matchQuery = query.isBlank() || 
                        account.email.contains(query, ignoreCase = true) || 
                        account.provider.contains(query, ignoreCase = true) ||
                        account.notes.contains(query, ignoreCase = true)
                
                val matchCat = cat == "Todos" || account.categoryName.equals(cat, ignoreCase = true)
                val matchSec = sec == "Todos" || account.sectionName.equals(sec, ignoreCase = true)
                val matchRole = role == "Todos" || account.roleName.equals(role, ignoreCase = true)

                matchQuery && matchCat && matchSec && matchRole
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Check if master password exists
        checkCryptoStatus()

        // Prepopulate database defaults
        viewModelScope.launch {
            repository.prepopulateDefaultsIfEmpty()
        }
    }

    private fun checkCryptoStatus() {
        val hasHash = sharedPrefs.contains("master_pass_hash")
        _isFirstRun.value = !hasHash
        _isUnlocked.value = false
    }

    /**
     * Set up Master Password for the very first time.
     */
    fun setupMasterPassword(password: String): Boolean {
        if (password.length < 6) {
            _unlockError.value = "La contraseña debe tener al menos 6 caracteres"
            return false
        }
        val hash = hashPassword(password)
        sharedPrefs.edit().putString("master_pass_hash", hash).apply()
        
        // Save in memory key
        _masterKey = CryptoUtils.deriveKey(password)
        _isUnlocked.value = true
        _isFirstRun.value = false
        _unlockError.value = null
        showFeedback("Contraseña Maestra establecida con éxito")
        return true
    }

    /**
     * Unlock the vaults using Master Password.
     */
    fun unlockVault(password: String): Boolean {
        val storedHash = sharedPrefs.getString("master_pass_hash", "") ?: ""
        val hash = hashPassword(password)
        if (hash == storedHash) {
            _masterKey = CryptoUtils.deriveKey(password)
            _isUnlocked.value = true
            _unlockError.value = null
            showFeedback("Bóveda desbloqueada")
            return true
        } else {
            _unlockError.value = "Contraseña Maestra incorrecta"
            return false
        }
    }

    fun lockVault() {
        _masterKey = null
        _isUnlocked.value = false
        showFeedback("Bóveda bloqueada manualmente")
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Filter adjustments
    fun updateQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategoryFilter(cat: String) {
        _selectedCategoryFilter.value = cat
    }

    fun updateSectionFilter(sec: String) {
        _selectedSectionFilter.value = sec
    }

    fun updateRoleFilter(role: String) {
        _selectedRoleFilter.value = role
    }

    // Feedback notifications helper
    fun showFeedback(msg: String) {
        _eventMessage.value = msg
    }

    fun clearFeedback() {
        _eventMessage.value = null
    }

    // Account Crud Interfaces
    fun addAccount(
        email: String,
        plainTextPass: String,
        provider: String,
        category: String,
        section: String,
        role: String,
        notes: String
    ) {
        viewModelScope.launch {
            val key = _masterKey
            if (key == null) {
                _unlockError.value = "Bóveda bloqueada. Desbloquee para guardar"
                return@launch
            }
            val encryptedPass = CryptoUtils.encrypt(plainTextPass, key)
            val newAcc = AccountEntity(
                email = email.trim(),
                encryptedPassword = encryptedPass,
                provider = provider.trim(),
                categoryName = category,
                sectionName = section,
                roleName = role,
                notes = notes.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertAccount(newAcc)
            showFeedback("Cuenta guardada exitosamente")
        }
    }

    fun updateAccount(
        id: Int,
        email: String,
        plainTextPass: String,
        provider: String,
        category: String,
        section: String,
        role: String,
        notes: String,
        keepOldPasswordIfEmpty: Boolean = false,
        oldEncryptedPass: String = ""
    ) {
        viewModelScope.launch {
            val key = _masterKey
            if (key == null) {
                _unlockError.value = "Bóveda bloqueada. No se pudo actualizar"
                return@launch
            }

            val finalEncryptedPass = if (keepOldPasswordIfEmpty && plainTextPass.isBlank()) {
                oldEncryptedPass
            } else {
                CryptoUtils.encrypt(plainTextPass, key)
            }

            val updatedAcc = AccountEntity(
                id = id,
                email = email.trim(),
                encryptedPassword = finalEncryptedPass,
                provider = provider.trim(),
                categoryName = category,
                sectionName = section,
                roleName = role,
                notes = notes.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertAccount(updatedAcc)
            showFeedback("Cuenta actualizada correctamente")
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            showFeedback("Cuenta eliminada de la base de datos")
        }
    }

    // Custom Category management
    fun addCategory(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertCategory(name)
                showFeedback("Categoría '$name' añadida")
            }
        }
    }

    fun removeCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCategory(name)
            showFeedback("Categoría eliminada")
        }
    }

    // Custom Section management
    fun addSection(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertSection(name)
                showFeedback("Sección '$name' añadida")
            }
        }
    }

    fun removeSection(name: String) {
        viewModelScope.launch {
            repository.deleteSection(name)
            showFeedback("Sección eliminada")
        }
    }

    // Custom Role management
    fun addRole(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertRole(name)
                showFeedback("Rol '$name' añadido")
            }
        }
    }

    fun removeRole(name: String) {
        viewModelScope.launch {
            repository.deleteRole(name)
            showFeedback("Rol eliminado")
        }
    }

    // Secure base64 AES-256 backup methods
    fun getEncryptedBackupString(password: String): String {
        val currentAccs = accounts.value
        val currentCats = categories.value
        val currentSecs = sections.value
        val currentRoles = roles.value
        return BackupUtils.exportBackup(currentAccs, currentCats, currentSecs, currentRoles, password)
    }

    fun importEncryptedBackupString(payload: String, password: String): Boolean {
        val backup = BackupUtils.importBackup(payload, password)
        if (backup == null) {
            showFeedback("Error al importar: Datos corruptos o contraseña inválida")
            return false
        }
        viewModelScope.launch {
            // Restore categories
            backup.categories.forEach { repository.insertCategory(it.name) }
            // Restore sections
            backup.sections.forEach { repository.insertSection(it.name) }
            // Restore roles
            backup.roles.forEach { repository.insertRole(it.name) }
            // Restore accounts
            backup.accounts.forEach { repository.insertAccount(it) }
            showFeedback("Sincronización segura completada con éxito. ${backup.accounts.size} cuentas importadas")
        }
        return true
    }

    // Decrypt Password directly for visibility
    fun decryptValue(encryptedText: String): String {
        val key = _masterKey ?: return "••••••••"
        return CryptoUtils.decrypt(encryptedText, key)
    }
}
