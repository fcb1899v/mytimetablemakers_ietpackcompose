package com.mytimetablemaker.extensions

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.mytimetablemaker.R
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
        "ERROR_INVALID_EMAIL" -> context.getString(R.string.incorrectEmailFormat)
        "ERROR_USER_NOT_FOUND" -> context.getString(R.string.incorrectEmailOrPassword)
        "ERROR_WRONG_PASSWORD" -> context.getString(R.string.incorrectEmailOrPassword)
        "ERROR_USER_DISABLED" -> context.getString(R.string.thisAccountIsDisabled)
        "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.thisEmailHasAlreadyBeenRegistered)
        "ERROR_WEAK_PASSWORD" -> context.getString(R.string.incorrectPasswordFormat)
        else -> context.getString(R.string.loginError)
    }
}

// MARK: - Validation Message Helpers
// Helper functions for generating validation error messages
object ValidationMessages {
    
    // MARK: - Common Messages
    // Standard validation error messages for form inputs
    fun inputError(context: Context): String = context.getString(R.string.inputError)
    fun checkError(context: Context): String = context.getString(R.string.checkError)
    fun enterEmail(context: Context): String = context.getString(R.string.enterYourEmail)
    fun enterPassword(context: Context): String = context.getString(R.string.enterYourPassword)
    fun enterConfirmPassword(context: Context): String = context.getString(R.string.enterYourConfirmPassword)
    fun incorrectEmailFormat(context: Context): String = context.getString(R.string.incorrectEmailFormat)
    fun incorrectPasswordFormat(context: Context): String = context.getString(R.string.incorrectPasswordFormat)
    fun passwordMismatch(context: Context): String = context.getString(R.string.confirmPasswordDontMatch)
    fun checkTerms(context: Context): String = context.getString(R.string.checkTheTermsAndPrivacyPolicy)
    fun enterEmailAgain(context: Context): String = context.getString(R.string.enterYourEmailAgain)
    
    // MARK: - Success Messages
    // Success messages for authentication operations
    fun loginSuccess(context: Context): String = context.getString(R.string.loginSuccessfully)
    fun logoutSuccess(context: Context): String = context.getString(R.string.loggedOutSuccessfully)
    fun signUpSuccess(context: Context): String = context.getString(R.string.signupSuccessfully)
    fun verificationEmailSent(context: Context): String = context.getString(R.string.verificationEmailSentSuccessfully)
    fun passwordResetSent(context: Context): String = context.getString(R.string.passwordResetEmailSentSuccessfully)
    fun deleteAccountSuccess(context: Context): String = context.getString(R.string.deleteAccountSuccessfully)
    fun accountDeletedSuccess(context: Context): String = context.getString(R.string.accountDeletedSuccessfully)
    
    // MARK: - Error Titles
    // Error titles for authentication operations
    fun loginErrorTitle(context: Context): String = context.getString(R.string.loginError)
    fun logoutErrorTitle(context: Context): String = context.getString(R.string.logoutError)
    fun signUpErrorTitle(context: Context): String = context.getString(R.string.signupError)
    fun passwordResetErrorTitle(context: Context): String = context.getString(R.string.passwordResetError)
    fun deleteAccountErrorTitle(context: Context): String = context.getString(R.string.deleteAccountError)
    
    // MARK: - Error Messages
    // Error messages for authentication operations
    fun notVerifiedAccount(context: Context): String = context.getString(R.string.notVerifiedAccount)
    fun confirmEmail(context: Context): String = context.getString(R.string.confirmYourEmail)
    fun verificationEmailNotSent(context: Context): String = context.getString(R.string.loginError) // Using login error as fallback
    fun accountNotDeleted(context: Context): String = context.getString(R.string.accountCouldNotBeDeleted)
    fun passwordResetTitle(context: Context): String = context.getString(R.string.passwordReset)
    fun incorrectEmail(context: Context): String = context.getString(R.string.incorrectEmail)
    
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

