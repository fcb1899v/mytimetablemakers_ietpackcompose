package com.mytimetablemaker.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.res.stringResource
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.ui.common.CommonComponents
import com.mytimetablemaker.ui.main.MainViewModel
import com.mytimetablemaker.ui.theme.*
import android.content.SharedPreferences
import androidx.core.net.toUri
import androidx.core.content.edit
import java.util.Locale

// MARK: - Settings Content Screen
// Main settings screen with route configuration, account management, and app information
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContentScreen(
    mainViewModel: MainViewModel,
    loginViewModel: com.mytimetablemaker.ui.login.LoginViewModel,
    firestoreViewModel: FirestoreViewModel,
    onNavigateToMain: () -> Unit = {},
    onNavigateToTransferSheet: () -> Unit = {},
    onNavigateToLineSheet: (goorback: String) -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    // Use the same SharedPreferences instance as MainViewModel to ensure listeners work
    val sharedPreferences = context.getSharedPreferences("MainViewModel", android.content.Context.MODE_PRIVATE)
    
    // MARK: - State Properties
    // Control visibility of sheets, alerts, and navigation state
    var showTransferSheet by remember { mutableStateOf(false) }
    var showLineSheet by remember { mutableStateOf(false) }
    var selectedRoute by remember { mutableStateOf("back1") }
    var showRoute2 by remember { mutableStateOf(false) }
    var showLogoutAlert by remember { mutableStateOf(false) }
    var showDeleteAlert by remember { mutableStateOf(false) }
    var showGetFirestoreAlert by remember { mutableStateOf(false) }
    var showSaveFirestoreAlert by remember { mutableStateOf(false) }
    
    // Language setting state
    var isJapaneseSelected by remember { mutableStateOf(true) }
    
    // Observe login status from LoginViewModel (matches SwiftUI myLogin.isLoginSuccess)
    val isLoginSuccess by loginViewModel.isLoginSuccess.collectAsState()
    
    // Set status bar color to Primary
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            @Suppress("DEPRECATION")
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = false
            }
        }
        onDispose { }
    }
    
    // Load Route 2 setting on appear
    LaunchedEffect(Unit) {
        loadRoute2Setting(sharedPreferences) { value ->
            showRoute2 = value
        }
        // Load language setting
        loadLanguageSetting(sharedPreferences) { isJapanese ->
            isJapaneseSelected = isJapanese
        }
    }
    
    // Listen to SharedPreferences changes for route2 setting
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "back2".isShowRoute2Key() || key == "go2".isShowRoute2Key()) {
                loadRoute2Setting(sharedPreferences) { value ->
                    showRoute2 = value
                }
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        // MARK: - Status Bar Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Primary)
                .align(Alignment.TopCenter)
        )
        
        // MARK: - Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // MARK: - Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        fontSize = ScreenSize.settingsTitleFontSize().value.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToMain) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.backToHomepage),
                            tint = White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary
                )
            )
            
            // MARK: - Loading Indicator
            if (firestoreViewModel.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Gray.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(ScreenSize.settingsSheetHorizontalPadding()),
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(ScreenSize.settingsSheetHorizontalPadding())
                        )
                    }
                }
            }
            
            // MARK: - Settings Form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // MARK: - Various Settings Section
                SettingsSection(
                    title = stringResource(R.string.variousSettings)
                ) {
                    // Home and Destination button
                    SettingsButton(
                        title = stringResource(R.string.homeDestinationSettings),
                        onClick = { showTransferSheet = true }
                    )
                    
                    // Route Settings button
                    SettingsButton(
                        title = stringResource(R.string.routeSettings),
                        onClick = {
                            selectedRoute = "back1"
                            showLineSheet = true
                        }
                    )
                    
                    // Route 2 display toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenSize.settingsSheetHorizontalPadding(), vertical = ScreenSize.settingsSheetInputPaddingVertical()),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.anotherRoute),
                            fontSize = ScreenSize.settingsFontSize().value.sp,
                            color = Black
                        )
                        CommonComponents.CustomToggle(
                            isLeftSelected = !showRoute2,
                            onToggle = { isLeftSelected ->
                                val newShowRoute2 = !isLeftSelected
                                showRoute2 = newShowRoute2
                                saveRoute2Setting(sharedPreferences, newShowRoute2, mainViewModel)
                            },
                            leftText = stringResource(R.string.hide),
                            leftColor = Gray,
                            rightText = stringResource(R.string.display),
                            rightColor = Primary,
                            circleColor = White,
                            offColor = Gray
                        )
                    }
                    
                    // Firestore data management buttons (only shown when logged in)
                    if (isLoginSuccess) {
                        SettingsButton(
                            title = stringResource(R.string.getSavedData),
                            onClick = { showGetFirestoreAlert = true }
                        )
                        SettingsButton(
                            title = stringResource(R.string.saveCurrentData),
                            onClick = { showSaveFirestoreAlert = true }
                        )
                    }
                }
                
                // MARK: - Account Section
                SettingsSection(
                    title = stringResource(R.string.account)
                ) {
                    if (isLoginSuccess) {
                        SettingsButton(
                            title = stringResource(R.string.logout),
                            onClick = { showLogoutAlert = true }
                        )
                        SettingsButton(
                            title = stringResource(R.string.deleteAccount),
                            onClick = { showDeleteAlert = true }
                        )
                    } else {
                        SettingsButton(
                            title = stringResource(R.string.manageYourDataAfterLogin),
                            onClick = onNavigateToLogin
                        )
                    }
                }
                
                // MARK: - About Section
                SettingsSection(
                    title = stringResource(R.string.about)
                ) {
                    val contactUrl = stringResource(R.string.contactUrl)
                    val termsUrl = stringResource(R.string.termsUrl)
                    
                    SettingsButton(
                        title = stringResource(R.string.contact),
                        onClick = {
                            openUrl(context, contactUrl)
                        }
                    )
                    SettingsButton(
                        title = stringResource(R.string.termsAndPrivacyPolicy),
                        onClick = {
                            openUrl(context, termsUrl)
                        }
                    )
                    // Language setting toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenSize.settingsSheetHorizontalPadding(), vertical = ScreenSize.settingsSheetInputPaddingVertical()),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.language),
                            fontSize = ScreenSize.settingsFontSize().value.sp,
                            color = Black
                        )
                        CommonComponents.CustomToggle(
                            isLeftSelected = isJapaneseSelected,
                            onToggle = { isLeftSelected ->
                                isJapaneseSelected = isLeftSelected
                                saveLanguageSetting(sharedPreferences, isLeftSelected, context)
                            },
                            leftText = stringResource(R.string.japanese),
                            leftColor = Primary,
                            rightText = stringResource(R.string.english),
                            rightColor = Primary,
                            circleColor = White,
                            offColor = Gray
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenSize.settingsSheetHorizontalPadding(), vertical = ScreenSize.settingsSheetInputPaddingVertical()),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.version),
                            fontSize = ScreenSize.settingsFontSize().value.sp,
                            color = Black
                        )
                        Text(
                            text = getAppVersion(context),
                            fontSize = ScreenSize.settingsFontSize().value.sp,
                            color = Gray
                        )
                    }
                }
                
                // MARK: - Ad Banner
                Spacer(modifier = Modifier.weight(1f))
                AdBannerSection()
            }
        }
    }
    
    // MARK: - Alerts
    // Logout Alert
    if (showLogoutAlert) {
        AlertDialog(
            onDismissRequest = { showLogoutAlert = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.logoutYourAccount)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutAlert = false
                        loginViewModel.logOut()
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutAlert = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Delete Account Alert
    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            title = { Text(stringResource(R.string.deleteAccount)) },
            text = { Text("⚠️ ${stringResource(R.string.deleteYourAccount)}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAlert = false
                        loginViewModel.delete()
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAlert = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Get Firestore Alert
    if (showGetFirestoreAlert) {
        AlertDialog(
            onDismissRequest = { showGetFirestoreAlert = false },
            title = { Text(stringResource(R.string.getSavedData)) },
            text = { Text("⚠️ ${stringResource(R.string.overwrittenCurrentData)}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGetFirestoreAlert = false
                        firestoreViewModel.getFirestore()
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGetFirestoreAlert = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Save Firestore Alert
    if (showSaveFirestoreAlert) {
        AlertDialog(
            onDismissRequest = { showSaveFirestoreAlert = false },
            title = { Text(stringResource(R.string.saveCurrentData)) },
            text = { Text("⚠️ ${stringResource(R.string.overwrittenSavedData)}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveFirestoreAlert = false
                        firestoreViewModel.setFirestore()
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveFirestoreAlert = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Handle sheet navigation
    LaunchedEffect(showTransferSheet) {
        if (showTransferSheet) {
            onNavigateToTransferSheet()
            showTransferSheet = false
        }
    }
    
    LaunchedEffect(showLineSheet) {
        if (showLineSheet) {
            onNavigateToLineSheet(selectedRoute)
            showLineSheet = false
        }
    }
}

// MARK: - Settings Section
// Reusable section component for settings form
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = ScreenSize.settingsHeaderFontSize().value.sp,
            fontWeight = FontWeight.Bold,
            color = Gray,
            modifier = Modifier.padding(horizontal = ScreenSize.settingsSheetHorizontalPadding(), vertical = ScreenSize.settingsHeaderFontSize())
        )
        content()
    }
}

// MARK: - Settings Button
// Reusable button component for settings actions
@Composable
private fun SettingsButton(
    title: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenSize.settingsSheetHorizontalPadding(), vertical = ScreenSize.settingsSheetInputPaddingVertical())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = ScreenSize.settingsFontSize().value.sp,
                color = Black
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Gray,
                modifier = Modifier.size(ScreenSize.settingsFontSize())
            )
        }
    }
}

// MARK: - Ad Banner Section
// Displays advertisement banner
@Composable
private fun AdBannerSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ScreenSize.admobBannerHeight())
            .background(Primary),
        contentAlignment = Alignment.Center
    ) {
        // TODO: Implement AdMob banner
        com.mytimetablemaker.ui.common.AdMobBannerView()
    }
}

// MARK: - Helper Functions

// Load Route 2 display setting from SharedPreferences
// Since saveRoute2Setting saves the same value to both back2 and go2, we can read either one
private fun loadRoute2Setting(
    sharedPreferences: SharedPreferences,
    onLoaded: (Boolean) -> Unit
) {
    val route2Value = "back2".isShowRoute2Key().userDefaultsBool(sharedPreferences, false)
    onLoaded(route2Value)
}

// Save Route 2 display setting to SharedPreferences and update ViewModel
// Match SwiftUI implementation: save the same value to both back2 and go2
private fun saveRoute2Setting(
    sharedPreferences: SharedPreferences,
    value: Boolean,
    mainViewModel: MainViewModel
) {
    // Save to SharedPreferences for both routes (match SwiftUI implementation)
    // Use commit() to ensure synchronous save
    sharedPreferences.edit(commit = true) {
        putBoolean("back2".isShowRoute2Key(), value)
        putBoolean("go2".isShowRoute2Key(), value)
    }
    
    // Update MainViewModel (update both back and go route flags)
    mainViewModel.setRoute2Value(value)
}

// Open URL in browser
private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}

