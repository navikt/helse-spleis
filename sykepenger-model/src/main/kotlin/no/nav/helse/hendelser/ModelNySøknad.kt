package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.IAktivitetslogger
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ModelNySøknad(
    hendelseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val rapportertdato: LocalDateTime,
    sykeperioder: List<Triple<LocalDate, LocalDate, Int>>,
    private val aktivitetslogger: Aktivitetslogger,
    private val originalJson: String
) : SykdomstidslinjeHendelse(hendelseId, Hendelsestype.NySøknad), IAktivitetslogger by aktivitetslogger {

    private val sykeperioder: List<Sykeperiode>

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): ModelNySøknad {
            return objectMapper.readTree(json).let {
                ModelNySøknad(
                    hendelseId = UUID.fromString(it["hendelseId"].textValue()),
                    fnr = it.path("søknad").path("fnr").asText(),
                    aktørId = it.path("søknad").path("aktorId").asText(),
                    orgnummer = it.path("søknad").path("arbeidsgiver").path("orgnummer").asText(),
                    rapportertdato = it.path("søknad").path("opprettet").asText().let { LocalDateTime.parse(it) },
                    sykeperioder = it.path("søknad").path("soknadsperioder").map { periode: JsonNode ->
                        Triple(
                            first = periode.path("fom").asLocalDate(),
                            second = periode.path("tom").asLocalDate(),
                            third = periode.path("sykmeldingsgrad").asInt()
                        )
                    },
                    aktivitetslogger = Aktivitetslogger(),
                    originalJson = objectMapper.writeValueAsString(it.path("søknad"))
                )
            }
        }

        private fun JsonNode.asLocalDate() =
            asText().let { LocalDate.parse(it) }

    }

    init {
        if (sykeperioder.isEmpty()) aktivitetslogger.severe("Ingen sykeperioder")
        this.sykeperioder = sykeperioder.sortedBy { it.first }.map { Sykeperiode(it.first, it.second, it.third) }
        if (!ingenOverlappende()) aktivitetslogger.severe("Sykeperioder overlapper")
    }

    private inner class Sykeperiode(
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val sykdomsgrad: Int
    ) {
        internal fun kanBehandles() = sykdomsgrad == 100

        internal fun sykdomstidslinje() =
            ConcreteSykdomstidslinje.sykedager(fom, tom, this@ModelNySøknad)

        internal fun ingenOverlappende(other: Sykeperiode) =
            maxOf(this.fom, other.fom) > minOf(this.tom, other.tom)
    }

    override fun kanBehandles() = !valider().hasErrors()

    fun valider(): Aktivitetslogger {
        if (!hundreProsentSykmeldt()) aktivitetslogger.error("Støtter bare 100%% sykmeldt")
        return aktivitetslogger
    }

    private fun hundreProsentSykmeldt() = sykeperioder.all { it.kanBehandles() }

    private fun ingenOverlappende() = sykeperioder.zipWithNext(Sykeperiode::ingenOverlappende).all { it }

    override fun sykdomstidslinje() =
        sykeperioder.map(Sykeperiode::sykdomstidslinje).reduce(ConcreteSykdomstidslinje::plus)

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Sykmelding

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun rapportertdato() = rapportertdato

    override fun aktørId() = aktørId

    // TODO: Should not be part of Model events
    override fun toJson(): String = objectMapper.writeValueAsString(
        mapOf(
            "hendelseId" to hendelseId(),
            "type" to hendelsetype(),
            "søknad" to objectMapper.readTree(originalJson)
        )
    )
}
