package com.example.ui.vault

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultNodeEntity
import com.example.ui.theme.PsbBorder
import com.example.ui.theme.PsbOrange
import com.example.ui.theme.PsbOrangeLight
import com.example.ui.theme.PsbSteelDark
import com.example.ui.theme.PsbSteelLight
import com.example.ui.theme.PsbSteelMuted
import com.example.ui.theme.PsbSuccess
import com.example.viewmodel.ClipboardMode
import com.example.viewmodel.VaultViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen(viewModel: VaultViewModel) {
    val context = LocalContext.current
    val guardian by viewModel.selectedGuardian.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val nodes by viewModel.currentNodes.collectAsState()
    val selectedIds by viewModel.selectedNodeIds.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    val totalStorageBytes by viewModel.totalStorageBytes.collectAsState()
    val autoLockSeconds by viewModel.autoLockSecondsRemaining.collectAsState()
    val activeToast by viewModel.activeToast.collectAsState()
    val previewMedia by viewModel.previewMedia.collectAsState()

    // Dialog state
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangePassDialog by remember { mutableStateOf(false) }
    var showAudioRecorderDialog by remember { mutableStateOf(false) }

    // Media & File Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val name = "Photo_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
            viewModel.importFileFromUri(uri, name, mime)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val name = "File_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            viewModel.importFileFromUri(uri, name, mime)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.saveCapturedPhoto(bitmap)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0E))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // TopBar
            VaultTopBar(
                guardian = guardian ?: "🐯",
                onLock = { viewModel.lockVault() },
                onChangePassword = { showChangePassDialog = true }
            )

            // Actions Toolbar
            VaultToolbar(
                onNewFolder = { showNewFolderDialog = true },
                onUpload = {
                    filePickerLauncher.launch("*/*")
                },
                onPhotoPicker = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        )
                    )
                },
                onCamera = {
                    cameraLauncher.launch()
                },
                onRecord = { showAudioRecorderDialog = true },
                onNote = { showCreateNoteDialog = true },
                onLink = { showAddLinkDialog = true },
                onCopy = { viewModel.copySelected() },
                onCut = { viewModel.cutSelected() },
                onPaste = { viewModel.pasteClipboard() },
                onOpen = {
                    val selNode = nodes.firstOrNull { it.id in selectedIds }
                    if (selNode != null) {
                        if (selNode.type == "link" && selNode.url != null) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selNode.url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        } else {
                            viewModel.openNode(selNode)
                        }
                    }
                },
                onRename = { showRenameDialog = true },
                onDelete = { showDeleteDialog = true },
                hasSelection = selectedIds.isNotEmpty(),
                hasSingleSelection = selectedIds.size == 1,
                hasClipboard = clipboard != null
            )

            // Breadcrumbs Navigation Bar
            BreadcrumbsBar(
                breadcrumbs = breadcrumbs,
                onBreadcrumbClick = { viewModel.navigateToBreadcrumb(it) }
            )

            // Compartment Grid View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (nodes.isEmpty()) {
                    EmptyCompartmentView()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 135.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(nodes, key = { it.id }) { node ->
                            val isSelected = selectedIds.contains(node.id)
                            VaultNodeCard(
                                node = node,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectedIds.isNotEmpty()) {
                                        viewModel.toggleNodeSelection(node.id, isMulti = true)
                                    } else {
                                        viewModel.toggleNodeSelection(node.id, isMulti = false)
                                    }
                                },
                                onDoubleClickOrOpen = {
                                    if (node.type == "link" && node.url != null) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(node.url))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    } else {
                                        viewModel.openNode(node)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Statusbar
            VaultStatusBar(
                itemCount = nodes.size,
                selectedCount = selectedIds.size,
                clipboardInfo = clipboard?.let { "${it.nodeIds.size} (${if (it.mode == ClipboardMode.COPY) "copy" else "cut"})" },
                storageBytes = totalStorageBytes,
                autoLockSeconds = autoLockSeconds
            )
        }

        // Animated Toast Banner
        AnimatedVisibility(
            visible = activeToast != null,
            enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { 50 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
        ) {
            activeToast?.let { toastMsg ->
                Box(
                    modifier = Modifier
                        .shadow(16.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1F1F28))
                        .border(1.dp, Color(0xFF35353F), RoundedCornerShape(12.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = toastMsg,
                        color = PsbSteelLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Active Dialogs
        if (showNewFolderDialog) {
            NewFolderDialog(
                onDismiss = { showNewFolderDialog = false },
                onConfirm = { name ->
                    viewModel.createFolder(name)
                    showNewFolderDialog = false
                }
            )
        }

        if (showAddLinkDialog) {
            AddLinkDialog(
                onDismiss = { showAddLinkDialog = false },
                onConfirm = { name, url ->
                    viewModel.saveLink(name, url)
                    showAddLinkDialog = false
                }
            )
        }

        if (showCreateNoteDialog) {
            CreateOrEditNoteDialog(
                onDismiss = { showCreateNoteDialog = false },
                onSave = { title, content ->
                    viewModel.saveNote(title, content)
                    showCreateNoteDialog = false
                }
            )
        }

        if (showRenameDialog) {
            val selectedNode = nodes.firstOrNull { it.id in selectedIds }
            if (selectedNode != null) {
                RenameDialog(
                    initialName = selectedNode.name,
                    onDismiss = { showRenameDialog = false },
                    onConfirm = { newName ->
                        viewModel.renameNode(selectedNode.id, newName)
                        showRenameDialog = false
                    }
                )
            }
        }

        if (showDeleteDialog) {
            ConfirmDeleteDialog(
                count = selectedIds.size,
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    viewModel.deleteSelectedNodes()
                    showDeleteDialog = false
                }
            )
        }

        if (showChangePassDialog) {
            ChangePasswordDialog(
                onDismiss = { showChangePassDialog = false },
                onConfirm = { newPass, newPin ->
                    viewModel.changePasswordAndPin(newPass, newPin)
                    showChangePassDialog = false
                }
            )
        }

        if (showAudioRecorderDialog) {
            AudioRecorderDialog(
                onDismiss = { showAudioRecorderDialog = false },
                onSave = { file, durationMs ->
                    viewModel.saveRecordedAudio(file, durationMs)
                    showAudioRecorderDialog = false
                }
            )
        }

        previewMedia?.let { preview ->
            MediaPreviewDialog(
                preview = preview,
                onDismiss = { viewModel.dismissPreview() },
                onSaveNote = { updatedContent ->
                    viewModel.updateNote(preview.node, preview.node.name, updatedContent)
                    viewModel.dismissPreview()
                }
            )
        }
    }
}

@Composable
fun VaultTopBar(
    guardian: String,
    onLock: () -> Unit,
    onChangePassword: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16161D))
            .border(1.dp, Color(0xFF26262F))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "🟠 PSB",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PsbOrangeLight
            )

            // Guardian Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1B1B23))
                    .border(1.dp, Color(0xFF2A2A34), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = guardian, fontSize = 16.sp)
                Text(
                    text = "vault · " + SimpleDateFormat("MMM d", Locale.US).format(Date()),
                    fontSize = 12.sp,
                    color = PsbSteelMuted
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onChangePassword,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbSteelLight),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(PsbBorder, PsbBorder))),
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Credentials", fontSize = 12.sp)
            }

            Button(
                onClick = onLock,
                colors = ButtonDefaults.buttonColors(containerColor = PsbOrange, contentColor = Color(0xFF1A0D00)),
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.height(36.dp).testTag("lock_box_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lock Box", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VaultToolbar(
    onNewFolder: () -> Unit,
    onUpload: () -> Unit,
    onPhotoPicker: () -> Unit,
    onCamera: () -> Unit,
    onRecord: () -> Unit,
    onNote: () -> Unit,
    onLink: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    hasSelection: Boolean,
    hasSingleSelection: Boolean,
    hasClipboard: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131319))
            .border(1.dp, Color(0xFF22222B))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarBtn(label = "Folder", icon = Icons.Default.CreateNewFolder, onClick = onNewFolder, tag = "action_folder")
        ToolbarBtn(label = "Import", icon = Icons.Default.UploadFile, onClick = onUpload, tag = "action_import")
        ToolbarBtn(label = "Photos", icon = Icons.Default.UploadFile, onClick = onPhotoPicker, tag = "action_photos")
        ToolbarBtn(label = "Camera", icon = Icons.Default.PhotoCamera, onClick = onCamera, tag = "action_camera")
        ToolbarBtn(label = "Record", icon = Icons.Default.Mic, onClick = onRecord, tag = "action_record")
        ToolbarBtn(label = "Note", icon = Icons.Default.EditNote, onClick = onNote, tag = "action_note")
        ToolbarBtn(label = "Link", icon = Icons.Default.Link, onClick = onLink, tag = "action_link")

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .width(1.dp)
                .height(20.dp)
                .background(Color(0xFF2C2C37))
        )

        ToolbarBtn(label = "Copy", icon = Icons.Default.ContentCopy, onClick = onCopy, enabled = hasSelection, tag = "action_copy")
        ToolbarBtn(label = "Cut", icon = Icons.Default.ContentCut, onClick = onCut, enabled = hasSelection, tag = "action_cut")
        ToolbarBtn(label = "Paste", icon = Icons.Default.ContentPaste, onClick = onPaste, enabled = hasClipboard, tag = "action_paste")

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .width(1.dp)
                .height(20.dp)
                .background(Color(0xFF2C2C37))
        )

        ToolbarBtn(label = "Open", icon = Icons.Default.Visibility, onClick = onOpen, enabled = hasSingleSelection, tag = "action_open")
        ToolbarBtn(label = "Rename", icon = Icons.Default.DriveFileRenameOutline, onClick = onRename, enabled = hasSingleSelection, tag = "action_rename")
        ToolbarBtn(label = "Delete", icon = Icons.Default.Delete, onClick = onDelete, enabled = hasSelection, isDestructive = true, tag = "action_delete")
    }
}

