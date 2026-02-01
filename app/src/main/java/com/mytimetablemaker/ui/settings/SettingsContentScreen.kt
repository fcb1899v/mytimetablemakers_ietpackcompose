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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.res.stringResource
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.ui.common.AdMobBannerView
import com.mytimetablemaker.ui.common.CommonComponents
import com.mytimetablemaker.ui.main.MainViewModel
import com.mytimetablemaker.ui.theme.*
import android.content.SharedPreferences
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.core.content.edit

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
    
    // Observe login status from LoginViewModel
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
    
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Primary)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(Primary)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(White)
                .padding(paddingValues)
        ) {
        // MARK: - Main Content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // MARK: - Top App Bar
            val topBarHeight = ScreenSize.settingsSheetTopBarHeight()
            val backButtonPadding = ScreenSize.settingsSheetBackButtonPadding()
            val titleFontSize = ScreenSize.settingsTitleFontSize()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topBarHeight)
                    .background(Primary)
            ) {
                // Back button aligned to the left
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = backButtonPadding)
                ) {
                    CommonComponents.CustomBackButton(
                        foregroundColor = White,
                        onClick = onNavigateToMain
                    )
                }
                
                // Title centered on screen
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = titleFontSize.value.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
            }
            
            // MARK: - Loading Indicator
            if (firestoreViewModel.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Gray.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(horizontal = ScreenSize.settingsSheetHorizontalPadding()),
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
                    .padding(top = ScreenSize.settingsHeaderPadding())
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
                    TextButton(
                        onClick = { },
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
                    TextButton(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenSize.settingsSheetHorizontalPadding())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                color = Gray,
                                modifier = Modifier.padding(end = ScreenSize.settingsFontSize())
                            )
                        }
                    }
                }
                
                // MARK: - Ad Banner (画面最下部)
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    AdMobBannerView()
                }
            }
        }
    }
    
    // MARK: - Alerts
    // Responsive font sizes for alerts
    val alertTitleFontSize = ScreenSize.alertDialogTitleFontSize()
    val alertTextFontSize = ScreenSize.alertDialogTextFontSize()
    val alertButtonFontSize = ScreenSize.alertDialogButtonFontSize()
    
    // Logout Alert
    if (showLogoutAlert) {
        AlertDialog(
            onDismissRequest = { showLogoutAlert = false },
            title = { 
                Text(
                    text = stringResource(R.string.logout),
                    fontSize = alertTitleFontSize.value.sp
                ) 
            },
            text = { 
                Text(
                    text = stringResource(R.string.logoutYourAccount),
                    fontSize = alertTextFontSize.value.sp
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutAlert = false
                        loginViewModel.logOut()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        fontSize = alertButtonFontSize.value.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutAlert = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = alertButtonFontSize.value.sp
                    )
                }
            },
            containerColor = White
        )
    }
    
    // Delete Account Alert
    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            title = { 
                Text(
                    text = stringResource(R.string.deleteAccount),
                    fontSize = alertTitleFontSize.value.sp
                ) 
            },
            text = { 
                Text(
                    text = "⚠️ ${stringResource(R.string.deleteYourAccount)}",
                    fontSize = alertTextFontSize.value.sp
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAlert = false
                        loginViewModel.delete()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        fontSize = alertButtonFontSize.value.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAlert = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = alertButtonFontSize.value.sp
                    )
                }
            },
            containerColor = White
        )
    }
    
    // Get Firestore Alert
    if (showGetFirestoreAlert) {
        AlertDialog(
            onDismissRequest = { showGetFirestoreAlert = false },
            title = { 
                Text(
                    text = stringResource(R.string.getSavedData),
                    fontSize = alertTitleFontSize.value.sp
                ) 
            },
            text = { 
                Text(
                    text = "⚠️ ${stringResource(R.string.overwrittenCurrentData)}",
                    fontSize = alertTextFontSize.value.sp
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGetFirestoreAlert = false
                        firestoreViewModel.getFirestore()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        fontSize = alertButtonFontSize.value.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGetFirestoreAlert = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = alertButtonFontSize.value.sp
                    )
                }
            },
            containerColor = White
        )
    }
    
    // Save Firestore Alert
    if (showSaveFirestoreAlert) {
        AlertDialog(
            onDismissRequest = { showSaveFirestoreAlert = false },
            title = { 
                Text(
                    text = stringResource(R.string.saveCurrentData),
                    fontSize = alertTitleFontSize.value.sp
                ) 
            },
            text = { 
                Text(
                    text = "⚠️ ${stringResource(R.string.overwrittenSavedData)}",
                    fontSize = alertTextFontSize.value.sp
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveFirestoreAlert = false
                        firestoreViewModel.setFirestore()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        fontSize = alertButtonFontSize.value.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveFirestoreAlert = false }
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = alertButtonFontSize.value.sp
                    )
                }
            },
            containerColor = White
        )
    }
    
    // MARK: - Firestore Result Alert
    if (firestoreViewModel.isShowMessage) {
        AlertDialog(
            onDismissRequest = { firestoreViewModel.dismissMessage() },
            title = {
                Text(
                    text = firestoreViewModel.title,
                    fontSize = alertTitleFontSize.value.sp
                )
            },
            text = {
                Text(
                    text = firestoreViewModel.message,
                    fontSize = alertTextFontSize.value.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val success = firestoreViewModel.isFirestoreSuccess
                        firestoreViewModel.dismissMessage()
                        if (success) {
                            // Update transfer data for GetFirestore operation
                            mainViewModel.updateAllDataFromUserDefaults()
                            onNavigateToMain()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        fontSize = alertButtonFontSize.value.sp
                    )
                }
            },
            containerColor = White
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
            modifier = Modifier.padding(
                horizontal = ScreenSize.settingsSheetHorizontalPadding(),
                vertical = ScreenSize.settingsHeaderPadding()
            )
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
            .padding(
                horizontal = ScreenSize.settingsSheetHorizontalPadding(),
                vertical = ScreenSize.settingsSheetInputPaddingVertical()
            )
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
// Save the same value to both back2 and go2
private fun saveRoute2Setting(
    sharedPreferences: SharedPreferences,
    value: Boolean,
    mainViewModel: MainViewModel
) {
    // Save to SharedPreferences for both routes
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