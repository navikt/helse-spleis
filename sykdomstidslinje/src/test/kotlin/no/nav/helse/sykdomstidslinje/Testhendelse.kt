package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelse.SykdomstidslinjeHendelse
import java.time.LocalDateTime
import java.util.*

class Testhendelse(
    private val rapportertdato: LocalDateTime = LocalDateTime.of(2019, 9, 16, 10, 45),
    private val hendelsetype: SykdomstidslinjeHendelse.Type = SykdomstidslinjeHendelse.Type.SendtSøknadMottatt
) :
    SykdomstidslinjeHendelse(UUID.randomUUID().toString()) {
    override fun hendelsetype(): SykdomstidslinjeHendelse.Type = hendelsetype

    override fun sykdomstidslinje(): Sykdomstidslinje {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rapportertdato(): LocalDateTime {
        return rapportertdato
    }

    override fun compareTo(other: SykdomstidslinjeHendelse): Int {
        return this.rapportertdato().compareTo(other.rapportertdato())
    }

    override fun toJson(): JsonNode {
        return ObjectMapper().readValue("{}")
    }

    override fun equals(other: Any?): Boolean = this === other
}
