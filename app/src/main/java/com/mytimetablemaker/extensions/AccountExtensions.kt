package com.mytimetablemaker.extensions

import android.content.Context
import com.google.firebase.auth.FirebaseAuthException
import com.mytimetablemaker.R
import java.util.regex.Pattern

// Account validation helpers and localized message utilities.
// Keeps input checks and error text in one place.

// Email format validation using a regex pattern.
fun String.isValidEmail(): Boolean {
    val emailRegex = "[A-Z0-9a-z._+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}"
    val pattern = Pattern.compile(emailRegex)
    return pattern.matcher(this).matches()
}

// Password validation: 8+ chars with at least one special character.
fun String.isValidPassword(): Boolean {
    val passwordRegex = "^(?=.*[A-Za-z0-9])(?=.*[!@#$&~]).{8,}$"
    val pattern = Pattern.compile(passwordRegex)
    return pattern.matcher(this).matches()
}

// Compares password and confirmation.
fun String.isMatching(confirmPassword: String): Boolean {
    return this == confirmPassword
}

// Maps Firebase Auth error codes to localized messages.
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

// Centralized validation text for UI messages.
object ValidationMessages {
    
    // Common validation messages for form inputs.
    fun inputError(context: Context): String = context.getString(R.string.inputError)
    fun checkError(context: Context): String = context.getString(R.string.checkError)
    fun enterEmail(context: Context): String = context.getString(R.string.enterYourEmail)
    fun enterPassword(context: Context): String = context.getString(R.string.enterYourPassword)
    fun enterConfirmPassword(context: Context): String = context.getString(R.string.enterYourConfirmPassword)
    fun enterEmailAgain(context: Context): String = context.getString(R.string.enterYourEmailAgain)
    fun incorrectEmailFormat(context: Context): String = context.getString(R.string.incorrectEmailFormat)
    fun incorrectPasswordFormat(context: Context): String = context.getString(R.string.incorrectPasswordFormat)
    fun passwordMismatch(context: Context): String = context.getString(R.string.confirmPasswordDontMatch)
    fun checkTerms(context: Context): String = context.getString(R.string.checkTheTermsAndPrivacyPolicy)
    
    // Success messages for account actions.
    fun loginSuccess(context: Context): String = context.getString(R.string.loginSuccessfully)
    fun logoutSuccess(context: Context): String = context.getString(R.string.loggedOutSuccessfully)
    fun signUpSuccess(context: Context): String = context.getString(R.string.signupSuccessfully)
    fun verificationEmailSent(context: Context): String = context.getString(R.string.verificationEmailSentSuccessfully)
    fun passwordResetSent(context: Context): String = context.getString(R.string.passwordResetEmailSentSuccessfully)
    fun deleteAccountSuccess(context: Context): String = context.getString(R.string.deleteAccountSuccessfully)
    fun accountDeletedSuccess(context: Context): String = context.getString(R.string.accountDeletedSuccessfully)
    
    // Error titles for dialogs or snack bars.
    fun loginErrorTitle(context: Context): String = context.getString(R.string.loginError)
    fun logoutErrorTitle(context: Context): String = context.getString(R.string.logoutError)
    fun signUpErrorTitle(context: Context): String = context.getString(R.string.signupError)
    fun passwordResetErrorTitle(context: Context): String = context.getString(R.string.passwordResetError)
    fun deleteAccountErrorTitle(context: Context): String = context.getString(R.string.deleteAccountError)
    
    // Detailed error messages for account flows.
    fun notVerifiedAccount(context: Context): String = context.getString(R.string.notVerifiedAccount)
    fun confirmEmail(context: Context): String = context.getString(R.string.confirmYourEmail)
    fun accountNotDeleted(context: Context): String = context.getString(R.string.accountCouldNotBeDeleted)
    fun passwordResetTitle(context: Context): String = context.getString(R.string.passwordReset)
    fun incorrectEmail(context: Context): String = context.getString(R.string.incorrectEmail)
    
    // Login validation with a title/message pair.
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
    
    // Sign-up validation with a title/message pair.
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

