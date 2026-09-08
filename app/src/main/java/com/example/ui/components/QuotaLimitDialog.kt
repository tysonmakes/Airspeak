package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.AiEngine
import com.example.data.remote.QuotaAlertEvent
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.EmeraldSuccess

@Composable
fun QuotaLimitDialog(
    quotaAlert: QuotaAlertEvent,
    onDismiss: () -> Unit,
    onChangeModelOrKey: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("quota_limit_dialog"),
        containerColor = Color(0xFF161821),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(AmberTertiary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = quotaAlert.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Zero Downtime Auto-Protection Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberTertiary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = quotaAlert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )

                // Current Auto-Switched Model Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212E)),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(EmeraldSuccess.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Fallback: ${quotaAlert.activeBackupEngine.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Speaking sessions & scoring continue without stopping.",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF262A38)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = null,
                            tint = Color(0xFFC77DFF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aap kabhi bhi new API key enter kar sakte hain ya model switch kar sakte hain.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onChangeModelOrKey()
                },
                modifier = Modifier.testTag("dialog_change_model_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B2CBF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Change Key / Model")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_continue_backup_btn"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF383B4A))
            ) {
                Text("Continue Free", color = Color.White)
            }
        }
    )
}
