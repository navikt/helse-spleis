package no.nav.helse.person.builders

import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.properties.Delegates
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Behandlinger.Behandling.Endring.Arbeidssituasjon
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.EventSubscription.GodkjenningEvent.Companion.tilBehovMap
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.FastsattEtterHovedregel
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.FastsattEtterSkjønn
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.FastsattIInfotrygd
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.Sykepengegrunnlagsfakta
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingtype.REVURDERING
import no.nav.helse.utbetalingslinjer.Utbetalingtype.UTBETALING
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt

internal class UtkastTilVedtakBuilder(
    private val vedtaksperiodeId: UUID,
    private val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
    private val kanForkastes: Boolean,
    forlengerVedtak: Boolean,
    private val harPeriodeRettFør: Boolean,
    overlapperMedInfotrygd: Boolean
) {
    private var forbrukteSykedager by Delegates.notNull<Int>()
    private var gjenståendeSykedager by Delegates.notNull<Int>()
    private lateinit var foreløpigBeregnetSluttPåSykepenger: LocalDate
    private lateinit var utbetalingsdager: List<EventSubscription.Utbetalingsdag>
    private val tags = mutableSetOf<Tag>()
    private val pensjonsgivendeInntekter = mutableListOf<SelvstendigFaktaavklartInntekt.PensjonsgivendeInntekt>()

    init {
        if (forlengerVedtak) tags.add(Tag.Forlengelse)
        else tags.add(Tag.Førstegangsbehandling)

        if (overlapperMedInfotrygd) tags.add(Tag.OverlapperMedInfotrygd)
    }

    internal fun pensjonsgivendeInntekter(pensjonsgivendeInntekter: List<SelvstendigFaktaavklartInntekt.PensjonsgivendeInntekt>) =
        apply { this.pensjonsgivendeInntekter.addAll(pensjonsgivendeInntekter) }

    internal fun grunnbeløpsregulert() = apply { tags.add(Tag.Grunnbeløpsregulering) }
    private data class RelevantPeriode(val vedtaksperiodeId: UUID, val behandlingId: UUID, val skjæringstidspunkt: LocalDate, val periode: Periode)

    private val relevantePerioder = mutableSetOf<RelevantPeriode>()
    internal fun relevantPeriode(vedtaksperiodeId: UUID, behandlingId: UUID, skjæringstidspunkt: LocalDate, periode: Periode) = apply {
        relevantePerioder.add(RelevantPeriode(vedtaksperiodeId, behandlingId, skjæringstidspunkt, periode))
    }

    private lateinit var behandlingId: UUID
    internal fun behandlingId(behandlingId: UUID) = apply { this.behandlingId = behandlingId }
    private lateinit var arbeidssituasjon: Arbeidssituasjon
    internal fun arbeidssituasjon(arbeidssituasjon: Arbeidssituasjon) = apply { this.arbeidssituasjon = arbeidssituasjon }
    private var vedtakFattet: LocalDateTime? = null
    internal fun vedtakFattet(vedtakFattet: LocalDateTime) = apply { this.vedtakFattet = vedtakFattet }
    private var relevanteSøknader: Set<UUID> = emptySet()
    internal fun relevanteSøknader(relevanteSøknader: Set<MeldingsreferanseId>) = apply { this.relevanteSøknader = relevanteSøknader.map { it.id }.toSet() }
    private lateinit var periode: Periode
    internal fun periode(arbeidsgiverperiode: List<Periode>, arbeidsgiverperiodeFerdigAvklart: Boolean, periode: Periode) = apply {
        this.periode = periode

        val arbeidsgiverperiodePåstartetITidligerePeriode = arbeidsgiverperiode.isNotEmpty() && periode.start >= arbeidsgiverperiode.last().endInclusive

        // flex viser denne teksten hvis vi har tagget med 'IngenNyArbeidsgiverperiode':
        //      Det er tidligere utbetalt en hel arbeidsgiverperiode.
        //      Etter dette har vi vurdert at du ikke har gjenopptatt arbeidet og deretter vært friskmeldt i mer enn 16 dager.
        //      NAV har derfor utbetalt sykepenger fra første dag du ble sykmeldt.
        val utbetalingFraFørsteDagEtterAGP = !harPeriodeRettFør && arbeidsgiverperiodeFerdigAvklart && arbeidsgiverperiodePåstartetITidligerePeriode
        if (utbetalingFraFørsteDagEtterAGP) {
            tags.add(Tag.IngenNyArbeidsgiverperiode)
        }
    }

    private lateinit var skjæringstidspunkt: LocalDate
    internal fun skjæringstidspunkt(skjæringstidspunkt: LocalDate) = apply {
        this.skjæringstidspunkt = skjæringstidspunkt
    }

    private lateinit var vilkårsgrunnlagId: UUID
    internal fun vilkårsgrunnlagId(vilkårsgrunnlagId: UUID) = apply { this.vilkårsgrunnlagId = vilkårsgrunnlagId }
    private lateinit var utbetalingId: UUID

    private fun utbetaling(utbetaling: Utbetaling) = apply {
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

    internal fun sykepengerettighet(forbrukteSykedager: Int, gjenståendeSykedager: Int, foreløpigBeregnetSluttPåSykepenger: LocalDate) = apply {
        this.forbrukteSykedager = forbrukteSykedager
        this.gjenståendeSykedager = gjenståendeSykedager
        this.foreløpigBeregnetSluttPåSykepenger = foreløpigBeregnetSluttPåSykepenger
    }

    internal fun utbetalingsinformasjon(
        utbetaling: Utbetaling,
        utbetalingstidslinje: Utbetalingstidslinje,
        sykdomstidslinje: Sykdomstidslinje,
        refusjonstidslinje: Beløpstidslinje
    ) = apply {
        this.utbetaling(utbetaling)
        this.sykdomstidslinje(sykdomstidslinje)
        this.refusjonstidslinje(refusjonstidslinje)

        tags.add(utbetalingstidslinje.behandlingsresultat)
        this.utbetalingsdager = UtbetalingsdagerBuilder(sykdomstidslinje).result(utbetalingstidslinje)
    }

    private val Utbetalingstidslinje.behandlingsresultat
        get(): Tag {
            val avvistDag = any { it is Utbetalingsdag.AvvistDag || it is Utbetalingsdag.ForeldetDag }
            val navDag = any { it is Utbetalingsdag.NavDag }

            return when {
                navDag && avvistDag -> Tag.DelvisInnvilget
                !navDag && avvistDag -> Tag.Avslag
                else -> Tag.Innvilget
            }
        }

    private fun sykdomstidslinje(sykdomstidslinje: Sykdomstidslinje) = apply {
        if (sykdomstidslinje.any { it is Dag.Feriedag }) tags.add(Tag.Ferie)
    }

    private fun refusjonstidslinje(refusjonstidslinje: Beløpstidslinje) = apply {
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
    internal fun buildGodkjenningEvent() = build.godkjenningEvent
    internal fun buildUtkastTilVedtak() = build.utkastTilVedtak
    internal fun buildAvsluttedMedVedtak() = build.avsluttetMedVedtak(vedtakFattet!!)
    private inner class Build {
        private val skjønnsfastsatt = arbeidsgiverinntekter.any { it.skjønnsfastsatt != null }.also {
            if (it) check(arbeidsgiverinntekter.all { arbeidsgiver -> arbeidsgiver.skjønnsfastsatt != null }) { "Enten må ingen eller alle arbeidsgivere i sykepengegrunnlaget være skjønnsmessig fastsatt." }
        }
        private val perioderMedSammeSkjæringstidspunkt = relevantePerioder.filter { it.skjæringstidspunkt == skjæringstidspunkt }

        init {
            check(arbeidsgiverinntekter.isNotEmpty()) { "Forventet ikke at det ikke er noen arbeidsgivere i sykepengegrunnlaget." }
            if (arbeidsgiverinntekter.size == 1) tags.add(Tag.EnArbeidsgiver) else tags.add(Tag.FlereArbeidsgivere)
            if (arbeidsgiverinntekter.singleOrNull { it.arbeidsgiver == (yrkesaktivitetssporing as? Behandlingsporing.Yrkesaktivitet.Arbeidstaker)?.organisasjonsnummer }?.inntektskilde == Inntektskilde.AOrdningen) tags.add(Tag.InntektFraAOrdningenLagtTilGrunn)
        }

        private val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta = when {
            tags.contains(Tag.InngangsvilkårFraInfotrygd) -> FastsattIInfotrygd(totalOmregnetÅrsinntekt, (yrkesaktivitetssporing as Behandlingsporing.Yrkesaktivitet.Arbeidstaker).organisasjonsnummer)

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

        // TODO: Bruk godkjenningEvent.behovInput. Bare logge litt for å se at de er helt like først.
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
            "behandlingId" to "$behandlingId",
            "relevanteSøknader" to relevanteSøknader,
            "perioderMedSammeSkjæringstidspunkt" to perioderMedSammeSkjæringstidspunkt.map {
                mapOf(
                    "vedtaksperiodeId" to "${it.vedtaksperiodeId}",
                    "behandlingId" to "${it.behandlingId}",
                    "fom" to "${it.periode.start}",
                    "tom" to "${it.periode.endInclusive}"
                )
            },
            "forbrukteSykedager" to forbrukteSykedager,
            "gjenståendeSykedager" to gjenståendeSykedager,
            "foreløpigBeregnetSluttPåSykepenger" to "$foreløpigBeregnetSluttPåSykepenger",
            "utbetalingsdager" to utbetalingsdager.map { dag -> dag.tilBehovMap() },
            "sykepengegrunnlagsfakta" to mapOf(
                "sykepengegrunnlag" to sykepengegrunnlag,
                "6G" to seksG,
            ).plus(
                when (yrkesaktivitetssporing) {
                    Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                    is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> when (sykepengegrunnlagsfakta) {
                        is FastsattIInfotrygd -> mapOf(
                            "fastsatt" to sykepengegrunnlagsfakta.fastsatt
                        )

                        is FastsattEtterHovedregel -> arbeidstakerHovedregelMap(sykepengegrunnlagsfakta)
                        is FastsattEtterSkjønn -> arbeidstakerEtterSkjønnMap(sykepengegrunnlagsfakta)
                    }.plus("selvstendig" to null)

                    Behandlingsporing.Yrkesaktivitet.Selvstendig -> selvstendigMap()

                    Behandlingsporing.Yrkesaktivitet.Frilans-> TODO("Har ikke implementert disse yrkesaktivitetstypene enda i sykepengegrunnlagsfakta")
                }
            ),
            "arbeidssituasjon" to arbeidssituasjon.name
        )

        val godkjenningEvent = EventSubscription.GodkjenningEvent(
            yrkesaktivitetssporing = yrkesaktivitetssporing,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            periodetype = periodetypeForGodkjenningsbehov,
            førstegangsbehandling = tags.contains(Tag.Førstegangsbehandling),
            utbetalingtype = if (tags.contains(Tag.Revurdering)) "REVURDERING" else "UTBETALING",
            inntektskilde = if (tags.contains(Tag.EnArbeidsgiver)) "EN_ARBEIDSGIVER" else "FLERE_ARBEIDSGIVERE",
            // Til ettertanke: Her kan det være orgnummer på tilkomnde arbeidsgivere i tillegg til de som er i "sykepengegrunnlagsfakta". Kanskje finne på noe smartere der?
            orgnummereMedRelevanteArbeidsforhold = (arbeidsgiverinntekter.map { it.arbeidsgiver }).toSet(),
            tags = tags.utgående,
            kanAvvises = kanForkastes,
            relevanteSøknader = relevanteSøknader,
            perioderMedSammeSkjæringstidspunkt = perioderMedSammeSkjæringstidspunkt.map {
                EventSubscription.GodkjenningEvent.PeriodeMedSammeSkjæringstidspunkt(
                    vedtaksperiodeId = it.vedtaksperiodeId,
                    behandlingId = it.behandlingId,
                    periode = it.periode
                )
            },
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            foreløpigBeregnetSluttPåSykepenger = foreløpigBeregnetSluttPåSykepenger,
            utbetalingsdager = utbetalingsdager,
            arbeidssituasjon = arbeidssituasjon.name,
            sykepengegrunnlagsfakta = when (yrkesaktivitetssporing) {
                Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                is Behandlingsporing.Yrkesaktivitet.Arbeidstaker -> when (sykepengegrunnlagsfakta) {
                    is FastsattEtterHovedregel -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterHovedregel(
                        sykepengegrunnlag = sykepengegrunnlag,
                        seksG = seksG,
                        arbeidsgivere = sykepengegrunnlagsfakta.arbeidsgivere.map {
                            EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterHovedregel.Arbeidsgiver(
                                arbeidsgiver = it.arbeidsgiver,
                                omregnetÅrsinntekt = it.omregnetÅrsinntekt,
                                inntektskilde = it.inntektskilde.name
                            )
                        }
                    )

                    is FastsattEtterSkjønn -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterSkjønn(
                        sykepengegrunnlag = sykepengegrunnlag,
                        seksG = seksG,
                        arbeidsgivere = sykepengegrunnlagsfakta.arbeidsgivere.map {
                            EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerEtterSkjønn.Arbeidsgiver(
                                arbeidsgiver = it.arbeidsgiver,
                                omregnetÅrsinntekt = it.omregnetÅrsinntekt,
                                skjønnsfastsatt = it.skjønnsfastsatt,
                            )
                        }
                    )

                    is FastsattIInfotrygd -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.ArbeidstakerFraInfotrygd(
                        sykepengegrunnlag = sykepengegrunnlag,
                        seksG = seksG
                    )
                }

                Behandlingsporing.Yrkesaktivitet.Selvstendig -> EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.SelvstendigEtterHovedregel(
                    sykepengegrunnlag = sykepengegrunnlag,
                    seksG = seksG,
                    pensjonsgivendeInntekter = pensjonsgivendeInntekter.map {
                        EventSubscription.GodkjenningEvent.Sykepengegrunnlagsfakta.SelvstendigEtterHovedregel.PensjonsgivendeInntekt(
                            årstall = it.årstall,
                            beløp = it.beløp.årlig.toDesimaler
                        )
                    },
                    beregningsgrunnlag = beregningsgrunnlag.årlig.toDesimaler
                )

                Behandlingsporing.Yrkesaktivitet.Frilans -> error("Har ikke implementert sykepengegrunnlag for frilansere ennå.")
            }
        )

        private fun arbeidstakerEtterSkjønnMap(sykepengegrunnlagsfakta: FastsattEtterSkjønn): Map<String, Any> = mapOf(
            "fastsatt" to sykepengegrunnlagsfakta.fastsatt,
            "arbeidsgivere" to sykepengegrunnlagsfakta.arbeidsgivere.map {
                mapOf(
                    "arbeidsgiver" to it.arbeidsgiver,
                    "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                    "skjønnsfastsatt" to it.skjønnsfastsatt,
                    "inntektskilde" to "Saksbehandler",
                )
            }
        )

        private fun arbeidstakerHovedregelMap(sykepengegrunnlagsfakta: FastsattEtterHovedregel): Map<String, Any> = mapOf(
            "fastsatt" to sykepengegrunnlagsfakta.fastsatt,
            "arbeidsgivere" to sykepengegrunnlagsfakta.arbeidsgivere.map {
                mapOf(
                    "arbeidsgiver" to it.arbeidsgiver,
                    "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                    "inntektskilde" to it.inntektskilde.name
                )
            }
        )

        private fun selvstendigMap(): Map<String, Any> = mapOf(
            "selvstendig" to mapOf(
                "pensjonsgivendeInntekter" to pensjonsgivendeInntekter.map {
                    mapOf(
                        "årstall" to it.årstall.value,
                        "beløp" to it.beløp.årlig.toDesimaler
                    )
                },
                "beregningsgrunnlag" to beregningsgrunnlag.årlig.toDesimaler,
            ),
            "arbeidsgivere" to emptyList<Map<String, Any>>(), // Selvstendig har ingen arbeidsgivere i sykepengegrunnlaget
            "fastsatt" to "EtterHovedregel"
        )

        // TODO: 10.12.24 Rename skjønssfastsatt til fastsattÅrsinntekt (som er lik omregnet for hovedregel med forskjellige beløp på skjønn)
        //  Burde omregnetÅrsinntekt være et objekt med beløp og kilde slik at det er tydligere at kilden hører til dét
        //  Burde det hete noe annet? Ikke gitt at det skal bli et vedtak. `behandling_beregnet` ? 🤔
        //  Skal Spleis lytte på noe àla `behandling_utført` / `behandling_ferdig_behandlet` / `behandling_vurdert` som tommel opp/ned på godkjenningsbehov ?
        //  Sende med en liste med dager & beløp ?
        val utkastTilVedtak = EventSubscription.UtkastTilVedtakEvent(
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            behandlingId = behandlingId,
            tags = tags.utgående,
            `6G` = when (val fakta = sykepengegrunnlagsfakta) {
                is FastsattIInfotrygd -> null
                is FastsattEtterHovedregel -> fakta.`6G`
                is FastsattEtterSkjønn -> fakta.`6G`
            },
            yrkesaktivitetssporing = yrkesaktivitetssporing
        )

        fun avsluttetMedVedtak(vedtakFattet: LocalDateTime) = EventSubscription.AvsluttetMedVedtakEvent(
            yrkesaktivitetssporing = yrkesaktivitetssporing,
            vedtaksperiodeId = vedtaksperiodeId,
            behandlingId = behandlingId,
            periode = periode,
            hendelseIder = relevanteSøknader,
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlag = beregningsgrunnlag.årlig.toDesimaler,
            `6G` = seksG,
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
        ArbeidsgiverØnskerRefusjon,
        OverlapperMedInfotrygd
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
    }
}
