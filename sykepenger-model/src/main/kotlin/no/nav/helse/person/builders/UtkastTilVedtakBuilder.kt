package no.nav.helse.person.builders

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeVisitor
import no.nav.helse.økonomi.Økonomi
import kotlin.properties.Delegates

internal class UtkastTilVedtakBuilder(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val vedtaksperiodeId: UUID,
    private val kanForkastes: Boolean,
    private val erForlengelse: Boolean,
    private val harPeriodeRettFør: Boolean,
    private val arbeidsgiverperiode: Arbeidsgiverperiode?
) {
    private val tags = mutableSetOf<String>()

    internal fun ingenNyArbeidsgiverperiode() = apply { tags.add("IngenNyArbeidsgiverperiode") }
    internal fun grunnbeløpsregulert() = apply { tags.add("Grunnbeløpsregulering") }

    private data class RelevantPeriode(val vedtaksperiodeId: UUID, val behandlingId: UUID, val skjæringstidspunkt: LocalDate, val periode: Periode)
    private val relevantePerioder = mutableSetOf<RelevantPeriode>()
    internal fun relevantPeriode(vedtaksperiodeId: UUID, behandlingId: UUID, skjæringstidspunkt: LocalDate, periode: Periode) = apply {
        relevantePerioder.add(RelevantPeriode(vedtaksperiodeId, behandlingId, skjæringstidspunkt, periode))
    }

    private lateinit var behandlingId: UUID
    internal fun behandlingId(behandlingId: UUID) = apply { this.behandlingId = behandlingId }

    private lateinit var periode: Periode
    internal fun periode(periode: Periode) = apply {
        this.periode = periode
        arbeidsgiverperiode?.berik(this, periode, harPeriodeRettFør)
    }

    private val hendelseIder = mutableSetOf<UUID>()
    internal fun hendelseIder(hendelseIder: Set<UUID>) = apply { this.hendelseIder.addAll(hendelseIder) }

    private lateinit var skjæringstidspunkt: LocalDate
    internal fun skjæringstidspunkt(skjæringstidspunkt: LocalDate) = apply {
        this.skjæringstidspunkt = skjæringstidspunkt
    }

    private var inngangsvilkårFraInfotrygd = false
    internal fun inngangsvilkårFraInfotrygd() = apply { inngangsvilkårFraInfotrygd = true }

    private lateinit var vilkårsgrunnlagId: UUID
    internal fun vilkårsgrunnlagId(vilkårsgrunnlagId: UUID) = apply { this.vilkårsgrunnlagId = vilkårsgrunnlagId }

    // TODO: utbetalingstype
    private lateinit var utbetalingId: UUID
    internal fun utbetaling(utbetaling: Utbetaling) = apply {
        this.utbetalingId = utbetaling.id

        val antallTagsFør = tags.size
        val arbeidsgiverNettoBeløp = utbetaling.arbeidsgiverOppdrag().nettoBeløp()
        val personNettoBeløp = utbetaling.personOppdrag().nettoBeløp()

        if (arbeidsgiverNettoBeløp > 0) tags.add("Arbeidsgiverutbetaling")
        else if (arbeidsgiverNettoBeløp < 0) tags.add("NegativArbeidsgiverutbetaling")

        if (personNettoBeløp > 0) tags.add("Personutbetaling")
        else if (personNettoBeløp < 0) tags.add("NegativPersonutbetaling")

        if (arbeidsgiverNettoBeløp == 0 && personNettoBeløp == 0) tags.add("IngenUtbetaling")

        check(tags.size > antallTagsFør) {
            "arbeidsgiverNettoBeløp=$arbeidsgiverNettoBeløp, personNettoBeløp=$personNettoBeløp burde bli minst én ny tag."
        }
    }

    internal fun utbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje) = apply {
        val behandlingsresultat = UtbetalingstidslinjeInfo(utbetalingstidslinje).behandlingsresultat()
        tags.add(behandlingsresultat)
    }

    private var totalOmregnetÅrsinntekt by Delegates.notNull<Double>()
    internal fun totalOmregnetÅrsinntekt(totalOmregnetÅrsinntekt: Double) = apply { this.totalOmregnetÅrsinntekt = totalOmregnetÅrsinntekt }

    private var seksG by Delegates.notNull<Double>()
    internal fun seksG(seksG: Double) = apply { this.seksG = seksG }

    private data class Arbeidsgiverinntekt(val arbeidsgiver: String, val omregnedeÅrsinntekt: Double, val skjønnsfastsatt: Double?, val gjelder: Periode)
    private val arbeidsgiverinntekter = mutableSetOf<Arbeidsgiverinntekt>()
    internal fun arbeidsgiverinntekt(arbeidsgiver: String, omregnedeÅrsinntekt: Double, skjønnsfastsatt: Double?, gjelder: Periode) {
        arbeidsgiverinntekter.add(Arbeidsgiverinntekt(arbeidsgiver, omregnedeÅrsinntekt, skjønnsfastsatt, gjelder))
    }

    private class UtbetalingstidslinjeInfo(utbetalingstidslinje: Utbetalingstidslinje): UtbetalingstidslinjeVisitor {
        init { utbetalingstidslinje.accept(this) }

        private var avvistDag = false
        private var navDag = false

        override fun visit(dag: Utbetalingsdag.AvvistDag, dato: LocalDate, økonomi: Økonomi) { avvistDag = true }

        override fun visit(dag: Utbetalingsdag.ForeldetDag, dato: LocalDate, økonomi: Økonomi) { avvistDag = true }

        override fun visit(dag: Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) { navDag = true }

        fun behandlingsresultat() = when {
            !navDag -> "Avslag"
            navDag && avvistDag -> "DelvisInnvilget"
            else -> "Innvilget"
        }
    }

    internal fun sammenlign(godkjenningsbehov: Map<String, Any>) {}
}