package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeUtbetalingVisitor
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.serde.api.dto.EndringskodeDTO.Companion.dto
import no.nav.helse.serde.api.dto.SpeilOppdrag
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.dto.Utbetaling
import no.nav.helse.serde.api.speil.Tidslinjeberegninger
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.økonomi.Prosent
import no.nav.helse.utbetalingslinjer.Utbetaling as InternUtbetaling

internal class UtbetalingerBuilder(vedtaksperiode: Vedtaksperiode) : VedtaksperiodeVisitor {
    val vilkårsgrunnlag = mutableListOf<Tidslinjeberegninger.Vedtaksperiodeutbetaling>()

    init {
        vedtaksperiode.accept(this)
    }

    internal fun build() = vilkårsgrunnlag.toList()

    override fun preVisitVedtaksperiodeUtbetaling(
        grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        utbetaling: no.nav.helse.utbetalingslinjer.Utbetaling,
        sykdomstidslinje: Sykdomstidslinje
    ) {
        vilkårsgrunnlag.add(VedtaksperiodeUtbetalingVilkårsgrunnlagBuilder(grunnlagsdata, utbetaling, sykdomstidslinje).build())
    }

    internal class VedtaksperiodeUtbetalingVilkårsgrunnlagBuilder(
        grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        utbetaling: InternUtbetaling,
        sykdomstidslinje: Sykdomstidslinje
    ): VedtaksperiodeUtbetalingVisitor {
        private lateinit var utbetalingId: UUID
        private lateinit var vilkårsgrunnlag: UUID
        private val sykdomstidslinjedager = SykdomstidslinjeBuilder(sykdomstidslinje).build()

        init {
            grunnlagsdata.accept(this)
            utbetaling.accept(this)
        }
        internal fun build() = Tidslinjeberegninger.Vedtaksperiodeutbetaling(utbetalingId, vilkårsgrunnlag, sykdomstidslinjedager)

        override fun preVisitUtbetaling(
            utbetaling: no.nav.helse.utbetalingslinjer.Utbetaling,
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
            beregningId: UUID,
            overføringstidspunkt: LocalDateTime?,
            avsluttet: LocalDateTime?,
            avstemmingsnøkkel: Long?,
            annulleringer: Set<UUID>
        ) {
            utbetalingId = id
        }

        override fun preVisitGrunnlagsdata(
            skjæringstidspunkt: LocalDate,
            grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
            sykepengegrunnlag: Sykepengegrunnlag,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            avviksprosent: Prosent?,
            opptjening: Opptjening,
            vurdertOk: Boolean,
            meldingsreferanseId: UUID?,
            vilkårsgrunnlagId: UUID,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
        ) {
            this.vilkårsgrunnlag = vilkårsgrunnlagId
        }

        override fun preVisitInfotrygdVilkårsgrunnlag(infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag, skjæringstidspunkt: LocalDate, sykepengegrunnlag: Sykepengegrunnlag, vilkårsgrunnlagId: UUID) {
            this.vilkårsgrunnlag = vilkårsgrunnlagId
        }
    }
}

// Besøker hele utbetaling-treet
internal class VurderingBuilder(utbetaling: InternUtbetaling) : UtbetalingVisitor {
    init {
        utbetaling.accept(this)
    }

    private var vurdering: Utbetaling.Vurdering? = null
    internal fun build() = vurdering

    override fun visitVurdering(
        vurdering: InternUtbetaling.Vurdering,
        ident: String,
        epost: String,
        tidspunkt: LocalDateTime,
        automatiskBehandling: Boolean,
        godkjent: Boolean
    ) {
        this.vurdering = Utbetaling.Vurdering(
            godkjent = godkjent,
            tidsstempel = tidspunkt,
            automatisk = automatiskBehandling,
            ident = ident
        )
    }
}

// Besøker hele utbetaling-treet
internal class OppdragBuilder(utbetaling: InternUtbetaling) : UtbetalingVisitor {
    private val speilOppdrag: MutableMap<String, SpeilOppdrag> = mutableMapOf()

    init {
        utbetaling.accept(this)
    }

    private lateinit var arbeidsgiverFagsystemId: String
    private lateinit var personFagsystemId: String


    private var linjer = mutableListOf<SpeilOppdrag.Utbetalingslinje>()

    internal fun arbeidsgiverFagsystemId() = arbeidsgiverFagsystemId

    internal fun personFagsystemId() = personFagsystemId

    internal fun oppdrag() = speilOppdrag

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        arbeidsgiverFagsystemId = oppdrag.fagsystemId()
    }

    override fun preVisitPersonOppdrag(oppdrag: Oppdrag) {
        personFagsystemId = oppdrag.fagsystemId()
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        stønadsdager: Int,
        totalbeløp: Int,
        satstype: Satstype,
        beløp: Int?,
        aktuellDagsinntekt: Int?,
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
        linjer.add(
            SpeilOppdrag.Utbetalingslinje(
                fom = fom,
                tom = tom,
                dagsats = beløp,
                grad = grad,
                endringskode = endringskode.dto()
            )
        )
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
        linjer = mutableListOf()
    }

    override fun postVisitOppdrag(
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
        speilOppdrag.putIfAbsent(
            fagsystemId,
            SpeilOppdrag(
                fagsystemId = fagsystemId,
                tidsstempel = tidsstempel,
                simulering = simuleringsResultat?.let { simulering ->
                    SpeilOppdrag.Simulering(
                        totalbeløp = simulering.totalbeløp,
                        perioder = simulering.perioder.map { periode ->
                            SpeilOppdrag.Simuleringsperiode(
                                fom = periode.periode.start,
                                tom = periode.periode.endInclusive,
                                utbetalinger = periode.utbetalinger.map { utbetaling ->
                                    SpeilOppdrag.Simuleringsutbetaling(
                                        mottakerId = utbetaling.utbetalesTil.id,
                                        mottakerNavn = utbetaling.utbetalesTil.navn,
                                        forfall = utbetaling.forfallsdato,
                                        feilkonto = utbetaling.feilkonto,
                                        detaljer = utbetaling.detaljer.map {
                                            SpeilOppdrag.Simuleringsdetaljer(
                                                faktiskFom = it.periode.start,
                                                faktiskTom = it.periode.endInclusive,
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
                utbetalingslinjer = linjer
            )
        )
    }
}