@Composable
fun ToolbarBtn(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    tag: String = ""
) {
    val bgColor = if (enabled) Color(0xFF1C1C24) else Color(0xFF141418)
    val contentColor = if (!enabled) Color(0xFF555562) else if (isDestructive) Color(0xFFFF6B6B) else PsbSteelLight

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, if (enabled) Color(0xFF2C2C37) else Color(0xFF1E1E26), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(15.dp))
        Text(text = label, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BreadcrumbsBar(
    breadcrumbs: List<com.example.viewmodel.BreadcrumbItem>,
    onBreadcrumbClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        breadcrumbs.forEachIndexed { index, item ->
            val isLast = index == breadcrumbs.lastIndex
            Text(
                text = item.name,
                color = if (isLast) PsbOrangeLight else PsbSteelMuted,
                fontSize = 13.sp,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onBreadcrumbClick(item.id) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            if (!isLast) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF3D3D4A),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun VaultNodeCard(
    node: VaultNodeEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClickOrOpen: () -> Unit
) {
    val (iconEmoji, tagLabel) = when (node.type) {
        "folder" -> Pair("📁", "FOLDER")
        "photo" -> Pair("🖼️", "PHOTO")
        "recording" -> Pair("🎙️", "AUDIO")
        "note" -> Pair("📝", "NOTE")
        "link" -> Pair("🔗", "LINK")
        else -> {
            if (node.mime.startsWith("image")) Pair("🖼️", "IMAGE")
            else if (node.mime.startsWith("audio")) Pair("🎵", "AUDIO")
            else if (node.mime.startsWith("video")) Pair("🎬", "VIDEO")
            else Pair("📦", "FILE")
        }
    }

    val subtitle = when (node.type) {
        "link" -> node.url?.removePrefix("https://")?.removePrefix("http://")?.take(20) ?: "link"
        "folder" -> "Compartment"
        else -> formatFileSize(node.size)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected)
                    Brush.verticalGradient(listOf(Color(0xFF261B0E), Color(0xFF1C1309)))
                else
                    Brush.verticalGradient(listOf(Color(0xFF1A1A22), Color(0xFF141419)))
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PsbOrange else Color(0xFF26262F),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("node_${node.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tag in top right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = tagLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PsbOrangeLight else PsbSteelMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF22222C))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Big Icon / Emoji
            Text(
                text = iconEmoji,
                fontSize = 36.sp,
                modifier = Modifier.clickable { onDoubleClickOrOpen() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Node name
            Text(
                text = node.name,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = PsbSteelLight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Subtitle
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = PsbSteelMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (node.type == "folder") {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onDoubleClickOrOpen,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbOrangeLight),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(PsbOrange, PsbOrange))),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .padding(0.dp)
                ) {
                    Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyCompartmentView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📦", fontSize = 54.sp, modifier = Modifier.padding(bottom = 12.dp))
        Text(
            text = "This compartment is empty.",
            color = PsbSteelLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Create a folder, upload files, take a photo, write a note, or record something.",
            color = PsbSteelMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun VaultStatusBar(
    itemCount: Int,
    selectedCount: Int,
    clipboardInfo: String?,
    storageBytes: Long,
    autoLockSeconds: Int
) {
    val mins = autoLockSeconds / 60
    val secs = autoLockSeconds % 60
    val lockCountdown = String.format(Locale.US, "%02d:%02d", mins, secs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF101015))
            .border(1.dp, Color(0xFF22222B))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val leftInfo = buildString {
            append("$itemCount item${if (itemCount == 1) "" else "s"} here")
            if (selectedCount > 0) append(" · $selectedCount selected")
            if (clipboardInfo != null) append(" · clip: $clipboardInfo")
        }

        Text(
            text = leftInfo,
            fontSize = 11.5.sp,
            color = PsbSteelMuted
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Used: ${formatFileSize(storageBytes)}",
                fontSize = 11.5.sp,
                color = PsbSteelMuted
            )
            Text(
                text = "·",
                fontSize = 11.5.sp,
                color = PsbSteelDark
            )
            Text(
                text = "🔒 $lockCountdown",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (autoLockSeconds < 60) Color(0xFFFF6B6B) else PsbOrangeLight
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024f
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024f
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024f
    return String.format(Locale.US, "%.2f GB", gb)
}
