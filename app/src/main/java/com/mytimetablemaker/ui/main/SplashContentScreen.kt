package com.mytimetablemaker.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.ui.settings.FirestoreViewModel
import com.mytimetablemaker.ui.theme.Accent
import com.mytimetablemaker.ui.theme.Primary
import kotlinx.coroutines.delay

// MARK: - Splash Content Screen
// Main screen that manages app navigation and core functionality
@Composable
fun SplashContentScreen(
    mainViewModel: MainViewModel? = null,
    firestoreViewModel: FirestoreViewModel? = null,
    onNavigateToMain: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    
    // Create ViewModels if not provided
    val mainVm = mainViewModel ?: remember { MainViewModel(application) }
    val firestoreVm = firestoreViewModel ?: remember { FirestoreViewModel(application) }
    
    // Loading state
    var isLoading by remember { mutableStateOf(false) }
    var isFinishSplash by remember { mutableStateOf(false) }
    
    // Animation for splash screen fade out
    val alpha by animateFloatAsState(
        targetValue = if (isFinishSplash) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "splash_alpha"
    )
    
    // Initialize data when screen appears
    LaunchedEffect(Unit) {
        // Set loading state immediately
        isLoading = true
        
        // Preload ads during splash screen (TODO: Implement AdMob preload)
        println("🚀 Splash screen appeared - starting initialization")
        // adBannerView = AdMobBannerView.preloadAds()
        
        // Initialize data and perform update check when app launches
        // TODO: Implement SharedDataManager equivalent
        // await sharedDataManager.performSplashInitialization()
        
        // Simulate initialization delay
        delay(1000)
        
        // Navigate to main content after loading completes
        isLoading = false
        delay(500) // Small delay to ensure smooth transition
        isFinishSplash = true
        delay(500) // Wait for animation
        onNavigateToMain()
    }
    
    // Splash screen content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Accent)
            .alpha(alpha)
    ) {
        // MARK: - Main Content
        // App title, icon, and splash image
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // App Title
            Text(
                text = stringResource(R.string.myTransitMakers),
                fontSize = ScreenSize.splashTitleFontSize().value.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // App Icon
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(ScreenSize.splashIconSize())
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.weight(3f))
        }
        
        // MARK: - Splash Image and Ad Banner Placeholder
        // Splash image at bottom with ad banner placeholder
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Splash Image
            Image(
                painter = painterResource(id = R.drawable.splash),
                contentDescription = "Splash Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height((ScreenSize.screenHeight() * 0.7f).dp),
                contentScale = ContentScale.FillWidth
            )
            
            // Ad Banner Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ScreenSize.admobBannerHeight())
                    .background(Primary)
            ) {
                // TODO: Implement AdMob banner
            }
        }
        
        // MARK: - Loading Overlay
        // Dark overlay with progress bar when loading initial data
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .zIndex(1f)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ScreenSize.splashLoadingSpacing())
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White
                    )
                    
                    Text(
                        text = stringResource(R.string.loadingData),
                        fontSize = ScreenSize.splashLoadingFontSize().value.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

