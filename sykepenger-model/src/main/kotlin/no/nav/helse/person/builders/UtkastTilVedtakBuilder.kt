package no.nav.helse.person.builders

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.PersonObserver
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingtype.REVURDERING
import no.nav.helse.utbetalingslinjer.Utbetalingtype.UTBETALING
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeVisitor
import no.nav.helse.økonomi.Økonomi
import org.slf4j.LoggerFactory
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
    internal fun inngangsvilkårFraInfotrygd() = apply {
        tags.add("InngangsvilkårFraInfotrygd")
        inngangsvilkårFraInfotrygd = true
    }

    private lateinit var vilkårsgrunnlagId: UUID
    internal fun vilkårsgrunnlagId(vilkårsgrunnlagId: UUID) = apply { this.vilkårsgrunnlagId = vilkårsgrunnlagId }

    private lateinit var utbetalingId: UUID
    private var revurdering by Delegates.notNull<Boolean>()
    internal fun utbetaling(utbetaling: Utbetaling) = apply {
        this.utbetalingId = utbetaling.id

        check(utbetaling.type == UTBETALING || utbetaling.type == REVURDERING) {
            "Utkast til vedtak på ${utbetaling.type.name}? Det kan jo ikke være rett."
        }

        this.revurdering = utbetaling.type == REVURDERING

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
    private var seksGBegrenset by Delegates.notNull<Boolean>()
    internal fun seksG(seksG: Double, begrenset: Boolean) = apply {
        this.seksG = seksG
        this.seksGBegrenset = begrenset
    }

    internal fun sykepengegrunnlagUnder2G() {
        tags.add("SykepengegrunnlagUnder2G")
    }

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

    private val build by lazy { Build() }

    internal fun sammenlign(godkjenningsbehov: Map<String, Any>) {
        try {
            if ("$godkjenningsbehov" == "${build.godkjenningsbehov}") return
            sikkerlogg.warn("Disse godkjenningsbehovene var jo ikke helt like:\nDagens:\n\t$godkjenningsbehov\nNytt:\n\t${build.godkjenningsbehov}")
        } catch (exception: Exception) {
            sikkerlogg.error("Nei, nå gikk det over stokk og styr med nytt godkjenningsbehov:\nDagens:\n\t$godkjenningsbehov", exception)
        }
    }

    internal fun sammenlign(utkastTilVedtak: PersonObserver.UtkastTilVedtakEvent) {
        try {
            if (utkastTilVedtak == build.utkastTilVedtak) return
            sikkerlogg.warn("Disse utkastene tilvedtak var jo ikke helt like:\nDagens:\n\t$utkastTilVedtak\nNytt:\n\t${build.utkastTilVedtak}")
        } catch (exception: Exception) {
            sikkerlogg.error("Nei, nå gikk det over stokk og styr med nytt utkast til vedtak:\nDagens:\n\t$utkastTilVedtak", exception)
        }
    }

    private inner class Build {
        private val arbeidsgiverinntekterPåSkjæringstidspunktet = arbeidsgiverinntekter.filter { it.gjelder.start == skjæringstidspunkt }
        private val skjønnsmessigeFastsattArbeidsgiverinntekterPåSkjæringstidspunktet = arbeidsgiverinntekterPåSkjæringstidspunktet.filter { it.skjønnsfastsatt != null }
        private val perioderMedSammeSkjæringstidspunkt = relevantePerioder.filter { it.skjæringstidspunkt == skjæringstidspunkt }

        // Til ettertanke: Nå tagges flere arbeidsgivere også ved tilkommen. Er det gæli?
        // Men beholder den tolkningen om det er bøgg eller ei.
        // Om vi her bytter til arbeidsgiverinntekterPåSkjæringstidspunktet.size så tar vi ikke med tilkommen.
        private val enArbeidsgiver = arbeidsgiverinntekter.size == 1

        init {
            check(arbeidsgiverinntekterPåSkjæringstidspunktet.isNotEmpty()) {
                "Forventet ikke at arbeidsgiverinntekterPåSkjæringstidspunktet er en tom liste"
            }
            check(emptyList<Arbeidsgiverinntekt>() == skjønnsmessigeFastsattArbeidsgiverinntekterPåSkjæringstidspunktet || arbeidsgiverinntekterPåSkjæringstidspunktet == skjønnsmessigeFastsattArbeidsgiverinntekterPåSkjæringstidspunktet) {
                "Enten må ingen eller alle arbeidsgiverinntekter på skjæringstidspunktet være skjønnsmessig fastsatt"
            }
            if (erForlengelse) tags.add("Forlengelse")
            else tags.add("Førstegangsbehandling")

            if (arbeidsgiverinntekter.any { it.gjelder.start > skjæringstidspunkt }) tags.add("TilkommenInntekt")

            if (enArbeidsgiver) tags.add("EnArbeidsgiver")
            else tags.add("FlereArbeidsgivere")

            if (!inngangsvilkårFraInfotrygd) {
                if (seksGBegrenset) tags.add("6GBegrenset")
            }
        }

        private val sykepengegrunnlagsfakta: PersonObserver.UtkastTilVedtakEvent.Sykepengegrunnlagsfakta = when {
            inngangsvilkårFraInfotrygd -> PersonObserver.UtkastTilVedtakEvent.FastsattIInfotrygd(totalOmregnetÅrsinntekt)
            skjønnsmessigeFastsattArbeidsgiverinntekterPåSkjæringstidspunktet.isEmpty() -> PersonObserver.UtkastTilVedtakEvent.FastsattEtterHovedregel(
                omregnetÅrsinntekt = totalOmregnetÅrsinntekt,
                `6G`= seksG,
                arbeidsgivere = arbeidsgiverinntekterPåSkjæringstidspunktet.map { PersonObserver.UtkastTilVedtakEvent.FastsattEtterHovedregel.Arbeidsgiver(
                    arbeidsgiver = it.arbeidsgiver,
                    omregnetÅrsinntekt = it.omregnedeÅrsinntekt
                )}
            )
            else -> PersonObserver.UtkastTilVedtakEvent.FastsattEtterSkjønn(
                omregnetÅrsinntekt = totalOmregnetÅrsinntekt,
                `6G`= seksG,
                arbeidsgivere = arbeidsgiverinntekterPåSkjæringstidspunktet.map { PersonObserver.UtkastTilVedtakEvent.FastsattEtterSkjønn.Arbeidsgiver(
                    arbeidsgiver = it.arbeidsgiver,
                    omregnetÅrsinntekt = it.omregnedeÅrsinntekt,
                    skjønnsfastsatt = it.skjønnsfastsatt!!
                )}
            )
        }

        private val periodetype = when (erForlengelse) {
            true -> when (inngangsvilkårFraInfotrygd) {
                true -> "INFOTRYGDFORLENGELSE"
                else -> "FORLENGELSE"
            }
            false -> when (inngangsvilkårFraInfotrygd) {
                true -> "OVERGANG_FRA_IT"
                else -> "FØRSTEGANGSBEHANDLING"
            }
        }

        val godkjenningsbehov = mapOf(
            "periodeFom" to periode.start.toString(),
            "periodeTom" to periode.endInclusive.toString(),
            "skjæringstidspunkt" to skjæringstidspunkt.toString(),
            "vilkårsgrunnlagId" to vilkårsgrunnlagId.toString(),
            "periodetype" to periodetype,
            "førstegangsbehandling" to !erForlengelse,
            "utbetalingtype" to if (revurdering) "REVURDERING" else "UTBETALING",
            "inntektskilde" to if (enArbeidsgiver) "EN_ARBEIDSGIVER" else "FLERE_ARBEIDSGIVERE",
            "orgnummereMedRelevanteArbeidsforhold" to arbeidsgiverinntekter.map { it.arbeidsgiver },
            "tags" to tags.sorted(),
            "kanAvvises" to kanForkastes,
            "omregnedeÅrsinntekter" to arbeidsgiverinntekterPåSkjæringstidspunktet.map {
                mapOf("organisasjonsnummer" to it.arbeidsgiver, "beløp" to it.omregnedeÅrsinntekt)
            },
            "behandlingId" to behandlingId.toString(),
            "hendelser" to hendelseIder,
            "perioderMedSammeSkjæringstidspunkt" to perioderMedSammeSkjæringstidspunkt.map {
                mapOf(
                    "vedtaksperiodeId" to it.vedtaksperiodeId.toString(),
                    "behandlingId" to it.behandlingId.toString(),
                    "fom" to it.periode.start.toString(),
                    "tom" to it.periode.endInclusive.toString()
                )
            },
            "sykepengegrunnlagsfakta" to when (sykepengegrunnlagsfakta) {
                is PersonObserver.UtkastTilVedtakEvent.FastsattIInfotrygd -> mapOf(
                    "omregnetÅrsinntektTotalt" to totalOmregnetÅrsinntekt,
                    "fastsatt" to sykepengegrunnlagsfakta.fastsatt
                )

                is PersonObserver.UtkastTilVedtakEvent.FastsattEtterHovedregel -> mapOf(
                    "omregnetÅrsinntektTotalt" to totalOmregnetÅrsinntekt,
                    "6G" to seksG,
                    "fastsatt" to sykepengegrunnlagsfakta.fastsatt,
                    "arbeidsgivere" to sykepengegrunnlagsfakta.arbeidsgivere.map {
                        mapOf(
                            "arbeidsgiver" to it.arbeidsgiver,
                            "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                        )
                    }
                )
                is PersonObserver.UtkastTilVedtakEvent.FastsattEtterSkjønn -> mapOf(
                    "omregnetÅrsinntektTotalt" to totalOmregnetÅrsinntekt,
                    "6G" to seksG,
                    "fastsatt" to sykepengegrunnlagsfakta.fastsatt,
                    "arbeidsgivere" to sykepengegrunnlagsfakta.arbeidsgivere.map {
                        mapOf(
                            "arbeidsgiver" to it.arbeidsgiver,
                            "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                            "skjønnsfastsatt" to it.skjønnsfastsatt
                        )
                    },
                    "skjønnsfastsatt" to sykepengegrunnlagsfakta.skjønnsfastsatt
                )
            }
        )

        val utkastTilVedtak = PersonObserver.UtkastTilVedtakEvent(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            behandlingId = behandlingId,
            tags = tags,
            `6G`= if (inngangsvilkårFraInfotrygd) null else seksG
        )
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}