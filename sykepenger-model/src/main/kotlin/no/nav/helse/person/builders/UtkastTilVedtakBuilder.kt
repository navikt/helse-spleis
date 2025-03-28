package no.nav.helse.person.builders

import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.properties.Delegates
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.FastsattEtterHovedregel
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.FastsattEtterSkjønn
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.FastsattIInfotrygd
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.Sykepengegrunnlagsfakta
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingtype.REVURDERING
import no.nav.helse.utbetalingslinjer.Utbetalingtype.UTBETALING
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt

internal class UtkastTilVedtakBuilder(
    private val vedtaksperiodeId: UUID,
    private val arbeidsgiver: String,
    private val kanForkastes: Boolean,
    erForlengelse: Boolean,
    private val harPeriodeRettFør: Boolean
) {
    private val tags = mutableSetOf<Tag>()

    init {
        if (erForlengelse) tags.add(Tag.Forlengelse)
        else tags.add(Tag.Førstegangsbehandling)
    }

    internal fun grunnbeløpsregulert() = apply { tags.add(Tag.Grunnbeløpsregulering) }
    private data class RelevantPeriode(val vedtaksperiodeId: UUID, val behandlingId: UUID, val skjæringstidspunkt: LocalDate, val periode: Periode)

    private val relevantePerioder = mutableSetOf<RelevantPeriode>()
    internal fun relevantPeriode(vedtaksperiodeId: UUID, behandlingId: UUID, skjæringstidspunkt: LocalDate, periode: Periode) = apply {
        relevantePerioder.add(RelevantPeriode(vedtaksperiodeId, behandlingId, skjæringstidspunkt, periode))
    }

    private lateinit var behandlingId: UUID
    internal fun behandlingId(behandlingId: UUID) = apply { this.behandlingId = behandlingId }
    private lateinit var periode: Periode
    internal fun periode(arbeidsgiverperiode: List<Periode>, periode: Periode) = apply {
        this.periode = periode

        val gjennomført = NormalArbeidstaker.arbeidsgiverperiodenGjennomført(arbeidsgiverperiode.periode()?.count() ?: 0)
        val arbeidsgiverperiodePåstartetITidligerePeriode = arbeidsgiverperiode.isNotEmpty() && periode.start >= arbeidsgiverperiode.last().endInclusive

        // flex viser denne teksten hvis vi har tagget med 'IngenNyArbeidsgiverperiode':
        //      Det er tidligere utbetalt en hel arbeidsgiverperiode.
        //      Etter dette har vi vurdert at du ikke har gjenopptatt arbeidet og deretter vært friskmeldt i mer enn 16 dager.
        //      NAV har derfor utbetalt sykepenger fra første dag du ble sykmeldt.
        val utbetalingFraFørsteDagEtterAGP = !harPeriodeRettFør && gjennomført && arbeidsgiverperiodePåstartetITidligerePeriode
        if (utbetalingFraFørsteDagEtterAGP) {
            tags.add(Tag.IngenNyArbeidsgiverperiode)
        }
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
    internal fun utbetaling(utbetaling: Utbetaling) = apply {
        this.utbetalingId = utbetaling.id

        check(utbetaling.type == UTBETALING || utbetaling.type == REVURDERING) {
            "Utkast til vedtak på ${utbetaling.type.name}? Det kan jo ikke være rett."
        }

        if (utbetaling.type == REVURDERING) {
            tags.add(Tag.Revurdering)
        }

        val antallTagsFør = tags.size
        val arbeidsgiverNettoBeløp = utbetaling.arbeidsgiverOppdrag().nettoBeløp()
        val personNettoBeløp = utbetaling.personOppdrag().nettoBeløp()

        if (arbeidsgiverNettoBeløp > 0) tags.add(Tag.Arbeidsgiverutbetaling)
        else if (arbeidsgiverNettoBeløp < 0) tags.add(Tag.NegativArbeidsgiverutbetaling)

        if (personNettoBeløp > 0) tags.add(Tag.Personutbetaling)
        else if (personNettoBeløp < 0) tags.add(Tag.NegativPersonutbetaling)

        if (arbeidsgiverNettoBeløp == 0 && personNettoBeløp == 0) tags.add(Tag.IngenUtbetaling)

        check(tags.size > antallTagsFør) {
            "arbeidsgiverNettoBeløp=$arbeidsgiverNettoBeløp, personNettoBeløp=$personNettoBeløp burde bli minst én ny tag."
        }
    }

    internal fun utbetalingstidslinje(utbetalingstidslinje: Utbetalingstidslinje) = apply {
        tags.add(utbetalingstidslinje.behandlingsresultat)
    }

    private val Utbetalingstidslinje.behandlingsresultat
        get(): Tag {
            val avvistDag = any { it is Utbetalingsdag.AvvistDag || it is Utbetalingsdag.ForeldetDag }
            val navDag = any { it is Utbetalingsdag.NavDag }

            return when {
                !navDag -> Tag.Avslag
                navDag && avvistDag -> Tag.DelvisInnvilget
                else -> Tag.Innvilget
            }
        }

    internal fun sykdomstidslinje(sykdomstidslinje: Sykdomstidslinje) = apply {
        if (sykdomstidslinje.any { it is Dag.Feriedag }) tags.add(Tag.Ferie)
    }

    internal fun refusjonstidslinje(refusjonstidslinje: Beløpstidslinje) = apply {
        if (refusjonstidslinje.sumOf { it.beløp.årlig } > 0) tags.add(Tag.ArbeidsgiverØnskerRefusjon)
    }

    private var sykepengegrunnlag by Delegates.notNull<Double>()
    private lateinit var beregningsgrunnlag: Inntekt
    private var totalOmregnetÅrsinntekt by Delegates.notNull<Double>()
    private var seksG by Delegates.notNull<Double>()
    internal fun sykepengegrunnlag(sykepengegrunnlag: Inntekt, beregningsgrunnlag: Inntekt, totalOmregnetÅrsinntekt: Inntekt, seksG: Inntekt, inngangsvilkårFraInfotrygd: Boolean) = apply {
        this.sykepengegrunnlag = sykepengegrunnlag.årlig
        this.beregningsgrunnlag = beregningsgrunnlag
        this.totalOmregnetÅrsinntekt = totalOmregnetÅrsinntekt.årlig
        this.seksG = seksG.årlig

        val toG = seksG / 3
        if (!inngangsvilkårFraInfotrygd && beregningsgrunnlag > seksG) tags.add(Tag.`6GBegrenset`)
        if (sykepengegrunnlag < toG) tags.add(Tag.SykepengegrunnlagUnder2G)
        if (inngangsvilkårFraInfotrygd) tags.add(Tag.InngangsvilkårFraInfotrygd)
    }

    private data class Arbeidsgiverinntekt(val arbeidsgiver: String, val omregnedeÅrsinntekt: Double, val skjønnsfastsatt: Double?, val inntektskilde: Inntektskilde)

    private val arbeidsgiverinntekter = mutableSetOf<Arbeidsgiverinntekt>()
    internal fun arbeidsgiverinntekt(arbeidsgiver: String, omregnedeÅrsinntekt: Inntekt, skjønnsfastsatt: Inntekt?, inntektskilde: Inntektskilde) = apply {
        arbeidsgiverinntekter.add(Arbeidsgiverinntekt(arbeidsgiver, omregnedeÅrsinntekt.årlig, skjønnsfastsatt?.årlig, inntektskilde))
    }

    private val build by lazy { Build() }
    internal fun buildGodkjenningsbehov() = build.godkjenningsbehov
    internal fun buildUtkastTilVedtak() = build.utkastTilVedtak
    internal fun buildAvsluttedMedVedtak(vedtakFattet: LocalDateTime, historiskeHendelseIder: Set<MeldingsreferanseId>) = build.avsluttetMedVedtak(vedtakFattet, historiskeHendelseIder)
    private inner class Build {
        private val skjønnsfastsatt = arbeidsgiverinntekter.any { it.skjønnsfastsatt != null }.also {
            if (it) check(arbeidsgiverinntekter.all { arbeidsgiver -> arbeidsgiver.skjønnsfastsatt != null }) { "Enten må ingen eller alle arbeidsgivere i sykepengegrunnlaget være skjønnsmessig fastsatt." }
        }
        private val perioderMedSammeSkjæringstidspunkt = relevantePerioder.filter { it.skjæringstidspunkt == skjæringstidspunkt }

        init {
            check(arbeidsgiverinntekter.isNotEmpty()) { "Forventet ikke at det ikke er noen arbeidsgivere i sykepengegrunnlaget." }
            if (arbeidsgiverinntekter.size == 1) tags.add(Tag.EnArbeidsgiver) else tags.add(Tag.FlereArbeidsgivere)
            if (arbeidsgiverinntekter.single { it.arbeidsgiver == arbeidsgiver }.inntektskilde == Inntektskilde.AOrdningen) tags.add(Tag.InntektFraAOrdningenLagtTilGrunn)
        }

        private val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta = when {
            tags.contains(Tag.InngangsvilkårFraInfotrygd) -> FastsattIInfotrygd(totalOmregnetÅrsinntekt)

            skjønnsfastsatt -> FastsattEtterSkjønn(
                omregnetÅrsinntekt = totalOmregnetÅrsinntekt,
                sykepengegrunnlag = sykepengegrunnlag,
                `6G` = seksG,
                arbeidsgivere = arbeidsgiverinntekter.map {
                    FastsattEtterSkjønn.Arbeidsgiver(
                        arbeidsgiver = it.arbeidsgiver,
                        omregnetÅrsinntekt = it.omregnedeÅrsinntekt,
                        skjønnsfastsatt = it.skjønnsfastsatt!!,
                        inntektskilde = Inntektskilde.Saksbehandler
                    )
                }
            )

            else -> FastsattEtterHovedregel(
                omregnetÅrsinntekt = totalOmregnetÅrsinntekt,
                sykepengegrunnlag = sykepengegrunnlag,
                `6G` = seksG,
                arbeidsgivere = arbeidsgiverinntekter.map {
                    FastsattEtterHovedregel.Arbeidsgiver(
                        arbeidsgiver = it.arbeidsgiver,
                        omregnetÅrsinntekt = it.omregnedeÅrsinntekt,
                        inntektskilde = it.inntektskilde
                    )
                }
            )
        }

        private val periodetypeForGodkjenningsbehov = sykepengegrunnlagsfakta.periodetypeForGodkjenningsbehov(tags)

        private val omregnedeÅrsinntekterForGodkjenningsbehov = sykepengegrunnlagsfakta.omregnedeÅrsinntekterForGodkjenningsbehov(arbeidsgiver)

        val godkjenningsbehov = mapOf(
            "periodeFom" to "${periode.start}",
            "periodeTom" to "${periode.endInclusive}",
            "skjæringstidspunkt" to "$skjæringstidspunkt",
            "vilkårsgrunnlagId" to "$vilkårsgrunnlagId",
            "periodetype" to periodetypeForGodkjenningsbehov,
            "førstegangsbehandling" to tags.contains(Tag.Førstegangsbehandling),
            "utbetalingtype" to if (tags.contains(Tag.Revurdering)) "REVURDERING" else "UTBETALING",
            "inntektskilde" to if (tags.contains(Tag.EnArbeidsgiver)) "EN_ARBEIDSGIVER" else "FLERE_ARBEIDSGIVERE",
            // Til ettertanke: Her kan det være orgnummer på tilkomnde arbeidsgivere i tillegg til de som er i "sykepengegrunnlagsfakta". Kanskje finne på noe smartere der?
            "orgnummereMedRelevanteArbeidsforhold" to (arbeidsgiverinntekter.map { it.arbeidsgiver }).toSet(),
            "tags" to tags.utgående,
            "kanAvvises" to kanForkastes,
            "omregnedeÅrsinntekter" to omregnedeÅrsinntekterForGodkjenningsbehov,
            "behandlingId" to "$behandlingId",
            "hendelser" to hendelseIder,
            "perioderMedSammeSkjæringstidspunkt" to perioderMedSammeSkjæringstidspunkt.map {
                mapOf(
                    "vedtaksperiodeId" to "${it.vedtaksperiodeId}",
                    "behandlingId" to "${it.behandlingId}",
                    "fom" to "${it.periode.start}",
                    "tom" to "${it.periode.endInclusive}"
                )
            },
            "sykepengegrunnlagsfakta" to when (sykepengegrunnlagsfakta) {
                is FastsattIInfotrygd -> mapOf(
                    "omregnetÅrsinntektTotalt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                    "fastsatt" to sykepengegrunnlagsfakta.fastsatt
                )

                is FastsattEtterHovedregel -> mapOf(
                    "omregnetÅrsinntektTotalt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                    "sykepengegrunnlag" to sykepengegrunnlag,
                    "6G" to seksG,
                    "fastsatt" to sykepengegrunnlagsfakta.fastsatt,
                    "arbeidsgivere" to sykepengegrunnlagsfakta.arbeidsgivere.map {
                        mapOf(
                            "arbeidsgiver" to it.arbeidsgiver,
                            "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                            "inntektskilde" to it.inntektskilde
                        )
                    }
                )

                is FastsattEtterSkjønn -> mapOf(
                    "omregnetÅrsinntektTotalt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                    "6G" to seksG,
                    "fastsatt" to sykepengegrunnlagsfakta.fastsatt,
                    "arbeidsgivere" to sykepengegrunnlagsfakta.arbeidsgivere.map {
                        mapOf(
                            "arbeidsgiver" to it.arbeidsgiver,
                            "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                            "skjønnsfastsatt" to it.skjønnsfastsatt,
                            "inntektskilde" to Inntektskilde.Saksbehandler,
                        )
                    },
                    "skjønnsfastsatt" to sykepengegrunnlagsfakta.skjønnsfastsatt
                )
            }
        )

        // TODO: 10.12.24 Rename skjønssfastsatt til fastsattÅrsinntekt (som er lik omregnet for hovedregel med forskjellige beløp på skjønn)
        //  Burde omregnetÅrsinntekt være et objekt med beløp og kilde slik at det er tydligere at kilden hører til dét
        //  Burde det hete noe annet? Ikke gitt at det skal bli et vedtak. `behandling_beregnet` ? 🤔
        //  Skal Spleis lytte på noe àla `behandling_utført` / `behandling_ferdig_behandlet` / `behandling_vurdert` som tommel opp/ned på godkjenningsbehov ?
        //  Sende med en liste med dager & beløp ?
        val utkastTilVedtak = PersonObserver.UtkastTilVedtakEvent(
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            behandlingId = behandlingId,
            tags = tags.utgående,
            `6G` = when (val fakta = sykepengegrunnlagsfakta) {
                is FastsattIInfotrygd -> null
                is FastsattEtterHovedregel -> fakta.`6G`
                is FastsattEtterSkjønn -> fakta.`6G`
            }
        )

        fun avsluttetMedVedtak(vedtakFattet: LocalDateTime, historiskeHendelseIder: Set<MeldingsreferanseId>) = PersonObserver.AvsluttetMedVedtakEvent(
            organisasjonsnummer = arbeidsgiver,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periode = periode,
            // Til ettertanke: AvsluttetMedVedtak har alle hendelseId'er ever på vedtaksperioden, mens godkjenningsbehov/utkast_til_vedtak har kun behandlingens
            hendelseIder = hendelseIder + historiskeHendelseIder.map { it.id }, // TODO: 10.12.24: Enten klaske på historiske på godkjenningsbehovet (eget felt) eller fjerne "dokumenter" i vedtak_fattet
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag, // TODO: 10.12.24: Legge til for skjønnsmessig og infotrygd i tillegg til hovedregel
            utbetalingId = utbetalingId,
            vedtakFattetTidspunkt = vedtakFattet,
            // Til ettertanke: Akkurat i avsluttet i vedtak blir beløp i sykepengegrunnlagsfakta avrundet til to desimaler.
            sykepengegrunnlagsfakta = when (val fakta = sykepengegrunnlagsfakta) {
                is FastsattIInfotrygd -> fakta.copy(omregnetÅrsinntekt = fakta.omregnetÅrsinntekt.toDesimaler)
                is FastsattEtterHovedregel -> fakta.copy(
                    omregnetÅrsinntekt = fakta.omregnetÅrsinntekt.toDesimaler,
                    `6G` = fakta.`6G`.toDesimaler,
                    arbeidsgivere = fakta.arbeidsgivere.map { it.copy(omregnetÅrsinntekt = it.omregnetÅrsinntekt.toDesimaler) }
                )

                is FastsattEtterSkjønn -> fakta.copy(
                    omregnetÅrsinntekt = fakta.omregnetÅrsinntekt.toDesimaler,
                    `6G` = fakta.`6G`.toDesimaler,
                    arbeidsgivere = fakta.arbeidsgivere.map { it.copy(omregnetÅrsinntekt = it.omregnetÅrsinntekt.toDesimaler, skjønnsfastsatt = it.skjønnsfastsatt.toDesimaler) }
                )
            }
        )
    }

    private enum class Tag {
        Forlengelse,
        Førstegangsbehandling,
        Revurdering,
        IngenNyArbeidsgiverperiode,
        Grunnbeløpsregulering,
        EnArbeidsgiver,
        FlereArbeidsgivere,
        `6GBegrenset`,
        Arbeidsgiverutbetaling,
        NegativArbeidsgiverutbetaling,
        Personutbetaling,
        NegativPersonutbetaling,
        IngenUtbetaling,
        SykepengegrunnlagUnder2G,
        InngangsvilkårFraInfotrygd,
        Avslag,
        DelvisInnvilget,
        Innvilget,
        Ferie,
        InntektFraAOrdningenLagtTilGrunn,
        ArbeidsgiverØnskerRefusjon
    }

    private companion object {
        private val Double.toDesimaler get() = toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
        private val Set<Tag>.utgående get() = map { it.name }.toSet()

        private fun Sykepengegrunnlagsfakta.periodetypeForGodkjenningsbehov(tags: Set<Tag>): String {
            val erForlengelse = tags.contains(Tag.Forlengelse)
            return when {
                this is FastsattIInfotrygd -> if (erForlengelse) "INFOTRYGDFORLENGELSE" else "OVERGANG_FRA_IT"
                else -> if (erForlengelse) "FORLENGELSE" else "FØRSTEGANGSBEHANDLING"
            }
        }

        private fun Sykepengegrunnlagsfakta.omregnedeÅrsinntekterForGodkjenningsbehov(arbeidsgiver: String): List<Map<String, Any>> = when (val fakta = this) {
            is FastsattIInfotrygd -> listOf(mapOf("organisasjonsnummer" to arbeidsgiver, "beløp" to fakta.omregnetÅrsinntekt))
            is FastsattEtterHovedregel -> fakta.arbeidsgivere.map { mapOf("organisasjonsnummer" to it.arbeidsgiver, "beløp" to it.omregnetÅrsinntekt) }
            is FastsattEtterSkjønn -> fakta.arbeidsgivere.map { mapOf("organisasjonsnummer" to it.arbeidsgiver, "beløp" to it.omregnetÅrsinntekt) } // Nei, ikke bug at det er omregnetÅrsinntekt
        }
    }
}
