package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelse.Sykdomshendelse
import java.time.LocalDateTime

internal class Testhendelse(private val rapportertdato: LocalDateTime = LocalDateTime.of(2019, 9, 16, 10, 45)) :
    Sykdomshendelse {
    override fun hendelsetype(): Sykdomshendelse.Type {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun aktørId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun organisasjonsnummer(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sykdomstidslinje(): Sykdomstidslinje {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rapportertdato(): LocalDateTime {
        return rapportertdato
    }

    override fun compareTo(other: Sykdomshendelse): Int {
        return this.rapportertdato().compareTo(other.rapportertdato())
    }

    override fun toJson(): JsonNode {
        return objectMapper.readValue("{}")
    }
}
