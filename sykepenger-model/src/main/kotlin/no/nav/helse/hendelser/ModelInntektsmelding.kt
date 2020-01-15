package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Problemer
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.lang.Double.min
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ModelInntektsmelding(
    hendelseId: UUID,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val mottattDato: LocalDateTime,
    private val førsteFraværsdag: LocalDate,
    orgnummer: String?,
    private val beregnetInntekt: Double,
    refusjon: Refusjon?,
    private val problemer: Problemer,
    private val arbeidsgiverperioder: List<ClosedRange<LocalDate>>,
    private val ferier: List<ClosedRange<LocalDate>>
) : SykdomstidslinjeHendelse(hendelseId, Hendelsestype.Inntektsmelding) {

    data class Refusjon(val opphørsdato: LocalDate, val beløpPrMåned: Double)

    private val orgnummer: String
    private val refusjon: Refusjon

    init {
        if (orgnummer == null) problemer.error("Inntektsmelding uten orgnummer")
        if (!ingenOverlappende()) problemer.error("Inntektsmelding har overlapp i arbeidsgiverperioder")
        if (refusjon?.beløpPrMåned != beregnetInntekt) problemer.error("Beregnet inntekt matcher ikke refusjon pr måned")

        if (problemer.hasErrors()) throw problemer

        this.orgnummer = orgnummer!!
        this.refusjon = refusjon!!
    }

    internal fun dagsats(dato: LocalDate, grunnbeløp: Grunnbeløp): Int {
        val årssats = min(beregnetInntekt * 12, grunnbeløp.beløp(dato))
        return (årssats / 260).toInt()
    }

    private fun ingenOverlappende() = arbeidsgiverperioder
        .sortedBy { it.start }
        .zipWithNext(ClosedRange<LocalDate>::ingenOverlappende)
        .all { it }

    override fun sykdomstidslinje(): ConcreteSykdomstidslinje {
        val arbeidsgivertidslinje = konstruerArbeidsgivertidslinje()
        val ferietidslinje = konstruerFerietidslinje()
            ?: return arbeidsgivertidslinje
        return arbeidsgivertidslinje + ferietidslinje
    }

    private fun konstruerArbeidsgivertidslinje() = arbeidsgiverperioder
        .map { ConcreteSykdomstidslinje.egenmeldingsdager(it.start, it.endInclusive, this) }
        .takeUnless { it.isEmpty() }
        ?.reduce { acc, sykdomstidslinje -> acc + sykdomstidslinje }
        ?: ConcreteSykdomstidslinje.egenmeldingsdag(førsteFraværsdag, this)

    private fun konstruerFerietidslinje() = ferier
        .map { ConcreteSykdomstidslinje.ferie(it.start, it.endInclusive, this) }
        .takeUnless { it.isEmpty() }
        ?.reduce { acc, sykdomstidslinje -> acc + sykdomstidslinje }

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Inntektsmelding

    override fun rapportertdato() = mottattDato

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    override fun toJson() = "no u" // Should not be part of Model events
}

private fun ClosedRange<LocalDate>.ingenOverlappende(other: ClosedRange<LocalDate>) =
    maxOf(this.start, other.start) > minOf(this.endInclusive, other.endInclusive)
