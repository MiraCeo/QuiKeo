package com.locationjoystick.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.locationjoystick.core.designsystem.LjSpacing
import com.locationjoystick.core.designsystem.R

@Composable
fun LjRouteStartOptions(
    loop: Boolean,
    onLoopChange: (Boolean) -> Unit,
    reverse: Boolean,
    onReverseChange: (Boolean) -> Unit,
    returnToLocation: Boolean,
    onReturnToLocationChange: (Boolean) -> Unit,
    followRoads: Boolean,
    onFollowRoadsChange: (Boolean) -> Unit,
    onTeleport: () -> Unit,
    onCancel: () -> Unit,
    onStart: () -> Unit,
    hideTeleport: Boolean = false,
    isTeleportRoute: Boolean = false,
    textColor: Color = Color.Unspecified,
    isStarting: Boolean = false,
) {
    Column {
        LjCheckboxRow(
            title = "Loop",
            checked = loop,
            enabled = !returnToLocation,
            onCheckedChange = onLoopChange,
            textColor = textColor,
        )
        LjCheckboxRow(
            title = "Reverse",
            checked = reverse,
            onCheckedChange = onReverseChange,
            textColor = textColor,
        )
        LjCheckboxRow(
            title = "Return to location",
            checked = returnToLocation,
            enabled = !loop,
            onCheckedChange = onReturnToLocationChange,
            textColor = textColor,
        )
        if (!isTeleportRoute) {
            LjCheckboxRow(
                title = "Follow roads",
                checked = followRoads,
                onCheckedChange = onFollowRoadsChange,
                textColor = textColor,
            )
        }
        if (!hideTeleport) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onTeleport, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.route_start_options_teleport), color = textColor)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LjSpacing.sm)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.route_start_options_cancel), color = textColor)
            }
            Button(onClick = onStart, enabled = !isStarting, modifier = Modifier.weight(1f)) {
                if (isStarting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.common_start), color = textColor)
                }
            }
        }
    }
}
