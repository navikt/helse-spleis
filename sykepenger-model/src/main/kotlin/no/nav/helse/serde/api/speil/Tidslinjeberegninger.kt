package no.nav.helse.serde.api.speil

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.person.Vedtaksperiode.AvsluttetUtenUtbetaling
import no.nav.helse.person.Vedtaksperiode.AvventerBlokkerendePeriode
import no.nav.helse.person.Vedtaksperiode.AvventerGodkjenning
import no.nav.helse.person.Vedtaksperiode.AvventerGodkjenningRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerHistorikk
import no.nav.helse.person.Vedtaksperiode.AvventerRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerSimulering
import no.nav.helse.person.Vedtaksperiode.AvventerSimuleringRevurdering
import no.nav.helse.person.Vedtaksperiode.AvventerVilkårsprøving
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.serde.api.dto.AvvistDag
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.HendelseDTO
import no.nav.helse.serde.api.dto.HendelseDTO.Companion.finn
import no.nav.helse.serde.api.dto.Periodetilstand
import no.nav.helse.serde.api.dto.SammenslåttDag
import no.nav.helse.serde.api.dto.SpeilOppdrag
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.dto.SøknadNavDTO
import no.nav.helse.serde.api.dto.Tidslinjeperiode
import no.nav.helse.serde.api.dto.Tidslinjeperiodetype
import no.nav.helse.serde.api.dto.UberegnetPeriode
import no.nav.helse.serde.api.dto.Utbetalingstatus.Annullert
import no.nav.helse.serde.api.dto.Utbetalingstatus.Godkjent
import no.nav.helse.serde.api.dto.Utbetalingstatus.GodkjentUtenUtbetaling
import no.nav.helse.serde.api.dto.Utbetalingstatus.IkkeGodkjent
import no.nav.helse.serde.api.dto.Utbetalingstatus.Overført
import no.nav.helse.serde.api.dto.Utbetalingstatus.Ubetalt
import no.nav.helse.serde.api.dto.Utbetalingstatus.Utbetalt
import no.nav.helse.serde.api.dto.Utbetalingstidslinjedag
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.serde.api.speil.builders.BeregningId
import no.nav.helse.serde.api.speil.builders.IVilkårsgrunnlag
import no.nav.helse.serde.api.speil.builders.IVilkårsgrunnlagHistorikk
import no.nav.helse.serde.api.speil.builders.OppdragBuilder
import no.nav.helse.serde.api.speil.builders.PeriodeVarslerBuilder
import no.nav.helse.serde.api.speil.builders.UtbetalingstidslinjeBuilder
import no.nav.helse.serde.api.speil.builders.VurderingBuilder
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.UtbetalingInntektskilde
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype

internal class Tidslinjeberegninger {
    private val sykdomstidslinjer = mutableMapOf<UUID, List<Sykdomstidslinjedag>>()
    private val opphørteUtbetalinger = mutableSetOf<UUID>()
    private var utbetalingTilGodkjenning: Vedtaksperiodeutbetaling? = null

    private val beregningklosser = mutableListOf<Beregningkloss>()
    private val utbetalingklosser = mutableListOf<Utbetalingkloss>()
    private val vedtaksperioderklosser = mutableListOf<Vedtaksperiodekloss>()

