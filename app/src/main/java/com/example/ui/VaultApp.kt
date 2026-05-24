package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AccountEntity
import com.example.utils.ExportUtils
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultApp(viewModel: VaultViewModel) {
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val isFirstRun by viewModel.isFirstRun.collectAsState()
    val eventMessage by viewModel.eventMessage.collectAsState()
    val context = LocalContext.current

    // SnackBar notification feedback
    LaunchedEffect(eventMessage) {
        eventMessage?.let {
            // We can show short Toast as robust overlay or handle Snackbar via host
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    if (!isUnlocked) {
        LockScreen(
            viewModel = viewModel,
            isFirstRun = isFirstRun
        )
    } else {
        MainWorkspaceScreen(viewModel = viewModel)
    }
}

/**
 * High-security authentic start screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(viewModel: VaultViewModel, isFirstRun: Boolean) {
    var passwordInput by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var viewPassword by remember { mutableStateOf(false) }
    val unlockError by viewModel.unlockError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Bóveda Cerrada",
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "CORREOVAULT",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (isFirstRun) 
                    "Establezca una Contraseña Maestra de 6+ caracteres para encriptar localmente con AES-256." 
                    else "Ingrese su Contraseña Maestra para desbloquear.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Password Input field
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Contraseña Maestra") },
                singleLine = true,
                visualTransformation = if (viewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { viewPassword = !viewPassword }) {
                        Icon(
                            imageVector = if (viewPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Mostrar"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (isFirstRun) {
                OutlinedTextField(
                    value = passwordConfirm,
                    onValueChange = { passwordConfirm = it },
                    label = { Text("Confirmar Contraseña") },
                    singleLine = true,
                    visualTransformation = if (viewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (unlockError != null) {
                Text(
                    text = unlockError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (isFirstRun) {
                        if (passwordInput != passwordConfirm) {
                            viewModel.showFeedback("Las contraseñas no coinciden")
                        } else {
                            viewModel.setupMasterPassword(passwordInput)
                        }
                    } else {
                        viewModel.unlockVault(passwordInput)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isFirstRun) "Configurar e Inicializar" else "Desbloquear Bóveda",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Main application navigation pane (Tab layout / Scaffold)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainWorkspaceScreen(viewModel: VaultViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Security, contentDescription = "Cuentas") },
                    label = { Text("Bóveda") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Category, contentDescription = "Organizar") },
                    label = { Text("Roles & Tags") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Sync, contentDescription = "Sincronizar") },
                    label = { Text("Respaldos V2") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> VaultScreen(viewModel = viewModel)
                1 -> OrganizationScreen(viewModel = viewModel)
                2 -> SyncExportScreen(viewModel = viewModel)
            }
        }
    }
}

/**
 * View 1: Main Vault List Screen with complete dynamic filters & options
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VaultScreen(viewModel: VaultViewModel) {
    val accounts by viewModel.filteredAccounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val roles by viewModel.roles.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    val selectedCatFilter by viewModel.selectedCategoryFilter.collectAsState()
    val selectedSecFilter by viewModel.selectedSectionFilter.collectAsState()
    val selectedRoleFilter by viewModel.selectedRoleFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mi Bóveda",
                        fontWeight = FontWeight.Light,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.lockVault() }) {
                        Icon(Icons.Filled.Lock, contentDescription = "Bloquear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Cuenta")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateQuery(it) },
                placeholder = { Text("Buscar por correo, proveedor o nota...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Filtering Row / Chips list
            Text(
                text = "Filtrar por Taxonomías",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Display filters horizontally with wrapping or horizontal scrolling row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Filter Line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cat: ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        Button(
                            onClick = { expanded = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(selectedCatFilter, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.FilterList, null, modifier = Modifier.size(12.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Todos") }, onClick = { viewModel.updateCategoryFilter("Todos"); expanded = false })
                            categories.forEach {
                                DropdownMenuItem(text = { Text(it.name) }, onClick = { viewModel.updateCategoryFilter(it.name); expanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Section Filter Line
                    Text("Sec: ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        Button(
                            onClick = { expanded = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(selectedSecFilter, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.FilterList, null, modifier = Modifier.size(12.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Todos") }, onClick = { viewModel.updateSectionFilter("Todos"); expanded = false })
                            sections.forEach {
                                DropdownMenuItem(text = { Text(it.name) }, onClick = { viewModel.updateSectionFilter(it.name); expanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Role Filter Line
                    Text("Rol: ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(30.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        Button(
                            onClick = { expanded = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(selectedRoleFilter, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.FilterList, null, modifier = Modifier.size(12.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Todos") }, onClick = { viewModel.updateRoleFilter("Todos"); expanded = false })
                            roles.forEach {
                                DropdownMenuItem(text = { Text(it.name) }, onClick = { viewModel.updateRoleFilter(it.name); expanded = false })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Active Filters resetting button
            if (selectedCatFilter != "Todos" || selectedSecFilter != "Todos" || selectedRoleFilter != "Todos") {
                TextButton(
                    onClick = {
                        viewModel.updateCategoryFilter("Todos")
                        viewModel.updateSectionFilter("Todos")
                        viewModel.updateRoleFilter("Todos")
                    },
                    modifier = Modifier.padding(horizontal = 16.dp).align(Alignment.End)
                ) {
                    Icon(Icons.Filled.FilterList, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpiar Filtros", fontSize = 11.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Accounts List
            if (accounts.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(accounts, key = { it.id }) { item ->
                        AccountCard(
                            account = item,
                            viewModel = viewModel,
                            onEdit = { accountToEdit = item },
                            onDelete = { viewModel.deleteAccount(item) }
                        )
                    }
                }
            }
        }

        // Add Account Dialog
        if (showAddDialog) {
            AddOrEditAccountDialog(
                account = null,
                categories = categories.map { it.name },
                sections = sections.map { it.name },
                roles = roles.map { it.name },
                onDismiss = { showAddDialog = false },
                onSave = { email, password, provider, category, section, role, notes ->
                    viewModel.addAccount(email, password, provider, category, section, role, notes)
                    showAddDialog = false
                }
            )
        }

        // Edit Account Dialog
        accountToEdit?.let { entity ->
            AddOrEditAccountDialog(
                account = entity,
                categories = categories.map { it.name },
                sections = sections.map { it.name },
                roles = roles.map { it.name },
                onDismiss = { accountToEdit = null },
                onSave = { email, password, provider, category, section, role, notes ->
                    viewModel.updateAccount(
                        id = entity.id,
                        email = email,
                        plainTextPass = password,
                        provider = provider,
                        category = category,
                        section = section,
                        role = role,
                        notes = notes,
                        keepOldPasswordIfEmpty = true,
                        oldEncryptedPass = entity.encryptedPassword
                    )
                    accountToEdit = null
                }
            )
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Security,
            contentDescription = "No hay cuentas",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bóveda Vacía",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No se encontraron credenciales. Presione el botón [+] para crear su primer registro encriptado localmente.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

/**
 * Beautiful Credential Render Card with Logo matching and visibility toggle
 */
@Composable
fun AccountCard(
    account: AccountEntity,
    viewModel: VaultViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val decryptedPassword = if (revealed) viewModel.decryptValue(account.encryptedPassword) else "••••••••"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Email Logo Indicator corresponding to requested detail: "poner un logo del correco electronico"
                ProviderLogo(provider = account.provider)
                
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.email,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = account.provider.uppercase(Locale.getDefault()),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Copy buttons & Actions
                IconButton(onClick = {
                    clipboardManager.setText(AnnotatedString(account.email))
                    viewModel.showFeedback("Correo copiado al portapapeles")
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar Correo", modifier = Modifier.size(16.dp))
                }

                var showMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Menú de Opciones", modifier = Modifier.size(16.dp))
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Filled.Settings, null, modifier = Modifier.size(16.dp)) },
                        onClick = { onEdit(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Lock, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grid of Attributes (Category, Section, Role)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagPill(label = account.categoryName, containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                TagPill(label = account.sectionName, containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                TagPill(label = account.roleName, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            if (account.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = account.notes,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Password row with display / hide toggle y decryption
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = decryptedPassword,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (revealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Row {
                    IconButton(
                        onClick = { revealed = !revealed },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Mostrar Contraseña",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            val copyPass = viewModel.decryptValue(account.encryptedPassword)
                            clipboardManager.setText(AnnotatedString(copyPass))
                            viewModel.showFeedback("Contraseña copiada al portapapeles")
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copiar Contraseña",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagPill(label: String, containerColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/**
 * Beautiful dynamic email provider logos component as requested (Gmail, Outlook, Yahoo, Custom)
 */
@Composable
fun ProviderLogo(provider: String) {
    val providerLower = provider.lowercase(Locale.getDefault()).trim()
    val bgColor: Color
    val initials: String
    val iconLogo: ImageVector?

    when {
        providerLower.contains("gmail") || providerLower.contains("google") -> {
            bgColor = Color(0xFFEA4335) // Gmail Red
            initials = "G"
            iconLogo = Icons.Filled.Security
        }
        providerLower.contains("outlook") || providerLower.contains("microsoft") || providerLower.contains("hotmail") -> {
            bgColor = Color(0xFF0078D4) // Microsoft Blue
            initials = "O"
            iconLogo = Icons.Filled.Person
        }
        providerLower.contains("yahoo") -> {
            bgColor = Color(0xFF6001D2) // Yahoo Purple
            initials = "Y"
            iconLogo = Icons.Filled.Category
        }
        else -> {
            bgColor = Color(0xFF475569) // Custom Slate Grey
            initials = "@"
            iconLogo = Icons.Filled.Lock
        }
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

/**
 * Dialog for establishing or editing secrets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditAccountDialog(
    account: AccountEntity?,
    categories: List<String>,
    sections: List<String>,
    roles: List<String>,
    onDismiss: () -> Unit,
    onSave: (email: String, password: String, provider: String, category: String, section: String, role: String, notes: String) -> Unit
) {
    var emailInput by remember { mutableStateOf(account?.email ?: "") }
    var passwordInput by remember { mutableStateOf("") }
    var providerInput by remember { mutableStateOf(account?.provider ?: "") }
    var notesInput by remember { mutableStateOf(account?.notes ?: "") }

    var selectedCategory by remember { mutableStateOf(account?.categoryName ?: categories.firstOrNull() ?: "Personal") }
    var selectedSection by remember { mutableStateOf(account?.sectionName ?: sections.firstOrNull() ?: "Principal") }
    var selectedRole by remember { mutableStateOf(account?.roleName ?: roles.firstOrNull() ?: "Usuario") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = if (account == null) "Agregar Credencial" else "Editar Credencial",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Correo Electrónico") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = providerInput,
                        onValueChange = { providerInput = it },
                        label = { Text("Proveedor (ej: Gmail, Outlook)") },
                        singleLine = true,
                        placeholder = { Text("Gmail") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { 
                            if (account == null) Text("Contraseña") else Text("Nueva Contraseña (vacío para mantener)")
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // Category Selection
                    Text("Seleccionar Categoría", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    CategorySelector(
                        selected = selectedCategory,
                        options = categories,
                        onSelected = { selectedCategory = it }
                    )
                }

                item {
                    // Section Selection
                    Text("Seleccionar Sección", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    SectionSelector(
                        selected = selectedSection,
                        options = sections,
                        onSelected = { selectedSection = it }
                    )
                }

                item {
                    // Role Selection
                    Text("Seleccionar Rol de Cuenta", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    RoleSelector(
                        selected = selectedRole,
                        options = roles,
                        onSelected = { selectedRole = it }
                    )
                }

                item {
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Notas Adicionales") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (emailInput.isBlank() || providerInput.isBlank()) {
                                // Handled via callback validation inside Viewmodel ideally, but let's prompt
                                return@Button
                            }
                            onSave(emailInput, passwordInput, providerInput, selectedCategory, selectedSection, selectedRole, notesInput)
                        }) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}

// Selectors
@Composable
fun CategorySelector(selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(selected)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.FilterList, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { onSelected(it); expanded = false })
            }
        }
    }
}

@Composable
fun SectionSelector(selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(selected)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.FilterList, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { onSelected(it); expanded = false })
            }
        }
    }
}

@Composable
fun RoleSelector(selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(selected)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.FilterList, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { onSelected(it); expanded = false })
            }
        }
    }
}

/**
 * View 2: Taxonomy Manager screen (can freely add and remove Custom Categories, Sections, & Roles)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationScreen(viewModel: VaultViewModel) {
    val categories by viewModel.categories.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val roles by viewModel.roles.collectAsState()

    var activeTaxonomyTab by remember { mutableStateOf(0) }
    var inputAddField by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organizar Taxonomías", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Tabs for deciding what to manage (Category / Section / Role)
            TabRow(selectedTabIndex = activeTaxonomyTab) {
                Tab(selected = activeTaxonomyTab == 0, onClick = { activeTaxonomyTab = 0; inputAddField = "" }, text = { Text("Categorías") })
                Tab(selected = activeTaxonomyTab == 1, onClick = { activeTaxonomyTab = 1; inputAddField = "" }, text = { Text("Secciones") })
                Tab(selected = activeTaxonomyTab == 2, onClick = { activeTaxonomyTab = 2; inputAddField = "" }, text = { Text("Roles") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputAddField,
                    onValueChange = { inputAddField = it },
                    label = {
                        Text(
                            when (activeTaxonomyTab) {
                                0 -> "Nueva Categoría"
                                1 -> "Nueva Sección"
                                else -> "Nuevo Rol"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (inputAddField.isNotBlank()) {
                            when (activeTaxonomyTab) {
                                0 -> viewModel.addCategory(inputAddField)
                                1 -> viewModel.addSection(inputAddField)
                                2 -> viewModel.addRole(inputAddField)
                            }
                            inputAddField = ""
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Añadir")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Taxonomías Disponibles",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Dynamic grid list of elements with delete option
            val currentList = when (activeTaxonomyTab) {
                0 -> categories.map { it.name }
                1 -> sections.map { it.name }
                else -> roles.map { it.name }
            }

            if (currentList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay elementos. Ingrese uno arriba.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentList) { name ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (activeTaxonomyTab) {
                                            0 -> Icons.Filled.Category
                                            1 -> Icons.Filled.FilterList
                                            else -> Icons.Filled.Person
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }

                                IconButton(onClick = {
                                    when (activeTaxonomyTab) {
                                        0 -> viewModel.removeCategory(name)
                                        1 -> viewModel.removeSection(name)
                                        2 -> viewModel.removeRole(name)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * View 3: Reports Center and Secure Synchronizer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncExportScreen(viewModel: VaultViewModel) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val roles by viewModel.roles.collectAsState()

    var showExportPasswordInput by remember { mutableStateOf("") }
    var showImportPasswordInput by remember { mutableStateOf("") }
    var importPayloadString by remember { mutableStateOf("") }
    
    var shareWithDecryptedPasswords by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes y Sincronización", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Export Documents section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Exportar Reportes A4", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            text = "Genera documentos oficiales foliados o plantillas en formato de página A4 con tus cuentas. Elige si deseas incluir las contraseñas desencriptadas o conservarlas ocultas de forma segura.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showExportPasswordInput = ""/* Reset */ }
                        ) {
                            Checkbox(
                                checked = shareWithDecryptedPasswords,
                                onCheckedChange = { shareWithDecryptedPasswords = it }
                            )
                            Text("Imprimir contraseñas legibles (Requiere boveda)", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    ExportUtils.sharePdfReport(
                                        context = context,
                                        accounts = accounts,
                                        masterKey = viewModel.masterKey,
                                        showPasswords = shareWithDecryptedPasswords
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PDF A4", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    ExportUtils.sharePngReport(
                                        context = context,
                                        accounts = accounts,
                                        masterKey = viewModel.masterKey,
                                        showPasswords = shareWithDecryptedPasswords
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pág PNG A4", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Sync with AES-256 cloud-less payload
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Sincronización Encriptada AES-256", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "Asegure sus datos emitiendo o leyendo bocks de sincronización. Este proceso encripta todo su archivo (cuentas, categorías, roles y notas) con AES-256 usando su contraseña elegida.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // Segment A: Export payload block
                        Text("Emitir Block de Sincronización", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        var generatedSyncBlock by remember { mutableStateOf("") }
                        val clipboardManager = LocalClipboardManager.current

                        OutlinedTextField(
                            value = showExportPasswordInput,
                            onValueChange = { showExportPasswordInput = it },
                            label = { Text("Contraseña de Respaldo") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (showExportPasswordInput.length < 6) {
                                    viewModel.showFeedback("Contraseña de respaldo muy corta")
                                    return@Button
                                }
                                val block = viewModel.getEncryptedBackupString(showExportPasswordInput)
                                generatedSyncBlock = block
                                if (block.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(block))
                                    viewModel.showFeedback("¡Bloque encriptado y copiado al portapapeles!")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Generar y Copiar Código de Sync")
                        }

                        if (generatedSyncBlock.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = generatedSyncBlock,
                                onValueChange = {},
                                label = { Text("Código de Sync AES-256") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Segment B: Import payload block
                        Text("Restaurar / Unir desde Sync Block", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = importPayloadString,
                            onValueChange = { importPayloadString = it },
                            placeholder = { Text("Pegar el código de sincronización encriptado aquí...") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = showImportPasswordInput,
                            onValueChange = { showImportPasswordInput = it },
                            label = { Text("Contraseña del Respaldo") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (importPayloadString.isBlank() || showImportPasswordInput.isBlank()) {
                                    viewModel.showFeedback("Complete todos los campos de restauración")
                                    return@Button
                                }
                                val success = viewModel.importEncryptedBackupString(
                                    importPayloadString.trim(),
                                    showImportPasswordInput
                                )
                                if (success) {
                                    importPayloadString = ""
                                    showImportPasswordInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Unir y Desencriptar Sync Block")
                        }
                    }
                }
            }
        }
    }
}
