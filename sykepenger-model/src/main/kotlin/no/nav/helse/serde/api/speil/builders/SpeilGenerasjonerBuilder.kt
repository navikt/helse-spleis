package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingtypeDto
import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.OppdragUtDto
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.Generasjoner
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.serde.api.SpekematDTO
import no.nav.helse.serde.api.dto.AnnullertUtbetaling
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.EndringskodeDTO
import no.nav.helse.serde.api.dto.SpeilGenerasjonDTO
import no.nav.helse.serde.api.dto.SpeilOppdrag
import no.nav.helse.serde.api.dto.SpeilTidslinjeperiode
import no.nav.helse.serde.api.dto.SpeilTidslinjeperiode.Companion.utledPeriodetyper
import no.nav.helse.serde.api.dto.UberegnetPeriode
import no.nav.helse.serde.api.speil.builders.ArbeidsgiverBuilder.Companion.fjernUnødvendigeRader
import no.nav.helse.serde.api.speil.builders.SpeilGenerasjonerBuilder.Byggetilstand.AktivePerioder
import no.nav.helse.serde.api.speil.builders.SpeilGenerasjonerBuilder.Byggetilstand.ForkastedePerioder
import no.nav.helse.serde.api.speil.builders.SpeilGenerasjonerBuilder.Byggetilstand.Initiell
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype

