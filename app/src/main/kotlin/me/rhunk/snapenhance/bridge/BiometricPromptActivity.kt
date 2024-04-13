package me.rhunk.snapenhance.bridge

import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import me.rhunk.snapenhance.SharedContextHolder
import java.util.concurrent.Executors

class BiometricPromptActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun cancel() {
            setResult(RESULT_CANCELED, Intent())
            finish()
        }

        val remoteSideContext = SharedContextHolder.remote(this)

        BiometricPrompt.Builder(this@BiometricPromptActivity)
            .setTitle(remoteSideContext.translation["biometric_auth.title"])
            .setSubtitle(remoteSideContext.translation["biometric_auth.subtitle"])
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    setDeviceCredentialAllowed(true)
                }
            }
            .build().authenticate(
                CancellationSignal().apply {
                    setOnCancelListener {
                        cancel()
                    }
                },
                Executors.newSingleThreadExecutor(),
                object: BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        cancel()
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        setResult(RESULT_OK, Intent())
                        finish()
                    }
                }
            )

        setContent {}
    }
}