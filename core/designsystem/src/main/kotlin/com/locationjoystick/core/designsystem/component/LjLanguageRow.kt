package com.locationjoystick.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.locationjoystick.core.designsystem.LjIcons
import com.locationjoystick.core.designsystem.LjSpacing
import com.locationjoystick.core.designsystem.R
import com.locationjoystick.core.model.AppLanguage

@Composable
fun LjLanguageRow(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = LjSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.language_row_label),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(appLanguageLabel(selected), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(LjIcons.ArrowDropDown, contentDescription = null)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(appLanguageLabel(language)) },
                    onClick = {
                        expanded = false
                        onSelect(language)
                    },
                )
            }
        }
    }
}

@Composable
private fun appLanguageLabel(language: AppLanguage): String =
    when (language) {
        AppLanguage.SYSTEM_DEFAULT -> stringResource(R.string.language_system_default)
        AppLanguage.ENGLISH -> stringResource(R.string.language_english)
        AppLanguage.CHINESE_SIMPLIFIED -> stringResource(R.string.language_simplified_chinese)
    }
