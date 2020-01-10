package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Grunnbeløp
import no.nav.helse.person.UtenforOmfangException
import no.nav.helse.serde.safelyUnwrapDate
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje.Companion.egenmeldingsdag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Inntektsmelding(hendelseId: UUID, private val inntektsmelding: JsonNode) :
    SykdomstidslinjeHendelse(hendelseId, Hendelsestype.Inntektsmelding) {

    private constructor(inntektsmelding: JsonNode) : this(UUID.randomUUID(), inntektsmelding)

    class Builder : ArbeidstakerHendelseBuilder {
        override fun build(json: String): Inntektsmelding? {
            return try {
                Inntektsmelding(objectMapper.readTree(json))
            } catch (err: Exception) {
                null
            }
        }
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        fun fromJson(json: String): Inntektsmelding {
            return objectMapper.readTree(json).let {
                Inntektsmelding(
                    UUID.fromString(it["hendelseId"].textValue()),
                    it["inntektsmelding"]
                )
            }
        }
    }

    internal val førsteFraværsdag: LocalDate get() = LocalDate.parse(inntektsmelding["foersteFravaersdag"].textValue())
    private val mottattDato: LocalDateTime get() = LocalDateTime.parse(inntektsmelding["mottattDato"].textValue())
    private val ferie
        get() = inntektsmelding["ferieperioder"]?.map {
            Periode(
                it
            )
        } ?: emptyList()
    private val arbeidstakerAktorId = inntektsmelding["arbeidstakerAktorId"].textValue() as String
    private val arbeidstakerFnr = inntektsmelding["arbeidstakerFnr"].textValue() as String
    private val virksomhetsnummer: String? get() = inntektsmelding["virksomhetsnummer"]?.textValue()
    private val arbeidsgiverperioder
        get() = inntektsmelding["arbeidsgiverperioder"]?.map {
            Periode(
                it
            )
        } ?: emptyList()
    internal val beregnetInntekt
        get() = inntektsmelding["beregnetInntekt"]
            ?.takeUnless { it.isNull }
            ?.textValue()?.toBigDecimal()
    private val refusjon get() = Refusjon(inntektsmelding["refusjon"])

    private val endringIRefusjoner
        get() = inntektsmelding["endringIRefusjoner"]
            .mapNotNull { it["endringsdato"].safelyUnwrapDate() }

    override fun kanBehandles() = inntektsmelding["mottattDato"] != null
        && inntektsmelding["foersteFravaersdag"] != null
        && inntektsmelding["virksomhetsnummer"] != null && !inntektsmelding["virksomhetsnummer"].isNull
        && inntektsmelding["beregnetInntekt"] != null && !inntektsmelding["beregnetInntekt"].isNull
        && inntektsmelding["arbeidstakerFnr"] != null
        && inntektsmelding["refusjon"]?.let { Refusjon(it) }?.beloepPrMnd == beregnetInntekt ?: false

    override fun fødselsnummer() = arbeidstakerFnr

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Inntektsmelding

    override fun aktørId() = arbeidstakerAktorId

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

    override fun toJson(): String = objectMapper.writeValueAsString(
        mapOf(
            "hendelseId" to hendelseId(),
            "type" to hendelsetype(),
            "inntektsmelding" to inntektsmelding
        )
    )

    fun harEndringIRefusjon(sisteUtbetalingsdag: LocalDate): Boolean {
        refusjon.opphoersdato?.also {
            if (it <= sisteUtbetalingsdag) {
                return true
            }
        }

        return endringIRefusjoner.any { it <= sisteUtbetalingsdag }
    }

    internal fun dagsats(dato: LocalDate, grunnbeløp: Grunnbeløp): Int {
        val beregnetInntekt =
            checkNotNull(beregnetInntekt) { "kan ikke regne ut dagsats fra inntektsmeldinger uten beregnet inntekt" }
        return beregnetInntekt
            .times(12.toBigDecimal())
            .min(grunnbeløp.beløp(dato).toBigDecimal())
            .divide(260.toBigDecimal(), 0, RoundingMode.HALF_UP)
            .toInt()
    }

    private class Periode(val jsonNode: JsonNode) {
        val fom get() = LocalDate.parse(jsonNode["fom"].textValue()) as LocalDate
        val tom get() = LocalDate.parse(jsonNode["tom"].textValue()) as LocalDate

    }
    private class Refusjon(val jsonNode: JsonNode) {
        val opphoersdato get() = jsonNode["opphoersdato"].safelyUnwrapDate()
        val beloepPrMnd get() = jsonNode["beloepPrMnd"]?.textValue()?.toBigDecimal()

    }
}

