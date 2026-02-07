package com.mytimetablemaker.ui.login

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.ScreenSize
import com.mytimetablemaker.ui.common.CommonComponents
import com.mytimetablemaker.ui.theme.*
import androidx.core.net.toUri

// MARK: - Sign Up Content Screen
// User registration screen with form validation and terms agreement
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpContentScreen(
    loginViewModel: LoginViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // MARK: - State Variables
    // Password visibility toggle states for input fields (now handled internally by CustomLoginTextField)
    
    // Observe ViewModel state
    val isLoading by loginViewModel.isLoading.collectAsState()
    val isValidSignUp by loginViewModel.isValidSignUp.collectAsState()
    val isShowMessage by loginViewModel.isShowMessage.collectAsState()
    val isSignUpSuccess by loginViewModel.isSignUpSuccess.collectAsState()
    val alertTitle by loginViewModel.alertTitle.collectAsState()
    val alertMessage by loginViewModel.alertMessage.collectAsState()
    val isTermsAgree by loginViewModel.isTermsAgree.collectAsState()
    val email by loginViewModel.email.collectAsState()
    val password by loginViewModel.password.collectAsState()
    val passwordConfirm by loginViewModel.passwordConfirm.collectAsState()
    
    // Note: Dismiss is handled in the alert's OK button click handler
    
    // Clear fields on appear (signUpCheck with updateAlert=false to set isValidSignUp without overwriting alert)
    LaunchedEffect(Unit) {
        loginViewModel.clearFields()
        loginViewModel.signUpCheck(updateAlert = false)
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
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
                    .background(Primary)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
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
                        onClick = onDismiss,
                        foregroundColor = White
                    )
                }

                // MARK: - Title
                Text(
                    text = stringResource(R.string.createAccount),
                    fontSize = ScreenSize.loginTitleFontSize().value.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                    modifier = Modifier.padding(bottom = ScreenSize.loginTitleBottomMargin())
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Email Input Field
                // Text field for email address entry with validation
                CommonComponents.CustomLoginTextField(
                    value = email,
                    onValueChange = { loginViewModel.updateEmail(it) },
                    placeholder = stringResource(R.string.email),
                    modifier = Modifier
                        .width(ScreenSize.loginButtonWidth())
                        .height(ScreenSize.loginTextHeight()),
                    keyboardType = KeyboardType.Email
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Password Input Field
                // Secure text field with visibility toggle for password entry
                CommonComponents.CustomLoginTextField(
                    value = password,
                    onValueChange = { loginViewModel.updatePassword(it) },
                    placeholder = stringResource(R.string.password),
                    modifier = Modifier
                        .width(ScreenSize.loginButtonWidth())
                        .height(ScreenSize.loginTextHeight()),
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Confirm Password Input Field
                // Secure text field with visibility toggle for password confirmation
                CommonComponents.CustomLoginTextField(
                    value = passwordConfirm,
                    onValueChange = { loginViewModel.updatePasswordConfirm(it) },
                    placeholder = stringResource(R.string.confirmPassword),
                    modifier = Modifier
                        .width(ScreenSize.loginButtonWidth())
                        .height(ScreenSize.loginTextHeight()),
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Sign Up Button
                // Button to submit registration form with loading indicator
                Box(
                    modifier = Modifier.width(ScreenSize.loginButtonWidth())
                ) {
                    CommonComponents.CustomButton(
                        title = stringResource(R.string.signup),
                        onClick = { loginViewModel.signUp() },
                        backgroundColor = if (isValidSignUp) Accent else Gray,
                        isEnabled = isValidSignUp
                    )
                    
                    // Loading indicator overlay during sign up process
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(ScreenSize.loginLoadingIndicatorSize()),
                                color = White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Terms and Conditions Agreement
                // Checkbox and link to terms and privacy policy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenSize.settingsSheetHorizontalSpacing()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox button to toggle terms agreement
                    IconButton(
                        onClick = { loginViewModel.toggle() },
                        modifier = Modifier.size(ScreenSize.loginCheckboxSize())
                    ) {
                        Icon(
                            imageVector = if (isTermsAgree) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = "Terms agreement",
                            tint = if (isTermsAgree) Accent else White,
                            modifier = Modifier.size(ScreenSize.loginCheckboxSize())
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(ScreenSize.settingsSheetHorizontalSpacing()))
                    
                    // Button to open terms and privacy policy in browser
                    val termsUrl = stringResource(R.string.termsUrl)
                    Row(
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, termsUrl.toUri())
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // Handle error
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.iHaveReadAndAgreeToThe),
                            fontSize = ScreenSize.loginSubheadlineFontSize().value.sp,
                            color = White
                        )
                        Text(
                            text = stringResource(R.string.termsAndPrivacyPolicyLower),
                            fontSize = ScreenSize.loginSubheadlineFontSize().value.sp,
                            color = White,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // MARK: - Loading Indicator
            // Display loading overlay during authentication process
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius()),
                        modifier = Modifier.padding(ScreenSize.alertDialogContentPadding())
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(ScreenSize.alertDialogContentPadding())
                        )
                    }
                }
            }
        }
        
        // MARK: - Sign Up Result Alert
        if (isShowMessage) {
            CommonComponents.CustomAlertDialog(
                onDismissRequest = { loginViewModel.dismissMessage() },
                title = alertTitle,
                alertMessage = alertMessage,
                confirmButtonText = stringResource(R.string.ok),
                onConfirmClick = {
                    loginViewModel.dismissMessage()
                    if (isSignUpSuccess) {
                        onDismiss()
                    }
                }
            )
        }
    }
    }
}

