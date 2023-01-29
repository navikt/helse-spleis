package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.AlderVisitor
import no.nav.helse.Alder

internal class V168Fødselsdato: JsonMigration(168) {
    override val description = "Fødselsdato fra fnr/dnr"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("fødselsnummer").asText().somFødselsnummer().alder().accept(object : AlderVisitor {
            override fun visitAlder(alder: Alder, fødselsdato: LocalDate) {
                jsonNode.put("fødselsdato", fødselsdato.toString())
            }
        })
    }

    private class Fødselsnummer private constructor(private val value: String) {
        private val individnummer = value.substring(6, 9).toInt()
        val fødselsdato: LocalDate = LocalDate.of(
            value.substring(4, 6).toInt().toYear(individnummer),
            value.substring(2, 4).toInt(),
            value.substring(0, 2).toInt().toDay()
        )

        override fun toString() = value
        override fun hashCode(): Int = value.hashCode()
        override fun equals(other: Any?) = other is Fødselsnummer && this.value == other.value

        fun alder() = Alder(this.fødselsdato)

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

    private fun String.somFødselsnummer() = Fødselsnummer.tilFødselsnummer(this)
}