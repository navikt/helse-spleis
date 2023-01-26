package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.harId
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class VedtaksperiodeUtbetalinger(private val arbeidsgiver: Arbeidsgiver, utbetalinger: List<Pair<VilkårsgrunnlagElement, Utbetaling>>) {
    internal constructor(arbeidsgiver: Arbeidsgiver) : this(arbeidsgiver, mutableListOf())

    private val utbetalingene get() = utbetalinger.map(Pair<*, Utbetaling>::second)
    private val utbetalinger = utbetalinger.toMutableList()
    private val siste get() = utbetalinger.lastOrNull()?.second

    internal fun accept(visitor: VedtaksperiodeUtbetalingVisitor) {
        visitor.preVisitVedtakserperiodeUtbetalinger(utbetalinger)
        utbetalinger.forEach { (grunnlagsdata, utbetaling) ->
            visitor.preVisitVedtaksperiodeUtbetaling(grunnlagsdata, utbetaling)
            grunnlagsdata.accept(visitor)
            utbetaling.accept(visitor)
            visitor.postVisitVedtaksperiodeUtbetaling(grunnlagsdata, utbetaling)
        }
        visitor.postVisitVedtakserperiodeUtbetalinger(utbetalinger)
    }

    private fun forrigeUtbetalte() = utbetalingene.aktive().lastOrNull()

    internal fun harUtbetaling() = siste != null && siste!!.gyldig()
    internal fun trekkerTilbakePenger() = siste?.trekkerTilbakePenger() == true
    internal fun utbetales() = siste?.erInFlight() == true
    internal fun erAvsluttet() = siste?.erAvsluttet() == true
    internal fun erAvvist() = siste?.erAvvist() == true
    internal fun harUtbetalinger() = siste?.harUtbetalinger() == true
    internal fun harFeilet() = siste?.harFeilet() == true
    internal fun erUtbetalt() = siste?.erUtbetalt() == true
    internal fun erUbetalt() = siste?.erUbetalt() == true
    internal fun kanIkkeForsøkesPåNy() = siste?.kanIkkeForsøkesPåNy() == true

    internal fun kanForkastes(arbeidsgiverUtbetalinger: List<Utbetaling>) =
        Utbetaling.kanForkastes(utbetalingene, arbeidsgiverUtbetalinger)
    internal fun harAvsluttede() = utbetalinger.any { (_, utbetaling) -> utbetaling.erAvsluttet() }
    internal fun harId(utbetalingId: UUID) = utbetalingene.harId(utbetalingId)
    internal fun hørerIkkeSammenMed(other: Utbetaling) = utbetalinger.lastOrNull { (_, utbetaling) -> utbetaling.gyldig() }?.second?.hørerSammen(other) == false
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
        vedtaksperiodeId: UUID,
        grunnlagsdata: VilkårsgrunnlagElement,
        utbetaling: Utbetaling,
        periode: Periode
    ): Utbetalingstidslinje {
        return nyUtbetaling(vedtaksperiodeId, grunnlagsdata, periode) { utbetaling }
    }

    internal fun lagUtbetaling(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        periode: Periode,
        grunnlagsdata: VilkårsgrunnlagElement,
        maksimumSykepenger: Alder.MaksimumSykepenger,
        hendelse: ArbeidstakerHendelse
    ): Utbetalingstidslinje {
        return nyUtbetaling(vedtaksperiodeId, grunnlagsdata, periode) {
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
        vedtaksperiodeId: UUID,
        grunnlagsdata: VilkårsgrunnlagElement,
        periode: Periode,
        generator: () -> Utbetaling
    ): Utbetalingstidslinje {
        return generator().also {
            it.nyVedtaksperiodeUtbetaling(vedtaksperiodeId)
            utbetalinger.add(grunnlagsdata to it)
        }.utbetalingstidslinje(periode)
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
        orgnummereMedRelevanteArbeidsforhold: List<String>
    ) {
        siste!!.godkjenning(
            hendelse = hendelse,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            førstegangsbehandling = førstegangsbehandling,
            inntektskilde = inntektskilde,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold,
            arbeidsforholdId = arbeidsforholdId
        )
    }
}
