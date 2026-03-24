package com.maomaochongapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val DefaultTargetSubdirs = listOf(
  "BOOK",
  "DICT",
  "DIYAUDIO",
  "DIYREC",
  "MP3",
  "DIYREC2",
  "REC",
)

@Composable
fun TargetSubdirEditor(
  selected: String,
  enabled: Boolean,
  onChange: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("目标子目录（毛毛虫盘内）", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        value = selected,
        onValueChange = onChange,
        label = { Text("例如 MP3 / BOOK / DIYAUDIO") },
        singleLine = true,
      )
      Button(onClick = { expanded = true }, enabled = enabled) { Text("选择") }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DefaultTargetSubdirs.forEach { dir ->
          DropdownMenuItem(
            text = { Text(dir) },
            onClick = { expanded = false; onChange(dir) },
          )
        }
      }
    }
  }
}