// Besøker hele arbeidsgiver-treet
internal class SpeilGenerasjonerBuilder(
    private val organisasjonsnummer: String,
    private val alder: Alder,
    arbeidsgiver: Arbeidsgiver,
    private val arbeidsgiverUtDto: ArbeidsgiverUtDto,
    private val vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk,
    private val pølsepakke: SpekematDTO.PølsepakkeDTO
) : ArbeidsgiverVisitor {
    private var tilstand: Byggetilstand = Initiell

    private val allePerioder = mutableListOf<SpeilTidslinjeperiode>()
    private val annullertePerioder = mutableListOf<Pair<UberegnetPeriode, BeregnetPeriode>>()

    private val utbetalinger = mapUtbetalinger()
    private val annulleringer = mapAnnulleringer()
    private val utbetalingstidslinjer = mapUtbetalingstidslinjer()

    init {
        arbeidsgiver.accept(this)
    }

    internal fun build(): List<SpeilGenerasjonDTO> {
        return buildSpekemat()
    }

    private fun mapUtbetalingstidslinjer(): List<Pair<UUID, UtbetalingstidslinjeBuilder>> {
        return arbeidsgiverUtDto.utbetalinger.map {
            it.id to UtbetalingstidslinjeBuilder(it.utbetalingstidslinje)
        }
    }

    private fun mapUtbetalinger(): List<no.nav.helse.serde.api.dto.Utbetaling> {
        return arbeidsgiverUtDto.utbetalinger
            .filter { it.type in setOf(UtbetalingtypeDto.REVURDERING, UtbetalingtypeDto.UTBETALING) }
            .mapNotNull {
                no.nav.helse.serde.api.dto.Utbetaling(
                    id = it.id,
                    type = when (it.type) {
                        UtbetalingtypeDto.REVURDERING -> no.nav.helse.serde.api.dto.Utbetalingtype.REVURDERING
                        UtbetalingtypeDto.UTBETALING -> no.nav.helse.serde.api.dto.Utbetalingtype.UTBETALING
                        else -> error("Forventer ikke mapping for utbetalingtype=${it.type}")
                    },
                    korrelasjonsId = it.korrelasjonsId,
                    status = when (it.tilstand) {
                        UtbetalingTilstandDto.ANNULLERT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Annullert
                        UtbetalingTilstandDto.GODKJENT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Godkjent
                        UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING -> no.nav.helse.serde.api.dto.Utbetalingstatus.GodkjentUtenUtbetaling
                        UtbetalingTilstandDto.IKKE_GODKJENT -> no.nav.helse.serde.api.dto.Utbetalingstatus.IkkeGodkjent
                        UtbetalingTilstandDto.IKKE_UTBETALT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Ubetalt
                        UtbetalingTilstandDto.OVERFØRT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Overført
                        UtbetalingTilstandDto.UTBETALT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Utbetalt
                        else -> return@mapNotNull null
                    },
                    maksdato = it.maksdato!!,
                    forbrukteSykedager = it.forbrukteSykedager!!,
                    gjenståendeDager = it.gjenståendeSykedager!!,
                    arbeidsgiverNettoBeløp = it.arbeidsgiverOppdrag.nettoBeløp,
                    arbeidsgiverFagsystemId = it.arbeidsgiverOppdrag.fagsystemId,
                    personNettoBeløp = it.personOppdrag.nettoBeløp,
                    personFagsystemId = it.personOppdrag.fagsystemId,
                    oppdrag = mapOf(
                        it.arbeidsgiverOppdrag.fagsystemId to mapOppdrag(it.arbeidsgiverOppdrag),
                        it.personOppdrag.fagsystemId to mapOppdrag(it.personOppdrag),
                    ),
                    vurdering = it.vurdering?.let { vurdering ->
                        no.nav.helse.serde.api.dto.Utbetaling.Vurdering(
                            godkjent = vurdering.godkjent,
                            tidsstempel = vurdering.tidspunkt,
                            automatisk = vurdering.automatiskBehandling,
                            ident = vurdering.ident
                        )
                    }
                )
            }
    }

    private fun mapAnnulleringer(): List<AnnullertUtbetaling> {
        return arbeidsgiverUtDto.utbetalinger
            .filter { it.type == UtbetalingtypeDto.ANNULLERING }
            .mapNotNull {
                AnnullertUtbetaling(
                    id = it.id,
                    korrelasjonsId = it.korrelasjonsId,
                    annulleringstidspunkt = it.tidsstempel,
                    utbetalingstatus = when (it.tilstand) {
                        UtbetalingTilstandDto.ANNULLERT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Annullert
                        UtbetalingTilstandDto.GODKJENT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Godkjent
                        UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING -> no.nav.helse.serde.api.dto.Utbetalingstatus.GodkjentUtenUtbetaling
                        UtbetalingTilstandDto.IKKE_GODKJENT -> no.nav.helse.serde.api.dto.Utbetalingstatus.IkkeGodkjent
                        UtbetalingTilstandDto.IKKE_UTBETALT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Ubetalt
                        UtbetalingTilstandDto.OVERFØRT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Overført
                        UtbetalingTilstandDto.UTBETALT -> no.nav.helse.serde.api.dto.Utbetalingstatus.Utbetalt
                        else -> return@mapNotNull null
                    }
                )
            }
    }

    private fun mapOppdrag(dto: OppdragUtDto): SpeilOppdrag {
        return SpeilOppdrag(
            fagsystemId = dto.fagsystemId,
            tidsstempel = dto.tidsstempel,
            nettobeløp = dto.nettoBeløp,
            simulering = dto.simuleringsResultat?.let {
                SpeilOppdrag.Simulering(
                    totalbeløp = it.totalbeløp,
                    perioder = it.perioder.map {
                        SpeilOppdrag.Simuleringsperiode(
                            fom = it.fom,
                            tom = it.tom,
                            utbetalinger = it.utbetalinger.map {
                                SpeilOppdrag.Simuleringsutbetaling(
                                    mottakerNavn = it.utbetalesTil.navn,
                                    mottakerId = it.utbetalesTil.id,
                                    forfall = it.forfallsdato,
                                    feilkonto = it.feilkonto,
                                    detaljer = it.detaljer.map {
                                        SpeilOppdrag.Simuleringsdetaljer(
                                            faktiskFom = it.fom,
                                            faktiskTom = it.tom,
                                            konto = it.konto,
                                            beløp = it.beløp,
                                            tilbakeføring = it.tilbakeføring,
                                            sats = it.sats.sats,
                                            typeSats = it.sats.type,
                                            antallSats = it.sats.antall,
                                            uføregrad = it.uføregrad,
                                            klassekode = it.klassekode.kode,
                                            klassekodeBeskrivelse = it.klassekode.beskrivelse,
                                            utbetalingstype = it.utbetalingstype,
                                            refunderesOrgNr = it.refunderesOrgnummer
                                        )
                                    }
                                )
                            }
                        )
                    }
                )
            },
            utbetalingslinjer = dto.linjer.map { linje ->
                SpeilOppdrag.Utbetalingslinje(
                    fom = linje.fom,
                    tom = linje.tom,
                    dagsats = linje.beløp,
                    grad = linje.grad!!,
                    endringskode = when (linje.endringskode) {
                        EndringskodeDto.ENDR -> EndringskodeDTO.ENDR
                        EndringskodeDto.NY -> EndringskodeDTO.NY
                        EndringskodeDto.UEND -> EndringskodeDTO.UEND
                    }
                )
            }
        )
    }

    private fun buildSpekemat(): List<SpeilGenerasjonDTO> {
        val generasjoner = buildTidslinjeperioder()
        return pølsepakke.rader
            .fjernUnødvendigeRader()
            .map { rad -> mapRadTilSpeilGenerasjon(rad, generasjoner) }
            .filterNot { rad -> rad.size == 0 } // fjerner tomme rader
    }

    private fun buildTidslinjeperioder(): List<SpeilTidslinjeperiode> {
        val annullertePerioder = annullertePerioder.mapNotNull { (uberegnet, beregnet) ->
            val annulleringen = annulleringer.firstOrNull { it.annullerer(beregnet.utbetaling.korrelasjonsId) }
            annulleringen?.let { uberegnet.somAnnullering(it, beregnet) }
        }
        return allePerioder.filterNot { periode -> annullertePerioder.any { it.generasjonId == periode.generasjonId } } + annullertePerioder
    }

    private fun mapRadTilSpeilGenerasjon(rad: SpekematDTO.PølsepakkeDTO.PølseradDTO, generasjoner: List<SpeilTidslinjeperiode>): SpeilGenerasjonDTO {
        val perioder = rad.pølser.mapNotNull { pølse -> generasjoner.firstOrNull { it.generasjonId == pølse.behandlingId } }
        return SpeilGenerasjonDTO(
            id = UUID.randomUUID(),
            kildeTilGenerasjon = rad.kildeTilRad,
            perioder = perioder
                .map { it.registrerBruk(vilkårsgrunnlaghistorikk, organisasjonsnummer) }
                .utledPeriodetyper()
        )
    }

    override fun preVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        this.tilstand = AktivePerioder()
    }

    override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        this.tilstand = Initiell
    }

    override fun preVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        this.tilstand = ForkastedePerioder()
    }

    override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        this.tilstand = Initiell
    }

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: () -> LocalDate,
        hendelseIder: Set<Dokumentsporing>
    ) {
        this.tilstand.besøkVedtaksperiode(this, vedtaksperiode, id, skjæringstidspunkt(), tilstand, opprettet, oppdatert)
    }

    private val Generasjoner.Generasjon.Tilstand.uberegnet get() = this in setOf(
        Generasjoner.Generasjon.Tilstand.Uberegnet,
        Generasjoner.Generasjon.Tilstand.UberegnetOmgjøring,
        Generasjoner.Generasjon.Tilstand.UberegnetRevurdering,
        Generasjoner.Generasjon.Tilstand.AvsluttetUtenVedtak,
        Generasjoner.Generasjon.Tilstand.TilInfotrygd
    )
    override fun preVisitGenerasjon(
        id: UUID,
        tidsstempel: LocalDateTime,
        tilstand: Generasjoner.Generasjon.Tilstand,
        periode: Periode,
        vedtakFattet: LocalDateTime?,
        avsluttet: LocalDateTime?,
        kilde: Generasjoner.Generasjonkilde
    ) {
        if (tilstand.uberegnet) return this.tilstand.besøkUberegnetPeriode(this, periode, id, kilde.meldingsreferanseId, tidsstempel, avsluttet, tilstand == Generasjoner.Generasjon.Tilstand.TilInfotrygd)
        this.tilstand.besøkBeregnetPeriode(this, periode, id, kilde.meldingsreferanseId, tidsstempel, vedtakFattet, avsluttet)
    }

    override fun preVisitGenerasjonendring(
        id: UUID,
        tidsstempel: LocalDateTime,
        sykmeldingsperiode: Periode,
        periode: Periode,
        grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement?,
        utbetaling: Utbetaling?,
        dokumentsporing: Dokumentsporing,
        sykdomstidslinje: Sykdomstidslinje
    ) {
        this.tilstand.dokumentsporing(dokumentsporing)
    }

    override fun postVisitGenerasjon(
        id: UUID,
        tidsstempel: LocalDateTime,
        tilstand: Generasjoner.Generasjon.Tilstand,
        periode: Periode,
        vedtakFattet: LocalDateTime?,
        avsluttet: LocalDateTime?,
        kilde: Generasjoner.Generasjonkilde
    ) {
        if (tilstand.uberegnet) return this.tilstand.forlatUberegnetPeriode(this)
        this.tilstand.forlatBeregnetPeriode(this)
    }

    override fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        opptjening: Opptjening,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    ) {
        this.tilstand.besøkVilkårsgrunnlagelement(this, vilkårsgrunnlagId, skjæringstidspunkt)
    }

    override fun preVisitInfotrygdVilkårsgrunnlag(
        infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        vilkårsgrunnlagId: UUID
    ) {
        this.tilstand.besøkVilkårsgrunnlagelement(this, vilkårsgrunnlagId, skjæringstidspunkt)
    }

    override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje, låstePerioder: List<Periode>) {
        this.tilstand.besøkSykdomstidslinje(this, tidslinje)
    }

    override fun postVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: () -> LocalDate,
        hendelseIder: Set<Dokumentsporing>
    ) {
        this.tilstand.forlatVedtaksperiode(this)
    }

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        utbetalingstatus: Utbetalingstatus,
        periode: Periode,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        stønadsdager: Int,
        overføringstidspunkt: LocalDateTime?,
        avsluttet: LocalDateTime?,
        avstemmingsnøkkel: Long?,
        annulleringer: Set<UUID>
    ) {
        if (utbetalingstatus == Utbetalingstatus.FORKASTET) return
        tilstand.besøkUtbetaling(
            builder = this,
            id = id,
            korrelasjonsId = korrelasjonsId,
            type = type,
            utbetalingstatus = utbetalingstatus,
            tidsstempel = tidsstempel,
            maksdato = maksdato,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager
        )
    }

    private interface Byggetilstand {
        fun besøkUtbetaling(
            builder: SpeilGenerasjonerBuilder,
            id: UUID,
            korrelasjonsId: UUID,
            type: Utbetalingtype,
            utbetalingstatus: Utbetalingstatus,
            tidsstempel: LocalDateTime,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?
        ) {}
        fun besøkVedtaksperiode(
            builder: SpeilGenerasjonerBuilder,
            vedtaksperiode: Vedtaksperiode,
            vedtaksperiodeId: UUID,
            skjæringstidspunkt: LocalDate,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime
        ) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun besøkUberegnetPeriode(
            builder: SpeilGenerasjonerBuilder,
            periode: Periode,
            generasjonId: UUID,
            kilde: UUID,
            generasjonOpprettet: LocalDateTime,
            avsluttet: LocalDateTime?,
            forkastet: Boolean
        ) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun forlatUberegnetPeriode(builder: SpeilGenerasjonerBuilder) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun dokumentsporing(dokumentsporing: Dokumentsporing) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun besøkBeregnetPeriode(
            builder: SpeilGenerasjonerBuilder,
            periode: Periode,
            generasjonId: UUID,
            kilde: UUID,
            generasjonOpprettet: LocalDateTime,
            vedtakFattet: LocalDateTime?,
            avsluttet: LocalDateTime?
        ) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun forlatBeregnetPeriode(builder: SpeilGenerasjonerBuilder) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun besøkVilkårsgrunnlagelement(builder: SpeilGenerasjonerBuilder, vilkårsgrunnlagId: UUID, skjæringstidspunkt: LocalDate) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun besøkSykdomstidslinje(builder: SpeilGenerasjonerBuilder, sykdomstidslinje: Sykdomstidslinje) {}
        fun forlatVedtaksperiode(builder: SpeilGenerasjonerBuilder) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }

        object Initiell : Byggetilstand

        abstract class Periodebygger : Byggetilstand {
            protected var vedtaksperiodebuilder: TidslinjeperioderBuilder? = null
            private var beregnetPeriodeBuilder: BeregnetPeriode.Builder? = null
            private var uberegnetPeriodeBuilder: UberegnetPeriode.Builder? = null

            protected abstract fun nyBeregnetPeriode(builder: SpeilGenerasjonerBuilder, beregnetPeriode: BeregnetPeriode)
            protected abstract fun nyUberegnetPeriode(builder: SpeilGenerasjonerBuilder, uberegnetPeriode: UberegnetPeriode)

            override fun besøkVedtaksperiode(
                builder: SpeilGenerasjonerBuilder,
                vedtaksperiode: Vedtaksperiode,
                vedtaksperiodeId: UUID,
                skjæringstidspunkt: LocalDate,
                tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
                opprettet: LocalDateTime,
                oppdatert: LocalDateTime
            ) {
                vedtaksperiodebuilder = TidslinjeperioderBuilder(vedtaksperiode, vedtaksperiodeId, skjæringstidspunkt, tilstand, opprettet, oppdatert)
            }

            override fun besøkUberegnetPeriode(
                builder: SpeilGenerasjonerBuilder,
                periode: Periode,
                generasjonId: UUID,
                kilde: UUID,
                generasjonOpprettet: LocalDateTime,
                avsluttet: LocalDateTime?,
                forkastet: Boolean
            ) {
                vedtaksperiodebuilder?.nyUberegnetPeriode(periode, generasjonId, kilde, generasjonOpprettet, avsluttet, forkastet)?.also {
                    this.uberegnetPeriodeBuilder = it
                }
            }

            override fun forlatUberegnetPeriode(builder: SpeilGenerasjonerBuilder) {
                uberegnetPeriodeBuilder?.build(vedtaksperiodebuilder?.dokumentsporinger?.toSet() ?: emptySet())?.also {
                    nyUberegnetPeriode(builder, it)
                }
                uberegnetPeriodeBuilder = null
            }

            override fun besøkBeregnetPeriode(
                builder: SpeilGenerasjonerBuilder,
                periode: Periode,
                generasjonId: UUID,
                kilde: UUID,
                generasjonOpprettet: LocalDateTime,
                vedtakFattet: LocalDateTime?,
                avsluttet: LocalDateTime?
            ) {
                vedtaksperiodebuilder?.nyBeregnetPeriode(periode, generasjonId, kilde, generasjonOpprettet)?.also {
                    this.beregnetPeriodeBuilder = it
                }
            }

            override fun dokumentsporing(dokumentsporing: Dokumentsporing) {
                this.vedtaksperiodebuilder?.medDokumentsporing(dokumentsporing)
            }

            override fun forlatBeregnetPeriode(builder: SpeilGenerasjonerBuilder) {
                beregnetPeriodeBuilder?.build(builder.alder, vedtaksperiodebuilder?.dokumentsporinger?.toSet() ?: emptySet())?.also {
                    vedtaksperiodebuilder?.nyBeregnetPeriode(it)
                    nyBeregnetPeriode(builder, it)
                }
                beregnetPeriodeBuilder = null
            }

            final override fun besøkUtbetaling(
                builder: SpeilGenerasjonerBuilder,
                id: UUID,
                korrelasjonsId: UUID,
                type: Utbetalingtype,
                utbetalingstatus: Utbetalingstatus,
                tidsstempel: LocalDateTime,
                maksdato: LocalDate,
                forbrukteSykedager: Int?,
                gjenståendeSykedager: Int?
            ) {
                beregnetPeriodeBuilder?.medUtbetaling(
                    utbetaling = builder.utbetalinger.single { it.id == id },
                    utbetalingstidslinje = builder.utbetalingstidslinjer.single { it.first == id }.second.build()
                )
            }

            override fun besøkVilkårsgrunnlagelement(builder: SpeilGenerasjonerBuilder, vilkårsgrunnlagId: UUID, skjæringstidspunkt: LocalDate) {
                this.beregnetPeriodeBuilder?.medVilkårsgrunnlag(vilkårsgrunnlagId, skjæringstidspunkt)
            }

            override fun besøkSykdomstidslinje(builder: SpeilGenerasjonerBuilder, sykdomstidslinje: Sykdomstidslinje) {
                val sykdomstidslinjeDto = SykdomstidslinjeBuilder(sykdomstidslinje.dto()).build()
                this.beregnetPeriodeBuilder?.medSykdomstidslinje(sykdomstidslinjeDto)
                this.uberegnetPeriodeBuilder?.medSykdomstidslinje(sykdomstidslinjeDto)
            }
        }

        class AktivePerioder : Periodebygger() {
            override fun nyBeregnetPeriode(builder: SpeilGenerasjonerBuilder, beregnetPeriode: BeregnetPeriode) {
                builder.allePerioder.add(beregnetPeriode)
            }

            override fun nyUberegnetPeriode(builder: SpeilGenerasjonerBuilder, uberegnetPeriode: UberegnetPeriode) {
                builder.allePerioder.add(uberegnetPeriode)
            }

            override fun forlatVedtaksperiode(builder: SpeilGenerasjonerBuilder) {}
        }

        class ForkastedePerioder : Periodebygger() {
            private val perioder = mutableListOf<SpeilTidslinjeperiode>()
            private var sisteBeregnetPeriode: BeregnetPeriode? = null
            private var sisteUBeregnetPeriode: UberegnetPeriode? = null

            override fun nyBeregnetPeriode(builder: SpeilGenerasjonerBuilder, beregnetPeriode: BeregnetPeriode) {
                sisteUBeregnetPeriode = null
                perioder.add(beregnetPeriode)
                sisteBeregnetPeriode = beregnetPeriode
            }

            override fun nyUberegnetPeriode(builder: SpeilGenerasjonerBuilder, uberegnetPeriode: UberegnetPeriode) {
                sisteUBeregnetPeriode = uberegnetPeriode
                perioder.add(uberegnetPeriode)
            }

            override fun forlatVedtaksperiode(builder: SpeilGenerasjonerBuilder) {
                // en forkastet periode bestående av én generasjon har ikke blitt
                // avsluttet tidligere, ellers ville den hatt minst to generasjoner.
                if (perioder.size <= 1) return perioder.clear()

                // omgjør siste uberegnede periode som en BeregnetPeriode/AnnullertPeriode,
                // med utgangspunkt i siste beregnede perioden før den uberegnede
                sisteBeregnetPeriode?.also { beregnetPeriode ->
                    sisteUBeregnetPeriode?.also { uberegnetPeriode ->
                        builder.annullertePerioder.add(uberegnetPeriode to beregnetPeriode)
                    }
                }

                builder.allePerioder.addAll(perioder)
                perioder.clear()
            }
        }

        class TidslinjeperioderBuilder(
            private val vedtaksperiode: Vedtaksperiode,
            private val vedtaksperiodeId: UUID,
            private val skjæringstidspunkt: LocalDate,
            private val tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            private val opprettet: LocalDateTime,
            private val oppdatert: LocalDateTime
        ) {
            private var forrigeBeregnetPeriode: BeregnetPeriode? = null
            internal val dokumentsporinger = mutableSetOf<Dokumentsporing>()
            fun medDokumentsporing(dokumentsporing: Dokumentsporing) {
                dokumentsporinger.add(dokumentsporing)
            }
            internal fun nyBeregnetPeriode(ny: BeregnetPeriode) { forrigeBeregnetPeriode = ny }
            internal fun nyBeregnetPeriode(periode: Periode, generasjonId: UUID, kilde: UUID, generasjonOpprettet: LocalDateTime) = BeregnetPeriode.Builder(
                vedtaksperiodeId,
                generasjonId,
                kilde,
                tilstand,
                opprettet,
                oppdatert,
                periode,
                forrigeBeregnetPeriode,
                generasjonOpprettet
            )

            internal fun nyUberegnetPeriode(periode: Periode, generasjonId: UUID, kilde: UUID, generasjonOpprettet: LocalDateTime, avsluttet: LocalDateTime?, forkastet: Boolean) =
                UberegnetPeriode.Builder(vedtaksperiodeId, generasjonId, kilde, skjæringstidspunkt, tilstand, generasjonOpprettet, forkastet, avsluttet, opprettet, oppdatert, periode)
        }
    }
}
