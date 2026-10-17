package com.example.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PsbBorder
import com.example.ui.theme.PsbError
import com.example.ui.theme.PsbOrange
import com.example.ui.theme.PsbOrangeDark
import com.example.ui.theme.PsbOrangeLight
import com.example.ui.theme.PsbSteel
import com.example.ui.theme.PsbSteelDark
import com.example.ui.theme.PsbSteelLight
import com.example.ui.theme.PsbSteelMuted
import com.example.ui.theme.PsbSuccess
import com.example.viewmodel.AuthStep
import com.example.viewmodel.PinStage
import com.example.viewmodel.VaultViewModel

@Composable
fun LockScreen(viewModel: VaultViewModel) {
    val authStep by viewModel.currentAuthStep.collectAsState()
    val selectedGuardian by viewModel.selectedGuardian.collectAsState()
    val existingAccounts by viewModel.existingAccounts.collectAsState()
    val passwordInput by viewModel.passwordInput.collectAsState()
    val confirmPasswordInput by viewModel.confirmPasswordInput.collectAsState()
    val passwordRules by viewModel.passwordRules.collectAsState()
    val pinBuffer by viewModel.pinBuffer.collectAsState()
    val pinStage by viewModel.pinStage.collectAsState()
    val authMessage by viewModel.authMessage.collectAsState()
    val isAuthError by viewModel.isAuthError.collectAsState()

    val isPadlockOpen by viewModel.isPadlockOpen.collectAsState()
    val isChainUnwound by viewModel.isChainUnwound.collectAsState()

    val isExistingAccount = selectedGuardian != null && existingAccounts.any { it.animal == selectedGuardian }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF22222C), Color(0xFF0A0A0E)),
                    radius = 1400f
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VaultBox3D(
                    isPadlockOpen = isPadlockOpen,
                    isChainUnwound = isChainUnwound,
                    modifier = Modifier.padding(end = 40.dp)
                )

                AuthCard(
                    modifier = Modifier.width(420.dp),
                    authStep = authStep,
                    selectedGuardian = selectedGuardian,
                    isExistingAccount = isExistingAccount,
                    allGuardians = viewModel.allGuardians,
                    existingAnimals = existingAccounts.map { it.animal }.toSet(),
                    passwordInput = passwordInput,
                    confirmPasswordInput = confirmPasswordInput,
                    passwordRules = passwordRules,
                    pinBuffer = pinBuffer,
                    pinStage = pinStage,
                    authMessage = authMessage,
                    isAuthError = isAuthError,
                    onSelectGuardian = { viewModel.selectGuardian(it) },
                    onProceedToPassword = { viewModel.proceedToPassword() },
                    onPasswordChange = { viewModel.onPasswordChange(it) },
                    onConfirmPasswordChange = { viewModel.onConfirmPasswordChange(it) },
                    onProceedToPin = { viewModel.proceedToPin() },
                    onStepBack = { viewModel.stepBack() },
                    onPinDigit = { viewModel.appendPinDigit(it) },
                    onPinDelete = { viewModel.deletePinDigit() },
                    onPinSubmit = { viewModel.submitPin() }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                VaultBox3D(
                    isPadlockOpen = isPadlockOpen,
                    isChainUnwound = isChainUnwound,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                AuthCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    authStep = authStep,
                    selectedGuardian = selectedGuardian,
                    isExistingAccount = isExistingAccount,
                    allGuardians = viewModel.allGuardians,
                    existingAnimals = existingAccounts.map { it.animal }.toSet(),
                    passwordInput = passwordInput,
                    confirmPasswordInput = confirmPasswordInput,
                    passwordRules = passwordRules,
                    pinBuffer = pinBuffer,
                    pinStage = pinStage,
                    authMessage = authMessage,
                    isAuthError = isAuthError,
                    onSelectGuardian = { viewModel.selectGuardian(it) },
                    onProceedToPassword = { viewModel.proceedToPassword() },
                    onPasswordChange = { viewModel.onPasswordChange(it) },
                    onConfirmPasswordChange = { viewModel.onConfirmPasswordChange(it) },
                    onProceedToPin = { viewModel.proceedToPin() },
                    onStepBack = { viewModel.stepBack() },
                    onPinDigit = { viewModel.appendPinDigit(it) },
                    onPinDelete = { viewModel.deletePinDigit() },
                    onPinSubmit = { viewModel.submitPin() }
                )
            }
        }
    }
}

