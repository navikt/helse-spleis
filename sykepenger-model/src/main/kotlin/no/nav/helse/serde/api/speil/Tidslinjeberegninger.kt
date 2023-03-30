package no.nav.helse.serde.api.speil

import no.nav.helse.serde.api.speil.builders.BeregningId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.serde.api.dto.AvvistDag
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.serde.api.dto.SammenslåttDag
import no.nav.helse.serde.api.dto.SpeilOppdrag
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.dto.Utbetalingstidslinjedag
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.serde.api.speil.builders.IVilkårsgrunnlag
import no.nav.helse.serde.api.speil.builders.OppdragBuilder
import no.nav.helse.serde.api.speil.builders.UtbetalingstidslinjeBuilder
import no.nav.helse.serde.api.speil.builders.VurderingBuilder
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype

internal class Tidslinjeberegninger {
    private val sykdomstidslinjer = mutableMapOf<UUID, List<Sykdomstidslinjedag>>()
    private val opphørteUtbetalinger = mutableSetOf<UUID>()

    private val beregningklosser = mutableListOf<Beregningkloss>()
    private val utbetalingklosser = mutableListOf<Utbetalingkloss>()
    private val vedtaksperioderklosser = mutableListOf<Vedtaksperiodekloss>()

    internal fun build(): List<IVedtaksperiode> {
        val beregninger = beregningklosser.map { it.tilTidslinjeberegning(sykdomstidslinjer) }
        val utbetalinger = utbetalingklosser.mapNotNull { it.tilUtbetaling(beregninger, opphørteUtbetalinger) }
        return vedtaksperioderklosser.mapNotNull { it.tilVedtakseriode(utbetalinger) }
    }

    internal fun leggTil(beregningId: UUID, sykdomshistorikkElementId: UUID) {
        beregningklosser.add(Beregningkloss(beregningId, sykdomshistorikkElementId))
    }

    internal fun leggTil(sykdomshistorikkElementId: UUID, tidslinje: List<Sykdomstidslinjedag>) {
        sykdomstidslinjer.putIfAbsent(sykdomshistorikkElementId, tidslinje)
    }

    internal fun leggTilUtbetaling(
        internutbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        utbetalingstatus: Utbetalingstatus,
        tidsstempel: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        beregningId: UUID,
        annulleringer: Set<UUID>
    ) {
        if (utbetalingstatus == Utbetalingstatus.IKKE_GODKJENT) return
        opphørteUtbetalinger.addAll(annulleringer)
        nyUtbetaling(
            internutbetaling = internutbetaling,
            id = id,
            korrelasjonsId = korrelasjonsId,
            type = type,
            utbetalingstatus = utbetalingstatus,
            tidsstempel = tidsstempel,
            arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
            personNettoBeløp = personNettoBeløp,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            beregningId = beregningId
        )
    }
    internal fun leggTilRevurdering(
        internutbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        utbetalingstatus: Utbetalingstatus,
        tidsstempel: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        beregningId: UUID,
        annulleringer: Set<UUID>
    ) {
        opphørteUtbetalinger.addAll(annulleringer)
        nyUtbetaling(
            internutbetaling = internutbetaling,
            id = id,
            korrelasjonsId = korrelasjonsId,
            type = type,
            utbetalingstatus = utbetalingstatus,
            tidsstempel = tidsstempel,
            arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
            personNettoBeløp = personNettoBeløp,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            beregningId = beregningId
        )
    }

    internal fun leggTilAnnullering(
        internutbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        utbetalingstatus: Utbetalingstatus,
        tidsstempel: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        beregningId: UUID
    ) {
        nyUtbetaling(
            internutbetaling = internutbetaling,
            id = id,
            korrelasjonsId = korrelasjonsId,
            type = type,
            utbetalingstatus = utbetalingstatus,
            tidsstempel = tidsstempel,
            arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
            personNettoBeløp = personNettoBeløp,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            beregningId = beregningId
        )
    }

