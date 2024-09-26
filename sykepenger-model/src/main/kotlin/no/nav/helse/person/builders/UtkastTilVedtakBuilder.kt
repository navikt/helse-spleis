package no.nav.helse.person.builders

import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
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
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi
import kotlin.properties.Delegates

internal class UtkastTilVedtakBuilder(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val vedtaksperiodeId: UUID,
    private val arbeidsgiver: String,
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

    private var sykepengegrunnlag by Delegates.notNull<Double>()
    private lateinit var beregningsgrunnlag: Inntekt
    private var totalOmregnetÅrsinntekt by Delegates.notNull<Double>()
    private var seksG by Delegates.notNull<Double>()
    private var inngangsvilkårFraInfotrygd by Delegates.notNull<Boolean>()
    internal fun sykepengegrunnlag(sykepengegrunnlag: Inntekt, beregningsgrunnlag: Inntekt, totalOmregnetÅrsinntekt: Inntekt, seksG: Inntekt, toG: Inntekt, inngangsvilkårFraInfotrygd: Boolean) = apply {
        this.sykepengegrunnlag = sykepengegrunnlag.årlig
        this.beregningsgrunnlag = beregningsgrunnlag
        this.totalOmregnetÅrsinntekt = totalOmregnetÅrsinntekt.årlig
        this.seksG = seksG.årlig
        this.inngangsvilkårFraInfotrygd = inngangsvilkårFraInfotrygd

        if (!inngangsvilkårFraInfotrygd && beregningsgrunnlag > seksG) tags.add("6GBegrenset")
        if (sykepengegrunnlag < toG) tags.add("SykepengegrunnlagUnder2G")
        if (inngangsvilkårFraInfotrygd) tags.add("InngangsvilkårFraInfotrygd")
    }

    private data class Arbeidsgiverinntekt(val arbeidsgiver: String, val omregnedeÅrsinntekt: Double, val skjønnsfastsatt: Double?, val gjelder: Periode)
    private val arbeidsgiverinntekter = mutableSetOf<Arbeidsgiverinntekt>()
    internal fun arbeidsgiverinntekt(arbeidsgiver: String, omregnedeÅrsinntekt: Inntekt, skjønnsfastsatt: Inntekt?, gjelder: Periode) {
        arbeidsgiverinntekter.add(Arbeidsgiverinntekt(arbeidsgiver, omregnedeÅrsinntekt.årlig, skjønnsfastsatt?.årlig, gjelder))
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

    internal fun buildGodkjenningsbehov() = build.godkjenningsbehov
    internal fun buildUtkastTilVedtak() = build.utkastTilVedtak
    internal fun buildAvsluttedMedVedtak(vedtakFattet: LocalDateTime, historiskeHendelseIder: Set<UUID>) = build.avsluttetMedVedtak(vedtakFattet, historiskeHendelseIder)

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

        private val beregningsgrunnlagForAvsluttetMedVedtak: Double = sykepengegrunnlagsfakta.beregningsgrunnlagForAvsluttetMedVedtak().also {
            check(it == beregningsgrunnlag.årlig) { "Beregningsgrunnlag ${beregningsgrunnlag.årlig} er noe annet enn beregningsgrunnlag beregnet fra sykepengegrunnlagsfakta $it" }
        }

        private val omregnetÅrsinntektPerArbeidsgiverForAvsluttedMedVedtak = sykepengegrunnlagsfakta.omregnetÅrsinntektPerArbeidsgiverForAvsluttedMedVedtak()

        private val sykepengegrunnlagsbegrensningForAvsluttetMedVedtak = sykepengegrunnlagsfakta.sykepengegrunnlagsbegrensningForAvsluttedMedVedtak(tags)

        private val periodetypeForGodkjenningsbehov = sykepengegrunnlagsfakta.periodetypeForGodkjenningsbehov(tags)

        val godkjenningsbehov = mapOf(
            "periodeFom" to periode.start.toString(),
            "periodeTom" to periode.endInclusive.toString(),
            "skjæringstidspunkt" to skjæringstidspunkt.toString(),
            "vilkårsgrunnlagId" to vilkårsgrunnlagId.toString(),
            "periodetype" to periodetypeForGodkjenningsbehov,
            "førstegangsbehandling" to !erForlengelse,
            "utbetalingtype" to if (revurdering) "REVURDERING" else "UTBETALING",
            "inntektskilde" to if (enArbeidsgiver) "EN_ARBEIDSGIVER" else "FLERE_ARBEIDSGIVERE",
            "orgnummereMedRelevanteArbeidsforhold" to (arbeidsgiverinntekter.map { it.arbeidsgiver }).toSet(),
            "tags" to tags,
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
            `6G`= when (val fakta = sykepengegrunnlagsfakta) {
                is PersonObserver.UtkastTilVedtakEvent.FastsattIInfotrygd -> null
                is PersonObserver.UtkastTilVedtakEvent.FastsattEtterHovedregel -> fakta.`6G`
                is PersonObserver.UtkastTilVedtakEvent.FastsattEtterSkjønn -> fakta.`6G`
            }
        )

        fun avsluttetMedVedtak(vedtakFattet: LocalDateTime, historiskeHendelseIder: Set<UUID>) = PersonObserver.AvsluttetMedVedtakEvent(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = arbeidsgiver,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periode = periode,
            // Til ettertanke: AvsluttetMedVedtak har alle hendelseId'er ever på vedtaksperioden, mens godkjenningsbehov/utkast_til_vedtak har kun behandlingens
            hendelseIder = hendelseIder + historiskeHendelseIder,
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag,
            // Til ettertanke: Denne mappes ut i JSON som "grunnlagForSykepengegrunnlag"
            beregningsgrunnlag = beregningsgrunnlagForAvsluttetMedVedtak,
            // Til ettertanke: Den var jo uventet, men er jo slik det har vært 🤷‍
            // Til ettertanke: Denne hentet data fra sykepengegrunnlagsfakta som har to desimaler
            omregnetÅrsinntektPerArbeidsgiver = omregnetÅrsinntektPerArbeidsgiverForAvsluttedMedVedtak,
            inntekt = beregningsgrunnlag.månedlig, // TODO: Til ettertanke: What? 👀 Denne håper jeg ingen bruker
            utbetalingId = utbetalingId,
            sykepengegrunnlagsbegrensning = sykepengegrunnlagsbegrensningForAvsluttetMedVedtak,
            vedtakFattetTidspunkt = vedtakFattet,
            // Til ettertanke: Akkurat i avsluttet i vedtak blir beløp i sykepengegrunnlagsfakta avrundet til to desimaler.
            sykepengegrunnlagsfakta = when (val fakta = sykepengegrunnlagsfakta) {
                is PersonObserver.UtkastTilVedtakEvent.FastsattIInfotrygd -> fakta.copy(omregnetÅrsinntekt = fakta.omregnetÅrsinntekt.toDesimaler)
                is PersonObserver.UtkastTilVedtakEvent.FastsattEtterHovedregel -> fakta.copy(
                    omregnetÅrsinntekt = fakta.omregnetÅrsinntekt.toDesimaler,
                    `6G` = fakta.`6G`.toDesimaler,
                    arbeidsgivere = fakta.arbeidsgivere.map { it.copy(omregnetÅrsinntekt = it.omregnetÅrsinntekt.toDesimaler) }
                )
                is PersonObserver.UtkastTilVedtakEvent.FastsattEtterSkjønn -> fakta.copy(
                    omregnetÅrsinntekt = fakta.omregnetÅrsinntekt.toDesimaler,
                    `6G` = fakta.`6G`.toDesimaler,
                    arbeidsgivere = fakta.arbeidsgivere.map { it.copy(omregnetÅrsinntekt = it.omregnetÅrsinntekt.toDesimaler, skjønnsfastsatt = it.skjønnsfastsatt.toDesimaler) }
                )
            }
        )
    }

    private companion object {
        private val Inntekt.årlig get() = reflection { årlig, _, _, _ -> årlig }
        private val Inntekt.månedlig get() = reflection { _, månedlig, _, _ -> månedlig }
        private val Double.toDesimaler get() = toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()

        private fun PersonObserver.UtkastTilVedtakEvent.Sykepengegrunnlagsfakta.beregningsgrunnlagForAvsluttetMedVedtak() = when (val fakta = this) {
            is PersonObserver.UtkastTilVedtakEvent.FastsattEtterSkjønn -> fakta.skjønnsfastsatt
            else -> fakta.omregnetÅrsinntekt
        }

        private fun PersonObserver.UtkastTilVedtakEvent.Sykepengegrunnlagsfakta.omregnetÅrsinntektPerArbeidsgiverForAvsluttedMedVedtak(): Map<String, Double> = when (val fakta = this) {
            is PersonObserver.UtkastTilVedtakEvent.FastsattIInfotrygd -> emptyMap()
            is PersonObserver.UtkastTilVedtakEvent.FastsattEtterHovedregel -> fakta.arbeidsgivere.associate { it.arbeidsgiver to it.omregnetÅrsinntekt.toDesimaler }
            is PersonObserver.UtkastTilVedtakEvent.FastsattEtterSkjønn -> fakta.arbeidsgivere.associate { it.arbeidsgiver to it.skjønnsfastsatt.toDesimaler }
        }

        private fun PersonObserver.UtkastTilVedtakEvent.Sykepengegrunnlagsfakta.sykepengegrunnlagsbegrensningForAvsluttedMedVedtak(tags: Set<String>) = when {
            this is PersonObserver.UtkastTilVedtakEvent.FastsattIInfotrygd -> "VURDERT_I_INFOTRYGD"
            tags.contains("6GBegrenset") -> "ER_6G_BEGRENSET"
            else -> "ER_IKKE_6G_BEGRENSET"
        }

        private fun PersonObserver.UtkastTilVedtakEvent.Sykepengegrunnlagsfakta.periodetypeForGodkjenningsbehov(tags: Set<String>): String {
            val erForlengelse = tags.contains("Forlengelse")
            return when {
                this is PersonObserver.UtkastTilVedtakEvent.FastsattIInfotrygd -> if (erForlengelse) "INFOTRYGDFORLENGELSE" else "OVERGANG_FRA_IT"
                else -> if (erForlengelse) "FORLENGELSE" else "FØRSTEGANGSBEHANDLING"
            }
        }
    }
}