@Composable
fun AuthCard(
    modifier: Modifier = Modifier,
    authStep: AuthStep,
    selectedGuardian: String?,
    isExistingAccount: Boolean,
    allGuardians: List<String>,
    existingAnimals: Set<String>,
    passwordInput: String,
    confirmPasswordInput: String,
    passwordRules: com.example.viewmodel.PasswordRules,
    pinBuffer: String,
    pinStage: PinStage,
    authMessage: String?,
    isAuthError: Boolean,
    onSelectGuardian: (String) -> Unit,
    onProceedToPassword: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onProceedToPin: () -> Unit,
    onStepBack: () -> Unit,
    onPinDigit: (String) -> Unit,
    onPinDelete: () -> Unit,
    onPinSubmit: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(24.dp, RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF2A2A34), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141B))
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            // Brand header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🟠 PSB",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PsbOrangeLight
                )
                Text(
                    text = "PRIVATE SECURITY BOX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = PsbSteelMuted
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (authStep) {
                AuthStep.GUARDIAN -> {
                    GuardianStep(
                        guardians = allGuardians,
                        existingAnimals = existingAnimals,
                        selectedGuardian = selectedGuardian,
                        onSelect = onSelectGuardian,
                        onContinue = onProceedToPassword
                    )
                }
                AuthStep.PASSWORD -> {
                    PasswordStep(
                        isExisting = isExistingAccount,
                        passwordInput = passwordInput,
                        confirmPasswordInput = confirmPasswordInput,
                        rules = passwordRules,
                        onPasswordChange = onPasswordChange,
                        onConfirmPasswordChange = onConfirmPasswordChange,
                        onBack = onStepBack,
                        onContinue = onProceedToPin
                    )
                }
                AuthStep.PIN -> {
                    PinStep(
                        pinStage = pinStage,
                        pinBuffer = pinBuffer,
                        onDigit = onPinDigit,
                        onDelete = onPinDelete,
                        onSubmit = onPinSubmit,
                        onBack = onStepBack
                    )
                }
            }

            // Message bar
            AnimatedVisibility(visible = authMessage != null, enter = fadeIn(), exit = fadeOut()) {
                authMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = if (isAuthError) PsbError else PsbSuccess,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GuardianStep(
    guardians: List<String>,
    existingAnimals: Set<String>,
    selectedGuardian: String?,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit
) {
    Text(
        text = "1 · Choose your guardian",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = PsbOrangeLight
    )
    Text(
        text = "Pick the wild animal that guards your box. Your folders, files and settings live behind it.",
        fontSize = 12.5.sp,
        color = PsbSteelMuted,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(guardians) { animal ->
            val isSelected = animal == selectedGuardian
            val hasVault = existingAnimals.contains(animal)

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Color(0x28FF8F2E) else Color(0xFF1D1D25)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PsbOrange else Color(0xFF2C2C37),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(animal) }
                    .testTag("animal_${animal}"),
                contentAlignment = Alignment.Center
            ) {
                Text(text = animal, fontSize = 22.sp)

                // Dot if vault already created
                if (hasVault) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PsbOrange)
                    )
                }
            }
        }
    }

    Button(
        onClick = onContinue,
        enabled = selectedGuardian != null,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("guardian_continue_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = PsbOrange,
            contentColor = Color(0xFF1A0D00)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun PasswordStep(
    isExisting: Boolean,
    passwordInput: String,
    confirmPasswordInput: String,
    rules: com.example.viewmodel.PasswordRules,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Text(
        text = if (isExisting) "2 · Enter your password" else "2 · Create your password",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = PsbOrangeLight
    )
    Text(
        text = if (isExisting)
            "Your set password is required every time the box is used."
        else
            "10–50 characters, must contain letters and digits.",
        fontSize = 12.5.sp,
        color = PsbSteelMuted,
        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
    )

    OutlinedTextField(
        value = passwordInput,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle password visibility",
                    tint = PsbSteelMuted
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PsbOrange,
            unfocusedBorderColor = Color(0xFF2C2C37),
            focusedContainerColor = Color(0xFF0D0D12),
            unfocusedContainerColor = Color(0xFF0D0D12)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("password_input")
    )

    if (!isExisting) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = confirmPasswordInput,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PsbOrange,
                unfocusedBorderColor = Color(0xFF2C2C37),
                focusedContainerColor = Color(0xFF0D0D12),
                unfocusedContainerColor = Color(0xFF0D0D12)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_password_input")
        )

        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            RuleRow(label = "10–50 characters", isMet = rules.hasValidLength)
            RuleRow(label = "Contains at least one letter", isMet = rules.hasLetter)
            RuleRow(label = "Contains at least one digit", isMet = rules.hasDigit)
            RuleRow(label = "Both entries match", isMet = rules.matchesConfirm)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbSteelLight),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(PsbBorder, PsbBorder)))
        ) {
            Text("Back")
        }

        Button(
            onClick = onContinue,
            enabled = if (isExisting) passwordInput.isNotEmpty() else rules.isValid,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("password_continue_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = PsbOrange,
                contentColor = Color(0xFF1A0D00)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RuleRow(label: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) PsbSuccess else PsbSteelDark,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isMet) PsbSuccess else PsbSteelMuted
        )
    }
}

