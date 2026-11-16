package com.senghol.knotx.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File
import android.util.Log
import androidx.compose.material3.Surface
import com.senghol.knotx.data.database.SandeshDao
import com.senghol.knotx.utils.SandeshImporter
import java.util.regex.Pattern

@Composable
fun WaImportScreen(
    context: Context,
    sandeshDao: SandeshDao,
    zipUri: Uri,
    userNumber: String,       // conversation partner's number
    onImportComplete: () -> Unit
) {

    // ─────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────
    var phase by remember { mutableStateOf<ImportPhase>(ImportPhase.Loading) }
    var importProgress by remember { mutableIntStateOf(0) }
    // Phase.NamPick state
    var senderNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var selfIndex by remember { mutableIntStateOf(0) } // which index user selected as self

    val scope = rememberCoroutineScope()

    // Block back button during processing
    val backHandlerEnabled by remember {
        derivedStateOf { phase is ImportPhase.Processing }
    }
    BackHandler(enabled = backHandlerEnabled) { /* blocked — do nothing */ }
    var cacheExtractFile by remember { mutableStateOf<File?>(null) }
    // ─────────────────────────────────────────────
    // On launch: copy zip to cache, read txt, extract names
    // ─────────────────────────────────────────────
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val tempZip = File(context.cacheDir, "import_chat.zip")
                context.contentResolver.openInputStream(zipUri)?.use { input ->
                    tempZip.outputStream().use { input.copyTo(it) }
                }

                val cacheExtract = File(context.cacheDir, "wa_name_read")
                cacheExtract.mkdirs()
                cacheExtractFile = cacheExtract
                ZipFile(tempZip.absolutePath).extractAll(cacheExtract.absolutePath)

                val txtFile = cacheExtract.listFiles()
                    ?.firstOrNull { it.extension == "txt" }

                if (txtFile == null) {
                    Log.e("WaImport", "ERROR: No txt file found in zip")
                    phase = ImportPhase.Error("No chat text file found in zip.")
                    cacheExtract.deleteRecursively()
                    return@withContext
                }

                val names = extractSenderNames(txtFile)
//                cacheExtract.deleteRecursively()

                if (names.size < 2) {
                    Log.e("WaImport", "ERROR: Less than 2 senders found: $names")
                    phase = ImportPhase.Error("Could not find sender names in chat file.")
                    return@withContext
                }

                withContext(Dispatchers.Main) {
                    senderNames = names.take(2) // single chat = exactly 2 senders
                    phase = ImportPhase.NamePick
                }

            } catch (e: Exception) {
                Log.e("WaImport", "EXCEPTION in LaunchedEffect: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    phase = ImportPhase.Error(e.message ?: "Unknown error")
                }
            }
        }
    }


    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
    when (val currentPhase = phase) {

        // ── Loading: reading zip ──────────────────
        ImportPhase.Loading -> {
            FullScreenLoader(message = "Reading chat file…")
        }

        // ── Name pick ────────────────────────────
        ImportPhase.NamePick -> {
            NamePickContent(
                senderNames = senderNames,
                selfIndex = selfIndex,
                onSelfIndexChanged = { selfIndex = it },
                onConfirm = {
//                    val dir = cacheExtractFile ?: return@NamePickContent // safety check
                    val dir = cacheExtractFile
                    if (dir != null) {
                        val selfName = senderNames[selfIndex]
                        val partnerName = senderNames[1 - selfIndex]
                        phase = ImportPhase.Processing

                        scope.launch(Dispatchers.IO) {
                            try {
                                val tempZip = File(context.cacheDir, "import_chat.zip")

                                SandeshImporter(
                                    context = context,
                                    sandeshDao = sandeshDao,
                                    userNumber = userNumber,
                                    extractedDir = dir, // already extracted in screen
                                    partnerName = partnerName,
                                    showInGallery = true,
                                    onProgress = { progress ->
                                        importProgress = progress
                                    }
                                ).import()
                                dir.deleteRecursively() // ✅ cleanup here
                                tempZip.delete()
                                withContext(Dispatchers.Main) {
                                    onImportComplete()
                                }

                            } catch (e: Exception) {
                                Log.e("WaImport", "EXCEPTION during import: ${e.message}", e)
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    phase = ImportPhase.Error(e.message ?: "Import failed")
                                }
                            }
                        }
                    }
                }
            )
        }

        is ImportPhase.Processing -> {
            FullScreenLoader(
                message = "Importing your chat…\nPlease wait, do not close the app.",
                progress = importProgress
            )
        }

        // ── Error ────────────────────────────────
        is ImportPhase.Error -> {
            ErrorContent(
                message = currentPhase.message,
                onDismiss = onImportComplete
            )
        }
    }
}
}

// ─────────────────────────────────────────────────────
// Phase sealed class
// ─────────────────────────────────────────────────────
private sealed class ImportPhase {
    object Loading : ImportPhase()
    object NamePick : ImportPhase()
    object Processing : ImportPhase()
    data class Error(val message: String) : ImportPhase()
}

// ─────────────────────────────────────────────────────
// Name Pick UI
// ─────────────────────────────────────────────────────
@Composable
private fun NamePickContent(
    senderNames: List<String>,
    selfIndex: Int,
    onSelfIndexChanged: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Who are you?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "We found 2 people in this chat. Tap to select which one is you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Name cards — tap to toggle self/partner
        senderNames.forEachIndexed { index, name ->
            val isSelf = index == selfIndex
            val isPartner = !isSelf

            SenderCard(
                name = name,
                label = if (isSelf) "This is me" else "Chatting with",
                isSelf = isSelf,
                onClick = {
                    // tapping either card reassigns self to that index
                    onSelfIndexChanged(index)
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ⚠️ Warning
        WarningBox()

        Spacer(modifier = Modifier.height(8.dp))

        // Confirm button
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Start Import",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────
// Single sender card
// ─────────────────────────────────────────────────────
@Composable
private fun SenderCard(
    name: String,
    label: String,
    isSelf: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelf)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    val bgColor = if (isSelf)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    val labelColor = if (isSelf)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelf) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = labelColor
                )
            }

            if (isSelf) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Warning box
// ─────────────────────────────────────────────────────
@Composable
private fun WarningBox() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Be careful — this cannot be undone",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Once import starts, all messages will be saved to this conversation permanently. Media files not present in the zip will be skipped.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// ─────────────────────────────────────────────────────
// Full screen loader (Loading + Processing phases)
// ─────────────────────────────────────────────────────
@Composable
private fun FullScreenLoader(message: String, progress: Int = -1) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (progress < 0) {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 4.dp
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
// ─────────────────────────────────────────────────────
// Error content
// ─────────────────────────────────────────────────────
@Composable
private fun ErrorContent(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Import Failed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Helper: extract unique sender names from txt file
// Skips system messages, returns ordered list of senders
// ─────────────────────────────────────────────────────
private fun extractSenderNames(txtFile: File): List<String> {
    val linePattern = Pattern.compile(
        """.* - ([^:]+):.*"""
    )

    val names = linkedSetOf<String>()

    txtFile.bufferedReader().forEachLine { line ->
        val matcher = linePattern.matcher(line)
        if (matcher.matches()) {
            val sender = matcher.group(1)?.trim() ?: return@forEachLine

            if (
                sender.isNotBlank() &&
                sender.length < 40 &&
                !sender.contains("Messages and calls are end-to-end encrypted")
            ) {
                names.add(sender)
            }
        }
    }

    return names.toList()
}