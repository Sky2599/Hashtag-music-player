package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Song
import com.example.player.EditableAudioTags
import com.example.player.TagEditorManager
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.DeepPurpleSurface
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun TagEditorDialog(
    song: Song,
    onSaveTags: (EditableAudioTags) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialTags = remember(song.id) { TagEditorManager.readTags(context, song) }

    var title by remember { mutableStateOf(initialTags.title) }
    var artist by remember { mutableStateOf(initialTags.artist) }
    var album by remember { mutableStateOf(initialTags.album) }
    var genre by remember { mutableStateOf(initialTags.genre) }
    var year by remember { mutableStateOf(initialTags.year) }
    var trackNum by remember { mutableStateOf(initialTags.trackNumber) }
    var composer by remember { mutableStateOf(initialTags.composer) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPurpleSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = ElectricVioletLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Audio Tag Editor",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TagInputField(label = "Title", value = title, onValueChange = { title = it })
                    TagInputField(label = "Artist", value = artist, onValueChange = { artist = it })
                    TagInputField(label = "Album", value = album, onValueChange = { album = it })
                    TagInputField(label = "Genre", value = genre, onValueChange = { genre = it })
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TagInputField(label = "Year", value = year, onValueChange = { year = it }, modifier = Modifier.weight(1f))
                        TagInputField(label = "Track #", value = trackNum, onValueChange = { trackNum = it }, modifier = Modifier.weight(1f))
                    }
                    TagInputField(label = "Composer", value = composer, onValueChange = { composer = it })
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val edited = EditableAudioTags(
                            title = title.ifBlank { song.title },
                            artist = artist.ifBlank { song.artist },
                            album = album.ifBlank { song.album },
                            albumArtist = artist,
                            genre = genre,
                            year = year,
                            trackNumber = trackNum,
                            discNumber = "1",
                            composer = composer,
                            lyrics = ""
                        )
                        onSaveTags(edited)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Embedded Metadata", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TagInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextTertiary, fontSize = 12.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = ElectricViolet,
            unfocusedBorderColor = DeepPurpleDark,
            focusedContainerColor = DeepPurpleDark,
            unfocusedContainerColor = DeepPurpleDark
        ),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    )
}
