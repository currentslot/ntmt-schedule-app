package ntmt.schedule.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

object CabinetLock {
    fun available(activity: Activity): Boolean {
        val km = activity.getSystemService(KeyguardManager::class.java)
        if (km?.isDeviceSecure != true) return false
        if (Build.VERSION.SDK_INT >= 30) {
            val bm = activity.getSystemService(BiometricManager::class.java)
            val ok = bm?.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            return ok == BiometricManager.BIOMETRIC_SUCCESS
        }
        return true
    }

    fun credentialIntent(activity: Activity): Intent? {
        val km = activity.getSystemService(KeyguardManager::class.java) ?: return null
        return km.createConfirmDeviceCredentialIntent(
            "Быстрый вход",
            "Код, отпечаток или лицо",
        )
    }

    fun biometric(activity: Activity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (Build.VERSION.SDK_INT < 29) {
            onError("Нет системной проверки")
            return
        }
        val builder = BiometricPrompt.Builder(activity)
            .setTitle("Быстрый вход")
            .setSubtitle("Код, отпечаток или лицо")
        val prompt = if (Build.VERSION.SDK_INT >= 30) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            ).build()
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true).build()
        }
        prompt.authenticate(
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                        errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                    ) {
                        return
                    }
                    onError(errString.toString().ifBlank { "Не удалось подтвердить вход" })
                }
            },
        )
    }
}
