package com.pushlock.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pushlock.app.ui.AppViewModel
import com.pushlock.app.ui.components.LogoMark
import com.pushlock.app.ui.components.PLButton
import com.pushlock.app.ui.components.fieldColors
import com.pushlock.app.ui.theme.Accent
import com.pushlock.app.ui.theme.Danger
import com.pushlock.app.ui.theme.TextMuted
import com.pushlock.app.ui.theme.appBackground

@Composable
fun AuthScreen(vm: AppViewModel) {
    var signUp by remember { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val canSubmit = email.contains("@") && password.length >= 6 && !vm.busy

    Column(
        Modifier
            .fillMaxSize()
            .appBackground()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        LogoMark(84.dp)
        Spacer(Modifier.height(20.dp))
        Text("PushLock", style = MaterialTheme.typography.headlineMedium)
        Text("Earn your scroll.", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(40.dp))

        Text(
            if (signUp) "Create your account" else "Welcome back",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Rounded.Email, null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null)
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors(),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        if (signUp) {
            Text(
                "At least 6 characters.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 6.dp),
            )
        }

        vm.message?.let {
            Spacer(Modifier.height(14.dp))
            Text(
                it,
                color = if (vm.messageIsError) Danger else Accent,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))
        PLButton(
            text = if (signUp) "Create account" else "Sign in",
            enabled = canSubmit,
            loading = vm.busy,
            onClick = { if (signUp) vm.signUp(email, password) else vm.signIn(email, password) },
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { signUp = !signUp; vm.clearMessage() }) {
            Text(
                if (signUp) "Already have an account? Sign in" else "New here? Create an account",
                color = TextMuted,
            )
        }
        if (vm.busy) {
            Text(
                "The first request can take up to a minute while the server wakes up.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
