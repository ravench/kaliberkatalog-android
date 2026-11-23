package ch.giotta.kaliberkatalog

import ch.giotta.kaliberkatalog.catalog.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryTitleTest {
    @Test
    fun everyCategoryHasItsOwnTitle() {
        assertEquals(Category.entries.size, Category.entries.mapNotNull { categoryTitle(it.german) }.toSet().size)
    }

    /** A category newer than the app keeps the data's own name instead of stopping the app. */
    @Test
    fun anUnknownCategoryHasNone() {
        assertNull(categoryTitle("Harpune"))
    }
}
