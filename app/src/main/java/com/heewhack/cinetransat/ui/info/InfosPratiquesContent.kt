package com.heewhack.cinetransat.ui.info

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.heewhack.cinetransat.R

data class InfosSection(
    val id: String,
    val title: String,
    val icon: String,
    val body: String,
)

@Composable
fun cinetransatInfosSections(seasonYear: Int): List<InfosSection> =
    listOf(
        InfosSection(
            id = "gratuit",
            title = stringResource(R.string.info_free_title),
            icon = "gift",
            body = stringResource(R.string.info_free_body),
        ),
        InfosSection(
            id = "horaires",
            title = stringResource(R.string.info_schedule_title),
            icon = "calendar",
            body = stringResource(R.string.info_schedule_body, seasonYear),
        ),
        InfosSection(
            id = "age",
            title = stringResource(R.string.info_age_title),
            icon = "age",
            body = stringResource(R.string.info_age_body),
        ),
        InfosSection(
            id = "langues",
            title = stringResource(R.string.info_languages_title),
            icon = "language",
            body = stringResource(R.string.info_languages_body),
        ),
        InfosSection(
            id = "buvette",
            title = stringResource(R.string.info_bar_title),
            icon = "drinks",
            body = stringResource(R.string.info_bar_body),
        ),
        InfosSection(
            id = "transats",
            title = stringResource(R.string.info_deckchairs_title),
            icon = "deckchair",
            body = stringResource(R.string.info_deckchairs_body),
        ),
        InfosSection(
            id = "acces",
            title = stringResource(R.string.info_transport_title),
            icon = "transport",
            body = stringResource(R.string.info_transport_body),
        ),
        InfosSection(
            id = "annulations",
            title = stringResource(R.string.info_cancellations_title),
            icon = "weather",
            body = stringResource(R.string.info_cancellations_body),
        ),
        InfosSection(
            id = "toilettes",
            title = stringResource(R.string.info_toilets_title),
            icon = "toilet",
            body = stringResource(R.string.info_toilets_body),
        ),
        InfosSection(
            id = "fumee",
            title = stringResource(R.string.info_smoking_title),
            icon = "smoke",
            body = stringResource(R.string.info_smoking_body),
        ),
        InfosSection(
            id = "dechets",
            title = stringResource(R.string.info_waste_title),
            icon = "trash",
            body = stringResource(R.string.info_waste_body),
        ),
        InfosSection(
            id = "chiens",
            title = stringResource(R.string.info_dogs_title),
            icon = "dog",
            body = stringResource(R.string.info_dogs_body),
        ),
        InfosSection(
            id = "velos",
            title = stringResource(R.string.info_bikes_title),
            icon = "bike",
            body = stringResource(R.string.info_bikes_body),
        ),
        InfosSection(
            id = "accessibilite",
            title = stringResource(R.string.info_accessibility_title),
            icon = "accessibility",
            body = stringResource(R.string.info_accessibility_body),
        ),
    )
