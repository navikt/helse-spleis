package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.ModelInntektsmelding.Periode.Arbeidsgiverperiode
import no.nav.helse.hendelser.ModelInntektsmelding.Periode.Ferieperiode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.lang.Double.min
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ModelInntektsmelding(
    hendelseId: UUID,
    private val refusjon: Refusjon,
    private val orgnummer: String,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val mottattDato: LocalDateTime,
    internal val førsteFraværsdag: LocalDate,
    internal val beregnetInntekt: Double,
    private val aktivitetslogger: Aktivitetslogger,
    arbeidsgiverperioder: List<ClosedRange<LocalDate>>,
    ferieperioder: List<ClosedRange<LocalDate>>
) : SykdomstidslinjeHendelse(hendelseId, Hendelsestype.Inntektsmelding) {

    data class Refusjon(val opphørsdato: LocalDate, val beløpPrMåned: Double, val endringerIRefusjon: List<LocalDate>?)

    private val arbeidsgiverperioder: List<Arbeidsgiverperiode>
    private val ferieperioder: List<Ferieperiode>

    init {
        if (refusjon.beløpPrMåned != beregnetInntekt) aktivitetslogger.error("Beregnet inntekt matcher ikke refusjon pr måned")
        if (aktivitetslogger.hasErrors()) throw aktivitetslogger

        this.arbeidsgiverperioder = arbeidsgiverperioder.sortedBy { it.start }.map { Arbeidsgiverperiode(it.start, it.endInclusive) }
        this.ferieperioder = ferieperioder.map { Ferieperiode(it.start, it.endInclusive) }
    }

    sealed class Periode(
        internal val fom: LocalDate,
        internal val tom: LocalDate
    ) {
        internal abstract fun sykdomstidslinje(inntektsmelding: ModelInntektsmelding): ConcreteSykdomstidslinje

        internal fun ingenOverlappende(other: Periode) =
            maxOf(this.fom, other.fom) > minOf(this.tom, other.tom)

        class Arbeidsgiverperiode(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(inntektsmelding: ModelInntektsmelding) =
                    ConcreteSykdomstidslinje.egenmeldingsdager(fom, tom, inntektsmelding)
        }

        class Ferieperiode(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(inntektsmelding: ModelInntektsmelding) =
                ConcreteSykdomstidslinje.ferie(fom, tom, inntektsmelding)
        }
    }

    internal fun dagsats(dato: LocalDate, grunnbeløp: Grunnbeløp): Int {
        val årssats = min(beregnetInntekt * 12, grunnbeløp.beløp(dato))
        return (årssats / 260).toInt()
    }

    private fun ingenOverlappende() = if (arbeidsgiverperioder.isEmpty()) true else arbeidsgiverperioder
        .sortedBy { it.fom }
        .zipWithNext(Periode::ingenOverlappende)
        .all { it }

    override fun sykdomstidslinje(): ConcreteSykdomstidslinje {
        val arbeidsgivertidslinje = this.arbeidsgiverperioder.takeUnless { it.isEmpty() }?.map { it.sykdomstidslinje(this) }?.reduce { acc, sykdomstidslinje ->
            acc.plus(sykdomstidslinje, ConcreteSykdomstidslinje.Companion::ikkeSykedag)
        }
        val ferietidslinje = this.ferieperioder.takeUnless { it.isEmpty() }?.map { it.sykdomstidslinje(this) }?.reduce(ConcreteSykdomstidslinje::plus)

        return arbeidsgivertidslinje.plus(ferietidslinje) ?: ConcreteSykdomstidslinje.egenmeldingsdag(
            førsteFraværsdag,
            this
        )
    }

    internal fun valider(): Aktivitetslogger {
        if (!ingenOverlappende()) aktivitetslogger.error("Inntektsmelding har overlapp i arbeidsgiverperioder")
        return aktivitetslogger
    }

    override fun kanBehandles() = !valider().hasErrors()

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Inntektsmelding

    override fun rapportertdato() = mottattDato

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    override fun toJson() = "no u" // Should not be part of Model events

    fun harEndringIRefusjon(sisteUtbetalingsdag: LocalDate): Boolean {
        if (refusjon.opphørsdato <= sisteUtbetalingsdag) return true
        return refusjon.endringerIRefusjon?.any { it <= sisteUtbetalingsdag } ?: false
    }
}

private fun ConcreteSykdomstidslinje?.plus(other: ConcreteSykdomstidslinje?): ConcreteSykdomstidslinje? {
    if (other == null) return this
    return this?.plus(other) ?: other
}
