package com.example.ritmo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ritmo.RitmoViewModel
import com.example.ritmo.data.ThemeMode
import com.example.ritmo.ui.components.BackHeader
import com.example.ritmo.util.songsCount

@Composable
fun SettingsScreen(vm: RitmoViewModel, nav: NavController) {
    val theme by vm.themeMode.collectAsStateWithLifecycle()
    val minSec by vm.minDurationSec.collectAsStateWithLifecycle()
    val musicOnly by vm.musicOnly.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "back") { BackHeader(onBack = { nav.popBackStack() }) }
        item(key = "title") {
            Text(
                "Configurações",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        item(key = "theme-title") { GroupTitle("Aparência") }
        items(ThemeMode.entries.toList(), key = { it.name }) { mode ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.setThemeMode(mode) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = theme == mode, onClick = null, modifier = Modifier.padding(12.dp))
                Text(mode.label, style = MaterialTheme.typography.bodyLarge)
            }
        }

        item(key = "library-title") { GroupTitle("Biblioteca") }
        item(key = "music-only") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.setMusicOnly(!musicOnly) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Somente músicas", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Ignora toques, alarmes, gravações e podcasts que o Android não classifica como música.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = musicOnly, onCheckedChange = { vm.setMusicOnly(it) })
            }
        }
        item(key = "min-duration") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Ignorar áudios curtos", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Esconde efeitos sonoros e áudios de mensagens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(0 to "Mostrar todos", 30 to "Menos de 30 s", 60 to "Menos de 1 min").forEach { (seconds, label) ->
                        FilterChip(
                            selected = minSec == seconds,
                            onClick = { vm.setMinDurationSec(seconds) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.onPrimary,
                            ),
                        )
                    }
                }
            }
        }
        item(key = "refresh") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                OutlinedButton(onClick = { vm.refreshLibrary() }) { Text("Atualizar biblioteca") }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${songsCount(songs.size)} no aparelho. A lista também atualiza sozinha quando você adiciona ou apaga arquivos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        item(key = "about-title") { GroupTitle("Sobre") }
        item(key = "about") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text("Ritmo 1.0", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Toca somente as músicas que já estão no seu aparelho. Sem internet, sem conta e sem anúncios.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp),
    )
}
