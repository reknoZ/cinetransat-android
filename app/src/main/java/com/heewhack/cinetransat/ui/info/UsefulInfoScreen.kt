package com.heewhack.cinetransat.ui.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Tram
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsefulInfoScreen(modifier: Modifier = Modifier) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Infos pratiques") })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            items(cinetransatInfosSections, key = { it.id }) { section ->
                val isOpen = expanded[section.id] == true
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                expanded[section.id] = !isOpen
                            },
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = iconForSection(section.icon),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        }
                        Icon(
                            imageVector = if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isOpen) "Réduire" else "Développer",
                        )
                    }
                    AnimatedVisibility(
                        visible = isOpen,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
                        Text(
                            text = section.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, bottom = 16.dp),
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                }
            }
        }
    }
}

private fun iconForSection(icon: String): ImageVector =
    when (icon) {
        "gift" -> Icons.Filled.CardGiftcard
        "calendar" -> Icons.Filled.CalendarMonth
        "age" -> Icons.Filled.Person
        "language" -> Icons.Filled.Translate
        "drinks" -> Icons.Filled.LocalBar
        "deckchair" -> Icons.Filled.EventSeat
        "transport" -> Icons.Filled.Tram
        "weather" -> Icons.Filled.Thunderstorm
        "toilet" -> Icons.Filled.Wc
        "smoke" -> Icons.Filled.SmokingRooms
        "trash" -> Icons.Filled.Delete
        "dog" -> Icons.Filled.Pets
        "bike" -> Icons.AutoMirrored.Filled.DirectionsBike
        "accessibility" -> Icons.Filled.Accessibility
        else -> Icons.Filled.Translate
    }
