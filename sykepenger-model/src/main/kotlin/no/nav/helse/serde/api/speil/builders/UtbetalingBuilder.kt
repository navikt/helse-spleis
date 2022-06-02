package no.nav.helse.serde.api.speil.builders

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.serde.api.speil.IUtbetaling
import no.nav.helse.serde.api.dto.SpeilOppdrag
import no.nav.helse.serde.api.dto.Utbetaling
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingslinjer.Utbetaling.Forkastet
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.serde.api.dto.EndringskodeDTO.Companion.dto
import no.nav.helse.utbetalingslinjer.Utbetaling as InternUtbetaling

// Besøker hele vedtaksperiode-treet
internal class UtbetalingerBuilder(
    vedtaksperiode: Vedtaksperiode,
    private val vedtaksperiodetilstand: Vedtaksperiode.Vedtaksperiodetilstand
) : VedtaksperiodeVisitor {
    val utbetalinger = mutableMapOf<UUID, IUtbetaling>()

    init {
        vedtaksperiode.accept(this)
    }

    internal fun build() = utbetalinger.values.toList()

    override fun preVisitUtbetaling(
        utbetaling: no.nav.helse.utbetalingslinjer.Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        tilstand: no.nav.helse.utbetalingslinjer.Utbetaling.Tilstand,
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
        avstemmingsnøkkel: Long?
    ) {
        if (tilstand == Forkastet && vedtaksperiodetilstand != Vedtaksperiode.RevurderingFeilet) return
        utbetalinger.entries.find { it.value.forkastet() }?.let { utbetalinger.remove(it.key) }
        utbetalinger.putIfAbsent(beregningId, UtbetalingBuilder(utbetaling).build())
    }
}

internal class UtbetalingBuilder(utbetaling: InternUtbetaling) : UtbetalingVisitor {
    private lateinit var utbetaling: IUtbetaling

    init {
        utbetaling.accept(this)
    }

    internal fun build() = utbetaling

    override fun preVisitUtbetaling(
        utbetaling: no.nav.helse.utbetalingslinjer.Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        tilstand: no.nav.helse.utbetalingslinjer.Utbetaling.Tilstand,
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
        avstemmingsnøkkel: Long?
    ) {
        val tidslinje = UtbetalingstidslinjeBuilder(utbetaling).build()
        val vurdering = VurderingBuilder(utbetaling).build()
        val oppdragBuilder = OppdragBuilder(utbetaling)
        this.utbetaling = IUtbetaling(
            id = id,
            beregningId = beregningId,
            opprettet = tidsstempel,
            utbetalingstidslinje = tidslinje,
            maksdato = maksdato,
            gjenståendeSykedager = gjenståendeSykedager,
            forbrukteSykedager = forbrukteSykedager,
            type = type.toString(),
            tilstand = tilstand::class.simpleName!!,
            arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
            personNettoBeløp = personNettoBeløp,
            arbeidsgiverFagsystemId = oppdragBuilder.arbeidsgiverFagsystemId(),
            personFagsystemId = oppdragBuilder.personFagsystemId(),
            vurdering = vurdering,
            oppdrag = oppdragBuilder.oppdrag()
        )
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
        førstedato: LocalDate,
        sistedato: LocalDate,
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
        simuleringsResultat: Simulering.SimuleringResultat?
    ) {
        linjer = mutableListOf()
    }

    override fun postVisitOppdrag(
        oppdrag: Oppdrag,
        fagområde: Fagområde,
        fagsystemId: String,
        mottaker: String,
        førstedato: LocalDate,
        sistedato: LocalDate,
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
        simuleringsResultat: Simulering.SimuleringResultat?
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
