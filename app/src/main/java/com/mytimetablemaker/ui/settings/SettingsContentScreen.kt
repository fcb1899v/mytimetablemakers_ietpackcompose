package com.mytimetablemaker.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.res.stringResource
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.*
import com.mytimetablemaker.ui.main.MainViewModel
import com.mytimetablemaker.ui.theme.Primary
import com.mytimetablemaker.ui.theme.Gray
import android.content.SharedPreferences

// MARK: - Settings Content Screen
// Main settings screen with route configuration, account management, and app information
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContentScreen(
    mainViewModel: MainViewModel,
    firestoreViewModel: FirestoreViewModel,
    onNavigateToMain: () -> Unit = {},
    onNavigateToTransferSheet: () -> Unit = {},
    onNavigateToLineSheet: (goorback: String) -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val sharedPreferences = application.getSharedPreferences("SettingsContentScreen", Application.MODE_PRIVATE)
    
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
    
    // Check login status from SharedPreferences
    val isLoginSuccess = remember {
        sharedPreferences.getBoolean("Login", false)
    }
    
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToMain) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.backToHomepage),
                            tint = Color.White
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
                        .background(Color.Gray.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
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
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.anotherRoute),
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        // TODO: Implement CustomToggle
                        Switch(
                            checked = showRoute2,
                            onCheckedChange = { newValue ->
                                showRoute2 = newValue
                                saveRoute2Setting(sharedPreferences, newValue, mainViewModel)
                            }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.version),
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Text(
                            text = getAppVersion(context),
                            fontSize = 16.sp,
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
                        // TODO: Implement logout
                        // loginViewModel.logOut()
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
                        // TODO: Implement delete account
                        // loginViewModel.delete()
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
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
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.Black
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Gray
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
            .height(50.dp)
            .background(Primary),
        contentAlignment = Alignment.Center
    ) {
        // TODO: Implement AdMob banner
        Text(
            text = "Ad Banner",
            color = Color.White
        )
    }
}

// MARK: - Helper Functions

// Load Route 2 display setting from SharedPreferences
// Checks both back2 and go2 settings, uses OR logic
private fun loadRoute2Setting(
    sharedPreferences: SharedPreferences,
    onLoaded: (Boolean) -> Unit
) {
    val back2Route2Value = sharedPreferences.getBoolean("back2".isShowRoute2Key(), false)
    val go2Route2Value = sharedPreferences.getBoolean("go2".isShowRoute2Key(), false)
    
    // Use OR logic: show Route 2 if either back2 or go2 is enabled
    onLoaded(back2Route2Value || go2Route2Value)
}

// Save Route 2 display setting to SharedPreferences and update ViewModel
// Saves the same value to both back2 and go2
private fun saveRoute2Setting(
    sharedPreferences: SharedPreferences,
    value: Boolean,
    mainViewModel: MainViewModel
) {
    // Save to SharedPreferences for both routes
    sharedPreferences.edit()
        .putBoolean("back2".isShowRoute2Key(), value)
        .putBoolean("go2".isShowRoute2Key(), value)
        .apply()
    
    // Update MainViewModel
    mainViewModel.isShowBackRoute2 = value
    mainViewModel.isShowGoRoute2 = value
}

// Open URL in browser
private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
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

