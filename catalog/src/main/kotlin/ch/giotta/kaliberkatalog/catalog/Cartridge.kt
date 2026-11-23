package ch.giotta.kaliberkatalog.catalog

import kotlinx.serialization.Serializable

/**
 * One catalogue calibre with its dimensions in millimetres and a side-view outline, as tools/generate_data.py
 * writes it into Cartridges.json. Ported from Kaliberfinder's Cartridge.swift.
 */
@Serializable
data class Cartridge(
    val id: String,
    val name: String,
    /** German category name, see [Category]. */
    val category: String,
    /** rimless, semi-rimmed, rimmed, belted, rimfire, shot, ball, minie or pellet. */
    val kind: String,
    val aliases: List<String>,
    /** Printable other names, e.g. "9 mm Luger" for 9x19 mm. */
    val displayAliases: List<String>,
    /** No longer in regular factory production. */
    val obsolete: Boolean,
    /** Hardly ever found (calibre-data RARE). */
    val rare: Boolean,
    /** The state that introduced the calibre; null for gauges, balls, pellets, blanks and open origins. */
    val country: Country? = null,
    /** In common use worldwide, independent of [country]: 9x19 mm is German and worldwide. */
    val worldwide: Boolean,
    /** No bullet: the case is the whole cartridge. */
    val blank: Boolean,
    /** Every drawn measurement has a source; the others stay hidden unless a developer switch shows them. */
    val wellSourced: Boolean,
    val dimensions: Dimensions,
    val outline: Outline,
    /** Null for rimfire and for balls, Minié bullets and pellets. */
    val primer: Primer? = null,
    /** The painted tips, bases and primer rings of military loads; empty for most calibres. */
    val colourCodes: List<ColourCode>,
    /** Where the calibre page's values come from. */
    val sources: List<SourceGroup>,
) {
    @Serializable
    data class Dimensions(
        val rim: Double,
        /** Only where the belt is wider than the rim (the .378 Weatherby head). */
        val belt: Double? = null,
        val base: Double,
        /** Only for bottleneck cases. */
        val shoulder: Double? = null,
        val neck: Double,
        /** 0 for balls, Minié bullets and pellets: they have no case. */
        val caseLength: Double,
        val overallLength: Double,
        val bullet: Double,
    )

    /** Side view in millimetres as SVG path data: tip at y = 0, base at y = [height], centred in [width]. */
    @Serializable
    data class Outline(
        val width: Double,
        val height: Double,
        val case: String? = null,
        val head: String? = null,
        val bullet: String? = null,
        /** Paper band over a paper-patched bullet; not part of the silhouette. */
        val patch: String? = null,
        /** Soft point or plastic tip drawn over the bullet; not part of the silhouette. */
        val tip: String? = null,
        val tipMaterial: String? = null,
        /** A hollow point's cavity, a hidden line inside the nose. */
        val cavity: String? = null,
        /** Null means copper (or lead without a case). */
        val bulletMaterial: String? = null,
        /** Null means brass. */
        val caseMaterial: String? = null,
        /** What is most commonly found today where it is not the original. */
        val todayBulletMaterial: String? = null,
        val todayCaseMaterial: String? = null,
        /** "steel" for the steel head of a hybrid case; null means brass. */
        val headMaterial: String? = null,
    )

    @Serializable
    data class Primer(
        /** small-pistol, large-pistol, small-rifle, large-rifle, miniature, shotshell-209, bmg-35 or berdan-nitro. */
        val type: String,
        /** Cup diameter in mm. */
        val diameter: Double,
    )

    @Serializable
    data class Country(
        /** ISO code; RUE, AH, SU, CS and OE for former states, NATO for the alliance. */
        val code: String,
        /** German name, the key of its translations. */
        val name: String,
        /** Emoji; null for former states and unions, whose flag the app draws. */
        val flag: String? = null,
    )

    @Serializable
    data class ColourCode(
        /** "tip", "base" or "annulus" (the lacquer ring around the primer). */
        val part: String,
        /** A tip's from the tip down: ["red", "silver"] is a red tip above a silver band. */
        val colours: List<String>,
        val meaning: List<String>,
        /** The loads that carry it, in English, e.g. "US M20, Swiss M.65". */
        val description: String? = null,
        /** The description in "de", "fr", "it" and "es" where calibre-data translates it. */
        val descriptions: Map<String, String>? = null,
        val countries: List<Country>,
    )

    /** The works one or more parts of the calibre page come from. */
    @Serializable
    data class SourceGroup(val parts: List<String>, val refs: List<Ref>) {
        @Serializable
        data class Ref(
            /** Index into [CartridgeCatalog.File.sources]. */
            val work: Int,
            val pages: List<String>? = null,
        )
    }

    val hasCase get() = dimensions.caseLength > 0

    /** Behind the developer switch «Kanonenkaliber zeigen»: too big for the screen. */
    val isCannon get() = category == Category.CANNON.german

    /** A partial entry (a cannon round whose measurements no source gives) has no paths: no drawing, only its page. */
    val isDrawn get() = outline.case != null || outline.bullet != null

    /** The case head seen from below: the rim, or the base or the belt where it is wider. */
    val headDiameter get() = maxOf(dimensions.rim, dimensions.base, dimensions.belt ?: 0.0)

    /** Every name the calibre is known by, for search. */
    val allNames get() = listOf(name) + displayAliases + aliases

    /** The everyday words of the group a round without a bullet belongs to, or null; see [groupWordsKey]. */
    val groupWordsKey get() = groupWordsKey(if (blank) "blank" else kind)

    companion object {
        /**
         * The everyday words of a group of rounds, German and comma-separated, the key of their translations: a blank
         * is a «Platzpatrone». Searched in every language of the app instead of an alias on every calibre.
         */
        fun groupWordsKey(group: String): String? = when (group) {
            "blank" -> "Platzpatrone, Knallpatrone, Knall"
            "shot" -> "Schrotpatrone, Schrot, Flintenpatrone"
            "ball" -> "Rundkugel, Bleikugel"
            "pellet" -> "Diabolo, Luftgewehrkugel, Luftgewehr"
            "minie" -> "Minié-Geschoss"
            else -> null
        }
    }
}
