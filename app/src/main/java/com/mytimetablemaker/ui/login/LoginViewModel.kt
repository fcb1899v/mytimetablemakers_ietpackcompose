package com.mytimetablemaker.ui.login

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.mytimetablemaker.extensions.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Authentication view model for login, signup, and account actions.
class LoginViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    // Lazy init so Firebase Auth is not touched at ViewModel creation (avoids crash on DEVELOPER_ERROR at app start)
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val sharedPreferences: SharedPreferences = appContext.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
    
    // State for user input and auth results.
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    
    private val _passwordConfirm = MutableStateFlow("")
    val passwordConfirm: StateFlow<String> = _passwordConfirm.asStateFlow()
    
    private val _isTermsAgree = MutableStateFlow(false)
    val isTermsAgree: StateFlow<Boolean> = _isTermsAgree.asStateFlow()
    
    private val _resetEmail = MutableStateFlow("")
    val resetEmail: StateFlow<String> = _resetEmail.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isShowMessage = MutableStateFlow(false)
    val isShowMessage: StateFlow<Boolean> = _isShowMessage.asStateFlow()
    
    private val _alertTitle = MutableStateFlow(ValidationMessages.inputError(appContext))
    val alertTitle: StateFlow<String> = _alertTitle.asStateFlow()
    
    private val _alertMessage = MutableStateFlow(ValidationMessages.enterEmail(appContext))
    val alertMessage: StateFlow<String> = _alertMessage.asStateFlow()
    
    private val _isValidLogin = MutableStateFlow(false)
    val isValidLogin: StateFlow<Boolean> = _isValidLogin.asStateFlow()
    
    private val _isValidSignUp = MutableStateFlow(false)
    val isValidSignUp: StateFlow<Boolean> = _isValidSignUp.asStateFlow()
    
    private val _isLoginSuccess = MutableStateFlow(sharedPreferences.getBoolean("Login", false))
    val isLoginSuccess: StateFlow<Boolean> = _isLoginSuccess.asStateFlow()
    
    private val _isSignUpSuccess = MutableStateFlow(false)
    val isSignUpSuccess: StateFlow<Boolean> = _isSignUpSuccess.asStateFlow()
    
    // Update email value; optionally skip alert overwrite.
    fun updateEmail(value: String, updateAlert: Boolean = true) {
        _email.value = value
        signUpCheck(updateAlert = updateAlert)
    }
    
    // Update password value.
    fun updatePassword(value: String, updateAlert: Boolean = true) {
        _password.value = value
        signUpCheck(updateAlert = updateAlert)
    }
    
    // Update password confirm value.
    fun updatePasswordConfirm(value: String, updateAlert: Boolean = true) {
        _passwordConfirm.value = value
        signUpCheck(updateAlert = updateAlert)
    }

    // Update reset email value.
    fun updateResetEmail(value: String) {
        _resetEmail.value = value
    }
    
    // Sign out and update login state.
    // preserveMessage keeps existing alerts.
    fun logOut(preserveMessage: Boolean = false) {
        _isShowMessage.value = false
        if (!preserveMessage) {
            _alertTitle.value = ValidationMessages.logoutErrorTitle(appContext)
            _alertMessage.value = ""
        }
        if (_isLoginSuccess.value) {
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    auth.signOut()
                    if (!preserveMessage) {
                        _alertTitle.value = ValidationMessages.logoutSuccess(appContext)
                        _alertMessage.value = ""
                    }
                    sharedPreferences.edit {
                        putBoolean("Login", false)
                    }
                    _isLoginSuccess.value = false
                    _isLoading.value = false
                    _isShowMessage.value = true
                } catch (_: Exception) {
                    if (!preserveMessage) {
                        _alertTitle.value = ValidationMessages.logoutErrorTitle(appContext)
                        _alertMessage.value = ""
                    }
                    sharedPreferences.edit {
                        putBoolean("Login", true)
                    }
                    _isLoginSuccess.value = true
                    _isLoading.value = false
                    _isShowMessage.value = true
                }
            }
        } else {
            _isShowMessage.value = true
        }
    }
    
    // Validate login inputs; optionally skip alert overwrite.
    fun loginCheck(updateAlert: Boolean = true) {
        val validation = ValidationMessages.loginValidationMessage(appContext, _email.value, _password.value)
        if (updateAlert) {
            _alertTitle.value = validation.first
            _alertMessage.value = validation.second
        }
        _isValidLogin.value = (validation.first.isEmpty() && validation.second.isEmpty())
    }
    
    // Authenticate user with Firebase Auth.
    fun login() {
        _isShowMessage.value = false
        if (_isValidLogin.value) {
            _alertTitle.value = ""
            _alertMessage.value = ""
            _isLoginSuccess.value = false
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    val result = auth.signInWithEmailAndPassword(_email.value, _password.value).await()
                    val user = result.user
                    if (user != null) {
                        if (user.isEmailVerified) {
                            _alertTitle.value = ValidationMessages.loginSuccess(appContext)
                            sharedPreferences.edit {
                                putBoolean("Login", true)
                            }
                            _isLoginSuccess.value = true
                            _isLoading.value = false
                            _isShowMessage.value = true
                        } else {
                            _alertTitle.value = ValidationMessages.notVerifiedAccount(appContext)
                            _alertMessage.value = ValidationMessages.confirmEmail(appContext)
                            _isLoading.value = false
                            _isShowMessage.value = true
                        }
                    }
                } catch (e: Exception) {
                    val authException = e as? FirebaseAuthException
                    _alertTitle.value = ValidationMessages.loginErrorTitle(appContext)
                    _alertMessage.value = authException?.getLocalizedMessage(appContext)
                        ?: ValidationMessages.loginErrorTitle(appContext)
                    _isLoading.value = false
                    _isShowMessage.value = true
                }
            }
        } else {
            _isShowMessage.value = true
        }
    }
    
    // Validate sign-up inputs, including terms agreement.
    fun signUpCheck(updateAlert: Boolean = true) {
        val validation = ValidationMessages.signUpValidationMessage(
            appContext,
            _email.value,
            _password.value,
            _passwordConfirm.value,
            _isTermsAgree.value
        )
        if (updateAlert) {
            _alertTitle.value = validation.first
            _alertMessage.value = validation.second
        }
        _isValidSignUp.value = (validation.first.isEmpty() && validation.second.isEmpty())
    }
    
    // Create account and send verification email.
    fun signUp() {
        if (_isValidSignUp.value) {
            _alertTitle.value = ""
            _alertMessage.value = ""
            _isSignUpSuccess.value = false
            _isLoading.value = true
            _isShowMessage.value = false
            viewModelScope.launch {
                try {
                    val result = auth.createUserWithEmailAndPassword(_email.value, _password.value).await()
                    val user = result.user
                    if (user != null) {
                        try {
                            user.sendEmailVerification().await()
                            _alertTitle.value = ValidationMessages.signUpSuccess(appContext)
                            _alertMessage.value = ValidationMessages.verificationEmailSent(appContext)
                            _isSignUpSuccess.value = true
                            _isLoading.value = false
                            _isShowMessage.value = true
                        } catch (_: Exception) {
                            _alertTitle.value = ValidationMessages.signUpErrorTitle(appContext)
                            _alertMessage.value = ValidationMessages.signUpErrorTitle(appContext)
                            _isLoading.value = false
                            _isShowMessage.value = true
                        }
                    }
                } catch (e: Exception) {
                    val authException = e as? FirebaseAuthException
                    _alertTitle.value = ValidationMessages.signUpErrorTitle(appContext)
                    _alertMessage.value = authException?.getLocalizedMessage(appContext)
                        ?: ValidationMessages.signUpErrorTitle(appContext)
                    _isLoading.value = false
                    _isShowMessage.value = true
                }
            }
        } else {
            _isShowMessage.value = true
        }
    }
    
    // Send password reset email.
    fun reset() {
        if (_resetEmail.value.isValidEmail()) {
            _alertTitle.value = ""
            _alertMessage.value = ""
            _isLoading.value = true
            _isShowMessage.value = false
            viewModelScope.launch {
                try {
                    auth.sendPasswordResetEmail(_resetEmail.value).await()
                    _alertTitle.value = ValidationMessages.passwordResetTitle(appContext)
                    _alertMessage.value = ValidationMessages.passwordResetSent(appContext)
                    _isLoading.value = false
                    _isShowMessage.value = true
                } catch (e: Exception) {
                    val authException = e as? FirebaseAuthException
                    _alertTitle.value = ValidationMessages.passwordResetErrorTitle(appContext)
                    _alertMessage.value = if (authException != null && authException.errorCode == "ERROR_USER_NOT_FOUND") {
                        ValidationMessages.incorrectEmail(appContext)
                    } else authException?.getLocalizedMessage(appContext) ?: ValidationMessages.signUpErrorTitle(appContext)
                    _isLoading.value = false
                    _isShowMessage.value = true
                }
            }
        } else {
            _alertTitle.value = ValidationMessages.inputError(appContext)
            _alertMessage.value = ValidationMessages.enterEmailAgain(appContext)
            _isLoading.value = false
            _isShowMessage.value = true
        }
    }
    
    // Toggle terms agreement.
    fun toggle() {
        _isTermsAgree.value = !_isTermsAgree.value
        signUpCheck()
    }
    
    // Clear all input fields.
    fun clearFields() {
        _email.value = ""
        _password.value = ""
        _passwordConfirm.value = ""
        _isTermsAgree.value = false
    }
    
    // Dismiss the alert message.
    fun dismissMessage() {
        _isShowMessage.value = false
    }
    
    // Delete current user account (requires re-auth).
    fun delete(password: String) {
        _isShowMessage.value = false
        if (password.isBlank()) {
            _alertTitle.value = ValidationMessages.inputError(appContext)
            _alertMessage.value = ValidationMessages.enterPassword(appContext)
            _isShowMessage.value = true
            return
        }
        _isLoading.value = true
        _alertTitle.value = ValidationMessages.deleteAccountErrorTitle(appContext)
        _alertMessage.value = ValidationMessages.accountNotDeleted(appContext)
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user == null || user.email.isNullOrEmpty()) {
                    _alertTitle.value = ValidationMessages.deleteAccountErrorTitle(appContext)
                    _alertMessage.value = ValidationMessages.accountNotDeleted(appContext)
                    _isLoading.value = false
                    _isShowMessage.value = true
                    return@launch
                }
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()
                user.delete().await()
                _alertTitle.value = ValidationMessages.deleteAccountSuccess(appContext)
                _alertMessage.value = ValidationMessages.accountDeletedSuccess(appContext)
                logOut(preserveMessage = true)
            } catch (e: Exception) {
                val authException = e as? FirebaseAuthException
                _alertTitle.value = ValidationMessages.deleteAccountErrorTitle(appContext)
                _alertMessage.value = authException?.getLocalizedMessage(appContext)
                    ?: ValidationMessages.accountNotDeleted(appContext)
                _isLoading.value = false
                _isShowMessage.value = true
            }
        }
    }
}
