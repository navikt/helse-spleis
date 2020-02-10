package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.ModelInntektsmelding.InntektsmeldingPeriode.Arbeidsgiverperiode
import no.nav.helse.hendelser.ModelInntektsmelding.InntektsmeldingPeriode.Ferieperiode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.tournament.KonfliktskyDagturnering
import java.lang.Double.min
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ModelInntektsmelding(
    hendelseId: UUID,
    private val refusjon: Refusjon?,
    private val orgnummer: String,
    private val fødselsnummer: String,
    private val aktørId: String,
    private val mottattDato: LocalDateTime,
    internal val førsteFraværsdag: LocalDate,
    internal val beregnetInntekt: Double,
    arbeidsgiverperioder: List<Periode>,
    ferieperioder: List<Periode>,
    aktivitetslogger: Aktivitetslogger
) : SykdomstidslinjeHendelse(hendelseId, Hendelsestype.Inntektsmelding, aktivitetslogger) {
    class Refusjon(
        val opphørsdato: LocalDate?,
        val beløpPrMåned: Double,
        val endringerIRefusjon: List<LocalDate> = emptyList()
    )

    private val arbeidsgiverperioder: List<Arbeidsgiverperiode>
    private val ferieperioder: List<Ferieperiode>

    init {
        this.arbeidsgiverperioder =
            arbeidsgiverperioder.sortedBy { it.start }.map { Arbeidsgiverperiode(it.start, it.endInclusive) }
        this.ferieperioder = ferieperioder.map { Ferieperiode(it.start, it.endInclusive) }
    }

    sealed class InntektsmeldingPeriode(
        internal val fom: LocalDate,
        internal val tom: LocalDate
    ) {
        internal abstract fun sykdomstidslinje(inntektsmelding: ModelInntektsmelding): ConcreteSykdomstidslinje

        internal fun ingenOverlappende(other: InntektsmeldingPeriode) =
            maxOf(this.fom, other.fom) > minOf(this.tom, other.tom)

        class Arbeidsgiverperiode(fom: LocalDate, tom: LocalDate) : InntektsmeldingPeriode(fom, tom) {
            override fun sykdomstidslinje(inntektsmelding: ModelInntektsmelding) =
                ConcreteSykdomstidslinje.egenmeldingsdager(fom, tom, Dag.NøkkelHendelseType.Inntektsmelding)
        }

        class Ferieperiode(fom: LocalDate, tom: LocalDate) : InntektsmeldingPeriode(fom, tom) {
            override fun sykdomstidslinje(inntektsmelding: ModelInntektsmelding) =
                ConcreteSykdomstidslinje.ferie(fom, tom, Dag.NøkkelHendelseType.Inntektsmelding)
        }
    }

    internal fun dagsats(dato: LocalDate, grunnbeløp: Grunnbeløp): Int {
        val årssats = min(beregnetInntekt * 12, grunnbeløp.beløp(dato))
        return (årssats / 260).toInt()
    }

    override fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Inntektsmelding")
    }

    private fun ingenOverlappende() = if (arbeidsgiverperioder.isEmpty()) true else arbeidsgiverperioder
        .sortedBy { it.fom }
        .zipWithNext(InntektsmeldingPeriode::ingenOverlappende)
        .all { it }

    override fun sykdomstidslinje() = (ferieperioder + arbeidsgiverperioder)
        .map { it.sykdomstidslinje(this) }
        .sortedBy { it.førsteDag() }
        .takeUnless { it.isEmpty() }
        ?.reduce { acc, tidslinje ->
            acc.plus(tidslinje, ConcreteSykdomstidslinje.Companion::ikkeSykedag, KonfliktskyDagturnering)
        }
        ?: ConcreteSykdomstidslinje.egenmeldingsdag(
            førsteFraværsdag,
            Dag.NøkkelHendelseType.Inntektsmelding
        )

    override fun valider(): Aktivitetslogger {
        if (!ingenOverlappende()) aktivitetslogger.error("Inntektsmelding har overlapp i arbeidsgiverperioder")
        if (refusjon == null) aktivitetslogger.error("Arbeidsgiver forskutterer ikke")
        else if (refusjon.beløpPrMåned != beregnetInntekt) aktivitetslogger.error("Beregnet inntekt ($beregnetInntekt) matcher ikke refusjon pr måned (${refusjon.beløpPrMåned})")
        return aktivitetslogger
    }

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Inntektsmelding

    override fun rapportertdato() = mottattDato

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    override fun accept(visitor: PersonVisitor) {
        visitor.visitInntektsmeldingHendelse(this)
    }

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {
        arbeidsgiver.håndter(this)
    }

    fun harEndringIRefusjon(sisteUtbetalingsdag: LocalDate): Boolean {
        if (refusjon == null) return false
        refusjon.opphørsdato?.also {
            if (it <= sisteUtbetalingsdag) {
                return true
            }
        }
        return refusjon.endringerIRefusjon.any { it <= sisteUtbetalingsdag }
    }
}
