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

// MARK: - Authentication View Model
// Handles user authentication, registration, and account management
class LoginViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    // Lazy init so Firebase Auth is not touched at ViewModel creation (avoids crash on DEVELOPER_ERROR at app start)
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val sharedPreferences: SharedPreferences = appContext.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
    
    // MARK: - State Variables
    // User input and authentication state
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
    
    // MARK: - Update Functions
    // Update email value
    // updateAlert: when false, signUpCheck does not overwrite alert (used when clearing fields on login screen to avoid overwriting logout message)
    fun updateEmail(value: String, updateAlert: Boolean = true) {
        _email.value = value
        signUpCheck(updateAlert = updateAlert)
    }
    
    // Update password value
    fun updatePassword(value: String, updateAlert: Boolean = true) {
        _password.value = value
        signUpCheck(updateAlert = updateAlert)
    }
    
    // Update password confirm value
    fun updatePasswordConfirm(value: String, updateAlert: Boolean = true) {
        _passwordConfirm.value = value
        signUpCheck(updateAlert = updateAlert)
    }

    // Update reset email value
    fun updateResetEmail(value: String) {
        _resetEmail.value = value
    }
    
    // MARK: - Logout Function
    // Signs out current user and updates login state
    // preserveMessage: when true, does not overwrite alert (used when called after delete() to show delete success message)
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
    
    // MARK: - Login Validation
    // Validates login form inputs before authentication
    // updateAlert: when false, only updates isValidLogin without overwriting alert (used on screen open to avoid overwriting logout message etc.)
    fun loginCheck(updateAlert: Boolean = true) {
        val validation = ValidationMessages.loginValidationMessage(appContext, _email.value, _password.value)
        if (updateAlert) {
            _alertTitle.value = validation.first
            _alertMessage.value = validation.second
        }
        _isValidLogin.value = (validation.first.isEmpty() && validation.second.isEmpty())
    }
    
    // MARK: - Login Authentication
    // Authenticates user with Firebase Auth
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
    
    // MARK: - Sign Up Validation
    // Validates sign up form inputs including terms agreement
    // updateAlert: when false, only updates isValidSignUp without overwriting alert (used on screen open to avoid overwriting logout message etc.)
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
    
    // MARK: - Sign Up Authentication
    // Creates new user account with Firebase Auth and sends verification email
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
    
    // MARK: - Password Reset
    // Sends password reset email to user's email address
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
    
    // MARK: - Terms Agreement Toggle
    // Toggles terms agreement state
    fun toggle() {
        _isTermsAgree.value = !_isTermsAgree.value
        signUpCheck()
    }
    
    // MARK: - Clear Fields
    // Clears all input fields
    fun clearFields() {
        _email.value = ""
        _password.value = ""
        _passwordConfirm.value = ""
        _isTermsAgree.value = false
    }
    
    // MARK: - Dismiss Message
    // Dismisses the alert message
    fun dismissMessage() {
        _isShowMessage.value = false
    }
    
    // MARK: - Account Deletion
    // Deletes current user account from Firebase Auth (requires password for re-authentication)
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
