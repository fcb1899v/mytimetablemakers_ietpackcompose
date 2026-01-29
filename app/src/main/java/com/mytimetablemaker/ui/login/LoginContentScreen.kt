package com.mytimetablemaker.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.ui.common.AdMobBannerView
import com.mytimetablemaker.ui.common.CommonComponents
import com.mytimetablemaker.ui.main.MainViewModel
import com.mytimetablemaker.ui.settings.FirestoreViewModel
import com.mytimetablemaker.ui.theme.*

// MARK: - Login Content Screen
// Main login screen with authentication form and navigation
// Matches SwiftUI LoginContentView implementation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContentScreen(
    mainViewModel: MainViewModel,
    loginViewModel: LoginViewModel,
    firestoreViewModel: FirestoreViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    
    // MARK: - State Variables
    var showSignUpSheet by remember { mutableStateOf(false) }
    var showResetAlert by remember { mutableStateOf(false) }
    var showLoginResultAlert by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    
    // Observe ViewModel state
    val isLoading by loginViewModel.isLoading.collectAsState()
    val isValidLogin by loginViewModel.isValidLogin.collectAsState()
    val isShowMessage by loginViewModel.isShowMessage.collectAsState()
    val isLoginSuccess by loginViewModel.isLoginSuccess.collectAsState()
    val alertTitle by loginViewModel.alertTitle.collectAsState()
    val alertMessage by loginViewModel.alertMessage.collectAsState()
    val email by loginViewModel.email.collectAsState()
    val password by loginViewModel.password.collectAsState()
    
    // Clear fields on appear
    LaunchedEffect(Unit) {
        loginViewModel.updateEmail("")
        loginViewModel.updatePassword("")
        loginViewModel.loginCheck()
    }
    
    // Handle login result message changes
    LaunchedEffect(isShowMessage) {
        if (isShowMessage) {
            showLoginResultAlert = true
            loginViewModel.dismissMessage()
        }
    }
    
    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Accent)
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
                .background(Accent)
                .padding(paddingValues)
        ) {

            // MARK: - Login Form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = (ScreenSize.loginButtonWidth().value * 0.06f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {

                Spacer(modifier = Modifier.height(ScreenSize.loginTitleTopMargin()))

                // MARK: - Top App Bar
                // Back button at top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    CommonComponents.CustomBackButton(
                        onClick = onNavigateBack,
                        foregroundColor = Primary
                    )
                }

                // MARK: - Title Section
                Text(
                    text = stringResource(R.string.login),
                    fontSize = ScreenSize.loginTitleFontSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = ScreenSize.loginTitleBottomMargin())
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Email Input Field
                CommonComponents.CustomLoginTextField(
                    value = email,
                    onValueChange = { 
                        // Directly update email value and check login
                        loginViewModel.updateEmail(it)
                        loginViewModel.loginCheck()
                    },
                    placeholder = stringResource(R.string.email),
                    modifier = Modifier
                        .width(ScreenSize.loginButtonWidth())
                        .height(ScreenSize.loginTextHeight()),
                    keyboardType = KeyboardType.Email
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Password Input Field
                CommonComponents.CustomLoginTextField(
                    value = password,
                    onValueChange = { 
                        // Directly update password value and check login
                        loginViewModel.updatePassword(it)
                        loginViewModel.loginCheck()
                    },
                    placeholder = stringResource(R.string.password),
                    modifier = Modifier
                        .width(ScreenSize.loginButtonWidth())
                        .height(ScreenSize.loginTextHeight()),
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Login Button
                CommonComponents.CustomButton(
                    title = stringResource(R.string.login),
                    backgroundColor = if (isValidLogin) Primary else Gray,
                    isEnabled = isValidLogin,
                    modifier = Modifier.width(ScreenSize.loginButtonWidth()),
                    onClick = { loginViewModel.login() }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Sign Up Button
                CommonComponents.CustomButton(
                    title = stringResource(R.string.signup),
                    backgroundColor = White,
                    textColor = Primary,
                    modifier = Modifier.width(ScreenSize.loginButtonWidth()),
                    onClick = { showSignUpSheet = true }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Password Reset Button
                TextButton(onClick = { showResetAlert = true }) {
                    Text(
                        text = stringResource(R.string.forgotPassword),
                        textDecoration = TextDecoration.Underline,
                        fontSize = ScreenSize.loginHeadlineFontSize().value.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
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
                        .height((ScreenSize.screenHeight().value * 0.7f).dp),
                    contentScale = ContentScale.FillWidth
                )

                // Ad Banner Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ScreenSize.admobBannerHeight())
                        .background(Primary)
                ) {
                    AdMobBannerView()
                }
            }

            // MARK: - Loading Indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(ScreenSize.alertDialogContentPadding()),
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(ScreenSize.alertDialogContentPadding())
                        )
                    }
                }
            }
        }
    }
    
    // MARK: - Sign Up Sheet
    if (showSignUpSheet) {
        Dialog(
            onDismissRequest = { showSignUpSheet = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            SignUpContentScreen(
                loginViewModel = loginViewModel,
                onDismiss = { showSignUpSheet = false }
            )
        }
    }
    
    // MARK: - Login Result Alert
    if (showLoginResultAlert) {
        AlertDialog(
            onDismissRequest = { 
                showLoginResultAlert = false
                loginViewModel.dismissMessage()
            },
            title = { Text(alertTitle) },
            text = { Text(alertMessage) },
            confirmButton = {
                TextButton(onClick = { 
                    showLoginResultAlert = false
                    loginViewModel.dismissMessage()
                    if (isLoginSuccess) {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100)
                            onNavigateToSettings()
                        }
                    }
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    // MARK: - Message Alert
    if (isShowMessage) {
        AlertDialog(
            onDismissRequest = { 
                loginViewModel.dismissMessage()
            },
            title = { Text(alertTitle) },
            text = { Text(alertMessage) },
            confirmButton = {
                TextButton(onClick = { 
                    loginViewModel.dismissMessage()
                }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    // MARK: - Password Reset Alert
    if (showResetAlert) {
        AlertDialog(
            onDismissRequest = { showResetAlert = false },
            title = { Text(stringResource(R.string.passwordReset)) },
            text = {
                Column {
                    Text(stringResource(R.string.resetYourPassword))
                    Spacer(modifier = Modifier.height(ScreenSize.loginSpacingSmall()))
                    CommonComponents.CustomLoginTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        placeholder = stringResource(R.string.email),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Email
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    loginViewModel.updateEmail(resetEmail)
                    loginViewModel.reset()
                    showResetAlert = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAlert = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

