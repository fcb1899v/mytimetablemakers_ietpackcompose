package com.mytimetablemaker.ui.login

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.mytimetablemaker.R
import com.mytimetablemaker.extensions.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// MARK: - Authentication View Model
// Handles user authentication, registration, and account management
class LoginViewModel(private val context: Context) : ViewModel() {
    // Lazy init so Firebase Auth is not touched at ViewModel creation (avoids crash on DEVELOPER_ERROR at app start)
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
    
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
    
    private val _alertTitle = MutableStateFlow(context.getString(R.string.inputError))
    val alertTitle: StateFlow<String> = _alertTitle.asStateFlow()
    
    private val _alertMessage = MutableStateFlow(context.getString(R.string.enterYourEmail))
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
    fun updateEmail(value: String) {
        _email.value = value
        signUpCheck()
    }
    
    // Update password value
    fun updatePassword(value: String) {
        _password.value = value
        signUpCheck()
    }
    
    // Update password confirm value
    fun updatePasswordConfirm(value: String) {
        _passwordConfirm.value = value
        signUpCheck()
    }
    
    // MARK: - Logout Function
    // Signs out current user and updates login state
    fun logOut() {
        _isShowMessage.value = false
        _alertTitle.value = context.getString(R.string.logoutError)
        _alertMessage.value = ""
        if (_isLoginSuccess.value) {
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    auth.signOut()
                    _alertTitle.value = context.getString(R.string.loggedOutSuccessfully)
                    sharedPreferences.edit {
                        putBoolean("Login", false)
                    }
                    _isLoginSuccess.value = false
                    _isLoading.value = false
                    _isShowMessage.value = true
                } catch (e: Exception) {
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
    fun loginCheck() {
        val validation = ValidationMessages.loginValidationMessage(context, _email.value, _password.value)
        _alertTitle.value = validation.first
        _alertMessage.value = validation.second
        _isValidLogin.value = (_alertTitle.value.isEmpty() && _alertMessage.value.isEmpty())
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
                            _alertTitle.value = context.getString(R.string.loginSuccessfully)
                            sharedPreferences.edit {
                                putBoolean("Login", true)
                            }
                            _isLoginSuccess.value = true
                            _isLoading.value = false
                            _isShowMessage.value = true
                        } else {
                            _alertTitle.value = context.getString(R.string.notVerifiedAccount)
                            _alertMessage.value = context.getString(R.string.confirmYourEmail)
                            _isLoading.value = false
                            _isShowMessage.value = true
                        }
                    }
                } catch (e: Exception) {
                    val authException = e as? FirebaseAuthException
                    _alertTitle.value = context.getString(R.string.loginError)
                    _alertMessage.value = if (authException != null) {
                        authException.getLocalizedMessage(context)
                    } else {
                        context.getString(R.string.loginError)
                    }
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
    fun signUpCheck() {
        val validation = ValidationMessages.signUpValidationMessage(
            context,
            _email.value,
            _password.value,
            _passwordConfirm.value,
            _isTermsAgree.value
        )
        _alertTitle.value = validation.first
        _alertMessage.value = validation.second
        _isValidSignUp.value = (_alertTitle.value.isEmpty() && _alertMessage.value.isEmpty())
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
                            _alertTitle.value = context.getString(R.string.signupSuccessfully)
                            _alertMessage.value = context.getString(R.string.verificationEmailSentSuccessfully)
                            _isSignUpSuccess.value = true
                            _isLoading.value = false
                            _isShowMessage.value = true
                        } catch (e: Exception) {
                            _alertTitle.value = context.getString(R.string.signupError)
                            _alertMessage.value = context.getString(R.string.signupError)
                            _isLoading.value = false
                            _isShowMessage.value = true
                        }
                    }
                } catch (e: Exception) {
                    val authException = e as? FirebaseAuthException
                    _alertTitle.value = context.getString(R.string.signupError)
                    _alertMessage.value = if (authException != null) {
                        authException.getLocalizedMessage(context)
                    } else {
                        context.getString(R.string.signupError)
                    }
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
                    _alertTitle.value = context.getString(R.string.passwordReset)
                    _alertMessage.value = context.getString(R.string.passwordResetEmailSentSuccessfully)
                    _isLoading.value = false
                    _isShowMessage.value = true
                } catch (e: Exception) {
                    val authException = e as? FirebaseAuthException
                    _alertTitle.value = context.getString(R.string.passwordResetError)
                    _alertMessage.value = if (authException != null && authException.errorCode == "ERROR_USER_NOT_FOUND") {
                        context.getString(R.string.incorrectEmail)
                    } else if (authException != null) {
                        authException.getLocalizedMessage(context)
                    } else {
                        context.getString(R.string.signupError)
                    }
                    _isLoading.value = false
                    _isShowMessage.value = true
                }
            }
        } else {
            _alertTitle.value = context.getString(R.string.inputError)
            _alertMessage.value = context.getString(R.string.enterYourEmailAgain)
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
    // Deletes current user account from Firebase Auth
    fun delete() {
        _isShowMessage.value = false
        _isLoading.value = true
        _alertTitle.value = context.getString(R.string.deleteAccountError)
        _alertMessage.value = context.getString(R.string.accountCouldNotBeDeleted)
        viewModelScope.launch {
            try {
                auth.currentUser?.delete()?.await()
                _alertTitle.value = context.getString(R.string.deleteAccountSuccessfully)
                _alertMessage.value = context.getString(R.string.accountDeletedSuccessfully)
                logOut()
            } catch (e: Exception) {
                _isLoading.value = false
                _isShowMessage.value = true
            }
        }
    }
}
