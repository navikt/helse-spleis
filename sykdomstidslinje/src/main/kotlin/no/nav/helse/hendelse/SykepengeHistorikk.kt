package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDateTime

data class SykepengeHistorikk(val aktørId: String) : Sykdomshendelse {
    val rapportertDato = LocalDateTime.now()

    override fun hendelsetype(): Sykdomshendelse.Type {
        return Sykdomshendelse.Type.SykepengeHistorikk
    }

    override fun aktørId(): String {
        return aktørId
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

}