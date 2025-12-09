package com.mytimetablemaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mytimetablemaker.ui.login.LoginViewModel
import com.mytimetablemaker.ui.login.LoginContentScreen
import com.mytimetablemaker.ui.main.MainContentScreen
import com.mytimetablemaker.ui.main.MainViewModel
import com.mytimetablemaker.ui.main.SplashContentScreen
import com.mytimetablemaker.ui.settings.FirestoreViewModel
import com.mytimetablemaker.ui.settings.SettingsContentScreen
import com.mytimetablemaker.ui.settings.SettingsLineSheetScreen
import com.mytimetablemaker.ui.settings.SettingsTransferSheetScreen
import com.mytimetablemaker.ui.theme.MyTransitMakers_JetpackComposeTheme

// MARK: - Main Activity
// Main entry point for the Android timetable maker application
// Matches SwiftUI mytimetablemaker_swiftuiApp structure
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyTransitMakers_JetpackComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

// MARK: - App Navigation
// Manages app navigation and ViewModel initialization
// Matches SwiftUI app structure with TransitViewModel, LoginViewModel, and FirestoreViewModel
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    
    // Initialize core ViewModels (matching SwiftUI structure)
    val mainViewModel = remember { MainViewModel(application) }
    val loginViewModel = remember { LoginViewModel(context) }
    val firestoreViewModel = remember { FirestoreViewModel(application) }
    
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // MARK: - Splash Screen
        // Initial screen shown when app launches
        composable("splash") {
            SplashContentScreen(
                mainViewModel = mainViewModel,
                firestoreViewModel = firestoreViewModel,
                onNavigateToMain = {
                    navController.navigate("main") {
                        // Clear splash screen from back stack
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        
        // MARK: - Main Content Screen
        // Primary screen displaying transfer information and timetables
        composable("main") {
            MainContentScreen(
                viewModel = mainViewModel,
                firestoreViewModel = firestoreViewModel,
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToLineSheet = { goorback, lineIndex ->
                    navController.navigate("line_settings/$goorback/$lineIndex")
                },
                onNavigateToTransferSheet = {
                    navController.navigate("transfer_settings/back1")
                }
            )
        }
        
        // MARK: - Login Screen
        // User authentication and account management
        composable("login") {
            LoginContentScreen(
                mainViewModel = mainViewModel,
                loginViewModel = loginViewModel,
                firestoreViewModel = firestoreViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        // Clear login screen from back stack
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        // MARK: - Settings Screen
        // Application settings and configuration
        composable("settings") {
            SettingsContentScreen(
                mainViewModel = mainViewModel,
                firestoreViewModel = firestoreViewModel,
                onNavigateToMain = {
                    navController.popBackStack()
                },
                onNavigateToTransferSheet = {
                    navController.navigate("transfer_settings/back1")
                },
                onNavigateToLineSheet = { goorback ->
                    navController.navigate("line_settings/$goorback/0")
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }
        
        // MARK: - Line Settings Screen
        // Configure transportation line settings
        composable("line_settings/{goorback}/{lineIndex}") { backStackEntry ->
            val goorback = backStackEntry.arguments?.getString("goorback") ?: "back1"
            val lineIndex = backStackEntry.arguments?.getString("lineIndex")?.toIntOrNull() ?: 0
            SettingsLineSheetScreen(
                goorback = goorback,
                lineIndex = lineIndex,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTimetableSettings = { gb, idx ->
                    navController.navigate("timetable_settings/$gb/$idx")
                }
            )
        }
        
        // MARK: - Transfer Settings Screen
        // Configure transfer time and transportation methods
        composable("transfer_settings/{goorback}") { backStackEntry ->
            SettingsTransferSheetScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // MARK: - Timetable Settings Screen
        // Configure timetable generation settings
        composable("timetable_settings/{goorback}/{lineIndex}") { backStackEntry ->
            val goorback = backStackEntry.arguments?.getString("goorback") ?: "back1"
            val lineIndex = backStackEntry.arguments?.getString("lineIndex")?.toIntOrNull() ?: 0
            // TODO: Implement SettingsTimetableSheetScreen navigation
            // SettingsTimetableSheetScreen(
            //     goorback = goorback,
            //     lineIndex = lineIndex,
            //     onNavigateBack = {
            //         navController.popBackStack()
            //     }
            // )
        }
    }
}
