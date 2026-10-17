package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VaultAccountEntity
import com.example.data.VaultNodeEntity
import com.example.repository.VaultRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.SecretKey

enum class AuthStep {
    GUARDIAN,
    PASSWORD,
    PIN
}

enum class PinStage {
    LOGIN,
    SET,
    CONFIRM
}

enum class ClipboardMode {
    COPY,
    CUT
}

data class ClipboardState(
    val mode: ClipboardMode,
    val nodeIds: List<String>
)

data class BreadcrumbItem(
    val id: String,
    val name: String
)

data class PasswordRules(
    val hasValidLength: Boolean = false,
    val hasLetter: Boolean = false,
    val hasDigit: Boolean = false,
    val matchesConfirm: Boolean = false
) {
    val isValid: Boolean get() = hasValidLength && hasLetter && hasDigit && matchesConfirm
}

data class PreviewMedia(
    val node: VaultNodeEntity,
    val imageBitmap: Bitmap? = null,
    val audioFile: File? = null,
    val textContent: String? = null
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    val repository = VaultRepository(application)

    // 30 Wild Animal Guardians from PSB prototype
    val allGuardians = listOf(
        "🐯", "🦁", "🐘", "🙀", "🦅", "🕊", "🦍", "🐆", "🐶", "🐺",
        "🐻", "🐼", "🦊", "🐨", "🐵", "🦒", "🦓", "🦌", "🐗", "🦏",
        "🐊", "🐢", "🦉", "🦜", "🐳", "🦈", "🐍", "🦂", "🐝", "🦋"
    )

    // Accounts list from DB
    val existingAccounts: StateFlow<List<VaultAccountEntity>> =
        repository.accountsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Auth & Lock Flow State
    private val _currentAuthStep = MutableStateFlow(AuthStep.GUARDIAN)
    val currentAuthStep: StateFlow<AuthStep> = _currentAuthStep.asStateFlow()

    private val _selectedGuardian = MutableStateFlow<String?>(null)
    val selectedGuardian: StateFlow<String?> = _selectedGuardian.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _confirmPasswordInput = MutableStateFlow("")
    val confirmPasswordInput: StateFlow<String> = _confirmPasswordInput.asStateFlow()

    private val _passwordRules = MutableStateFlow(PasswordRules())
    val passwordRules: StateFlow<PasswordRules> = _passwordRules.asStateFlow()

    private val _pinBuffer = MutableStateFlow("")
    val pinBuffer: StateFlow<String> = _pinBuffer.asStateFlow()

    private val _pinStage = MutableStateFlow(PinStage.LOGIN)
    val pinStage: StateFlow<PinStage> = _pinStage.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    private val _isAuthError = MutableStateFlow(false)
    val isAuthError: StateFlow<Boolean> = _isAuthError.asStateFlow()

    // Unlock Animations
    private val _isPadlockOpen = MutableStateFlow(false)
    val isPadlockOpen: StateFlow<Boolean> = _isPadlockOpen.asStateFlow()

    private val _isChainUnwound = MutableStateFlow(false)
    val isChainUnwound: StateFlow<Boolean> = _isChainUnwound.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    // Session State (Memory only)
    private var sessionKek: SecretKey? = null
    private var sessionDataKey: SecretKey? = null
    private var pendingPassword: String? = null
    private var pendingPin: String? = null

    // Inside Vault State
    private val _currentFolderId = MutableStateFlow("root")
    val currentFolderId: StateFlow<String> = _currentFolderId.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<BreadcrumbItem>>(listOf(BreadcrumbItem("root", "🟠 PSB")))
    val breadcrumbs: StateFlow<List<BreadcrumbItem>> = _breadcrumbs.asStateFlow()

    val currentNodes: StateFlow<List<VaultNodeEntity>> = combine(
        _selectedGuardian,
        _currentFolderId
    ) { guardian, folderId ->
        Pair(guardian, folderId)
    }.flatMapLatest { (guardian, folderId) ->
        if (guardian != null) {
            repository.getNodesInFolder(guardian, folderId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedNodeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNodeIds: StateFlow<Set<String>> = _selectedNodeIds.asStateFlow()

    private val _clipboard = MutableStateFlow<ClipboardState?>(null)
    val clipboard: StateFlow<ClipboardState?> = _clipboard.asStateFlow()

    private val _totalStorageBytes = MutableStateFlow(0L)
    val totalStorageBytes: StateFlow<Long> = _totalStorageBytes.asStateFlow()

    private val _activeToast = MutableStateFlow<String?>(null)
    val activeToast: StateFlow<String?> = _activeToast.asStateFlow()

    // Active Previews & Modals
    private val _previewMedia = MutableStateFlow<PreviewMedia?>(null)
    val previewMedia: StateFlow<PreviewMedia?> = _previewMedia.asStateFlow()

    // Auto-Lock Timer (5 minutes = 300s)
    private var autoLockJob: Job? = null
    private val _autoLockSecondsRemaining = MutableStateFlow(300)
    val autoLockSecondsRemaining: StateFlow<Int> = _autoLockSecondsRemaining.asStateFlow()

    init {
        resetActivityTimer()
    }

    fun selectGuardian(animal: String) {
        _selectedGuardian.value = animal
        _authMessage.value = null
        _isAuthError.value = false
    }

    fun proceedToPassword() {
        val guardian = _selectedGuardian.value ?: return
        val isExisting = existingAccounts.value.any { it.animal == guardian }

        _passwordInput.value = ""
        _confirmPasswordInput.value = ""
        _authMessage.value = null
        _isAuthError.value = false

        if (isExisting) {
            _pinStage.value = PinStage.LOGIN
        } else {
            _pinStage.value = PinStage.SET
            validatePasswordInputs()
        }
        _currentAuthStep.value = AuthStep.PASSWORD
    }

    fun onPasswordChange(text: String) {
        _passwordInput.value = text
        validatePasswordInputs()
    }

    fun onConfirmPasswordChange(text: String) {
        _confirmPasswordInput.value = text
        validatePasswordInputs()
    }

    private fun validatePasswordInputs() {
        val p1 = _passwordInput.value
        val p2 = _confirmPasswordInput.value

        _passwordRules.value = PasswordRules(
            hasValidLength = p1.length in 10..50,
            hasLetter = p1.any { it.isLetter() },
            hasDigit = p1.any { it.isDigit() },
            matchesConfirm = p1.isNotEmpty() && p1 == p2
        )
    }

    fun proceedToPin() {
        val guardian = _selectedGuardian.value ?: return
        val isExisting = existingAccounts.value.any { it.animal == guardian }

        if (isExisting) {
            val pw = _passwordInput.value
            if (pw.isEmpty()) {
                setAuthMsg("Please enter your vault password.", true)
                return
            }
            viewModelScope.launch {
                setAuthMsg("Checking password…", false)
                val result = repository.verifyPasswordAndDeriveKek(guardian, pw)
                result.onSuccess { kek ->
                    sessionKek = kek
                    pendingPassword = pw
                    _pinStage.value = PinStage.LOGIN
                    _pinBuffer.value = ""
                    _authMessage.value = null
                    _currentAuthStep.value = AuthStep.PIN
                }.onFailure { err ->
                    setAuthMsg(err.message ?: "Incorrect password. The box stays chained.", true)
                }
            }
        } else {
            if (!_passwordRules.value.isValid) {
                setAuthMsg("Password must meet all 4 security criteria.", true)
                return
            }
            pendingPassword = _passwordInput.value
            _pinStage.value = PinStage.SET
            _pinBuffer.value = ""
            _authMessage.value = null
            _currentAuthStep.value = AuthStep.PIN
        }
    }

    fun stepBack() {
        when (_currentAuthStep.value) {
            AuthStep.PASSWORD -> {
                _currentAuthStep.value = AuthStep.GUARDIAN
                _authMessage.value = null
            }
            AuthStep.PIN -> {
                _pinBuffer.value = ""
                _currentAuthStep.value = AuthStep.PASSWORD
                _authMessage.value = null
            }
            AuthStep.GUARDIAN -> {}
        }
    }

    fun appendPinDigit(digit: String) {
        if (_pinBuffer.value.length < 4) {
            val next = _pinBuffer.value + digit
            _pinBuffer.value = next
            if (next.length == 4) {
                viewModelScope.launch {
                    delay(180)
                    submitPin()
                }
            }
        }
    }

    fun deletePinDigit() {
        if (_pinBuffer.value.isNotEmpty()) {
            _pinBuffer.value = _pinBuffer.value.dropLast(1)
        }
    }

    fun submitPin() {
        val pin = _pinBuffer.value
        if (pin.length != 4) {
            setAuthMsg("Please enter all four digits.", true)
            return
        }
        _pinBuffer.value = ""

        when (_pinStage.value) {
            PinStage.SET -> {
                pendingPin = pin
                _pinStage.value = PinStage.CONFIRM
                setAuthMsg("Confirm your 4-digit PIN.", false)
            }
            PinStage.CONFIRM -> {
                if (pin != pendingPin) {
                    setAuthMsg("PINs do not match — start again.", true)
                    _pinStage.value = PinStage.SET
                    pendingPin = null
                    return
                }
                // Forge and create vault
                viewModelScope.launch {
                    setAuthMsg("Forging your Private Security Box…", false)
                    val guardian = _selectedGuardian.value ?: return@launch
                    val pw = pendingPassword ?: return@launch
                    try {
                        val dataKey = repository.createVault(guardian, pw, pin)
                        sessionDataKey = dataKey
                        showToast("Box created. Unlocking…")
                        triggerUnlockAnimation()
                    } catch (e: Exception) {
                        setAuthMsg("Could not create box: ${e.message}", true)
                    }
                }
            }
            PinStage.LOGIN -> {
                viewModelScope.launch {
                    val guardian = _selectedGuardian.value ?: return@launch
                    val kek = sessionKek ?: return@launch
                    setAuthMsg("Opening padlock…", false)
                    val result = repository.verifyPinAndUnwrapKey(guardian, kek, pin)
                    result.onSuccess { dataKey ->
                        sessionDataKey = dataKey
                        triggerUnlockAnimation()
                    }.onFailure { err ->
                        setAuthMsg(err.message ?: "Wrong PIN. The padlock stays shut.", true)
                    }
                }
            }
        }
    }

    private fun triggerUnlockAnimation() {
        viewModelScope.launch {
            _isPadlockOpen.value = true
            setAuthMsg("Padlock open…", false)
            delay(500)
            _isChainUnwound.value = true
            setAuthMsg("Chain unwinding…", false)
            delay(800)
            _isUnlocked.value = true
            _currentFolderId.value = "root"
            _breadcrumbs.value = listOf(BreadcrumbItem("root", "🟠 PSB"))
            _selectedNodeIds.value = emptySet()
            _clipboard.value = null
            refreshStorageUsage()
            resetActivityTimer()
        }
    }

    fun lockVault() {
        sessionKek = null
        sessionDataKey = null
        pendingPassword = null
        pendingPin = null

        _isUnlocked.value = false
        _isPadlockOpen.value = false
        _isChainUnwound.value = false
        _currentAuthStep.value = AuthStep.GUARDIAN
        _selectedGuardian.value = null
        _passwordInput.value = ""
        _confirmPasswordInput.value = ""
        _pinBuffer.value = ""
        _selectedNodeIds.value = emptySet()
        _clipboard.value = null
        _previewMedia.value = null
        autoLockJob?.cancel()

        setAuthMsg("Box locked. Enter your password to open it again.", false)
    }

    fun resetActivityTimer() {
        autoLockJob?.cancel()
        _autoLockSecondsRemaining.value = 300
        autoLockJob = viewModelScope.launch {
            while (_autoLockSecondsRemaining.value > 0) {
                delay(1000)
                _autoLockSecondsRemaining.value -= 1
            }
            if (_isUnlocked.value) {
                lockVault()
                showToast("Auto-locked after 5 minutes of inactivity.")
            }
        }
    }

    private fun setAuthMsg(msg: String, isErr: Boolean) {
        _authMessage.value = msg
        _isAuthError.value = isErr
    }

    fun showToast(msg: String) {
        _activeToast.value = msg
        viewModelScope.launch {
            delay(2800)
            if (_activeToast.value == msg) {
                _activeToast.value = null
            }
        }
    }

    // Vault Interior Navigation & Operations
    fun navigateToFolder(folder: VaultNodeEntity) {
        resetActivityTimer()
        if (folder.type != "folder") return
        _currentFolderId.value = folder.id
        _breadcrumbs.value = _breadcrumbs.value + BreadcrumbItem(folder.id, folder.name)
        _selectedNodeIds.value = emptySet()
    }

    fun navigateToBreadcrumb(targetId: String) {
        resetActivityTimer()
        val index = _breadcrumbs.value.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            _breadcrumbs.value = _breadcrumbs.value.take(index + 1)
            _currentFolderId.value = targetId
            _selectedNodeIds.value = emptySet()
        }
    }

    fun toggleNodeSelection(nodeId: String, isMulti: Boolean = false) {
        resetActivityTimer()
        if (isMulti) {
            val current = _selectedNodeIds.value.toMutableSet()
            if (current.contains(nodeId)) current.remove(nodeId)
            else current.add(nodeId)
            _selectedNodeIds.value = current
        } else {
            if (_selectedNodeIds.value.contains(nodeId) && _selectedNodeIds.value.size == 1) {
                _selectedNodeIds.value = emptySet()
            } else {
                _selectedNodeIds.value = setOf(nodeId)
            }
        }
    }

    fun clearSelection() {
        resetActivityTimer()
        _selectedNodeIds.value = emptySet()
    }

    fun createFolder(name: String) {
        resetActivityTimer()
        val guardian = _selectedGuardian.value ?: return
        viewModelScope.launch {
            repository.createFolder(guardian, _currentFolderId.value, name.trim())
            showToast("Folder “$name” created.")
        }
    }

    fun saveLink(name: String, url: String) {
        resetActivityTimer()
        val guardian = _selectedGuardian.value ?: return
        viewModelScope.launch {
            repository.saveLink(guardian, _currentFolderId.value, name.trim(), url.trim())
            showToast("Link stored inside the box.")
        }
    }

    fun saveNote(title: String, content: String) {
        resetActivityTimer()
        val guardian = _selectedGuardian.value ?: return
        val key = sessionDataKey ?: return
        viewModelScope.launch {
            repository.saveEncryptedNote(guardian, _currentFolderId.value, title.trim(), content, key)
            refreshStorageUsage()
            showToast("Secure note saved.")
        }
    }

    fun updateNote(node: VaultNodeEntity, newTitle: String, newContent: String) {
        resetActivityTimer()
        val key = sessionDataKey ?: return
        viewModelScope.launch {
            repository.updateEncryptedNote(node, newTitle.trim(), newContent, key)
            refreshStorageUsage()
            showToast("Note updated.")
        }
    }

    fun importFileFromUri(uri: Uri, displayName: String?, mimeType: String?) {
        resetActivityTimer()
        val guardian = _selectedGuardian.value ?: return
        val key = sessionDataKey ?: return

        viewModelScope.launch {
            try {
                val stream = getApplication<Application>().contentResolver.openInputStream(uri)
                val bytes = stream?.use { it.readBytes() } ?: return@launch
                val name = displayName ?: ("file_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()))
                val mime = mimeType ?: "application/octet-stream"

                val type = when {
                    mime.startsWith("image") -> "photo"
                    mime.startsWith("audio") -> "recording"
                    mime.startsWith("video") -> "video"
                    else -> "file"
                }

                repository.saveEncryptedFile(
                    owner = guardian,
                    parentId = _currentFolderId.value,
                    name = name,
                    mime = mime,
                    bytes = bytes,
                    dataKey = key,
                    type = type
                )
                refreshStorageUsage()
                showToast("“$name” encrypted and saved in box.")
            } catch (e: Exception) {
                showToast("Import error: ${e.message}")
            }
        }
    }

    fun saveCapturedPhoto(bitmap: Bitmap) {
        resetActivityTimer()
        val guardian = _selectedGuardian.value ?: return
        val key = sessionDataKey ?: return

        viewModelScope.launch {
            try {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
                val bytes = baos.toByteArray()
                val name = "Photo_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"

                repository.saveEncryptedFile(
                    owner = guardian,
                    parentId = _currentFolderId.value,
                    name = name,
                    mime = "image/jpeg",
                    bytes = bytes,
                    dataKey = key,
                    type = "photo"
                )
                refreshStorageUsage()
                showToast("Photo captured & encrypted inside box.")
            } catch (e: Exception) {
                showToast("Camera save error: ${e.message}")
            }
        }
    }

    fun saveRecordedAudio(tempAudioFile: File, durationMs: Long) {
        resetActivityTimer()
        val guardian = _selectedGuardian.value ?: return
        val key = sessionDataKey ?: return

        viewModelScope.launch {
            try {
                val bytes = tempAudioFile.readBytes()
                tempAudioFile.delete()
                val name = "VoiceRecord_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".m4a"

                repository.saveEncryptedFile(
                    owner = guardian,
                    parentId = _currentFolderId.value,
                    name = name,
                    mime = "audio/mp4",
                    bytes = bytes,
                    dataKey = key,
                    type = "recording"
                )
                refreshStorageUsage()
                showToast("Audio recording encrypted into box.")
            } catch (e: Exception) {
                showToast("Recorder save error: ${e.message}")
            }
        }
    }

    fun copySelected() {
        resetActivityTimer()
        val sel = _selectedNodeIds.value.toList()
        if (sel.isEmpty()) {
            showToast("Select items first.")
            return
        }
        _clipboard.value = ClipboardState(ClipboardMode.COPY, sel)
        showToast("Copied ${sel.size} item(s).")
    }

    fun cutSelected() {
        resetActivityTimer()
        val sel = _selectedNodeIds.value.toList()
        if (sel.isEmpty()) {
            showToast("Select items first.")
            return
        }
        _clipboard.value = ClipboardState(ClipboardMode.CUT, sel)
        showToast("Cut ${sel.size} item(s).")
    }

    fun pasteClipboard() {
        resetActivityTimer()
        val clip = _clipboard.value ?: run {
            showToast("Clipboard is empty.")
            return
        }
        val targetFolder = _currentFolderId.value

        viewModelScope.launch {
            try {
                for (nodeId in clip.nodeIds) {
                    if (clip.mode == ClipboardMode.COPY) {
                        repository.copyNodeTree(nodeId, targetFolder)
                    } else {
                        repository.moveNode(nodeId, targetFolder)
                    }
                }
                if (clip.mode == ClipboardMode.CUT) {
                    _clipboard.value = null
                }
                clearSelection()
                refreshStorageUsage()
                showToast("Pasted into this compartment.")
            } catch (e: Exception) {
                showToast("Paste failed: ${e.message}")
            }
        }
    }

    fun renameNode(nodeId: String, newName: String) {
        resetActivityTimer()
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameNode(nodeId, newName.trim())
            clearSelection()
            showToast("Renamed.")
        }
    }

    fun deleteSelectedNodes() {
        resetActivityTimer()
        val sel = _selectedNodeIds.value.toList()
        if (sel.isEmpty()) return

        viewModelScope.launch {
            for (id in sel) {
                repository.deleteNodeAndChildren(id)
            }
            clearSelection()
            refreshStorageUsage()
            showToast("Deleted ${sel.size} item(s).")
        }
    }

    fun openNode(node: VaultNodeEntity) {
        resetActivityTimer()
        if (node.type == "folder") {
            navigateToFolder(node)
            return
        }
        val key = sessionDataKey ?: return
        viewModelScope.launch {
            try {
                val decrypted = repository.decryptNode(node, key)
                when (node.type) {
                    "photo" -> {
                        val bitmap = BitmapFactory.decodeByteArray(decrypted, 0, decrypted.size)
                        _previewMedia.value = PreviewMedia(node, imageBitmap = bitmap)
                    }
                    "recording" -> {
                        val tempAudio = File(getApplication<Application>().cacheDir, "preview_${node.id}.m4a")
                        tempAudio.writeBytes(decrypted)
                        _previewMedia.value = PreviewMedia(node, audioFile = tempAudio)
                    }
                    "note" -> {
                        val text = String(decrypted, Charsets.UTF_8)
                        _previewMedia.value = PreviewMedia(node, textContent = text)
                    }
                    else -> {
                        if (node.mime.startsWith("image")) {
                            val bitmap = BitmapFactory.decodeByteArray(decrypted, 0, decrypted.size)
                            _previewMedia.value = PreviewMedia(node, imageBitmap = bitmap)
                        } else if (node.mime.startsWith("audio")) {
                            val tempAudio = File(getApplication<Application>().cacheDir, "preview_${node.id}.m4a")
                            tempAudio.writeBytes(decrypted)
                            _previewMedia.value = PreviewMedia(node, audioFile = tempAudio)
                        } else {
                            val text = String(decrypted, Charsets.UTF_8)
                            _previewMedia.value = PreviewMedia(node, textContent = text)
                        }
                    }
                }
            } catch (e: Exception) {
                showToast("Could not decrypt: ${e.message}")
            }
        }
    }

    fun dismissPreview() {
        _previewMedia.value?.audioFile?.delete()
        _previewMedia.value = null
    }

    fun changePasswordAndPin(newPass: String, newPin: String) {
        resetActivityTimer()
        val guardian = _selectedGuardian.value ?: return
        val kek = sessionKek ?: return
        val dataKey = sessionDataKey ?: return

        viewModelScope.launch {
            val result = repository.changePasswordAndPin(guardian, kek, dataKey, newPass, newPin)
            result.onSuccess { newKek ->
                sessionKek = newKek
                showToast("Password and PIN updated successfully.")
            }.onFailure { err ->
                showToast("Error updating credentials: ${err.message}")
            }
        }
    }

    private fun refreshStorageUsage() {
        val guardian = _selectedGuardian.value ?: return
        viewModelScope.launch {
            _totalStorageBytes.value = repository.getTotalStorageBytes(guardian)
        }
    }
}
