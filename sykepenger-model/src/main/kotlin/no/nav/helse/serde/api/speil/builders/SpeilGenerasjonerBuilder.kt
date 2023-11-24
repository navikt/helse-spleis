package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidsgiverVisitor
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.Generasjoner
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.serde.api.dto.AnnullertUtbetaling
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.EndringskodeDTO.Companion.dto
import no.nav.helse.serde.api.dto.SpeilGenerasjonDTO
import no.nav.helse.serde.api.dto.SpeilOppdrag
import no.nav.helse.serde.api.dto.SpeilTidslinjeperiode
import no.nav.helse.serde.api.dto.SpeilTidslinjeperiode.Companion.sorterEtterHendelse
import no.nav.helse.serde.api.dto.UberegnetPeriode
import no.nav.helse.serde.api.speil.SpeilGenerasjoner
import no.nav.helse.serde.api.speil.builders.SpeilGenerasjonerBuilder.Byggetilstand.AktivePerioder
import no.nav.helse.serde.api.speil.builders.SpeilGenerasjonerBuilder.Byggetilstand.ForkastedePerioder
import no.nav.helse.serde.api.speil.builders.SpeilGenerasjonerBuilder.Byggetilstand.Initiell
import no.nav.helse.serde.api.speil.builders.SpeilGenerasjonerBuilder.Byggetilstand.Utbetalinger
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

