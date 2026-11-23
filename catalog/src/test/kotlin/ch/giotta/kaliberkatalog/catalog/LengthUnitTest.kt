package ch.giotta.kaliberkatalog.catalog

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ported from Kaliberfinder's LengthUnitTests.swift. */
class LengthUnitTest {
    private val us = Locale.US
    private val german = Locale.GERMANY
    private val swiss = Locale.forLanguageTag("de-CH")

    @Test
    fun formatting() {
        assertEquals("29.7 mm", LengthUnit.METRIC.format(29.69, us))
        assertEquals("29,7 mm", LengthUnit.METRIC.format(29.69, german))
        assertEquals("29.7 mm", LengthUnit.METRIC.format(29.69, swiss))
        assertEquals("12.0 mm", LengthUnit.METRIC.format(12.0, us))
        // Inches with three decimals, like US load data.
        assertEquals("1.169 in", LengthUnit.IMPERIAL.format(29.69, us))
        assertEquals("0.308 in", LengthUnit.IMPERIAL.format(7.82, us))
        assertEquals("2.800 in", LengthUnit.IMPERIAL.format(71.12, us))
        assertEquals("0,308 in", LengthUnit.IMPERIAL.format(7.82, german))
    }

    /** As on iOS: the value as written, rounded half to even. */
    @Test
    fun roundingMatchesKaliberfinder() {
        assertEquals("19.2 mm", LengthUnit.METRIC.format(19.15, us)) // the 9x19 mm's case, 19.1499… as a double
        assertEquals("22.4 mm", LengthUnit.METRIC.format(22.45, us)) // the 12/70's rim, 22.5 mm when rounded half up
        assertEquals("9.4 mm", LengthUnit.METRIC.format(9.35, us))
    }
}
