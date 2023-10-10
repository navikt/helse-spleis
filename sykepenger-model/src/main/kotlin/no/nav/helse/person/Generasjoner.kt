package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.person.aktivitetslogg.GodkjenningsbehovBuilder
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.harId
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal class Generasjoner(generasjoner: List<Generasjon>) {
    internal constructor() : this(mutableListOf())

    private val utbetalingene get() = generasjoner.map(Generasjon::utbetaling)
    private val generasjoner = generasjoner.toMutableList()
    private val siste get() = generasjoner.lastOrNull()?.utbetaling

    internal fun accept(visitor: GenerasjonerVisistor) {
        visitor.preVisitGenerasjoner(generasjoner)
        generasjoner.forEach { generasjon ->
            generasjon.accept(visitor)
        }
        visitor.postVisitGenerasjoner(generasjoner)
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
    internal fun harAvsluttede() = generasjoner.any { generasjon -> generasjon.utbetaling.erAvsluttet() }
    internal fun harId(utbetalingId: UUID) = utbetalingene.harId(utbetalingId)
    internal fun hørerIkkeSammenMed(other: Utbetaling) = generasjoner.lastOrNull { generasjon  -> generasjon.utbetaling.gyldig() }?.utbetaling?.hørerSammen(other) == false
    internal fun hørerIkkeSammenMed(other: Generasjoner) = other.siste != null && hørerIkkeSammenMed(other.siste!!)
    internal fun gjelderIkkeFor(hendelse: UtbetalingHendelse) = siste?.gjelderFor(hendelse) != true

    internal fun lagreTidsnæreInntekter(
        arbeidsgiver: Arbeidsgiver,
        skjæringstidspunkt: LocalDate,
        hendelse: IAktivitetslogg,
        oppholdsperiodeMellom: Periode?
    ) {
        generasjoner.lastOrNull()?.lagreTidsnæreInntekter(skjæringstidspunkt, arbeidsgiver, hendelse, oppholdsperiodeMellom)
    }

    internal fun gjelderIkkeFor(hendelse: Utbetalingsgodkjenning) = siste?.gjelderFor(hendelse) != true

    internal fun erHistorikkEndretSidenBeregning(infotrygdhistorikk: Infotrygdhistorikk) =
        infotrygdhistorikk.harEndretHistorikk(siste!!)

    internal fun forkast(hendelse: IAktivitetslogg) {
        siste?.forkast(hendelse)
    }

    internal fun build(builder: VedtakFattetBuilder) {
        if (!harUtbetaling()) return
        siste?.build(builder)
    }

    internal fun overlapperMed(other: Generasjoner): Boolean {
        if (!this.harUtbetalinger() || !other.harUtbetalinger()) return false
        return this.siste!!.overlapperMed(other.siste!!)
    }

    internal fun valider(simulering: Simulering) {
        siste!!.valider(simulering)
    }

    internal fun erKlarForGodkjenning() = siste!!.erKlarForGodkjenning()

    internal fun simuler(hendelse: IAktivitetslogg) = siste!!.simuler(hendelse)

    internal fun godkjenning(hendelse: IAktivitetslogg, builder: GodkjenningsbehovBuilder) {
        siste!!.godkjenning(hendelse, builder)
    }

    internal fun nyUtbetaling(
        vedtaksperiodeSomLagerUtbetaling: UUID,
        fødselsnummer: String,
        arbeidsgiver: Arbeidsgiver,
        arbeidsgiverSomBeregner: Arbeidsgiver,
        sykdomstidslinje: Sykdomstidslinje,
        periode: Periode,
        hendelse: IAktivitetslogg,
        grunnlagsdata: VilkårsgrunnlagElement,
        maksimumSykepenger: Alder.MaksimumSykepenger,
        utbetalingstidslinje: Utbetalingstidslinje
    ): Utbetalingstidslinje {
        val strategi = if (this.harAvsluttede()) Arbeidsgiver::lagRevurdering else Arbeidsgiver::lagUtbetaling
        val denNyeUtbetalingen = strategi(arbeidsgiver, hendelse, fødselsnummer, arbeidsgiverSomBeregner, utbetalingstidslinje, maksimumSykepenger.sisteDag(), maksimumSykepenger.forbrukteDager(), maksimumSykepenger.gjenståendeDager(), periode)
        denNyeUtbetalingen.nyVedtaksperiodeUtbetaling(vedtaksperiodeSomLagerUtbetaling)
        generasjoner.add(Generasjon(grunnlagsdata, denNyeUtbetalingen, sykdomstidslinje))
        return utbetalingstidslinje.subset(periode)
    }

    internal class Generasjon(
        private val id: UUID,
        private val tidsstempel: LocalDateTime,
        private val grunnlagsdata: VilkårsgrunnlagElement,
        val utbetaling: Utbetaling,
        private val sykdomstidslinje: Sykdomstidslinje
    ) {
        constructor(grunnlagsdata: VilkårsgrunnlagElement, utbetaling: Utbetaling, sykdomstidslinje: Sykdomstidslinje) : this(UUID.randomUUID(), LocalDateTime.now(), grunnlagsdata, utbetaling, sykdomstidslinje)

        fun accept(visitor: GenerasjonerVisistor) {
            visitor.preVisitGenerasjon(id, tidsstempel, grunnlagsdata, utbetaling, sykdomstidslinje)
            grunnlagsdata.accept(visitor)
            utbetaling.accept(visitor)
            sykdomstidslinje.accept(visitor)
            visitor.postVisitGenerasjon(id, tidsstempel, grunnlagsdata, utbetaling, sykdomstidslinje)
        }

        fun lagreTidsnæreInntekter(
            nyttSkjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            hendelse: IAktivitetslogg,
            oppholdsperiodeMellom: Periode?
        ) {
            grunnlagsdata.lagreTidsnæreInntekter(nyttSkjæringstidspunkt, arbeidsgiver, hendelse, oppholdsperiodeMellom)
        }
    }
}