// Get app version from package manager
private fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "Unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

// Load language setting from SharedPreferences
private fun loadLanguageSetting(
    sharedPreferences: SharedPreferences,
    onLoaded: (Boolean) -> Unit
) {
    val currentLanguage = Locale.getDefault().language
    val savedLanguage = sharedPreferences.getString("app_language", currentLanguage) ?: currentLanguage
    onLoaded(savedLanguage == "ja")
}

// Save language setting to SharedPreferences and apply it
private fun saveLanguageSetting(
    sharedPreferences: SharedPreferences,
    isJapanese: Boolean,
    context: android.content.Context
) {
    val languageCode = if (isJapanese) "ja" else "en"
    sharedPreferences.edit {
        putString("app_language", languageCode)
    }
    
    // Apply language setting
    applyLanguageSetting(context, languageCode)
}

// Apply language setting to the app
private fun applyLanguageSetting(context: android.content.Context, languageCode: String) {
    val locale = Locale.forLanguageTag(languageCode)
    Locale.setDefault(locale)
    
    val configuration = context.resources.configuration
    configuration.setLocale(locale)
    // Use createConfigurationContext for API 17+ instead of deprecated updateConfiguration
    // This creates a new context with the updated configuration
    context.createConfigurationContext(configuration)
    
    // Restart activity to apply language change to the entire app
    if (context is android.app.Activity) {
        context.recreate()
    }
}

