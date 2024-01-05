package no.nav.helse.økonomi

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class Avviksprosent private constructor(private val desimal: Double) {

    companion object {
        private fun ratio(ratio: Double) = Avviksprosent(ratio)
        fun avvik(a: Double, b: Double) =
            if (b == 0.0) ratio(1.0)
            else ratio((a - b).absoluteValue / b)
    }

    override fun toString() = "${toInt()}%"

    fun ratio() = desimal

    fun rundTilToDesimaler() = (desimal * 10000).roundToInt() / 100.0

    fun prosent() = desimal * 100.0

    private fun toInt() = prosent().toInt()
}
