package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

data class SykepengeHistorikk(val json: JsonNode) : Sykdomshendelse {
    val rapportertDato = LocalDateTime.now()
    val aktørId = json["aktørId"]?.textValue()
    val sisteDato: LocalDate = LocalDate.parse(json["sistedato"]?.textValue())

    override fun hendelsetype(): Sykdomshendelse.Type {
        return Sykdomshendelse.Type.SykepengeHistorikk
    }

    override fun aktørId(): String {
        return aktørId()
    }

    override fun rapportertdato(): LocalDateTime {
        return rapportertdato()
    }

    override fun organisasjonsnummer(): String? {
        return null
    }

    override fun sykdomstidslinje(): Sykdomstidslinje {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toJson(): JsonNode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun påvirkerSakensMaksdato(sakensTidslinje: Sykdomstidslinje): Boolean {
        return sakensTidslinje.startdato().minusMonths(6).isBefore(this.sisteDato)
    }

}