    internal fun build(organisasjonsnummer: String, alder: Alder, vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk): List<Tidslinjeperiode> {
        val beregninger = beregningklosser.map { it.tilTidslinjeberegning(sykdomstidslinjer) }
        val utbetalinger = utbetalingklosser.mapNotNull { it.tilUtbetaling(beregninger, opphørteUtbetalinger, utbetalingTilGodkjenning) }
        return vedtaksperioderklosser.flatMap { it.tilTidslinjeperiode(organisasjonsnummer, alder, utbetalinger, vilkårsgrunnlaghistorikk) }
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
        fom: LocalDate,
        tom: LocalDate,
        hendelser: List<HendelseDTO>,
        utbetalinger: List<Vedtaksperiodeutbetaling>,
        sykdomstidslinje: List<Sykdomstidslinjedag>,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        tilstand: Vedtaksperiodetilstand,
        skjæringstidspunkt: LocalDate,
        aktivitetsloggForPeriode: Aktivitetslogg
    ) {
        if (utbetalinger.isEmpty()) {
            vedtaksperioderklosser.add(Vedtaksperiodekloss.PeriodeUtenUtbetaling(
                vedtaksperiodeId = vedtaksperiode,
                fom = fom,
                tom = tom,
                hendelser = hendelser,
                vedtaksperiodeutbetalinger = utbetalinger,
                sykdomstidslinje = sykdomstidslinje,
                opprettet = opprettet,
                oppdatert = oppdatert,
                tilstand = tilstand,
                skjæringstidspunkt = skjæringstidspunkt,
                aktivitetsloggForPeriode = aktivitetsloggForPeriode
            ))
            return
        }
        if (tilstand in listOf(AvventerGodkjenning, AvventerGodkjenningRevurdering)) {
            utbetalingTilGodkjenning = utbetalinger.last()
        }
        vedtaksperioderklosser.add(Vedtaksperiodekloss.PeriodeMedUtbetaling(
            vedtaksperiodeId = vedtaksperiode,
            fom = fom,
            tom = tom,
            hendelser = hendelser,
            vedtaksperiodeutbetalinger = utbetalinger,
            sykdomstidslinje = sykdomstidslinje,
            opprettet = opprettet,
            oppdatert = oppdatert,
            tilstand = tilstand,
            skjæringstidspunkt = skjæringstidspunkt,
            aktivitetsloggForPeriode = aktivitetsloggForPeriode
        ))
    }

    fun leggTilForkastetVedtaksperiode(
        vedtaksperiode: UUID,
        fom: LocalDate,
        tom: LocalDate,
        hendelser: List<HendelseDTO>,
        utbetalinger: List<Vedtaksperiodeutbetaling>,
        sykdomstidslinje: List<Sykdomstidslinjedag>,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        tilstand: Vedtaksperiodetilstand,
        skjæringstidspunkt: LocalDate,
        aktivitetsloggForPeriode: Aktivitetslogg
    ) {
        // forkastede perioder tas med bare dersom de har vært annullert; dvs. de må ha minst én utbetaling iallfall
        if (utbetalinger.isEmpty()) return
        vedtaksperioderklosser.add(Vedtaksperiodekloss.AnnullertPeriode(
            vedtaksperiodeId = vedtaksperiode,
            fom = fom,
            tom = tom,
            hendelser = hendelser,
            vedtaksperiodeutbetalinger = utbetalinger,
            sykdomstidslinje = sykdomstidslinje,
            opprettet = opprettet,
            oppdatert = oppdatert,
            tilstand = tilstand,
            skjæringstidspunkt = skjæringstidspunkt,
            aktivitetsloggForPeriode = aktivitetsloggForPeriode
        ))
    }

    internal class Vedtaksperiodeutbetaling(private val utbetalingId: UUID, private val vilkårsgrunnlagId: UUID) {
        fun utbetaling(utbetalinger: List<IUtbetaling>): IUtbetaling? {
            return utbetalinger.singleOrNull { it.id == utbetalingId }
        }
        fun vilkårsgrunnlag(vilkårsgrunnlag: IVilkårsgrunnlagHistorikk): IVilkårsgrunnlag {
            return vilkårsgrunnlag.leggIBøtta(this.vilkårsgrunnlagId)
        }

        fun erTilGodkjenning(id: UUID): Boolean {
            return this.utbetalingId == id
        }
    }

