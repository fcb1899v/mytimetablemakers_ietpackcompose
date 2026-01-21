package com.mytimetablemaker.ui.login

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    // Password visibility toggle states for input fields
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    
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
    
    // Clear fields on appear
    LaunchedEffect(Unit) {
        loginViewModel.clearFields()
        loginViewModel.signUpCheck()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = (ScreenSize.loginButtonWidth().value * 0.06f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(ScreenSize.loginTitleTopMargin()))
                
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
                OutlinedTextField(
                    value = email,
                    onValueChange = { loginViewModel.updateEmail(it) },
                    label = { Text(stringResource(R.string.email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .width(ScreenSize.loginButtonWidth())
                        .height(ScreenSize.loginTextHeight()),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = ScreenSize.loginTextFieldFontSize().value.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedBorderColor = Gray.copy(alpha = 0.5f),
                        unfocusedBorderColor = Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Password Input Field
                // Secure text field with visibility toggle for password entry
                OutlinedTextField(
                    value = password,
                    onValueChange = { loginViewModel.updatePassword(it) },
                    label = { Text(stringResource(R.string.password)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = Gray
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .width(ScreenSize.loginButtonWidth())
                        .height(ScreenSize.loginTextHeight()),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = ScreenSize.loginTextFieldFontSize().value.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedBorderColor = Gray.copy(alpha = 0.5f),
                        unfocusedBorderColor = Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
                )
                
                Spacer(modifier = Modifier.height(ScreenSize.loginMargin()))
                
                // MARK: - Confirm Password Input Field
                // Secure text field with visibility toggle for password confirmation
                OutlinedTextField(
                    value = passwordConfirm,
                    onValueChange = { loginViewModel.updatePasswordConfirm(it) },
                    label = { Text(stringResource(R.string.confirmPassword)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (isConfirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password",
                                tint = Gray
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .width(ScreenSize.loginButtonWidth())
                        .height(ScreenSize.loginTextHeight()),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = ScreenSize.loginTextFieldFontSize().value.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedBorderColor = Gray.copy(alpha = 0.5f),
                        unfocusedBorderColor = Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(ScreenSize.settingsSheetCornerRadius())
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
                                modifier = Modifier.size(24.dp),
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
                        .width(ScreenSize.loginButtonWidth())
                        .padding(horizontal = ScreenSize.settingsSheetHorizontalSpacing()),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Top
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
                            } catch (e: Exception) {
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
            
            // MARK: - Top App Bar
            // Back button at top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                CommonComponents.CustomBackButton(
                    onClick = onDismiss,
                    foregroundColor = White
                )
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
        
        // MARK: - Sign Up Result Alert
        // Alert for displaying sign up result messages
        if (isShowMessage) {
            AlertDialog(
                onDismissRequest = { loginViewModel.dismissMessage() },
                title = { Text(alertTitle) },
                text = { Text(alertMessage) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            loginViewModel.dismissMessage()
                            if (isSignUpSuccess) {
                                onDismiss()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                },
                containerColor = White
            )
        }
    }
}

