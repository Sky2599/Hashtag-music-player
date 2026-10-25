package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.db.CustomPlaylistEntity
import com.example.data.engine.SmartPlaylistConfig
import com.example.data.engine.SmartPlaylistEngine
import com.example.data.engine.SmartPlaylistRule
import com.example.data.engine.SmartRuleField
import com.example.data.engine.SmartRuleOperator
import com.example.data.model.Song
import com.example.ui.components.AlbumArtContainer
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun CreatePlaylistDialog(
    initialName: String = "",
    allSongs: List<Song> = emptyList(),
    onConfirm: (String) -> Unit,
    onConfirmSmartPlaylist: ((String, SmartPlaylistConfig) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var isSmartMode by remember { mutableStateOf(false) }
    var matchAll by remember { mutableStateOf(true) } // true = AND, false = OR
    var sortBy by remember { mutableStateOf("MOST_PLAYED") }

    val rules = remember {
        mutableStateListOf(
            SmartPlaylistRule(SmartRuleField.FORMAT, SmartRuleOperator.EQUALS, "FLAC"),
            SmartPlaylistRule(SmartRuleField.SAMPLE_RATE, SmartRuleOperator.GREATER_OR_EQUAL, "96000")
        )
    }

    val liveSmartConfig = SmartPlaylistConfig(
        matchAll = matchAll,
        rules = rules.toList(),
        sortBy = sortBy
    )
    val matchingPreviewCount = remember(allSongs, isSmartMode, matchAll, rules.toList(), sortBy) {
        if (!isSmartMode) 0 else SmartPlaylistEngine.evaluate(allSongs, liveSmartConfig).size
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("create_playlist_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSmartMode) Icons.Default.AutoAwesome else Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            tint = ElectricVioletLight,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSmartMode) "New Smart Playlist (Rule Builder)" else "New Playlist",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Switcher: Static Playlist vs Dynamic Smart Playlist (Rule Builder)
                if (onConfirmSmartPlaylist != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepPurpleDark)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (!isSmartMode) ElectricViolet else DeepPurpleDark)
                                .clickable { isSmartMode = false }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Static Playlist",
                                color = if (!isSmartMode) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSmartMode) ElectricViolet else DeepPurpleDark)
                                .clickable {
                                    isSmartMode = true
                                    if (name.isBlank()) name = "Hi-Res FLAC 96kHz+"
                                }
                                .padding(vertical = 8.dp)
                                .testTag("smart_playlist_mode_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isSmartMode) TextPrimary else ElectricVioletLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Smart Rule Builder",
                                    color = if (isSmartMode) TextPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricVioletLight,
                        unfocusedBorderColor = DeepPurpleDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("playlist_name_input")
                )

                if (isSmartMode) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Preset Templates Row
                    Text(
                        text = "Quick Rule Templates:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmartTemplateChip("FLAC & >= 96kHz") {
                            name = "Hi-Res FLAC 96kHz+"
                            matchAll = true
                            rules.clear()
                            rules.add(SmartPlaylistRule(SmartRuleField.FORMAT, SmartRuleOperator.EQUALS, "FLAC"))
                            rules.add(SmartPlaylistRule(SmartRuleField.SAMPLE_RATE, SmartRuleOperator.GREATER_OR_EQUAL, "96000"))
                        }
                        SmartTemplateChip("Plays > 10 & Last 30d") {
                            name = "Heavy Rotation (30d)"
                            matchAll = true
                            rules.clear()
                            rules.add(SmartPlaylistRule(SmartRuleField.PLAY_COUNT, SmartRuleOperator.GREATER_THAN, "10"))
                            rules.add(SmartPlaylistRule(SmartRuleField.ADDED_WITHIN_DAYS, SmartRuleOperator.LESS_OR_EQUAL, "30"))
                        }
                        SmartTemplateChip("21 Savage & < 4:00") {
                            name = "21 Savage Short Cuts"
                            matchAll = true
                            rules.clear()
                            rules.add(SmartPlaylistRule(SmartRuleField.ARTIST, SmartRuleOperator.CONTAINS, "21 Savage"))
                            rules.add(SmartPlaylistRule(SmartRuleField.DURATION_SECONDS, SmartRuleOperator.LESS_THAN, "4:00"))
                        }
                        SmartTemplateChip("Bitrate >= 320 kbps") {
                            name = "Audiophile 320+ kbps"
                            matchAll = true
                            rules.clear()
                            rules.add(SmartPlaylistRule(SmartRuleField.BITRATE, SmartRuleOperator.GREATER_OR_EQUAL, "320"))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // AND / OR Combinator + Live Match Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Match:", color = TextSecondary, fontSize = 12.sp)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (matchAll) ElectricViolet else DeepPurpleDark,
                                modifier = Modifier.clickable { matchAll = true }
                            ) {
                                Text(
                                    text = "ALL (AND)",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (!matchAll) ElectricViolet else DeepPurpleDark,
                                modifier = Modifier.clickable { matchAll = false }
                            ) {
                                Text(
                                    text = "ANY (OR)",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DeepPurpleDark
                        ) {
                            Text(
                                text = "$matchingPreviewCount matching tracks",
                                color = ElectricVioletLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rules List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(rules.size) { index ->
                            val rule = rules[index]
                            SmartRuleEditorRow(
                                rule = rule,
                                onUpdate = { updated -> rules[index] = updated },
                                onRemove = if (rules.size > 1) {
                                    { rules.removeAt(index) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Add Rule Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepPurpleDark)
                            .clickable {
                                rules.add(
                                    SmartPlaylistRule(
                                        field = SmartRuleField.ARTIST,
                                        operator = SmartRuleOperator.CONTAINS,
                                        value = ""
                                    )
                                )
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = ElectricVioletLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Condition Rule", color = ElectricVioletLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                if (isSmartMode && onConfirmSmartPlaylist != null) {
                                    onConfirmSmartPlaylist(name.trim(), liveSmartConfig)
                                } else {
                                    onConfirm(name.trim())
                                }
                            }
                        },
                        enabled = name.isNotBlank() && (!isSmartMode || rules.any { it.value.isNotBlank() }),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_create_playlist_button")
                    ) {
                        Text(
                            text = if (isSmartMode) "Create Smart Playlist" else "Create",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartTemplateChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DeepPurpleDark,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricVioletLight, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SmartRuleEditorRow(
    rule: SmartPlaylistRule,
    onUpdate: (SmartPlaylistRule) -> Unit,
    onRemove: (() -> Unit)?
) {
    var fieldMenuExpanded by remember { mutableStateOf(false) }
    var opMenuExpanded by remember { mutableStateOf(false) }
    val validOps = remember(rule.field) { SmartPlaylistEngine.availableOperatorsForField(rule.field) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DeepPurpleDark)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Field selector
            Box(modifier = Modifier.weight(1.1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DeepPurpleSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fieldMenuExpanded = true }
                ) {
                    Text(
                        text = rule.field.label,
                        color = ElectricVioletLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    )
                }
                DropdownMenu(
                    expanded = fieldMenuExpanded,
                    onDismissRequest = { fieldMenuExpanded = false },
                    modifier = Modifier.background(DeepPurpleSurface)
                ) {
                    SmartRuleField.values().forEach { candidateField ->
                        DropdownMenuItem(
                            text = { Text(candidateField.label, color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                fieldMenuExpanded = false
                                val defaultOp = SmartPlaylistEngine.availableOperatorsForField(candidateField).first()
                                val defaultVal = when (candidateField) {
                                    SmartRuleField.FORMAT -> "FLAC"
                                    SmartRuleField.SAMPLE_RATE -> "96000"
                                    SmartRuleField.BITRATE -> "320"
                                    SmartRuleField.BIT_DEPTH -> "24"
                                    SmartRuleField.PLAY_COUNT -> "10"
                                    SmartRuleField.ADDED_WITHIN_DAYS -> "30"
                                    SmartRuleField.DURATION_SECONDS -> "4:00"
                                    SmartRuleField.IS_HI_RES, SmartRuleField.IS_FAVORITE -> "true"
                                    else -> ""
                                }
                                onUpdate(SmartPlaylistRule(candidateField, defaultOp, defaultVal))
                            }
                        )
                    }
                }
            }

            // Operator selector
            Box(modifier = Modifier.weight(0.75f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DeepPurpleSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { opMenuExpanded = true }
                ) {
                    Text(
                        text = "${rule.operator.symbol} (${rule.operator.label.substringBefore(" ")})",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                    )
                }
                DropdownMenu(
                    expanded = opMenuExpanded,
                    onDismissRequest = { opMenuExpanded = false },
                    modifier = Modifier.background(DeepPurpleSurface)
                ) {
                    validOps.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op.label, color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                opMenuExpanded = false
                                onUpdate(rule.copy(operator = op))
                            }
                        )
                    }
                }
            }

            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Rule", tint = TextTertiary, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = rule.value,
            onValueChange = { onUpdate(rule.copy(value = it)) },
            placeholder = { Text(rule.field.unitHint, color = TextTertiary, fontSize = 11.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricVioletLight,
                unfocusedBorderColor = DeepPurpleSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    playlists: List<CustomPlaylistEntity>,
    onSelectPlaylist: (CustomPlaylistEntity) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface),
            modifier = Modifier.fillMaxWidth().testTag("add_to_playlist_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add to Playlist",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = song.title,
                            color = ElectricVioletLight,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // New Playlist Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepPurpleDark)
                        .clickable { onCreateNewPlaylist() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = ElectricVioletLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "New Playlist",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No custom playlists yet", color = TextTertiary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(playlists, key = { it.id }, contentType = { "playlist_picker_row" }) { playlist ->
                            val containsSong = playlist.getSongIds().contains(song.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (containsSong) DeepPurpleDark.copy(alpha = 0.5f) else DeepPurpleDark)
                                    .clickable { onSelectPlaylist(playlist) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = if (containsSong) ElectricVioletLight else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlist.getSongIds().size} tracks" + if (containsSong) " • Already added" else "",
                                        color = if (containsSong) ElectricVioletLight else TextTertiary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailDialog(
    playlist: CustomPlaylistEntity,
    allSongs: List<Song>,
    onPlayAll: (List<Song>) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onDeletePlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    val playlistSongs = remember(playlist.songIdsCsv, allSongs) {
        playlist.resolveSongs(allSongs)
    }
    val smartSummary = remember(playlist.songIdsCsv) {
        if (playlist.isSmartPlaylist) {
            SmartPlaylistEngine.formatConfigSummary(playlist.smartRulesJson)
        } else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface),
            modifier = Modifier.fillMaxWidth().testTag("playlist_detail_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlistSongs.size} tracks" + if (playlist.isSmartPlaylist) " • Dynamic Smart Rules" else "",
                            color = ElectricVioletLight,
                            fontSize = 12.sp
                        )
                        if (smartSummary != null) {
                            Text(
                                text = smartSummary,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = onDeletePlaylist, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Playlist", tint = TextSecondary)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons (Play All & Shuffle)
                if (playlistSongs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onPlayAll(playlistSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("play_playlist_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { onShufflePlay(playlistSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepPurpleDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("shuffle_playlist_button")
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = ElectricVioletLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Song List
                if (playlistSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("This playlist is empty", color = TextTertiary, fontSize = 14.sp)
                            Text("Add songs from the Songs tab", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(playlistSongs, key = { it.id }, contentType = { "playlist_song_row" }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DeepPurpleDark)
                                    .clickable { onPlaySong(song, playlistSongs) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtContainer(
                                    albumArtUri = song.albumArtUri,
                                    size = 40.dp,
                                    cornerRadius = 6.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • ${song.durationFormatted}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveSong(song.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove from playlist",
                                        tint = TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
