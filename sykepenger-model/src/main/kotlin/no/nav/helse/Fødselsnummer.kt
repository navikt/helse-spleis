package no.nav.helse

import no.nav.helse.utbetalingstidslinje.Alder
import java.time.LocalDate

class Fødselsnummer private constructor(private val value: String) {
    private val individnummer = value.substring(6, 9).toInt()
    val fødselsdato: LocalDate = LocalDate.of(
        value.substring(4, 6).toInt().toYear(individnummer),
        value.substring(2, 4).toInt(),
        value.substring(0, 2).toInt().toDay()
    )

    override fun toString() = value
    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = other is Fødselsnummer && this.value == other.value

    fun toLong() = value.toLong()
    internal fun alder() = Alder(this)

    private fun Int.toDay() = if (this > 40) this - 40 else this
    private fun Int.toYear(individnummer: Int): Int {
        return this + when {
            this in (54..99) && individnummer in (500..749) -> 1800
            this in (0..99) && individnummer in (0..499) -> 1900
            this in (40..99) && individnummer in (900..999) -> 1900
            else -> 2000
        }
    }

    companion object {
        fun tilFødselsnummer(fnr: String): Fødselsnummer {
            if (fnr.length == 11 && alleTegnErSiffer(fnr)) return Fødselsnummer(fnr)
            else throw RuntimeException("$fnr er ikke et gyldig fødselsnummer")
        }
        private fun alleTegnErSiffer(string: String) = string.matches(Regex("\\d*"))
    }
}

fun String.somFødselsnummer() = Fødselsnummer.tilFødselsnummer(this)
