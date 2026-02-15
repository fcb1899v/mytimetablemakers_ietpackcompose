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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.core.content.edit

// Settings screen for routes, account, and app info.
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
    // SharedPreferences used by MainViewModel.
    val sharedPreferences = context.getSharedPreferences("MainViewModel", android.content.Context.MODE_PRIVATE)
    
    // UI state for sheets, alerts, and navigation.
    var showTransferSheet by remember { mutableStateOf(false) }
    var showLineSheet by remember { mutableStateOf(false) }
    var selectedRoute by remember { mutableStateOf("back1") }
    var showRoute2 by remember { mutableStateOf(false) }
    var showLogoutAlert by remember { mutableStateOf(false) }
    var showDeleteAlert by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var showGetFirestoreAlert by remember { mutableStateOf(false) }
    var getFirestorePassword by remember { mutableStateOf("") }
    var showSaveFirestoreAlert by remember { mutableStateOf(false) }
    var saveFirestorePassword by remember { mutableStateOf("") }
    
    // Login state and alert messages.
    val isLoginSuccess by loginViewModel.isLoginSuccess.collectAsState()
    val isShowLoginMessage by loginViewModel.isShowMessage.collectAsState()
    val loginAlertTitle by loginViewModel.alertTitle.collectAsState()
    val loginAlertMessage by loginViewModel.alertMessage.collectAsState()
    
    // Set status bar to match the header color.
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
    
    // Load Route 2 setting on appear.
    LaunchedEffect(Unit) {
        loadRoute2Setting(sharedPreferences) { value ->
            showRoute2 = value
        }
    }
    
    // Sync Route 2 toggle with SharedPreferences.
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
        // Main content.
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top app bar.
            val topBarHeight = ScreenSize.settingsSheetTopBarHeight()
            val backButtonPadding = ScreenSize.settingsSheetBackButtonPadding()
            val titleFontSize = ScreenSize.settingsTitleFontSize()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topBarHeight)
                    .background(Primary)
            ) {
                // Back button.
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
                
                // Title.
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = titleFontSize.value.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
            }
            
            // Loading overlay.
            if (firestoreViewModel.isLoading) {
                CommonComponents.CustomProgressIndicator(text = firestoreViewModel.loadingMessage)
            }
            
            // Settings form.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = ScreenSize.settingsHeaderPadding())
            ) {
                // Various settings.
                SettingsSection(
                    title = stringResource(R.string.variousSettings)
                ) {
                    // Home/destination settings.
                    SettingsButton(
                        title = stringResource(R.string.homeDestinationSettings),
                        onClick = { showTransferSheet = true }
                    )
                    
                    // Route settings.
                    SettingsButton(
                        title = stringResource(R.string.routeSettings),
                        onClick = {
                            selectedRoute = "back1"
                            showLineSheet = true
                        }
                    )
                    
                    // Route 2 display toggle.
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
                                rightText = stringResource(R.string.display),
                            )
                        }
                    }
                    
                    // Firestore actions (logged-in only).
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
                
                // Account actions.
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
                
                // About section.
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
                
                // Ad banner at the bottom.
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
    
    // Alerts.
    if (showLogoutAlert) {
        CommonComponents.CustomAlertDialog(
            onDismissRequest = { showLogoutAlert = false },
            title = stringResource(R.string.logout),
            alertMessage = stringResource(R.string.logoutYourAccount),
            confirmButtonText = stringResource(R.string.ok),
            onConfirmClick = {
                showLogoutAlert = false
                loginViewModel.logOut()
            },
            dismissButtonText = stringResource(R.string.cancel),
        )
    }
    
    // Delete account alert.
    if (showDeleteAlert) {
        CommonComponents.CustomAlertDialog(
            onDismissRequest = {
                showDeleteAlert = false
                deletePassword = ""
            },
            title = stringResource(R.string.deleteAccount),
            confirmButtonText = stringResource(R.string.delete),
            textContent = {
                Column {
                    Text(
                        text = stringResource(R.string.deleteYourAccount),
                        fontSize = ScreenSize.alertDialogTextFontSize().value.sp,
                        color = Black
                    )
                    Spacer(modifier = Modifier.height(ScreenSize.loginSpacingSmall()))
                    CommonComponents.CustomLoginTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        placeholder = stringResource(R.string.deleteAccountPasswordPlaceholder),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )
                }
            },
            onConfirmClick = {
                loginViewModel.delete(deletePassword)
                showDeleteAlert = false
                deletePassword = ""
            },
            dismissButtonText = stringResource(R.string.cancel),
            icon = Icons.Default.Warning,
            isDestructive = true
        )
    }
    
    // Get Firestore alert.
    if (showGetFirestoreAlert) {
        CommonComponents.CustomAlertDialog(
            onDismissRequest = {
                showGetFirestoreAlert = false
                getFirestorePassword = ""
            },
            title = stringResource(R.string.getSavedData),
            confirmButtonText = stringResource(R.string.ok),
            textContent = {
                Column {
                    Text(
                        text = stringResource(R.string.overwrittenCurrentData),
                        fontSize = ScreenSize.alertDialogTextFontSize().value.sp,
                        color = Black
                    )
                    Spacer(modifier = Modifier.height(ScreenSize.loginSpacingSmall()))
                    CommonComponents.CustomLoginTextField(
                        value = getFirestorePassword,
                        onValueChange = { getFirestorePassword = it },
                        placeholder = stringResource(R.string.deleteAccountPasswordPlaceholder),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )
                }
            },
            onConfirmClick = {
                firestoreViewModel.getFirestore(getFirestorePassword)
                showGetFirestoreAlert = false
                getFirestorePassword = ""
            },
            dismissButtonText = stringResource(R.string.cancel),
            icon = Icons.Default.Warning
        )
    }
    
    // Save Firestore alert.
    if (showSaveFirestoreAlert) {
        CommonComponents.CustomAlertDialog(
            onDismissRequest = {
                showSaveFirestoreAlert = false
                saveFirestorePassword = ""
            },
            title = stringResource(R.string.saveCurrentData),
            confirmButtonText = stringResource(R.string.ok),
            textContent = {
                Column {
                    Text(
                        text = stringResource(R.string.overwrittenSavedData),
                        fontSize = ScreenSize.alertDialogTextFontSize().value.sp,
                        color = Black
                    )
                    Spacer(modifier = Modifier.height(ScreenSize.loginSpacingSmall()))
                    CommonComponents.CustomLoginTextField(
                        value = saveFirestorePassword,
                        onValueChange = { saveFirestorePassword = it },
                        placeholder = stringResource(R.string.deleteAccountPasswordPlaceholder),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )
                }
            },
            onConfirmClick = {
                firestoreViewModel.setFirestore(saveFirestorePassword)
                showSaveFirestoreAlert = false
                saveFirestorePassword = ""
            },
            dismissButtonText = stringResource(R.string.cancel),
            icon = Icons.Default.Warning
        )
    }
    
    // Logout result alert.
    if (isShowLoginMessage) {
        CommonComponents.CustomAlertDialog(
            onDismissRequest = { loginViewModel.dismissMessage() },
            title = loginAlertTitle,
            alertMessage = loginAlertMessage,
            confirmButtonText = stringResource(R.string.ok),
            onConfirmClick = { loginViewModel.dismissMessage() }
        )
    }
    
    // Firestore result alert.
    if (firestoreViewModel.isShowMessage) {
        CommonComponents.CustomAlertDialog(
            onDismissRequest = { firestoreViewModel.dismissMessage() },
            title = firestoreViewModel.title,
            alertMessage = firestoreViewModel.message,
            confirmButtonText = stringResource(R.string.ok),
            onConfirmClick = {
                val success = firestoreViewModel.isFirestoreSuccess
                firestoreViewModel.dismissMessage()
                if (success) {
                    mainViewModel.updateAllDataFromUserDefaults()
                    onNavigateToMain()
                }
            }
        )
    }
    
    // Handle sheet navigation.
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

