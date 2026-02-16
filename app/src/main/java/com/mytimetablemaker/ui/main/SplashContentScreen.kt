package com.mytimetablemaker.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.services.SharedDataManager
import com.mytimetablemaker.ui.common.CommonComponents
import com.mytimetablemaker.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Splash screen with startup initialization.
@Composable
fun SplashContentScreen(
    onNavigateToMain: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    
    // Loading state for the overlay.
    var isLoading by remember { mutableStateOf(false) }
    var isFinishSplash by remember { mutableStateOf(false) }
    
    // Fade-out animation for the splash.
    val alpha by animateFloatAsState(
        targetValue = if (isFinishSplash) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "splash_alpha"
    )
    
    // Initialize data when the screen appears.
    LaunchedEffect(Unit) {
        isLoading = true
        
        // Run initialization off the UI thread and let it continue after dispose.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val sharedDataManager = SharedDataManager.getInstance(application)
                sharedDataManager.performSplashInitialization()
            } catch (e: Exception) {
                android.util.Log.e("SplashContentScreen", "Splash initialization failed", e)
            }
        }
        
        delay(1500)
        
        isLoading = false
        delay(1000)
        onNavigateToMain()
    }
    
    // Splash screen content.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Accent)
            .alpha(alpha)
    ) {
        // App title and icon.
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = stringResource(R.string.myTransitMakers),
                fontSize = ScreenSize.splashTitleFontSize().value.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = stringResource(R.string.appIcon),
                modifier = Modifier
                    .size(ScreenSize.splashIconSize()),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.weight(3f))
        }
        
        // Splash image with an ad banner placeholder.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash),
                contentDescription = stringResource(R.string.splashImage),
                modifier = Modifier
                    .fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ScreenSize.admobBannerHeight())
                    .background(Primary)
            ) 
        }
        
        // Dark overlay with a progress indicator.
        if (isLoading) {
            CommonComponents.CustomProgressIndicator(
                text = stringResource(R.string.loadingData)
            )
        }
    }
}

