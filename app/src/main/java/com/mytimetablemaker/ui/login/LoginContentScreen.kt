package com.mytimetablemaker.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
import com.mytimetablemaker.ui.theme.*

// Main login screen with auth form and navigation.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContentScreen(
    loginViewModel: LoginViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // State variables.
    var showSignUpSheet by remember { mutableStateOf(false) }
    var showResetAlert by remember { mutableStateOf(false) }
    var showLoginResultAlert by remember { mutableStateOf(false) }
    
    // Observe ViewModel state.
    val isLoading by loginViewModel.isLoading.collectAsState()
    val isValidLogin by loginViewModel.isValidLogin.collectAsState()
    val isShowMessage by loginViewModel.isShowMessage.collectAsState()
    val isLoginSuccess by loginViewModel.isLoginSuccess.collectAsState()
    val alertTitle by loginViewModel.alertTitle.collectAsState()
    val alertMessage by loginViewModel.alertMessage.collectAsState()
    val email by loginViewModel.email.collectAsState()
    val password by loginViewModel.password.collectAsState()
    val resetEmail by loginViewModel.resetEmail.collectAsState()
    val loadingMessage by loginViewModel.loadingMessage.collectAsState()
    
    // Clear fields on appear without overwriting logout messages.
    LaunchedEffect(Unit) {
        loginViewModel.updateEmail("", updateAlert = false)
        loginViewModel.updatePassword("", updateAlert = false)
        loginViewModel.loginCheck(updateAlert = !isShowMessage)
    }
    
    // Handle login result messages.
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

            // Login form.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = (ScreenSize.loginButtonWidth().value * 0.06f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {

                Spacer(modifier = Modifier.height(ScreenSize.loginTitleTopMargin()))

                // Back button at top.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    CommonComponents.CustomBackButton(
                        onClick = onNavigateBack,
                        foregroundColor = Primary
                    )
                }

                // Title section.
                Text(
                    text = stringResource(R.string.login),
                    fontSize = ScreenSize.loginTitleFontSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = ScreenSize.loginTitleBottomMargin())
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // Email input.
                CommonComponents.CustomLoginTextField(
                    value = email,
                    onValueChange = { 
                        // Update email and re-validate.
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
                
                // Password input.
                CommonComponents.CustomLoginTextField(
                    value = password,
                    onValueChange = { 
                        // Update password and re-validate.
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
                
                // Login button.
                CommonComponents.CustomButton(
                    title = stringResource(R.string.login),
                    backgroundColor = if (isValidLogin) Primary else Gray,
                    isEnabled = isValidLogin,
                    modifier = Modifier.width(ScreenSize.loginButtonWidth()),
                    onClick = { loginViewModel.login() }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // Sign-up button.
                CommonComponents.CustomButton(
                    title = stringResource(R.string.signup),
                    backgroundColor = White,
                    textColor = Primary,
                    modifier = Modifier.width(ScreenSize.loginButtonWidth()),
                    onClick = { showSignUpSheet = true }
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // Password reset button.
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

            // Bottom splash image and ad banner.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Splash image.
                Image(
                    painter = painterResource(id = R.drawable.splash),
                    contentDescription = "Splash Image",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )

                // Ad banner container.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ScreenSize.admobBannerHeight())
                        .background(Primary)
                ) {
                    AdMobBannerView()
                }
            }

            // Loading indicator.
            if (isLoading) {
                CommonComponents.CustomProgressIndicator(text = loadingMessage)
            }
        }
    }
    
    // Sign-up sheet.
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

    // Login result alert (also handles logout message from Settings).
    // Uses showLoginResultAlert to avoid double display.
    if (showLoginResultAlert) {
        CommonComponents.CustomAlertDialog(
            onDismissRequest = {
                showLoginResultAlert = false
                loginViewModel.dismissMessage()
            },
            title = alertTitle,
            alertMessage = alertMessage,
            confirmButtonText = stringResource(R.string.ok),
            onConfirmClick = {
                showLoginResultAlert = false
                loginViewModel.dismissMessage()
                if (isLoginSuccess) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(100)
                        onNavigateToSettings()
                    }
                }
            }
        )
    }
    
    // Password reset alert.
    if (showResetAlert) {
        CommonComponents.CustomAlertDialog(
            onDismissRequest = { showResetAlert = false },
            title = stringResource(R.string.passwordReset),
            alertMessage = stringResource(R.string.resetYourPassword),
            confirmButtonText = stringResource(R.string.ok),
            dismissButtonText = stringResource(R.string.cancel),
            textContent = {
                Column {
                    Text(
                        text = stringResource(R.string.resetYourPassword),
                        fontSize = ScreenSize.alertDialogTextFontSize().value.sp,
                        color = Black
                    )
                    Spacer(modifier = Modifier.height(ScreenSize.loginSpacingSmall()))
                    CommonComponents.CustomLoginTextField(
                        value = resetEmail,
                        onValueChange = { loginViewModel.updateResetEmail(it) },
                        placeholder = stringResource(R.string.email),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Email
                    )
                }
            },
            onConfirmClick = {
                loginViewModel.reset()
                showResetAlert = false
            },
        )
    }
}

