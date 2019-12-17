package no.nav.helse.hendelser.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.Hendelsetype
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.UtenforOmfangException
import no.nav.helse.serde.safelyUnwrapDate
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje.Companion.egenmeldingsdag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class InntektsmeldingHendelse private constructor(hendelseId: String, private val inntektsmelding: JsonNode) :
    ArbeidstakerHendelse, SykdomstidslinjeHendelse(hendelseId) {
    constructor(inntektsmelding: JsonNode) : this(UUID.randomUUID().toString(), inntektsmelding)

    companion object {

        private val log = LoggerFactory.getLogger(InntektsmeldingHendelse::class.java)

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromInntektsmelding(json: String): InntektsmeldingHendelse? {
            return try {
                InntektsmeldingHendelse(objectMapper.readTree(json))
            } catch (err: IOException) {
                log.info("kunne ikke lese inntektsmelding som json: ${err.message}", err)
                null
            }
        }

        fun fromJson(jsonNode: JsonNode): InntektsmeldingHendelse {
            return InntektsmeldingHendelse(
                jsonNode["hendelseId"].textValue(),
                jsonNode["inntektsmelding"]
            )
        }
    }

    val arbeidsgiverFnr: String? get() = inntektsmelding["arbeidsgiverFnr"]?.textValue()
    val førsteFraværsdag: LocalDate get() = LocalDate.parse(inntektsmelding["foersteFravaersdag"].textValue())
    val mottattDato: LocalDateTime get() = LocalDateTime.parse(inntektsmelding["mottattDato"].textValue())
    val ferie get() = inntektsmelding["ferieperioder"]?.map { Periode(it) } ?: emptyList()
    val inntektsmeldingId = inntektsmelding["inntektsmeldingId"].asText() as String
    val arbeidstakerAktorId = inntektsmelding["arbeidstakerAktorId"].textValue() as String
    val arbeidstakerFnr = inntektsmelding["arbeidstakerFnr"].textValue() as String
    val virksomhetsnummer: String? get() = inntektsmelding["virksomhetsnummer"]?.textValue()
    val arbeidsgiverAktorId: String? get() = inntektsmelding["arbeidsgiverAktorId"]?.textValue()
    val arbeidsgiverperioder get() = inntektsmelding["arbeidsgiverperioder"]?.map { Periode(it) } ?: emptyList()
    val beregnetInntekt
        get() = inntektsmelding["beregnetInntekt"]
            ?.takeUnless { it.isNull }
            ?.textValue()?.toBigDecimal()
    val refusjon get() = Refusjon(inntektsmelding["refusjon"])
    val endringIRefusjoner
        get() = inntektsmelding["endringIRefusjoner"]
            .mapNotNull { it["endringsdato"].safelyUnwrapDate() }

    override fun hendelsetype(): Hendelsetype {
        return Hendelsetype.Inntektsmelding
    }

    override fun kanBehandles() = inntektsmelding["mottattDato"] != null
        && inntektsmelding["foersteFravaersdag"] != null
        && inntektsmelding["virksomhetsnummer"] != null && !inntektsmelding["virksomhetsnummer"].isNull
        && inntektsmelding["beregnetInntekt"] != null && !inntektsmelding["beregnetInntekt"].isNull
        && inntektsmelding["arbeidstakerFnr"] != null
        && inntektsmelding["refusjon"]?.let { Refusjon(it) }?.beloepPrMnd == beregnetInntekt ?: false

    fun beregnetInntekt() = checkNotNull(beregnetInntekt) { "Vi kan ikke håndtere inntektsmeldinger uten beregnet inntekt" }

    fun refusjon() = refusjon

    fun endringIRefusjoner() = endringIRefusjoner

    override fun fødselsnummer() = arbeidstakerFnr

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Inntektsmelding

    override fun aktørId() = arbeidstakerAktorId

    override fun opprettet() = mottattDato

    override fun rapportertdato() = mottattDato

    override fun organisasjonsnummer() = requireNotNull(virksomhetsnummer)

    override fun sykdomstidslinje(): ConcreteSykdomstidslinje {
        val arbeidsgiverperiode = arbeidsgiverperioder
            .takeIf { it.isNotEmpty() }
            ?.map { ConcreteSykdomstidslinje.egenmeldingsdager(it.fom, it.tom, this) }
            ?.reduce { acc, sykdomstidslinje ->
                if (acc.overlapperMed(sykdomstidslinje)) {
                    throw UtenforOmfangException(
                        "Inntektsmeldingen inneholder overlappende arbeidsgiverperioder",
                        this
                    )
                }
                acc.plus(sykdomstidslinje, ConcreteSykdomstidslinje.Companion::ikkeSykedag)
            }?.let {
                ConcreteSykdomstidslinje.ikkeSykedager(
                    it.førsteDag().minusDays(16),
                    it.førsteDag().minusDays(1),
                    this
                ) + it
            }

        val ferietidslinje = ferie
            .map { ConcreteSykdomstidslinje.ferie(it.fom, it.tom, this) }
            .takeUnless { it.isEmpty() }
            ?.reduce { resultat, sykdomstidslinje -> resultat + sykdomstidslinje }

        return arbeidsgiverperiode.plus(ferietidslinje) ?: egenmeldingsdag(førsteFraværsdag, this)
    }

    private fun ConcreteSykdomstidslinje?.plus(other: ConcreteSykdomstidslinje?): ConcreteSykdomstidslinje? {
        if (other == null) return this
        return this?.plus(other) ?: other
    }

    override fun toJsonNode(): JsonNode {
        return objectMapper.readTree(toJson())
    }

    override fun toJson(): String = objectMapper.writeValueAsString(mapOf(
        "hendelseId" to hendelseId(),
        "type" to SykdomshendelseType.InntektsmeldingMottatt.name,
        "inntektsmelding" to inntektsmelding
    ))

    data class Periode(val jsonNode: JsonNode) {
        val fom get() = LocalDate.parse(jsonNode["fom"].textValue()) as LocalDate
        val tom get() = LocalDate.parse(jsonNode["tom"].textValue()) as LocalDate
    }

    data class Refusjon(val jsonNode: JsonNode) {
        val opphoersdato get() = jsonNode["opphoersdato"].safelyUnwrapDate()
        val beloepPrMnd get() = jsonNode["beloepPrMnd"]?.textValue()?.toBigDecimal()
    }
}

