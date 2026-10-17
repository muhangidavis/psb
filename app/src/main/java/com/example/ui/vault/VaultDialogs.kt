package com.example.ui.vault

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.PsbBorder
import com.example.ui.theme.PsbError
import com.example.ui.theme.PsbOrange
import com.example.ui.theme.PsbOrangeLight
import com.example.ui.theme.PsbSteel
import com.example.ui.theme.PsbSteelDark
import com.example.ui.theme.PsbSteelLight
import com.example.ui.theme.PsbSteelMuted
import com.example.ui.theme.PsbSuccess
import com.example.viewmodel.PreviewMedia
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun NewFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181820),
        title = {
            Text("📁 New Folder", color = PsbOrangeLight, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Name your compartment (e.g. Information, Movies, Music, Records, Photos):",
                    color = PsbSteelMuted,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("Folder name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsbOrange,
                        unfocusedBorderColor = Color(0xFF2C2C37),
                        focusedContainerColor = Color(0xFF0D0D12),
                        unfocusedContainerColor = Color(0xFF0D0D12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_folder_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) onConfirm(folderName)
                },
                enabled = folderName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PsbOrange, contentColor = Color(0xFF1A0D00)),
                modifier = Modifier.testTag("new_folder_confirm")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbSteelLight)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181820),
        title = {
            Text("🔗 Add Link", color = PsbOrangeLight, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Save a bookmark safely inside this compartment:", color = PsbSteelMuted, fontSize = 13.sp)
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (name.isBlank() && it.isNotBlank()) {
                            name = it.removePrefix("https://").removePrefix("http://").take(30)
                        }
                    },
                    placeholder = { Text("https://...") },
                    label = { Text("URL") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsbOrange,
                        unfocusedBorderColor = Color(0xFF2C2C37),
                        focusedContainerColor = Color(0xFF0D0D12),
                        unfocusedContainerColor = Color(0xFF0D0D12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Link Title") },
                    label = { Text("Display Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsbOrange,
                        unfocusedBorderColor = Color(0xFF2C2C37),
                        focusedContainerColor = Color(0xFF0D0D12),
                        unfocusedContainerColor = Color(0xFF0D0D12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank() && name.isNotBlank()) onConfirm(name, url)
                },
                enabled = url.isNotBlank() && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PsbOrange, contentColor = Color(0xFF1A0D00))
            ) {
                Text("Save Link")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbSteelLight)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateOrEditNoteDialog(
    initialTitle: String = "",
    initialContent: String = "",
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(max = 580.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181822))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "✎ Edit Note" else "📝 Secure Encrypted Note",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = PsbOrangeLight
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = PsbSteelMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Note Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsbOrange,
                        unfocusedBorderColor = Color(0xFF2C2C37),
                        focusedContainerColor = Color(0xFF0D0D12),
                        unfocusedContainerColor = Color(0xFF0D0D12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Write your confidential notes, passwords, or secret info here...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsbOrange,
                        unfocusedBorderColor = Color(0xFF2C2C37),
                        focusedContainerColor = Color(0xFF0D0D12),
                        unfocusedContainerColor = Color(0xFF0D0D12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbSteelLight)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) onSave(title, content)
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PsbOrange, contentColor = Color(0xFF1A0D00)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Encrypted")
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181820),
        title = {
            Text("✎ Rename Item", color = PsbOrangeLight, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Enter a new name:", color = PsbSteelMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsbOrange,
                        unfocusedBorderColor = Color(0xFF2C2C37),
                        focusedContainerColor = Color(0xFF0D0D12),
                        unfocusedContainerColor = Color(0xFF0D0D12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newName.isNotBlank()) onConfirm(newName) },
                enabled = newName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PsbOrange, contentColor = Color(0xFF1A0D00))
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbSteelLight)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181820),
        title = {
            Text("🗑️ Delete Confirmation", color = PsbError, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "Permanently delete $count item(s) and everything inside them? This action cannot be undone.",
                color = PsbSteelLight,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PsbError, contentColor = Color.White)
            ) {
                Text("Delete Permanently")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbSteelLight)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (newPass: String, newPin: String) -> Unit
) {
    var pass1 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181820),
        title = {
            Text("🔑 Change Password & PIN", color = PsbOrangeLight, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Update your vault master credentials:", color = PsbSteelMuted, fontSize = 12.sp)

                OutlinedTextField(
                    value = pass1,
                    onValueChange = { pass1 = it },
                    label = { Text("New Password (10+ chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsbOrange,
                        unfocusedBorderColor = Color(0xFF2C2C37),
                        focusedContainerColor = Color(0xFF0D0D12),
                        unfocusedContainerColor = Color(0xFF0D0D12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pass2,
                    onValueChange = { pass2 = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsbOrange,
                        unfocusedBorderColor = Color(0xFF2C2C37),
                        focusedContainerColor = Color(0xFF0D0D12),
                        unfocusedContainerColor = Color(0xFF0D0D12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("New 4-digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsbOrange,
                        unfocusedBorderColor = Color(0xFF2C2C37),
                        focusedContainerColor = Color(0xFF0D0D12),
                        unfocusedContainerColor = Color(0xFF0D0D12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(it, color = PsbError, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pass1.length < 10) {
                        errorMsg = "Password must be at least 10 characters."
                        return@Button
                    }
                    if (pass1 != pass2) {
                        errorMsg = "Passwords do not match."
                        return@Button
                    }
                    if (pin.length != 4) {
                        errorMsg = "PIN must be exactly 4 digits."
                        return@Button
                    }
                    onConfirm(pass1, pin)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PsbOrange, contentColor = Color(0xFF1A0D00))
            ) {
                Text("Update Credentials")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbSteelLight)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AudioRecorderDialog(
    onDismiss: () -> Unit,
    onSave: (tempFile: File, durationMs: Long) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var recordedSeconds by remember { mutableIntStateOf(0) }
    var isDoneRecording by remember { mutableStateOf(false) }

    // Pulsing recording dot
    val infiniteTransition = rememberInfiniteTransition(label = "recPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse"
    )

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordedSeconds = 0
            while (isRecording) {
                delay(1000)
                recordedSeconds += 1
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {}
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181822))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎙️ Voice Memo Recorder",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PsbOrangeLight
                )
                Text(
                    text = "Audio records directly into encrypted vault memory.",
                    fontSize = 12.sp,
                    color = PsbSteelMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                if (!hasPermission) {
                    Text(
                        "Microphone permission is required to record encrypted voice memos.",
                        fontSize = 13.sp,
                        color = PsbSteelLight,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = PsbOrange, contentColor = Color(0xFF1A0D00))
                    ) {
                        Text("Grant Microphone Access")
                    }
                } else {
                    // Recording timer
                    val mins = recordedSeconds / 60
                    val secs = recordedSeconds % 60
                    val timerStr = String.format(Locale.US, "%02d:%02d", mins, secs)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = timerStr,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) Color.Red else PsbSteelLight
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRecording && !isDoneRecording) {
                            Button(
                                onClick = {
                                    try {
                                        val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
                                        outputFile = file
                                        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            MediaRecorder(context)
                                        } else {
                                            @Suppress("DEPRECATION")
                                            MediaRecorder()
                                        }
                                        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
                                        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                        mr.setOutputFile(file.absolutePath)
                                        mr.prepare()
                                        mr.start()
                                        recorder = mr
                                        isRecording = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Recording")
                            }
                        } else if (isRecording) {
                            Button(
                                onClick = {
                                    try {
                                        recorder?.stop()
                                        recorder?.release()
                                        recorder = null
                                        isRecording = false
                                        isDoneRecording = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33333F), contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop Recording")
                            }
                        } else {
                            // Done recording, show save or discard
                            OutlinedButton(
                                onClick = {
                                    outputFile?.delete()
                                    outputFile = null
                                    isDoneRecording = false
                                    recordedSeconds = 0
                                }
                            ) {
                                Text("Discard")
                            }
                            Button(
                                onClick = {
                                    outputFile?.let { file ->
                                        onSave(file, (recordedSeconds * 1000).toLong())
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PsbOrange, contentColor = Color(0xFF1A0D00))
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Vault")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbSteelMuted)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun MediaPreviewDialog(
    preview: PreviewMedia,
    onDismiss: () -> Unit,
    onSaveNote: (updatedContent: String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preview.node.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PsbOrangeLight,
                        maxLines = 1
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = PsbSteelMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (preview.imageBitmap != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = preview.imageBitmap.asImageBitmap(),
                            contentDescription = preview.node.name,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (preview.audioFile != null) {
                    AudioPlayerView(audioFile = preview.audioFile)
                } else if (preview.textContent != null) {
                    var editedText by remember { mutableStateOf(preview.textContent) }

                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PsbOrange,
                            unfocusedBorderColor = Color(0xFF2C2C37),
                            focusedContainerColor = Color(0xFF0D0D12),
                            unfocusedContainerColor = Color(0xFF0D0D12)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSaveNote(editedText) },
                        colors = ButtonDefaults.buttonColors(containerColor = PsbOrange, contentColor = Color(0xFF1A0D00)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayerView(audioFile: File) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(1) }

    DisposableEffect(audioFile) {
        val mp = MediaPlayer().apply {
            setDataSource(audioFile.absolutePath)
            prepare()
            setOnCompletionListener {
                isPlaying = false
                currentPosition = 0
            }
        }
        duration = mp.duration.coerceAtLeast(1)
        mediaPlayer = mp

        onDispose {
            mp.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let {
                currentPosition = it.currentPosition
            }
            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎵 Encrypted Audio Playback", color = PsbSteelLight, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        val curSec = currentPosition / 1000
        val durSec = duration / 1000
        val timeLabel = String.format(Locale.US, "%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, durSec / 60, durSec % 60)
        Text(timeLabel, color = PsbOrangeLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Slider(
            value = currentPosition.toFloat(),
            onValueChange = {
                currentPosition = it.toInt()
                mediaPlayer?.seekTo(it.toInt())
            },
            valueRange = 0f..duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = PsbOrange,
                activeTrackColor = PsbOrange,
                inactiveTrackColor = Color(0xFF33333F)
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    mediaPlayer?.let { mp ->
                        if (isPlaying) {
                            mp.pause()
                            isPlaying = false
                        } else {
                            mp.start()
                            isPlaying = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PsbOrange, contentColor = Color(0xFF1A0D00))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isPlaying) "Pause" else "Play")
            }
        }
    }
}
