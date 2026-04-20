package com.example.cinetransat.ui.info

/**
 * Sections aligned with https://www.cinetransat.ch/infos-pratiques (wording condensed for mobile).
 */
data class InfosSection(
    val id: String,
    val title: String,
    val icon: String,
    val body: String,
)

val cinetransatInfosSections: List<InfosSection> =
    listOf(
        InfosSection(
            id = "gratuit",
            title = "Projections gratuites",
            icon = "gift",
            body =
                "Les projections de CinéTransat ont lieu dans le parc public de la Perle du Lac à Genève, et sont gratuites. " +
                    "Elles sont accessibles à toutes et à tous et sans réservation !",
        ),
        InfosSection(
            id = "horaires",
            title = "Jours et horaires",
            icon = "calendar",
            body =
                "Projections du jeudi au dimanche, du 10 juillet au 17 août 2025.\n\n" +
                    "Début des films à la tombée de la nuit, entre 22h00 (mi-juillet) et 21h15 (fin août).\n\n" +
                    "Buvette, location de transats et animations dès 19h00.\n\n" +
                    "Fin de la soirée vers minuit.",
        ),
        InfosSection(
            id = "age",
            title = "Âge légal",
            icon = "age",
            body =
                "L’âge légal pour le visionnage des films varie de 7 à 16 ans.\n\n" +
                    "Merci de respecter ces consignes et de ne pas venir avec des enfants plus jeunes que l’âge légal indiqué. " +
                    "Des contrôles pourront être effectués par la Brigade des mineurs.",
        ),
        InfosSection(
            id = "langues",
            title = "Langues et sous-titres",
            icon = "language",
            body =
                "Les films sont diffusés en version originale afin de préserver au mieux la qualité de l’œuvre et son empreinte culturelle. " +
                    "Genève étant une ville internationale, il nous tient à cœur de toucher tous les publics et communautés représentés.\n\n" +
                    "De manière générale, les films en français sont sous-titrés en anglais ; tous les autres films sont sous-titrés en français.\n\n" +
                    "Les langues et les sous-titres sont indiqués dans le programme.",
        ),
        InfosSection(
            id = "buvette",
            title = "Buvette et pique-niques",
            icon = "drinks",
            body =
                "La buvette CinéTransat propose des boissons uniquement. Paiement cash, carte et par Twint. Pas de vente de nourriture sur place. " +
                    "Vous pouvez amener vos propres boissons et pique-niques.\n\n" +
                    "Attention, grillades interdites dans le parc.",
        ),
        InfosSection(
            id = "transats",
            title = "Location de transats",
            icon = "deckchair",
            body =
                "Des transats sont disponibles à la location pour CHF 5.- tous les jours de projection dès 19h00. Paiement cash, carte ou par Twint. " +
                    "Attention, le nombre de transats à la location est limité.\n\n" +
                    "Le placement dans le parc est libre. Prévoyez des vêtements chauds et une couverture, les fins de soirées peuvent être fraîches.",
        ),
        InfosSection(
            id = "acces",
            title = "Accès et transports publics",
            icon = "transport",
            body =
                "Lieu : parc de la Perle du Lac, rue de Lausanne, 1202 Genève.\n\n" +
                    "CinéTransat vous encourage à venir en transports publics, à pied ou à vélo.\n\n" +
                    "En tram : ligne 15, arrêt Butini.\n" +
                    "En bus : lignes 1 et 25, arrêts De-Chateaubriand ou Perle du Lac.\n" +
                    "En train : ligne Lancy-Pont-Rouge – Coppet, arrêt Genève-Sécheron.\n" +
                    "En bateau : ligne M4, arrêt De-Chateaubriand.\n" +
                    "À pied : 15 min. depuis la gare Cornavin ou 5 min. depuis les Bains des Pâquis.\n\n" +
                    "Attention ! Certains films peuvent se terminer après le départ des derniers bus/trams.\n\n" +
                    "Pour l’accès des personnes à mobilité réduite, contactez-nous sur info@cinetransat.ch. " +
                    "Le chemin de la partie basse du parc est large, plat et praticable. Certains chemins intérieurs présentent des pentes de 10–12 %. " +
                    "Il n’y a malheureusement pas de places de stationnement dédiées à proximité directe, " +
                    "mais une dépose-minute vers le restaurant de la Perle-du-lac est possible.",
        ),
        InfosSection(
            id = "annulations",
            title = "Annulations",
            icon = "weather",
            body =
                "Projections annulées en cas de pluie ou de fort vent. Décision au plus tard le jour même de la projection à 19h30.\n\n" +
                    "Annonce sur la page d’accueil du site et sur la page Facebook ou Instagram.",
        ),
        InfosSection(
            id = "toilettes",
            title = "Toilettes",
            icon = "toilet",
            body =
                "Toilettes sèches à disposition sur le site de CinéTransat et WC publics situés au bas du parc, à 100 m et à 300 m avec accès chaises roulantes.",
        ),
        InfosSection(
            id = "fumee",
            title = "Fumée",
            icon = "smoke",
            body =
                "Merci de ne pas jeter vos mégots dans l’herbe afin de nous aider à laisser le parc propre après les séances. " +
                    "Par égard pour vos voisin·e·s de pelouse, nous vous remercions de bien vouloir vous abstenir de fumer pendant le film.",
        ),
        InfosSection(
            id = "dechets",
            title = "Déchets",
            icon = "trash",
            body =
                "Des zones de tri sont à disposition dans le parc. Merci de les utiliser après votre pique-nique pour ne rien laisser sur place à votre départ.",
        ),
        InfosSection(
            id = "chiens",
            title = "Chiens",
            icon = "dog",
            body =
                "S’il peut rester calme pendant tout le film et ne pas déranger la projection, votre animal de compagnie peut participer à la fête. " +
                    "Il doit être tenu en laisse. Merci pour votre compréhension.",
        ),
        InfosSection(
            id = "velos",
            title = "Vélos",
            icon = "bike",
            body =
                "Accès à vélo possible.\n\nMerci de ne pas attacher les vélos aux vaubans de la manifestation.",
        ),
        InfosSection(
            id = "accessibilite",
            title = "Accessibilité",
            icon = "accessibility",
            body =
                "CinéTransat est un cinéma éphémère, en extérieur, dans un parc : les mesures d’accessibilité sont donc plus compliquées à mettre en place que dans une salle de cinéma.\n\n" +
                    "L’accès au parc par le bas de la pelouse est accessible aux personnes à mobilité réduite. " +
                    "Les films n’ont pas d’audio-description ni de sous-titres pour malentendants (CC). " +
                    "Ils sont sous-titrés en français pour les films étrangers et en anglais pour les films francophones.\n\n" +
                    "Les WC avec accès chaise roulante sont en bas du parc, à 300 m vers le restaurant de la Perle du Lac. " +
                    "Les personnes à mobilité réduite peuvent nous contacter si elles ont besoin d’une zone dégagée en bas de la pelouse, " +
                    "d’assistance ou d’un transat mis de côté.\n\n" +
                    "En cas de question ou de besoin particulier : info@cinetransat.ch — nous ferons au mieux pour vous accommoder.",
        ),
    )