    private fun nyUtbetaling(
        internutbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        utbetalingstatus: Utbetalingstatus,
        tidsstempel: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        beregningId: UUID
    ) {
        // tar ikke vare på forkastede utbetalinger i det hele tatt
        if (utbetalingstatus == Utbetalingstatus.FORKASTET) return
        val oppdragBuilder = OppdragBuilder(internutbetaling)
        utbetalingklosser.add(Utbetalingkloss(
            id = id,
            korrelasjonsId = korrelasjonsId,
            beregningId = beregningId,
            opprettet = tidsstempel,
            utbetalingstidslinje = UtbetalingstidslinjeBuilder(internutbetaling).build(),
            maksdato = maksdato,
            gjenståendeSykedager = gjenståendeSykedager,
            forbrukteSykedager = forbrukteSykedager,
            type = type.toString(),
            tilstand = utbetalingstatus.tilstandsnavn(),
            arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
            personNettoBeløp = personNettoBeløp,
            arbeidsgiverFagsystemId = oppdragBuilder.arbeidsgiverFagsystemId(),
            personFagsystemId = oppdragBuilder.personFagsystemId(),
            vurdering = VurderingBuilder(internutbetaling).build(),
            oppdrag = oppdragBuilder.oppdrag()
        ))
    }

    fun leggTilVedtaksperiode(
        vedtaksperiode: UUID,
        forkastet: Boolean,
        fom: LocalDate,
        tom: LocalDate,
        inntektskilde: Inntektskilde,
        hendelser: List<HendelseDTO>,
        utbetalinger: List<Pair<UUID, IVilkårsgrunnlag>>,
        periodetype: Periodetype,
        sykdomstidslinje: List<Sykdomstidslinjedag>,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        skjæringstidspunkt: LocalDate,
        aktivitetsloggForPeriode: Aktivitetslogg
    ) {
        vedtaksperioderklosser.add(Vedtaksperiodekloss(
            vedtaksperiodeId = vedtaksperiode,
            forkastet = forkastet,
            fom = fom,
            tom = tom,
            inntektskilde = inntektskilde,
            hendelser = hendelser,
            vedtaksperiodeutbetalinger = utbetalinger,
            periodetype = periodetype,
            sykdomstidslinje = sykdomstidslinje,
            opprettet = opprettet,
            oppdatert = oppdatert,
            tilstand = tilstand,
            skjæringstidspunkt = skjæringstidspunkt,
            aktivitetsloggForPeriode = aktivitetsloggForPeriode
        ))
    }

    private class Vedtaksperiodekloss(
        private val vedtaksperiodeId: UUID,
        private val forkastet: Boolean,
        private val fom: LocalDate,
        private val tom: LocalDate,
        private val inntektskilde: Inntektskilde,
        private val hendelser: List<HendelseDTO>,
        private val vedtaksperiodeutbetalinger: List<Pair<UUID, IVilkårsgrunnlag>>,
        private val periodetype: Periodetype,
        private val sykdomstidslinje: List<Sykdomstidslinjedag>,
        private val opprettet: LocalDateTime,
        private val oppdatert: LocalDateTime,
        private val tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        private val skjæringstidspunkt: LocalDate,
        private val aktivitetsloggForPeriode: Aktivitetslogg
    ) {
        internal fun tilVedtakseriode(utbetalinger: List<IUtbetaling>): IVedtaksperiode? {
            val sammenkobletUtbetalinger = vedtaksperiodeutbetalinger
                .mapNotNull { (utbetalingId, vilkårsgrunnlag) -> utbetalinger.singleOrNull { it.id == utbetalingId }?.let { vilkårsgrunnlag to it } }

            // legger til evt. annulleringer
            val annulleringerForVedtaksperioden = sammenkobletUtbetalinger
                .groupBy { it.second.korrelasjonsId }
                .mapNotNull { (_, utbetalingene) ->
                    // mapper annullering til å peke på samme vilkårsgrunnlag som siste utbetaling
                    val sisteVilkårsgrunnlagId = utbetalingene.last().first
                    utbetalinger.firstOrNull { it.annulleringFor(utbetalingene.first().second) }?.let {
                        sisteVilkårsgrunnlagId to it
                    }
                }

            // beholder ikke vedtaksperioder som er forkastet og ikke annullert
            if (forkastet && annulleringerForVedtaksperioden.isEmpty()) return null

            return IVedtaksperiode(
                vedtaksperiodeId = vedtaksperiodeId,
                forkastet = forkastet,
                fom = fom,
                tom = tom,
                inntektskilde = inntektskilde,
                hendelser = hendelser,
                utbetalinger = sammenkobletUtbetalinger + annulleringerForVedtaksperioden,
                periodetype = periodetype,
                sykdomstidslinje = sykdomstidslinje,
                opprettet = opprettet,
                oppdatert = oppdatert,
                tilstand = tilstand,
                skjæringstidspunkt = skjæringstidspunkt,
                aktivitetsloggForPeriode = aktivitetsloggForPeriode
            )
        }
    }

