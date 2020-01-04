package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.UtenforOmfangException
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class NySøknad(hendelseId: UUID, søknad: JsonNode) : SøknadHendelse(hendelseId, Hendelsetype.NySøknad, søknad) {

    constructor(søknad: JsonNode) : this(UUID.randomUUID(), søknad)

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): NySøknad {
            return objectMapper.readTree(json).let {
                NySøknad(UUID.fromString(it["hendelseId"].textValue()), it["søknad"])
            }
        }
    }

    private val opprettet
        get() = søknad["opprettetDato"]?.takeUnless { it.isNull }?.let {
            LocalDate.parse(it.textValue()).atStartOfDay()
        } ?: LocalDateTime.parse(søknad["opprettet"].asText())

    override fun rapportertdato(): LocalDateTime = opprettet

    override fun kanBehandles(): Boolean {
        return super.kanBehandles() && sykeperioder.all { it.sykmeldingsgrad == 100 }
    }

    private val sykeperiodeTidslinje
        get(): List<ConcreteSykdomstidslinje> = sykeperioder
            .map { ConcreteSykdomstidslinje.sykedager(it.fom, it.tom, this) }

    override fun sykdomstidslinje() =
        sykeperiodeTidslinje.reduce { resultatTidslinje, delTidslinje ->
            if (resultatTidslinje.overlapperMed(delTidslinje)) throw UtenforOmfangException(
                "Søknaden inneholder overlappende sykdomsperioder",
                this
            )
            resultatTidslinje + delTidslinje
        }

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Sykmelding
}
