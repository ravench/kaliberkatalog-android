package ch.giotta.kaliberkatalog.catalog

import java.text.Normalizer
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Catalogue categories, in the list's order. */
enum class Category(
    /** The name stored in the data, the key of its translations. */
    val german: String,
) {
    RIMFIRE("Randfeuer"),
    HANDGUN("Kurzwaffe"),
    RIFLE("Langwaffe"),
    BLACK_POWDER("Schwarzpulver"),
    SHOTGUN("Flinte"),
    AIR("Luftdruck"),
    CANNON("Kanone"),
}

/** The calibre catalogue generated from calibre-data (tools/generate_data.py). Ported from Kaliberfinder's CartridgeCatalog.swift. */
class CartridgeCatalog(val file: File) {
    @Serializable
    data class File(
        val version: Int,
        val calibreData: Source? = null,
        val cartridges: List<Cartridge>,
        val lookalikes: List<LookalikeGroup> = emptyList(),
        /** The works the calibres' sources name ([Cartridge.SourceGroup.Ref.work]). */
        val sources: List<Work> = emptyList(),
    )

    /** The calibre-data commit the catalogue was generated from, shown in Settings. */
    @Serializable
    data class Source(
        val commit: String,
        /** ISO 8601. Text, so a date that doesn't parse can't cost the catalogue. */
        val date: String,
    )

    /** A book, a web page or a caliper measurement that a calibre's values come from. */
    @Serializable
    data class Work(
        /** Null for a measurement. */
        val title: String? = null,
        /** A book's authors or editors, a web page's table (C.I.P.) or host. */
        val detail: String? = null,
        val year: Int? = null,
        /** Null for a page that is named but not linked (shops, load data). */
        val url: String? = null,
        /** A caliper measurement's day, "2026-11-08". */
        val measured: String? = null,
    )

    /** Every calibre with a drawing. A partial entry has only its page, which [visible] lists on request. */
    val all = file.cartridges.filter { it.isDrawn }

    private val byId = file.cartridges.associateBy { it.id }

    fun cartridge(id: String) = byId[id]

    /** The lookalike group [c] belongs to, if any. */
    fun lookalikeGroup(c: Cartridge) = file.lookalikes.firstOrNull { c.id in it.calibres }

    /** The group that stands for [c] in rows and tags: one whose cases are of one size. */
    fun sizeGroup(c: Cartridge) = lookalikeGroup(c)?.takeIf { it.sameSize }

    fun members(group: LookalikeGroup) = group.calibres.mapNotNull(::cartridge)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val marks = Regex("\\p{Mn}+")

        /** Null for text that is no catalogue. */
        fun decode(text: String): CartridgeCatalog? = try {
            CartridgeCatalog(json.decodeFromString<File>(text))
        } catch (e: IllegalArgumentException) {
            null
        }

        /**
         * What the user gets to see: the well-sourced calibres unless the developer switch [showUnsourced] is on, and
         * no cannon round without [showCannon]. Both rules apply to a cannon round.
         */
        fun visible(cartridges: List<Cartridge>, showUnsourced: Boolean, showCannon: Boolean) =
            cartridges.filter { (showUnsourced || it.wellSourced) && (showCannon || !it.isCannon) }

        /** A name for comparison: case, accents, spaces, hyphens, dots and "mm" don't matter, "×" is "x", "," is ".". */
        fun normalized(s: String) = marks.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "")
            .lowercase(Locale.ROOT)
            .replace("×", "x")
            .replace(",", ".")
            .filterNot { it.isWhitespace() || it == '-' }
            .replace("mm", "")
            .replace(".", "")

        /**
         * The cartridges known by [query], in catalogue order: one of their names ([Cartridge.allNames]) or of their
         * group's words ([wordsByGroup], by [Cartridge.groupWordsKey]) contains it, or these hold each of its words
         * in any order, so «Vetterli 10.4» finds «10.4x38 mm R Vetterli».
         */
        fun search(query: String, cartridges: List<Cartridge>, wordsByGroup: Map<String, List<String>> = emptyMap()): List<Cartridge> {
            val whole = normalized(query)
            if (whole.isEmpty()) return cartridges
            val words = query.map { if (it.isWhitespace() || it == '/') '-' else it }.joinToString("")
                .split('-').map(::normalized).filter { it.isNotEmpty() }
            return cartridges.filter { c ->
                val names = (c.allNames + c.groupWordsKey?.let { wordsByGroup[it] }.orEmpty()).map(::normalized)
                // The whole query too: dropping "mm" can join its words («357 REM MAG» is "357reag").
                names.any { whole in it } || words.all { word -> names.any { word in it } }
            }
        }

        /** Cartridges by category, in the order of [Category]. */
        fun grouped(cartridges: List<Cartridge>): List<Pair<String, List<Cartridge>>> {
            val order = Category.entries.map { it.german }
            return cartridges.groupBy { it.category }.toList()
                .sortedWith(compareBy({ order.indexOf(it.first).takeIf { i -> i >= 0 } ?: order.size }, { it.first }))
        }
    }
}

/**
 * Calibres that look alike but are cartridges of their own (calibre-data's lookalikes): a measurement, the load or
 * the ignition tells them apart.
 */
@Serializable
data class LookalikeGroup(
    /** The parent's name, or the family's («7.63x25 mm», whose parent is the .30 Borchardt). */
    val name: String,
    /** What tells them apart: "rim", "bore", "length", "seating", "load" or "ignition". */
    val differs: List<String>,
    /** Catalogue ids, the parent first. */
    val calibres: List<String>,
) {
    /** The cases are of one size, told apart by load, seating or ignition only: one row stands for the group. */
    val sameSize get() = differs.none { it in listOf("rim", "length", "bore") }
}