    private class Beregningkloss(
        private val beregningId: BeregningId,
        private val sykdomshistorikkElementId: UUID
    ) {
        internal fun tilTidslinjeberegning(sykdomstidslinjer: MutableMap<UUID, List<Sykdomstidslinjedag>>) =
            ITidslinjeberegning(beregningId, sykdomstidslinjer[sykdomshistorikkElementId] ?: error("Finner ikke tidslinjeberegning for beregningId'en! Hvordan kan det skje?"))
    }

    private class Utbetalingkloss(
        private val id: UUID,
        private val korrelasjonsId: UUID,
        private val beregningId: BeregningId,
        private val opprettet: LocalDateTime,
        private val utbetalingstidslinje: List<Utbetalingstidslinjedag>,
        private val maksdato: LocalDate,
        private val gjenståendeSykedager: Int?,
        private val forbrukteSykedager: Int?,
        private val type: String,
        private val tilstand: String,
        private val arbeidsgiverNettoBeløp: Int,
        private val personNettoBeløp: Int,
        private val arbeidsgiverFagsystemId: String,
        private val personFagsystemId: String,
        private val vurdering: no.nav.helse.serde.api.dto.Utbetaling.Vurdering?,
        private val oppdrag: Map<String, SpeilOppdrag>
    ) {
        internal fun tilUtbetaling(tidslinjeberegninger: List<ITidslinjeberegning>, opphørteUtbetalinger: MutableSet<UUID>): IUtbetaling? {
            if (this.id in opphørteUtbetalinger) return null
            return IUtbetaling(
                id = id,
                korrelasjonsId = korrelasjonsId,
                beregning = tidslinjeberegninger.single { it.beregningId == beregningId },
                opprettet = opprettet,
                utbetalingstidslinje = utbetalingstidslinje,
                maksdato = maksdato,
                gjenståendeSykedager = gjenståendeSykedager,
                forbrukteSykedager = forbrukteSykedager,
                type = type,
                tilstand = tilstand,
                arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
                personNettoBeløp = personNettoBeløp,
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                vurdering = vurdering,
                oppdrag = oppdrag
            )
        }
    }

    internal class ITidslinjeberegning(
        internal val beregningId: BeregningId,
        private val sykdomstidslinje: List<Sykdomstidslinjedag>
    ) {
        fun sammenslåttTidslinje(utbetalingstidslinje: List<Utbetalingstidslinjedag>, fom: LocalDate, tom: LocalDate): List<SammenslåttDag> {
            return sykdomstidslinje
                .subset(fom, tom)
                .merge(utbetalingstidslinje)
        }

        private fun List<Sykdomstidslinjedag>.subset(fom: LocalDate, tom: LocalDate) = this.filter { it.dagen in fom..tom }
    }
}

internal fun List<Sykdomstidslinjedag>.merge(utbetalingstidslinje: List<Utbetalingstidslinjedag>): List<SammenslåttDag> {

    fun begrunnelser(utbetalingsdag: Utbetalingstidslinjedag) =
        if (utbetalingsdag is AvvistDag) utbetalingsdag.begrunnelser else null

    return map { sykdomsdag ->
        val utbetalingsdag = utbetalingstidslinje.find { it.dato.isEqual(sykdomsdag.dagen) }
        SammenslåttDag(
            sykdomsdag.dagen,
            sykdomsdag.type,
            utbetalingsdag?.type ?: UtbetalingstidslinjedagType.UkjentDag,
            kilde = sykdomsdag.kilde,
            grad = sykdomsdag.grad,
            utbetalingsinfo = utbetalingsdag?.utbetalingsinfo(),
            begrunnelser = utbetalingsdag?.let { begrunnelser(it) }
        )
    }
}
