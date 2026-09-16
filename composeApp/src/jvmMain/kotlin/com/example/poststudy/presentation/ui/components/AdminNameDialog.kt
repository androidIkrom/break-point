package com.example.poststudy.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.poststudy.domain.model.AdminUsername
import com.example.poststudy.presentation.theme.AppDesign

/**
 * Asks for the admin's username (shown to students in the list of active admins). With
 * [mandatory] it cannot be closed until a valid username is saved.
 */
@Composable
fun AdminNameDialog(
    initialName: String,
    mandatory: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(AdminUsername.filterTyping(initialName)) }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    PostStudyDialog(
        onDismissRequest = { if (!mandatory) onDismiss() },
        title = if (mandatory) "Username kiriting" else "Username",
        text = if (mandatory) {
            "Davom etish uchun username kiriting. Tinglovchilar adminlar ro'yxatida sizni shu nom bilan ko'radi."
        } else {
            "Bosh sahifada va tinglovchilarning adminlar ro'yxatida shu nom ko'rinadi."
        },
        confirmText = "Saqlash",
        showDismissButton = !mandatory,
        onConfirm = {
            error = AdminUsername.validate(name)
            if (error == null) onSave(name.trim())
        },
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = AdminUsername.filterTyping(it); error = null },
                label = { Text("Username") },
                singleLine = true,
                isError = error != null,
                supportingText = {
                    Text(error ?: "Bitta so'z, ${AdminUsername.MAX_LENGTH} ta belgigacha (${name.length}/${AdminUsername.MAX_LENGTH})")
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                shape = AppDesign.ComponentShape
            )
        }
    )
}