@Composable
private fun PinStep(
    pinStage: PinStage,
    pinBuffer: String,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    val title = when (pinStage) {
        PinStage.SET -> "3 · Set your 4-digit PIN"
        PinStage.CONFIRM -> "3 · Confirm your 4-digit PIN"
        PinStage.LOGIN -> "3 · Enter your 4-digit PIN"
    }

    val hint = when (pinStage) {
        PinStage.SET -> "The four digits are your “okay / enter” — the box opens the moment the 4th digit lands."
        PinStage.CONFIRM -> "Enter the same four digits again."
        PinStage.LOGIN -> "Press the digits — the 4th digit opens the box."
    }

    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = PsbOrangeLight
    )
    Text(
        text = hint,
        fontSize = 12.5.sp,
        color = PsbSteelMuted,
        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
    )

    // Animated PIN Dots
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 4) {
            val isFilled = i < pinBuffer.length
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isFilled) PsbOrange else Color.Transparent)
                    .border(
                        2.dp,
                        if (isFilled) PsbOrange else Color(0xFF3A3A46),
                        CircleShape
                    )
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Keypad 3x4
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("⌫", "0", "OK")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        for (row in keys) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (key in row) {
                    val isOk = key == "OK"
                    val isDel = key == "⌫"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isOk) PsbOrange else Color(0xFF1C1C24)
                            )
                            .border(
                                1.dp,
                                if (isOk) PsbOrangeLight else Color(0xFF2C2C37),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                when (key) {
                                    "⌫" -> onDelete()
                                    "OK" -> onSubmit()
                                    else -> onDigit(key)
                                }
                            }
                            .testTag("keypad_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDel) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Delete digit",
                                tint = PsbSteelLight,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (isOk) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Submit PIN",
                                tint = Color(0xFF1A0D00),
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PsbSteelLight
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
        onClick = onBack,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PsbSteelMuted),
        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(PsbBorder, PsbBorder)))
    ) {
        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Back")
    }
}
