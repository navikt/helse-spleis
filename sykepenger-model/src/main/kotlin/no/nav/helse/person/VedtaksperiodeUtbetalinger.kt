package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.harId
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class VedtaksperiodeUtbetalinger(private val arbeidsgiver: Arbeidsgiver, utbetalinger: List<Utbetaling>) {
    internal constructor(arbeidsgiver: Arbeidsgiver) : this(arbeidsgiver, mutableListOf())

    private val utbetalinger = utbetalinger.toMutableList()
    private val siste get() = utbetalinger.lastOrNull()

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        visitor.preVisitVedtakserperiodeUtbetalinger(utbetalinger)
        utbetalinger.forEach { it.accept(visitor) }
        visitor.postVisitVedtakserperiodeUtbetalinger(utbetalinger)
    }

    private fun forrigeUtbetalte() = utbetalinger.aktive().lastOrNull()

    internal fun harUtbetaling() = siste != null && siste!!.gyldig()
    internal fun trekkerTilbakePenger() = siste?.trekkerTilbakePenger() == true
    private fun erSiste(other: Utbetaling) = siste == other
    internal fun erSiste(other: VedtaksperiodeUtbetalinger) = erSiste(other.siste!!)
    internal fun utbetales() = siste?.erInFlight() == true
    internal fun erAvsluttet() = siste?.erAvsluttet() == true
    internal fun erAvvist() = siste?.erAvvist() == true
    internal fun harUtbetalinger() = siste?.harUtbetalinger() == true
    internal fun harFeilet() = siste?.harFeilet() == true
    internal fun erUtbetalt() = siste?.erUtbetalt() == true
    internal fun erUbetalt() = siste?.erUbetalt() == true
    internal fun kanIkkeForsøkesPåNy() = siste?.kanIkkeForsøkesPåNy() == true

    internal fun kanForkastes(other: List<Utbetaling>) =
        Utbetaling.kanForkastes(utbetalinger, other)
    internal fun harAvsluttede() = utbetalinger.any { it.erAvsluttet() }
    internal fun harUtbetalt() = utbetalinger.any { it.erUtbetalt() }
    internal fun harId(utbetalingId: UUID) = utbetalinger.harId(utbetalingId)
    internal fun hørerIkkeSammenMed(other: Utbetaling) = utbetalinger.lastOrNull { it.gyldig() }?.hørerSammen(other) == false
    internal fun hørerIkkeSammenMed(other: VedtaksperiodeUtbetalinger) = other.siste != null && hørerIkkeSammenMed(other.siste!!)
    internal fun gjelderIkkeFor(hendelse: UtbetalingHendelse) = siste?.gjelderFor(hendelse) != true
    internal fun gjelderIkkeFor(hendelse: Utbetalingsgodkjenning) = siste?.gjelderFor(hendelse) != true

    internal fun erHistorikkEndretSidenBeregning(infotrygdhistorikk: Infotrygdhistorikk) =
        infotrygdhistorikk.harEndretHistorikk(siste!!)

    internal fun reberegnUtbetaling(hendelse: IAktivitetslogg, hvisRevurdering: () -> Unit, hvisUtbetaling: () -> Unit) =
        siste!!.reberegnUtbetaling(hendelse, hvisRevurdering, hvisUtbetaling)

    internal fun forkast(hendelse: IAktivitetslogg) {
        siste?.forkast(hendelse)
    }

    internal fun mottaRevurdering(
        aktivitetslogg: IAktivitetslogg,
        utbetaling: Utbetaling,
        periode: Periode
    ): Utbetalingstidslinje {
        return nyUtbetaling(aktivitetslogg, periode) { utbetaling }
    }

    internal fun lagUtbetaling(
        builder: Utbetaling.Builder,
        vedtaksperiode: Vedtaksperiode,
        organisasjonsnummer: String
    ) {
        builder.vedtaksperiode(vedtaksperiode, organisasjonsnummer, siste)
    }

    internal fun lagUtbetaling(
        fødselsnummer: String,
        periode: Periode,
        maksimumSykepenger: Alder.MaksimumSykepenger,
        hendelse: ArbeidstakerHendelse
    ): Utbetalingstidslinje {
        return nyUtbetaling(hendelse, periode) {
            arbeidsgiver.lagUtbetaling(
                aktivitetslogg = hendelse,
                fødselsnummer = fødselsnummer,
                maksdato = maksimumSykepenger.sisteDag(),
                forbrukteSykedager = maksimumSykepenger.forbrukteDager(),
                gjenståendeSykedager = maksimumSykepenger.gjenståendeDager(),
                periode = periode,
                forrige = siste
            )
        }
    }

    private fun nyUtbetaling(
        hendelse: IAktivitetslogg,
        periode: Periode,
        generator: () -> Utbetaling
    ): Utbetalingstidslinje {
        return generator().also { utbetalinger.add(it) }.utbetalingstidslinje(periode)
    }

    fun lagRevurdering(
        vedtaksperiode: Vedtaksperiode,
        fødselsnummer: String,
        aktivitetslogg: IAktivitetslogg,
        maksimumSykepenger: Alder.MaksimumSykepenger,
        periode: Periode
    ) {
        arbeidsgiver.lagRevurdering(
            vedtaksperiode = vedtaksperiode,
            aktivitetslogg = aktivitetslogg,
            fødselsnummer = fødselsnummer,
            maksdato = maksimumSykepenger.sisteDag(),
            forbrukteSykedager = maksimumSykepenger.forbrukteDager(),
            gjenståendeSykedager = maksimumSykepenger.gjenståendeDager(),
            periode = periode,
            forrige = forrigeUtbetalte()
        )
    }

    internal fun build(builder: VedtakFattetBuilder) {
        siste?.build(builder)
    }

    internal fun overlapperMed(other: VedtaksperiodeUtbetalinger): Boolean {
        if (!this.harUtbetalinger() || !other.harUtbetalinger()) return false
        return this.siste!!.overlapperMed(other.siste!!)
    }

    internal fun valider(simulering: Simulering) = siste!!.valider(simulering)

    internal fun erKlarForGodkjenning() = siste!!.erKlarForGodkjenning()
    internal fun simuler(hendelse: IAktivitetslogg) = siste!!.simuler(hendelse)

    internal fun godkjenning(
        hendelse: IAktivitetslogg,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        førstegangsbehandling: Boolean,
        inntektskilde: Inntektskilde,
        arbeidsforholdId: String?,
        orgnummereMedRelevanteArbeidsforhold: List<String>,
        aktivitetslogg: Aktivitetslogg
    ) {
        siste!!.godkjenning(
            hendelse = hendelse,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            førstegangsbehandling = førstegangsbehandling,
            inntektskilde = inntektskilde,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold,
            arbeidsforholdId = arbeidsforholdId,
            aktivitetslogg = aktivitetslogg
        )
    }
}
