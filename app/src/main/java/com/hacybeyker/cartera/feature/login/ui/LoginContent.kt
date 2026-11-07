package com.hacybeyker.cartera.feature.login.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.hacybeyker.cartera.R
import com.hacybeyker.cartera.core.designsystem.CarteraTheme
import com.hacybeyker.cartera.core.designsystem.component.BlockingProgress
import com.hacybeyker.cartera.core.designsystem.component.ErrorDialog
import com.hacybeyker.cartera.core.designsystem.spacing
import com.hacybeyker.cartera.core.ui.messageRes

@Composable
fun LoginContent(uiState: LoginUiState, onAction: (LoginAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.screen)
            .testTag(LoginTestTags.SCREEN),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md, Alignment.CenterVertically)
    ) {
        Text(text = stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = uiState.username,
            onValueChange = { onAction(LoginAction.UsernameChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginTestTags.USERNAME),
            label = { Text(text = stringResource(R.string.login_username)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next)
        )
        PasswordField(uiState = uiState, onAction = onAction)
        uiState.formError?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                modifier = Modifier.testTag(LoginTestTags.ERROR),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Button(
            onClick = { onAction(LoginAction.SubmitClicked) },
            enabled = uiState.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LoginTestTags.SUBMIT)
        ) {
            Text(text = stringResource(R.string.login_submit))
        }
    }
    LoginDialogs(uiState = uiState, onAction = onAction)
}

@Composable
private fun PasswordField(uiState: LoginUiState, onAction: (LoginAction) -> Unit) {
    OutlinedTextField(
        value = uiState.password,
        onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(LoginTestTags.PASSWORD),
        label = { Text(text = stringResource(R.string.login_password)) },
        singleLine = true,
        visualTransformation = if (uiState.isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAction(LoginAction.SubmitClicked) }),
        trailingIcon = {
            TextButton(
                onClick = { onAction(LoginAction.PasswordVisibilityToggled) },
                modifier = Modifier.testTag(LoginTestTags.SHOW_PASSWORD)
            ) {
                val label = if (uiState.isPasswordVisible) {
                    R.string.login_hide_password
                } else {
                    R.string.login_show_password
                }
                Text(text = stringResource(label))
            }
        }
    )
}

@Composable
private fun LoginDialogs(uiState: LoginUiState, onAction: (LoginAction) -> Unit) {
    if (uiState.isSubmitting) BlockingProgress()
    uiState.dialogError?.let { error ->
        ErrorDialog(
            title = stringResource(R.string.login_error_title),
            message = stringResource(error.messageRes()),
            onRetry = { onAction(LoginAction.RetryClicked) },
            onCancel = { onAction(LoginAction.ErrorDismissed) }
        )
    }
}

@StringRes
private fun LoginFormError.messageRes(): Int = when (this) {
    LoginFormError.INVALID_CREDENTIALS -> R.string.login_invalid_credentials
    LoginFormError.INVALID_INPUT -> R.string.error_validation
}

@PreviewLightDark
@Composable
private fun LoginEmptyPreview() {
    CarteraTheme {
        LoginContent(uiState = LoginUiState(), onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun LoginInvalidCredentialsPreview() {
    CarteraTheme {
        LoginContent(
            uiState = LoginUiState(
                username = "ana.torres",
                password = "incorrecta",
                formError = LoginFormError.INVALID_CREDENTIALS
            ),
            onAction = {}
        )
    }
}
