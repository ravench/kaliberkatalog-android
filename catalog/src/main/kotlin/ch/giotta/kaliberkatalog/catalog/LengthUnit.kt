package ch.giotta.kaliberkatalog.catalog

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Millimetres or inches, the unit the calibre page shows its lengths in. Ported from Kaliberfinder's LengthUnit.swift. */
enum class LengthUnit(val symbol: String) {
    METRIC("mm"),
    IMPERIAL("in"),
    ;

    /** Converts millimetres to this unit. */
    fun value(fromMm: Double) = if (this == METRIC) fromMm else fromMm / MM_PER_INCH

    /**
     * "29.7 mm" or "1.169 in": the shown values are rounded, as the calibre page says, millimetres to 0.1 mm, inches to
     * 0.001 in as US load data writes them (2.800 in, 0.308 in). Display only: the data keeps the full value.
     */
    fun format(mm: Double, locale: Locale): String {
        val digits = if (this == METRIC) 1 else 3
        // Half to even from the shortest decimal, as Kaliberfinder's formatting (ICU) rounds: 19.15 mm is 19.2 mm.
        val rounded = BigDecimal.valueOf(value(mm)).setScale(digits, RoundingMode.HALF_EVEN)
        val number = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
        return number.format(rounded) + " " + symbol
    }

    companion object {
        const val MM_PER_INCH = 25.4
    }
}