// Settings section for grouped items.
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

// Settings button row.
@Composable
private fun SettingsButton(
    title: String,
    subtitle: String? = null,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = ScreenSize.settingsFontSize().value.sp,
                    color = Black
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = (ScreenSize.settingsFontSize().value * 0.85f).sp,
                        color = Gray
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Gray,
                modifier = Modifier.size(ScreenSize.settingsFontSize())
            )
        }
    }
}

// Load Route 2 display setting from SharedPreferences.
private fun loadRoute2Setting(
    sharedPreferences: SharedPreferences,
    onLoaded: (Boolean) -> Unit
) {
    val route2Value = "back2".isShowRoute2Key().userDefaultsBool(sharedPreferences, false)
    onLoaded(route2Value)
}

// Save Route 2 display setting and update the ViewModel.
private fun saveRoute2Setting(
    sharedPreferences: SharedPreferences,
    value: Boolean,
    mainViewModel: MainViewModel
) {
    // Save to both back2 and go2.
    sharedPreferences.edit(commit = true) {
        putBoolean("back2".isShowRoute2Key(), value)
        putBoolean("go2".isShowRoute2Key(), value)
    }
    
    // Update MainViewModel flags.
    mainViewModel.setRoute2Value(value)
}

// Open URL in browser.
private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("SettingsContentScreen", "Failed to open URL: $url", e)
        android.widget.Toast.makeText(context, context.getString(R.string.couldNotOpenLink), android.widget.Toast.LENGTH_SHORT).show()
    }
}

// Get app version from PackageManager.
private fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "Unknown"
    } catch (_: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}