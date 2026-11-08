package com.hacybeyker.cartera.core.data.biometric.device

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.common.MainDispatcher
import com.hacybeyker.cartera.core.data.biometric.BiometricPrompter
import com.hacybeyker.cartera.core.data.biometric.PromptPurpose
import com.hacybeyker.cartera.core.data.biometric.PromptResult
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * BiometricPrompt sobre la Activity visible, que [attach] registra y suelta al destruirse. El callback vive en el
 * ViewModel de BiometricPrompt, así que una rotación con el diálogo abierto sigue entregando el resultado.
 */
@Singleton
class ActivityBiometricPrompter @Inject constructor(@MainDispatcher private val main: CoroutineDispatcher) :
    BiometricPrompter {

    private var host: FragmentActivity? = null

    fun attach(activity: FragmentActivity) {
        host = activity
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (host === activity) host = null
                }
            }
        )
    }

    override suspend fun authenticate(cipher: Cipher, purpose: PromptPurpose): PromptResult = withContext(main) {
        val activity = host ?: return@withContext PromptResult.Dismissed
        suspendCancellableCoroutine<PromptResult> { continuation ->
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback(continuation))
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(promptInfo(activity, purpose), BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun promptInfo(activity: FragmentActivity, purpose: PromptPurpose): BiometricPrompt.PromptInfo {
        val title = when (purpose) {
            PromptPurpose.ENROLL -> R.string.biometric_prompt_enroll_title
            PromptPurpose.LOG_IN -> R.string.biometric_prompt_login_title
        }
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(title))
            .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
            .setNegativeButtonText(activity.getString(R.string.common_cancel))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }

    private fun callback(continuation: CancellableContinuation<PromptResult>) =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val cipher = result.cryptoObject?.cipher
                continuation.resumeOnce(cipher?.let { PromptResult.Authenticated(it) } ?: PromptResult.Dismissed)
            }

            // Cancelar, demasiados intentos o un error del sensor: el diálogo del sistema ya se lo mostró al usuario.
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                continuation.resumeOnce(PromptResult.Dismissed)
            }
        }

    private fun CancellableContinuation<PromptResult>.resumeOnce(result: PromptResult) {
        if (isActive) resume(result)
    }
}