// Besøker hele arbeidsgiver-treet
internal class SpeilGenerasjonerBuilder(
    private val organisasjonsnummer: String,
    private val alder: Alder,
    arbeidsgiver: Arbeidsgiver,
    private val vilkårsgrunnlaghistorikk: IVilkårsgrunnlagHistorikk
) : ArbeidsgiverVisitor {
    private var tilstand: Byggetilstand = Initiell

    private val aktivePerioder = mutableListOf<SpeilTidslinjeperiode>()
    private val forkastedePerioder = mutableListOf<List<BeregnetPeriode>>()
    private val annulleringer = mutableListOf<AnnullertUtbetaling>()

    init {
        arbeidsgiver.accept(this)
    }

    internal fun build(): List<SpeilGenerasjonDTO> {
        val perioder = aktivePerioder + forkastedePerioder.flatMap { tidligere ->
            val siste = tidligere.last()
            val annullering = siste.somAnnullering(annulleringer)
            annullering?.let { tidligere + it } ?: emptyList()
        }
        return SpeilGenerasjoner().apply {
            perioder
                .sorterEtterHendelse()
                .map { it.registrerBruk(vilkårsgrunnlaghistorikk, organisasjonsnummer) }
                .forEach { periode -> periode.tilGenerasjon(this) }
        }.build()
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
        val hendelser = hendelseIder.ider()
        this.tilstand.besøkVedtaksperiode(this, vedtaksperiode, id, skjæringstidspunkt(), tilstand, opprettet, oppdatert, periode, hendelser)
    }

    private val Generasjoner.Generasjon.Tilstand.uberegnet get() = this in setOf(
        Generasjoner.Generasjon.Tilstand.Uberegnet,
        Generasjoner.Generasjon.Tilstand.UberegnetOmgjøring,
        Generasjoner.Generasjon.Tilstand.UberegnetRevurdering,
        Generasjoner.Generasjon.Tilstand.AvsluttetUtenVedtak,
        Generasjoner.Generasjon.Tilstand.AvsluttetUtenVedtakRevurdering,
        Generasjoner.Generasjon.Tilstand.TilInfotrygd
    )
    override fun preVisitGenerasjon(
        id: UUID,
        tidsstempel: LocalDateTime,
        tilstand: Generasjoner.Generasjon.Tilstand,
        periode: Periode,
        vedtakFattet: LocalDateTime?,
        avsluttet: LocalDateTime?,
        kilde: Generasjoner.Generasjonkilde?
    ) {
        if (tilstand.uberegnet) return this.tilstand.besøkUberegnetPeriode(this, periode, tidsstempel, avsluttet)
        this.tilstand.besøkBeregnetPeriode(this, periode, tidsstempel, vedtakFattet, avsluttet)
    }

    override fun postVisitGenerasjon(
        id: UUID,
        tidsstempel: LocalDateTime,
        tilstand: Generasjoner.Generasjon.Tilstand,
        periode: Periode,
        vedtakFattet: LocalDateTime?,
        avsluttet: LocalDateTime?,
        kilde: Generasjoner.Generasjonkilde?
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

    override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        this.tilstand = Utbetalinger
    }

    override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
        this.tilstand = Initiell
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

    override fun preVisitUtbetalingstidslinje(tidslinje: Utbetalingstidslinje, gjeldendePeriode: Periode?) {
        tilstand.besøkUtbetalingstidslinje(this, tidslinje)
    }

    override fun preVisitOppdrag(
        oppdrag: Oppdrag,
        fagområde: Fagområde,
        fagsystemId: String,
        mottaker: String,
        sisteArbeidsgiverdag: LocalDate?,
        stønadsdager: Int,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        endringskode: Endringskode,
        avstemmingsnøkkel: Long?,
        status: Oppdragstatus?,
        overføringstidspunkt: LocalDateTime?,
        erSimulert: Boolean,
        simuleringsResultat: SimuleringResultat?
    ) {
        tilstand.besøkOppdrag(this, fagsystemId, tidsstempel, nettoBeløp, simuleringsResultat)
    }

    override fun postVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        tilstand.forlatArbeidsgiveroppdrag(this)
    }
    override fun postVisitPersonOppdrag(oppdrag: Oppdrag) {
        tilstand.forlatPersonoppdrag(this)
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        stønadsdager: Int,
        totalbeløp: Int,
        satstype: Satstype,
        beløp: Int?,
        grad: Int?,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?,
        statuskode: String?,
        klassekode: Klassekode
    ) {
        if (beløp == null || grad == null) return
        tilstand.besøkOppdragslinje(this, fom, tom, beløp, grad, endringskode)
    }

    override fun visitVurdering(
        vurdering: Utbetaling.Vurdering,
        ident: String,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        godkjent: Boolean
    ) {
        tilstand.besøkUtbetalingvurdering(this, godkjent, tidspunkt, automatiskBehandling, ident)
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
        fun besøkUtbetalingvurdering(builder: SpeilGenerasjonerBuilder, godkjent: Boolean, tidsstempel: LocalDateTime, automatisk: Boolean, ident: String) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun besøkOppdrag(builder: SpeilGenerasjonerBuilder, fagsystemId: String, tidsstempel: LocalDateTime, nettobeløp: Int, simulering: SimuleringResultat?) {}
        fun forlatArbeidsgiveroppdrag(builder: SpeilGenerasjonerBuilder) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun forlatPersonoppdrag(builder: SpeilGenerasjonerBuilder) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun besøkOppdragslinje(
            builder: SpeilGenerasjonerBuilder,
            fom: LocalDate,
            tom: LocalDate,
            beløp: Int,
            grad: Int,
            endringskode: Endringskode
        ) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun besøkVedtaksperiode(
            builder: SpeilGenerasjonerBuilder,
            vedtaksperiode: Vedtaksperiode,
            vedtaksperiodeId: UUID,
            skjæringstidspunkt: LocalDate,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            hendelser: Set<UUID>
        ) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun besøkUberegnetPeriode(
            builder: SpeilGenerasjonerBuilder,
            periode: Periode,
            generasjonOpprettet: LocalDateTime,
            avsluttet: LocalDateTime?
        ) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun forlatUberegnetPeriode(builder: SpeilGenerasjonerBuilder) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }
        fun besøkBeregnetPeriode(
            builder: SpeilGenerasjonerBuilder,
            periode: Periode,
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
        fun besøkUtbetalingstidslinje(builder: SpeilGenerasjonerBuilder, utbetalingstidslinje: Utbetalingstidslinje) {}
        fun besøkSykdomstidslinje(builder: SpeilGenerasjonerBuilder, sykdomstidslinje: Sykdomstidslinje) {}
        fun forlatVedtaksperiode(builder: SpeilGenerasjonerBuilder) {
            throw IllegalStateException("a-hoy! dette var ikke forventet gitt!")
        }

        object Initiell : Byggetilstand
        object Utbetalinger : Byggetilstand {
            override fun besøkUtbetalingvurdering(builder: SpeilGenerasjonerBuilder, godkjent: Boolean, tidsstempel: LocalDateTime, automatisk: Boolean, ident: String) {}
            override fun forlatArbeidsgiveroppdrag(builder: SpeilGenerasjonerBuilder) {}
            override fun forlatPersonoppdrag(builder: SpeilGenerasjonerBuilder) {}
            override fun besøkOppdrag(builder: SpeilGenerasjonerBuilder, fagsystemId: String, tidsstempel: LocalDateTime, nettobeløp: Int, simulering: SimuleringResultat?) {}
            override fun besøkOppdragslinje(builder: SpeilGenerasjonerBuilder, fom: LocalDate, tom: LocalDate, beløp: Int, grad: Int, endringskode: Endringskode) {}

            override fun besøkUtbetaling(
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
                when (type) {
                    Utbetalingtype.ANNULLERING -> {
                        val status = if (utbetalingstatus == Utbetalingstatus.ANNULLERT) no.nav.helse.serde.api.dto.Utbetalingstatus.Annullert else no.nav.helse.serde.api.dto.Utbetalingstatus.Overført
                        builder.annulleringer.add(AnnullertUtbetaling(id, korrelasjonsId, tidsstempel, status))
                    }
                    else -> { /* ignorer */ }
                }
            }
        }

        abstract class Periodebygger : Byggetilstand {
            protected var vedtaksperiodebuilder: TidslinjeperioderBuilder? = null
            private var beregnetPeriodeBuilder: BeregnetPeriode.Builder? = null
            private var uberegnetPeriodeBuilder: UberegnetPeriode.Builder? = null
            private var oppdragbuilder: SpeilOppdrag.Builder? = null

            protected abstract fun nyBeregnetPeriode(builder: SpeilGenerasjonerBuilder, beregnetPeriode: BeregnetPeriode)
            protected abstract fun nyUberegnetPeriode(builder: SpeilGenerasjonerBuilder, uberegnetPeriode: UberegnetPeriode)

            override fun besøkVedtaksperiode(
                builder: SpeilGenerasjonerBuilder,
                vedtaksperiode: Vedtaksperiode,
                vedtaksperiodeId: UUID,
                skjæringstidspunkt: LocalDate,
                tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
                opprettet: LocalDateTime,
                oppdatert: LocalDateTime,
                periode: Periode,
                hendelser: Set<UUID>
            ) {
                vedtaksperiodebuilder = TidslinjeperioderBuilder(vedtaksperiode, vedtaksperiodeId, skjæringstidspunkt, tilstand, opprettet, oppdatert, periode, hendelser)
            }

            override fun besøkUberegnetPeriode(
                builder: SpeilGenerasjonerBuilder,
                periode: Periode,
                generasjonOpprettet: LocalDateTime,
                avsluttet: LocalDateTime?
            ) {
                vedtaksperiodebuilder?.nyUberegnetPeriode(periode, generasjonOpprettet, avsluttet)?.also {
                    this.uberegnetPeriodeBuilder = it
                }
            }

            override fun forlatUberegnetPeriode(builder: SpeilGenerasjonerBuilder) {
                uberegnetPeriodeBuilder?.build()?.also {
                    nyUberegnetPeriode(builder, it)
                }
                uberegnetPeriodeBuilder = null
            }

            override fun besøkBeregnetPeriode(
                builder: SpeilGenerasjonerBuilder,
                periode: Periode,
                generasjonOpprettet: LocalDateTime,
                vedtakFattet: LocalDateTime?,
                avsluttet: LocalDateTime?
            ) {
                vedtaksperiodebuilder?.nyBeregnetPeriode(periode, generasjonOpprettet)?.also {
                    this.beregnetPeriodeBuilder = it
                }
            }

            override fun forlatBeregnetPeriode(builder: SpeilGenerasjonerBuilder) {
                beregnetPeriodeBuilder?.build(builder.alder)?.also {
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
                when (type) {
                    Utbetalingtype.UTBETALING -> {
                        if (utbetalingstatus != Utbetalingstatus.IKKE_GODKJENT) {
                            beregnetPeriodeBuilder?.medUtbetaling(id, korrelasjonsId, utbetalingstatus.name, tidsstempel, maksdato, forbrukteSykedager!!, gjenståendeSykedager!!)
                        }
                    }
                    Utbetalingtype.REVURDERING -> beregnetPeriodeBuilder?.medRevurdering(id, korrelasjonsId, utbetalingstatus.name, tidsstempel, maksdato, forbrukteSykedager!!, gjenståendeSykedager!!)
                    else -> { /* ignorer, aktive perioder kan ikke være annullerte */ }
                }
            }

            override fun besøkUtbetalingvurdering(builder: SpeilGenerasjonerBuilder, godkjent: Boolean, tidsstempel: LocalDateTime, automatisk: Boolean, ident: String) {
                beregnetPeriodeBuilder?.medVurdering(godkjent, tidsstempel, automatisk, ident)
            }

            override fun besøkUtbetalingstidslinje(builder: SpeilGenerasjonerBuilder, utbetalingstidslinje: Utbetalingstidslinje) {
                beregnetPeriodeBuilder?.medUtbetalingstidslinje(UtbetalingstidslinjeBuilder(utbetalingstidslinje).build())
            }

            override fun besøkOppdrag(builder: SpeilGenerasjonerBuilder, fagsystemId: String, tidsstempel: LocalDateTime, nettobeløp: Int, simulering: SimuleringResultat?) {
                oppdragbuilder = SpeilOppdrag.Builder(fagsystemId, tidsstempel, nettobeløp, simulering)
            }

            override fun besøkOppdragslinje(builder: SpeilGenerasjonerBuilder, fom: LocalDate, tom: LocalDate, beløp: Int, grad: Int, endringskode: Endringskode) {
                oppdragbuilder?.medOppdragslinje(fom, tom, beløp, grad, endringskode.dto())
            }

            override fun forlatArbeidsgiveroppdrag(builder: SpeilGenerasjonerBuilder) {
                beregnetPeriodeBuilder?.medArbeidsgiveroppdrag(checkNotNull(oppdragbuilder).build())
            }

            override fun forlatPersonoppdrag(builder: SpeilGenerasjonerBuilder) {
                beregnetPeriodeBuilder?.medPersonoppdrag(checkNotNull(oppdragbuilder).build())
            }

            override fun besøkVilkårsgrunnlagelement(builder: SpeilGenerasjonerBuilder, vilkårsgrunnlagId: UUID, skjæringstidspunkt: LocalDate) {
                this.beregnetPeriodeBuilder?.medVilkårsgrunnlag(vilkårsgrunnlagId, skjæringstidspunkt)
            }

            override fun besøkSykdomstidslinje(builder: SpeilGenerasjonerBuilder, sykdomstidslinje: Sykdomstidslinje) {
                val sykdomstidslinjeDto = SykdomstidslinjeBuilder(sykdomstidslinje).build()
                this.beregnetPeriodeBuilder?.medSykdomstidslinje(sykdomstidslinjeDto)
                this.uberegnetPeriodeBuilder?.medSykdomstidslinje(sykdomstidslinjeDto)
            }
        }

        class AktivePerioder : Periodebygger() {
            override fun nyBeregnetPeriode(builder: SpeilGenerasjonerBuilder, beregnetPeriode: BeregnetPeriode) {
                builder.aktivePerioder.add(beregnetPeriode)
            }

            override fun nyUberegnetPeriode(builder: SpeilGenerasjonerBuilder, uberegnetPeriode: UberegnetPeriode) {
                val indeks = builder.aktivePerioder.indexOfLast { it.erSammeVedtaksperiode(uberegnetPeriode) }
                val forrigeUberegnet = builder.aktivePerioder.getOrNull(indeks)
                val oppdatertEksisterende = forrigeUberegnet?.medOpplysningerFra(uberegnetPeriode)
                // alle like AUU-generasjoner slås sammen til én pølse
                if (oppdatertEksisterende == null) builder.aktivePerioder.add(uberegnetPeriode)
                else builder.aktivePerioder[indeks] = oppdatertEksisterende
            }

            override fun forlatVedtaksperiode(builder: SpeilGenerasjonerBuilder) {}
        }

        class ForkastedePerioder : Periodebygger() {
            private val beregnedePerioder = mutableListOf<BeregnetPeriode>()

            override fun nyBeregnetPeriode(builder: SpeilGenerasjonerBuilder, beregnetPeriode: BeregnetPeriode) {
                beregnedePerioder.add(beregnetPeriode)
            }

            override fun besøkUberegnetPeriode(
                builder: SpeilGenerasjonerBuilder,
                periode: Periode,
                generasjonOpprettet: LocalDateTime,
                avsluttet: LocalDateTime?
            ) {}

            override fun forlatUberegnetPeriode(builder: SpeilGenerasjonerBuilder) {}

            override fun nyUberegnetPeriode(builder: SpeilGenerasjonerBuilder, uberegnetPeriode: UberegnetPeriode) {}

            override fun forlatVedtaksperiode(builder: SpeilGenerasjonerBuilder) {
                if (beregnedePerioder.isEmpty()) return
                // skal bare ta vare på dem dersom de har blitt annullert!
                builder.forkastedePerioder.add(beregnedePerioder.toList())
                beregnedePerioder.clear()
            }
        }

        class TidslinjeperioderBuilder(
            private val vedtaksperiode: Vedtaksperiode,
            private val vedtaksperiodeId: UUID,
            private val skjæringstidspunkt: LocalDate,
            private val tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            private val opprettet: LocalDateTime,
            private val oppdatert: LocalDateTime,
            private val periode: Periode,
            private val hendelser: Set<UUID>
        ) {
            private var forrigeBeregnetPeriode: BeregnetPeriode? = null

            internal fun nyBeregnetPeriode(ny: BeregnetPeriode) { forrigeBeregnetPeriode = ny }
            internal fun nyBeregnetPeriode(periode: Periode, generasjonOpprettet: LocalDateTime) = BeregnetPeriode.Builder(
                vedtaksperiodeId,
                tilstand,
                opprettet,
                oppdatert,
                periode,
                hendelser,
                forrigeBeregnetPeriode,
                generasjonOpprettet
            )
            internal fun nyUberegnetPeriode(periode: Periode, generasjonOpprettet: LocalDateTime, avsluttet: LocalDateTime?) = UberegnetPeriode.Builder(vedtaksperiodeId, skjæringstidspunkt, tilstand, generasjonOpprettet, avsluttet, opprettet, oppdatert, periode, hendelser)
        }
    }
}
