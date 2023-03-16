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
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.harId
import no.nav.helse.utbetalingslinjer.utbetalingport
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class VedtaksperiodeUtbetalinger(utbetalinger: List<Pair<VilkårsgrunnlagElement, Utbetaling>>) {
    internal constructor() : this(mutableListOf())

    private val utbetalingene get() = utbetalinger.map(Pair<*, Utbetaling>::second)
    private val utbetalinger = utbetalinger.toMutableList()
    private val sisteVilkårsgrunnlag get() = utbetalinger.lastOrNull()?.first
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

    internal fun harUtbetaling() = siste != null && siste!!.gyldig()
    internal fun trekkerTilbakePenger() = siste?.trekkerTilbakePenger() == true
    internal fun utbetales() = siste?.erInFlight() == true
    internal fun erAvsluttet() = siste?.erAvsluttet() == true
    internal fun erAvvist() = siste?.erAvvist() == true
    internal fun harUtbetalinger() = siste?.harUtbetalinger() == true
    internal fun erUtbetalt() = siste?.erUtbetalt() == true
    internal fun erUbetalt() = siste?.erUbetalt() == true

    internal fun kanForkastes(arbeidsgiverUtbetalinger: List<Utbetaling>) =
        Utbetaling.kanForkastes(utbetalingene, arbeidsgiverUtbetalinger)
    internal fun harAvsluttede() = utbetalinger.any { (_, utbetaling) -> utbetaling.erAvsluttet() }
    internal fun harId(utbetalingId: UUID) = utbetalingene.harId(utbetalingId)
    internal fun hørerIkkeSammenMed(other: Utbetaling) = utbetalinger.lastOrNull { (_, utbetaling) -> utbetaling.gyldig() }?.second?.hørerSammen(other) == false
    internal fun hørerIkkeSammenMed(other: VedtaksperiodeUtbetalinger) = other.siste != null && hørerIkkeSammenMed(other.siste!!)
    internal fun gjelderIkkeFor(hendelse: UtbetalingHendelse) = siste?.gjelderFor(hendelse.utbetalingport()) != true

    internal fun lagreTidsnæreInntekter(arbeidsgiver: Arbeidsgiver, skjæringstidspunkt: LocalDate) {
        val forrige = sisteVilkårsgrunnlag ?: return
        forrige.lagreTidsnæreInntekter(skjæringstidspunkt, arbeidsgiver)
    }

    internal fun gjelderIkkeFor(hendelse: Utbetalingsgodkjenning) = siste?.gjelderFor(hendelse) != true

    internal fun erHistorikkEndretSidenBeregning(infotrygdhistorikk: Infotrygdhistorikk) =
        infotrygdhistorikk.harEndretHistorikk(siste!!)

    internal fun forkast(hendelse: IAktivitetslogg) {
        siste?.forkast(hendelse)
    }

    internal fun nyUtbetaling(
        vedtaksperiodeId: UUID,
        grunnlagsdata: VilkårsgrunnlagElement,
        periode: Periode,
        utbetaling: Utbetaling
    ): Utbetalingstidslinje {
        check(utbetaling !== siste) { "kan ikke legge til lik utbetaling som forrige" }
        utbetaling.nyVedtaksperiodeUtbetaling(vedtaksperiodeId)
        utbetalinger.add(grunnlagsdata to utbetaling)
        return utbetaling.utbetalingstidslinje(periode)
    }

    internal fun build(builder: VedtakFattetBuilder) {
        siste?.build(builder)
    }

    internal fun overlapperMed(other: VedtaksperiodeUtbetalinger): Boolean {
        if (!this.harUtbetalinger() || !other.harUtbetalinger()) return false
        return this.siste!!.overlapperMed(other.siste!!)
    }

    internal fun valider(simulering: Simulering) {
        siste!!.valider(simulering)
    }

    internal fun erKlarForGodkjenning() = siste!!.erKlarForGodkjenning()

    internal fun simuler(hendelse: IAktivitetslogg) = siste!!.simuler(hendelse)

    internal fun godkjenning(
        hendelse: IAktivitetslogg,
        periode: Periode,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        førstegangsbehandling: Boolean,
        inntektskilde: Inntektskilde,
        orgnummereMedRelevanteArbeidsforhold: List<String>
    ) {
        siste!!.godkjenning(
            hendelse = hendelse,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype.tilUtbetalingPeriodetype(),
            førstegangsbehandling = førstegangsbehandling,
            inntektskilde = inntektskilde.tilUtbetalingInntektskilde(),
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold
        )
    }
}
