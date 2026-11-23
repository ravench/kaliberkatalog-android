package ch.giotta.kaliberkatalog

import ch.giotta.kaliberkatalog.catalog.Cartridge
import ch.giotta.kaliberkatalog.catalog.CartridgeCatalog
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ported from Kaliberfinder's testAGroupsWordsFindItInEveryLanguage, on the catalogue the app ships. */
class GroupWordsTest {
    private val all = checkNotNull(CartridgeCatalog.decode(File("src/main/assets/Cartridges.json").readText())).all

    private fun found(words: String) = CartridgeCatalog.search(words, all, wordsByGroup).map { it.id }.toSet()

    private fun group(belongs: (Cartridge) -> Boolean) = all.filter(belongs).map { it.id }.toSet()

    @Test
    fun aGroupsWordsFindItInEveryLanguage() {
        val groups = listOf(
            group { it.blank } to listOf(
                "Platzpatrone", "Knall", "blank", "blank cartridge", "à blanc", "cartouche à blanc", "a salve", "cartuccia a salve",
                "de fogueo", "cartucho de fogueo",
            ),
            group { it.kind == "shot" } to listOf(
                "Schrotpatrone", "Schrot", "shotshell", "shotgun shell", "cartouche de chasse", "cartouche à plombs",
                "cartuccia da caccia", "cartuccia a pallini", "cartucho de caza", "cartucho de escopeta",
            ),
            group { it.kind == "ball" && !it.blank } to listOf(
                "Rundkugel", "round ball", "balle ronde", "palla tonda", "palla sferica", "bala esférica",
            ),
            group { it.kind == "pellet" } to listOf(
                "Diabolo", "Luftgewehr", "pellet", "air rifle", "air gun", "plomb", "air comprimé", "piombino", "pallino",
                "aria compressa", "balín", "aire comprimido",
            ),
            group { it.kind == "minie" } to listOf("Minié-Geschoss", "minie ball", "balle minié", "palla minié", "bala minié"),
        )
        assertEquals(listOf(10, 8, 8, 2, 2), groups.map { it.first.size }) // the 6.8/11 is the tenth blank
        for ((members, words) in groups) {
            for (word in words) assertTrue("«$word» misses ${members - found(word)}", found(word).containsAll(members))
        }
        // The blanks' words find the blanks and nothing else.
        for (word in listOf("Platzpatrone", "blank", "à blanc", "a salve", "de fogueo")) assertEquals(word, groups[0].first, found(word))
        assertEquals(
            listOf("Rundkugel", "Bleikugel", "round ball", "lead ball", "balle ronde", "palla tonda", "palla sferica", "bala esférica",
                "bala redonda"),
            wordsByGroup["Rundkugel, Bleikugel"],
        )
    }
}