    private sealed class Vedtaksperiodekloss(
        protected val vedtaksperiodeId: UUID,
        protected val fom: LocalDate,
        protected val tom: LocalDate,
        protected val hendelser: List<HendelseDTO>,
        protected val vedtaksperiodeutbetalinger: List<Vedtaksperiodeutbetaling>,
        protected val sykdomstidslinje: List<Sykdomstidslinjedag>,
        protected val opprettet: LocalDateTime,
        protected val oppdatert: LocalDateTime,
        protected val tilstand: Vedtaksperiodetilstand,
        protected val skjæringstidspunkt: LocalDate,
        protected val aktivitetsloggForPeriode: Aktivitetslogg
    ) {
        abstract fun tilTidslinjeperiode(
            organisasjonsnummer: String,
            alder: Alder,
            utbetalinger: List<IUtbetaling>,
            vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk
        ): List<Tidslinjeperiode>

        class PeriodeUtenUtbetaling(
            vedtaksperiodeId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            hendelser: List<HendelseDTO>,
            vedtaksperiodeutbetalinger: List<Vedtaksperiodeutbetaling>,
            sykdomstidslinje: List<Sykdomstidslinjedag>,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            tilstand: Vedtaksperiodetilstand,
            skjæringstidspunkt: LocalDate,
            aktivitetsloggForPeriode: Aktivitetslogg
        ) : Vedtaksperiodekloss(vedtaksperiodeId, fom, tom, hendelser, vedtaksperiodeutbetalinger, sykdomstidslinje, opprettet, oppdatert, tilstand, skjæringstidspunkt, aktivitetsloggForPeriode) {
            override fun tilTidslinjeperiode(
                organisasjonsnummer: String,
                alder: Alder,
                utbetalinger: List<IUtbetaling>,
                vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk
            ): List<Tidslinjeperiode> {
                val tidslinje = sykdomstidslinje.merge(emptyList())
                return listOf(UberegnetPeriode(
                    vedtaksperiodeId = vedtaksperiodeId,
                    fom = fom,
                    tom = tom,
                    sammenslåttTidslinje = tidslinje,
                    periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING, // feltet gir ikke mening for uberegnede perioder
                    inntektskilde = UtbetalingInntektskilde.EN_ARBEIDSGIVER, // feltet gir ikke mening for uberegnede perioder
                    erForkastet = false,
                    opprettet = opprettet,
                    oppdatert = oppdatert,
                    skjæringstidspunkt = skjæringstidspunkt,
                    hendelser = hendelser,
                    periodetilstand = when (tilstand) {
                        is AvsluttetUtenUtbetaling -> Periodetilstand.IngenUtbetaling
                        is AvventerRevurdering,
                        is AvventerBlokkerendePeriode -> Periodetilstand.VenterPåAnnenPeriode
                        is AvventerHistorikk,
                        is AvventerVilkårsprøving -> Periodetilstand.ForberederGodkjenning
                        else -> Periodetilstand.ManglerInformasjon
                    }
                ))
            }
        }
        class PeriodeMedUtbetaling(
            vedtaksperiodeId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            hendelser: List<HendelseDTO>,
            vedtaksperiodeutbetalinger: List<Vedtaksperiodeutbetaling>,
            sykdomstidslinje: List<Sykdomstidslinjedag>,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            tilstand: Vedtaksperiodetilstand,
            skjæringstidspunkt: LocalDate,
            aktivitetsloggForPeriode: Aktivitetslogg
        ) : Vedtaksperiodekloss(vedtaksperiodeId, fom, tom, hendelser, vedtaksperiodeutbetalinger, sykdomstidslinje, opprettet, oppdatert, tilstand, skjæringstidspunkt, aktivitetsloggForPeriode) {
            override fun tilTidslinjeperiode(
                organisasjonsnummer: String,
                alder: Alder,
                utbetalinger: List<IUtbetaling>,
                vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk
            ) = vedtaksperiodeutbetalinger.mapNotNull { vedtaksperiodeutbetaling ->
                vedtaksperiodeutbetaling.utbetaling(utbetalinger)?.let { mapTilBeregnetPeriode(organisasjonsnummer, alder, vedtaksperiodeutbetaling.vilkårsgrunnlag(vilkårsgrunnlaghistorikk), it) }
            }

            private fun mapTilBeregnetPeriode(organisasjonsnummer: String, alder: Alder, vilkårsgrunnlag: IVilkårsgrunnlag, utbetaling: IUtbetaling): BeregnetPeriode {
                val avgrensetUtbetalingstidslinje = utbetaling.utbetalingstidslinje.filter { it.dato in fom..tom }
                val sammenslåttTidslinje = utbetaling.sammenslåttTidslinje(fom, tom)
                val varsler = PeriodeVarslerBuilder(aktivitetsloggForPeriode).build()
                val utbetalingDTO = utbetaling.toDTO()

                return BeregnetPeriode(
                    vedtaksperiodeId = vedtaksperiodeId,
                    beregningId = utbetaling.beregning.beregningId,
                    fom = fom,
                    tom = tom,
                    erForkastet = false,
                    periodetype = vilkårsgrunnlag.utledPeriodetype(organisasjonsnummer, vedtaksperiodeId),
                    inntektskilde = if (vilkårsgrunnlag.inntekter.count { it.omregnetÅrsinntekt != null } > 1) UtbetalingInntektskilde.FLERE_ARBEIDSGIVERE else UtbetalingInntektskilde.EN_ARBEIDSGIVER,
                    skjæringstidspunkt = vilkårsgrunnlag.skjæringstidspunkt,
                    hendelser = hendelser,
                    maksdato = utbetaling.maksdato,
                    beregnet = utbetaling.opprettet,
                    opprettet = opprettet,
                    oppdatert = oppdatert,
                    periodevilkår = periodevilkår(alder, vilkårsgrunnlag.skjæringstidspunkt, utbetaling, avgrensetUtbetalingstidslinje, hendelser),
                    sammenslåttTidslinje = sammenslåttTidslinje,
                    gjenståendeSykedager = utbetaling.gjenståendeSykedager,
                    forbrukteSykedager = utbetaling.forbrukteSykedager,
                    utbetaling = utbetalingDTO,
                    vilkårsgrunnlagId = vilkårsgrunnlag.id,
                    aktivitetslogg = varsler,
                    periodetilstand = utledePeriodetilstand(utbetalingDTO, tilstand, avgrensetUtbetalingstidslinje),
                )
            }

            private fun utledePeriodetilstand(utbetalingDTO: no.nav.helse.serde.api.dto.Utbetaling, periodetilstand: Vedtaksperiodetilstand, avgrensetUtbetalingstidslinje: List<Utbetalingstidslinjedag>) =
                when (utbetalingDTO.status) {
                    IkkeGodkjent -> Periodetilstand.RevurderingFeilet
                    Utbetalt -> when {
                        periodetilstand == AvventerRevurdering -> Periodetilstand.UtbetaltVenterPåAnnenPeriode
                        avgrensetUtbetalingstidslinje.none { it.type == UtbetalingstidslinjedagType.NavDag } -> Periodetilstand.IngenUtbetaling
                        else -> Periodetilstand.Utbetalt
                    }
                    Ubetalt -> when {
                        utbetalingDTO.tilGodkjenning() -> Periodetilstand.TilGodkjenning
                        periodetilstand in setOf(
                            AvventerSimulering,
                            AvventerSimuleringRevurdering
                        ) -> Periodetilstand.ForberederGodkjenning
                        else -> Periodetilstand.VenterPåAnnenPeriode
                    }
                    GodkjentUtenUtbetaling -> when (utbetalingDTO.type) {
                        no.nav.helse.serde.api.dto.Utbetalingtype.REVURDERING -> Periodetilstand.Utbetalt
                        else -> Periodetilstand.IngenUtbetaling
                    }
                    Godkjent,
                    Overført -> Periodetilstand.TilUtbetaling
                    else -> error("har ikke mappingregel for ${utbetalingDTO.status}")
                }

            private fun List<Utbetalingstidslinjedag>.sisteNavDag() =
                lastOrNull { it.type == UtbetalingstidslinjedagType.NavDag }

            private fun periodevilkår(
                alder: Alder,
                skjæringstidspunkt: LocalDate,
                utbetaling: IUtbetaling,
                avgrensetUtbetalingstidslinje: List<Utbetalingstidslinjedag>,
                hendelser: List<HendelseDTO>
            ): BeregnetPeriode.Vilkår {
                val sisteSykepengedag = avgrensetUtbetalingstidslinje.sisteNavDag()?.dato ?: tom
                val sykepengedager = BeregnetPeriode.Sykepengedager(
                    skjæringstidspunkt,
                    utbetaling.maksdato,
                    utbetaling.forbrukteSykedager,
                    utbetaling.gjenståendeSykedager,
                    utbetaling.maksdato > sisteSykepengedag
                )
                val alderSisteSykepengedag = alder.let {
                    BeregnetPeriode.Alder(it.alderPåDato(sisteSykepengedag), it.innenfor70årsgrense(sisteSykepengedag))
                }
                val søknadsfrist = hendelser.finn<SøknadNavDTO>()?.let {
                    BeregnetPeriode.Søknadsfrist(
                        sendtNav = it.sendtNav,
                        søknadFom = it.fom,
                        søknadTom = it.tom,
                        oppfylt = it.søknadsfristOppfylt()
                    )
                }

                return BeregnetPeriode.Vilkår(sykepengedager, alderSisteSykepengedag, søknadsfrist)
            }
        }
        class AnnullertPeriode(
            vedtaksperiodeId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            hendelser: List<HendelseDTO>,
            vedtaksperiodeutbetalinger: List<Vedtaksperiodeutbetaling>,
            sykdomstidslinje: List<Sykdomstidslinjedag>,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            tilstand: Vedtaksperiodetilstand,
            skjæringstidspunkt: LocalDate,
            aktivitetsloggForPeriode: Aktivitetslogg
        ) : Vedtaksperiodekloss(vedtaksperiodeId, fom, tom, hendelser, vedtaksperiodeutbetalinger, sykdomstidslinje, opprettet, oppdatert, tilstand, skjæringstidspunkt, aktivitetsloggForPeriode) {
            private fun annullerteUtbetalinger(utbetalinger: List<IUtbetaling>) = vedtaksperiodeutbetalinger
                .mapNotNull { it.utbetaling(utbetalinger) }
                .groupBy { it.korrelasjonsId }
                .mapNotNull { (_, utbetalingene) ->
                    utbetalinger.firstOrNull { it.annulleringFor(utbetalingene.first()) }?.let {
                        it to utbetalingene
                    }
                }

            override fun tilTidslinjeperiode(
                organisasjonsnummer: String,
                alder: Alder,
                utbetalinger: List<IUtbetaling>,
                vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk
            ) = annullerteUtbetalinger(utbetalinger).flatMap { (annulleringen, utbetalingene) ->
                // en annullert periode har også en tidligere utbetalt periode med minst én utbetalt utbetaling
                val utbetaltePerioder = PeriodeMedUtbetaling(vedtaksperiodeId, fom, tom, hendelser, vedtaksperiodeutbetalinger, sykdomstidslinje, opprettet, oppdatert, tilstand, skjæringstidspunkt, aktivitetsloggForPeriode)
                    .tilTidslinjeperiode(organisasjonsnummer, alder, utbetalingene, vilkårsgrunnlaghistorikk)

                val utbetalingDTO = annulleringen.toDTO()
                utbetaltePerioder + listOf(BeregnetPeriode(
                    vedtaksperiodeId = vedtaksperiodeId,
                    beregningId = annulleringen.beregning.beregningId,
                    fom = fom,
                    tom = tom,
                    erForkastet = true,
                    periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING, // feltet gir ikke mening for annullert periode
                    inntektskilde = UtbetalingInntektskilde.EN_ARBEIDSGIVER, // feltet gir ikke mening for annullert periode
                    skjæringstidspunkt = fom, // feltet gir ikke mening for annullert periode
                    hendelser = hendelser,
                    maksdato = LocalDate.MAX, // feltet gir ikke mening for annullert periode
                    beregnet = annulleringen.opprettet,
                    opprettet = opprettet,
                    oppdatert = oppdatert,
                    periodevilkår = periodevilkårForAnnullertPeriode(alder), // feltet gir ikke mening for annullert periode
                    sammenslåttTidslinje = emptyList(), // feltet gir ikke mening for annullert periode
                    gjenståendeSykedager = null, // feltet gir ikke mening for annullert periode
                    forbrukteSykedager = null, // feltet gir ikke mening for annullert periode
                    utbetaling = utbetalingDTO,
                    vilkårsgrunnlagId = null, // feltet gir ikke mening for annullert periode
                    aktivitetslogg = emptyList(), // feltet gir ikke mening for annullert periode
                    periodetilstand = when (utbetalingDTO.status) {
                        Annullert -> Periodetilstand.Annullert
                        else -> Periodetilstand.TilAnnullering
                    }
                ))
            }

            private fun periodevilkårForAnnullertPeriode(alder: Alder) = BeregnetPeriode.Vilkår(
                sykepengedager = BeregnetPeriode.Sykepengedager(fom, LocalDate.MAX, null, null, false),
                alder = BeregnetPeriode.Alder(alder.alderPåDato(tom), alder.innenfor70årsgrense(tom)),
                søknadsfrist = null
            )
        }

    }

    private class Beregningkloss(
        private val beregningId: BeregningId,
        private val sykdomshistorikkElementId: UUID
    ) {
        fun tilTidslinjeberegning(sykdomstidslinjer: MutableMap<UUID, List<Sykdomstidslinjedag>>) =
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
        fun tilUtbetaling(tidslinjeberegninger: List<ITidslinjeberegning>, opphørteUtbetalinger: Set<UUID>, utbetalingTilGodkjenning: Vedtaksperiodeutbetaling?): IUtbetaling? {
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
                oppdrag = oppdrag,
                erTilGodkjenning = utbetalingTilGodkjenning?.erTilGodkjenning(this.id) ?: false
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

private fun List<Sykdomstidslinjedag>.merge(utbetalingstidslinje: List<Utbetalingstidslinjedag>): List<SammenslåttDag> {

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
