package com.mytimetablemaker.extensions

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import java.util.regex.Pattern

// MARK: - String Extensions for Account Validation
// Extensions for email and password validation

// MARK: - Email Validation
// Validates email format using regex pattern
fun String.isValidEmail(): Boolean {
    val emailRegex = "[A-Z0-9a-z._+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}"
    val pattern = Pattern.compile(emailRegex)
    return pattern.matcher(this).matches()
}

// MARK: - Password Validation
// Validates password strength (minimum 8 characters with special characters)
fun String.isValidPassword(): Boolean {
    val passwordRegex = "^(?=.*[A-Za-z0-9])(?=.*[!@#$&~]).{8,}$"
    val pattern = Pattern.compile(passwordRegex)
    return pattern.matcher(this).matches()
}

// MARK: - Password Comparison
// Checks if password matches confirmation password
fun String.isMatching(confirmPassword: String): Boolean {
    return this == confirmPassword
}

// MARK: - Firebase Auth Extensions
// Helper extensions for Firebase Authentication user management

// MARK: - Current User ID
// Safely retrieves current authenticated user ID, returns null if not logged in
fun FirebaseAuth.currentUserID(): String? {
    return currentUser?.uid
}

// MARK: - Authentication State
// Checks if user is currently authenticated
fun FirebaseAuth.isAuthenticated(): Boolean {
    return currentUser != null
}

// MARK: - AuthErrorCode Extensions
// Helper extensions for Firebase Authentication error handling

// MARK: - Localized Error Message
// Converts Firebase Auth error code to localized user-friendly message
fun FirebaseAuthException.getLocalizedMessage(context: Context): String {
    return when (errorCode) {
        "ERROR_INVALID_EMAIL" -> "Incorrect email format".localized(context)
        "ERROR_USER_NOT_FOUND" -> "Incorrect email or password".localized(context)
        "ERROR_WRONG_PASSWORD" -> "Incorrect email or password".localized(context)
        "ERROR_USER_DISABLED" -> "This account is disabled".localized(context)
        "ERROR_EMAIL_ALREADY_IN_USE" -> "This email has already been registered".localized(context)
        "ERROR_WEAK_PASSWORD" -> "Incorrect password format".localized(context)
        else -> "Authentication error occurred".localized(context)
    }
}

// MARK: - Validation Message Helpers
// Helper functions for generating validation error messages
object ValidationMessages {
    
    // MARK: - Common Messages
    // Standard validation error messages for form inputs
    fun inputError(context: Context): String = "Input error".localized(context)
    fun checkError(context: Context): String = "Check error".localized(context)
    fun enterEmail(context: Context): String = "Enter your email".localized(context)
    fun enterPassword(context: Context): String = "Enter your password".localized(context)
    fun enterConfirmPassword(context: Context): String = "Enter your confirm password".localized(context)
    fun incorrectEmailFormat(context: Context): String = "Incorrect email format".localized(context)
    fun incorrectPasswordFormat(context: Context): String = "Incorrect password format".localized(context)
    fun passwordMismatch(context: Context): String = "Confirm password don't match".localized(context)
    fun checkTerms(context: Context): String = "Check the terms and privacy policy".localized(context)
    fun enterEmailAgain(context: Context): String = "Enter your email again".localized(context)
    
    // MARK: - Success Messages
    // Success messages for authentication operations
    fun loginSuccess(context: Context): String = "Login successfully".localized(context)
    fun logoutSuccess(context: Context): String = "Logged out successfully".localized(context)
    fun signUpSuccess(context: Context): String = "Signup successfully".localized(context)
    fun verificationEmailSent(context: Context): String = "Verification email Sent successfully".localized(context)
    fun passwordResetSent(context: Context): String = "Password reset email Sent successfully".localized(context)
    fun deleteAccountSuccess(context: Context): String = "Delete account successfully".localized(context)
    fun accountDeletedSuccess(context: Context): String = "Account deleted successfully".localized(context)
    
    // MARK: - Error Titles
    // Error titles for authentication operations
    fun loginErrorTitle(context: Context): String = "Login error".localized(context)
    fun logoutErrorTitle(context: Context): String = "Logout error".localized(context)
    fun signUpErrorTitle(context: Context): String = "Signup error".localized(context)
    fun passwordResetErrorTitle(context: Context): String = "Password reset error".localized(context)
    fun deleteAccountErrorTitle(context: Context): String = "Delete account error".localized(context)
    
    // MARK: - Error Messages
    // Error messages for authentication operations
    fun notVerifiedAccount(context: Context): String = "Not verified account".localized(context)
    fun confirmEmail(context: Context): String = "Confirm your email".localized(context)
    fun verificationEmailNotSent(context: Context): String = "Verification email could not be sent".localized(context)
    fun accountNotDeleted(context: Context): String = "Account could not be deleted".localized(context)
    fun passwordResetTitle(context: Context): String = "Password Reset".localized(context)
    fun incorrectEmail(context: Context): String = "Incorrect email".localized(context)
    
    // MARK: - Login Validation
    // Generates validation messages for login form
    fun loginValidationMessage(
        context: Context,
        email: String,
        password: String
    ): Pair<String, String> {
        return when {
            email.isEmpty() -> Pair(
                inputError(context),
                enterEmail(context)
            )
            !email.isValidEmail() -> Pair(
                inputError(context),
                incorrectEmailFormat(context)
            )
            password.isEmpty() -> Pair(
                inputError(context),
                enterPassword(context)
            )
            !password.isValidPassword() -> Pair(
                inputError(context),
                incorrectPasswordFormat(context)
            )
            else -> Pair("", "")
        }
    }
    
    // MARK: - Sign Up Validation
    // Generates validation messages for sign up form
    fun signUpValidationMessage(
        context: Context,
        email: String,
        password: String,
        passwordConfirm: String,
        isTermsAgree: Boolean
    ): Pair<String, String> {
        return when {
            !isTermsAgree -> Pair(
                checkError(context),
                checkTerms(context)
            )
            email.isEmpty() -> Pair(
                inputError(context),
                enterEmail(context)
            )
            !email.isValidEmail() -> Pair(
                inputError(context),
                incorrectEmailFormat(context)
            )
            password.isEmpty() -> Pair(
                inputError(context),
                enterPassword(context)
            )
            passwordConfirm.isEmpty() -> Pair(
                inputError(context),
                enterConfirmPassword(context)
            )
            !password.isValidPassword() -> Pair(
                inputError(context),
                incorrectPasswordFormat(context)
            )
            !password.isMatching(passwordConfirm) -> Pair(
                inputError(context),
                passwordMismatch(context)
            )
            else -> Pair("", "")
        }
    }
}

