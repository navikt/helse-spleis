package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.serde.api.SimuleringsdataDTO
import no.nav.helse.serde.api.v2.IUtbetaling
import no.nav.helse.serde.api.v2.Utbetaling
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling.Forkastet
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.utbetalingslinjer.Utbetaling as InternUtbetaling

// Besøker hele vedtaksperiode-treet
internal class UtbetalingerBuilder(vedtaksperiode: Vedtaksperiode): VedtaksperiodeVisitor {
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
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        stønadsdager: Int,
        beregningId: UUID
    ) {
        if (tilstand == Forkastet) return
        utbetalinger.putIfAbsent(beregningId, UtbetalingBuilder(utbetaling).build())
    }
}

internal class UtbetalingBuilder(utbetaling: InternUtbetaling): UtbetalingVisitor {
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
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?,
        stønadsdager: Int,
        beregningId: UUID
    ) {
        val tidslinje = UtbetalingstidslinjeBuilder(utbetaling).build()
        val vurdering = VurderingBuilder(utbetaling).build()
        val oppdragBuilder = OppdragBuilder(utbetaling)
        this.utbetaling = IUtbetaling(
            beregningId,
            tidsstempel,
            tidslinje,
            maksdato,
            gjenståendeSykedager,
            forbrukteSykedager,
            type.toString(),
            tilstand::class.simpleName!!,
            arbeidsgiverNettoBeløp,
            personNettoBeløp,
            oppdragBuilder.arbeidsgiverFagsystemId(),
            oppdragBuilder.personFagsystemId(),
            vurdering
        )
    }
}

// Besøker hele utbetaling-treet
internal class VurderingBuilder(utbetaling: InternUtbetaling): UtbetalingVisitor {
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
    init {
        utbetaling.accept(this)
    }

    private lateinit var arbeidsgiverFagsystemId: String
    private lateinit var personFagsystemId: String

    internal fun arbeidsgiverFagsystemId() = arbeidsgiverFagsystemId

    internal fun personFagsystemId() = personFagsystemId

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        arbeidsgiverFagsystemId = oppdrag.fagsystemId()
    }

    override fun preVisitPersonOppdrag(oppdrag: Oppdrag) {
        personFagsystemId = oppdrag.fagsystemId()
    }
}

// Besøker hele vedtaksperiode-treet
internal class SimuleringBuilder(vedtaksperiode: Vedtaksperiode): VedtaksperiodeVisitor {
    private var simulering: SimuleringsdataDTO? = null
    init {
        vedtaksperiode.accept(this)
    }

    internal fun build() = simulering

    override fun visitDataForSimulering(dataForSimuleringResultat: Simulering.SimuleringResultat?) {
        if (dataForSimuleringResultat == null) return
        simulering = SimuleringsdataDTO(
            totalbeløp = dataForSimuleringResultat.totalbeløp,
            perioder = dataForSimuleringResultat.perioder.map { periode ->
                SimuleringsdataDTO.PeriodeDTO(
                    fom = periode.periode.start,
                    tom = periode.periode.endInclusive,
                    utbetalinger = periode.utbetalinger.map { utbetaling ->
                        SimuleringsdataDTO.UtbetalingDTO(
                            utbetalesTilId = utbetaling.utbetalesTil.id,
                            utbetalesTilNavn = utbetaling.utbetalesTil.navn,
                            forfall = utbetaling.forfallsdato,
                            detaljer = utbetaling.detaljer.map { detaljer ->
                                SimuleringsdataDTO.DetaljerDTO(
                                    faktiskFom = detaljer.periode.start,
                                    faktiskTom = detaljer.periode.endInclusive,
                                    konto = detaljer.konto,
                                    beløp = detaljer.beløp,
                                    tilbakeføring = detaljer.tilbakeføring,
                                    sats = detaljer.sats.sats,
                                    typeSats = detaljer.sats.type,
                                    antallSats = detaljer.sats.antall,
                                    uføregrad = detaljer.uføregrad,
                                    klassekode = detaljer.klassekode.kode,
                                    klassekodeBeskrivelse = detaljer.klassekode.beskrivelse,
                                    utbetalingstype = detaljer.utbetalingstype,
                                    refunderesOrgNr = detaljer.refunderesOrgnummer
                                )
                            },
                            feilkonto = utbetaling.feilkonto
                        )
                    }
                )
            }
        )
    }
}

