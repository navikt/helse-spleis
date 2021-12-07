package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.PersonObserver.UtbetaltEvent
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal fun tilUtbetaltEvent(
    sykepengegrunnlag: Inntekt,
    inntekt: Inntekt,
    hendelseIder: Set<UUID>,
    utbetaling: Utbetaling,
    utbetalingstidslinje: Utbetalingstidslinje,
    periode: Periode,
    forbrukteSykedager: Int,
    gjenståendeSykedager: Int,
    godkjentAv: String,
    automatiskBehandling: Boolean,
    maksdato: LocalDate
) = UtbetaltEventBuilder(
    hendelseIder = hendelseIder,
    sykepengegrunnlag = sykepengegrunnlag,
    inntekt = inntekt,
    utbetaling = utbetaling,
    utbetalingstidslinje = utbetalingstidslinje,
    periode = periode,
    forbrukteSykedager = forbrukteSykedager,
    gjenståendeSykedager = gjenståendeSykedager,
    godkjentAv = godkjentAv,
    automatiskBehandling = automatiskBehandling,
    maksdato = maksdato
).result()

private class UtbetaltEventBuilder(
    private val hendelseIder: Set<UUID>,
    private val sykepengegrunnlag: Inntekt,
    private val inntekt: Inntekt,
    utbetaling: Utbetaling,
    utbetalingstidslinje: Utbetalingstidslinje,
    private val periode: Periode,
    private val forbrukteSykedager: Int,
    private val gjenståendeSykedager: Int,
    private val godkjentAv: String,
    private val automatiskBehandling: Boolean,
    private val maksdato: LocalDate
) : UtbetalingVisitor {
    private lateinit var utbetalingId: UUID
    private lateinit var opprettet: LocalDateTime
    private val dagsats = sykepengegrunnlag.reflection { _, _, _, daglig -> daglig }
    private val oppdragListe = mutableListOf<UtbetaltEvent.Utbetalt>()
    private val utbetalingslinjer = mutableListOf<UtbetaltEvent.Utbetalt.Utbetalingslinje>()
    private val ikkeUtbetalteDager = finnIkkeUtbetalteDager(utbetalingstidslinje)

    init {
        utbetaling.accept(this)
        utbetalingstidslinje.accept(this)
    }

    fun result(): UtbetaltEvent {
        return UtbetaltEvent(
            hendelser = hendelseIder.toSet(),
            utbetalingId = utbetalingId,
            oppdrag = oppdragListe.toList(),
            ikkeUtbetalteDager = ikkeUtbetalteDager,
            fom = periode.start,
            tom = periode.endInclusive,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            godkjentAv = godkjentAv,
            automatiskBehandling = automatiskBehandling,
            opprettet = opprettet,
            sykepengegrunnlag = sykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
            månedsinntekt = inntekt.reflection { _, månedlig, _, _ -> månedlig },
            maksdato = maksdato
        )
    }

    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        korrelasjonsId: UUID,
        type: Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
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
        utbetalingId = id
        opprettet = tidsstempel
    }

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        utbetalingslinjer.clear()
    }

    override fun postVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        oppdragListe.add(
            UtbetaltEvent.Utbetalt(
                mottaker = oppdrag.mottaker(),
                fagområde = oppdrag.fagområde().verdi,
                fagsystemId = oppdrag.fagsystemId(),
                totalbeløp = oppdrag.totalbeløp(),
                utbetalingslinjer = utbetalingslinjer.toList()
            )
        )
    }

    override fun preVisitPersonOppdrag(oppdrag: Oppdrag) {
        utbetalingslinjer.clear()
    }

    override fun postVisitPersonOppdrag(oppdrag: Oppdrag) {
        oppdragListe.add(
            UtbetaltEvent.Utbetalt(
                mottaker = oppdrag.mottaker(),
                fagområde = oppdrag.fagområde().verdi,
                fagsystemId = oppdrag.fagsystemId(),
                totalbeløp = oppdrag.totalbeløp(),
                utbetalingslinjer = utbetalingslinjer.toList()
            )
        )
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        satstype: Satstype,
        beløp: Int?,
        aktuellDagsinntekt: Int?,
        grad: Double?,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?,
        klassekode: Klassekode
    ) {
        if (linje.erOpphør()) return
        utbetalingslinjer.add(
            UtbetaltEvent.Utbetalt.Utbetalingslinje(
                fom = fom,
                tom = tom,
                sats = dagsats,
                beløp = beløp!!,
                grad = grad!!,
                sykedager = linje.stønadsdager()
            )
        )
    }

    fun finnIkkeUtbetalteDager(utbetalingstidslinje: Utbetalingstidslinje): List<UtbetaltEvent.IkkeUtbetaltDag> {
        val ikkeUtbetalteDager = mutableListOf<UtbetaltEvent.IkkeUtbetaltDag>()

        utbetalingstidslinje.accept(object : UtbetalingsdagVisitor {
            override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag, dato: LocalDate, økonomi: Økonomi) {
                ikkeUtbetalteDager.add(
                    UtbetaltEvent.IkkeUtbetaltDag(
                        dato = dato, type = UtbetaltEvent.IkkeUtbetaltDag.Type.AvvistDag,  begrunnelser = mapBegrunnelser(dag.begrunnelser)
                    )
                )
            }

            private fun mapBegrunnelser(begrunnelser: List<Begrunnelse>): List<UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse> = begrunnelser.map {when (it) {
                Begrunnelse.SykepengedagerOppbrukt -> UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.SykepengedagerOppbrukt
                Begrunnelse.SykepengedagerOppbruktOver67 -> UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.SykepengedagerOppbruktOver67
                Begrunnelse.MinimumInntekt -> UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.MinimumInntekt
                Begrunnelse.MinimumInntektOver67 -> UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.MinimumInntektOver67
                Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode -> UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode
                Begrunnelse.MinimumSykdomsgrad -> UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.MinimumSykdomsgrad
                Begrunnelse.EtterDødsdato -> UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.EtterDødsdato
                Begrunnelse.ManglerMedlemskap -> UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.ManglerMedlemskap
                Begrunnelse.ManglerOpptjening -> UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.ManglerOpptjening
                Begrunnelse.Over70 -> UtbetaltEvent.IkkeUtbetaltDag.Begrunnelse.Over70
            }
        }

            override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
                ikkeUtbetalteDager.add(UtbetaltEvent.IkkeUtbetaltDag(dato, UtbetaltEvent.IkkeUtbetaltDag.Type.Fridag))
            }

            override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag, dato: LocalDate, økonomi: Økonomi) {
                if (!dato.isBefore(utbetalingstidslinje.sykepengeperiode()?.start ?: LocalDate.MIN)) {
                    ikkeUtbetalteDager.add(
                        UtbetaltEvent.IkkeUtbetaltDag(
                            dato,
                            UtbetaltEvent.IkkeUtbetaltDag.Type.Arbeidsdag
                        )
                    )
                }
            }
        })
        return ikkeUtbetalteDager
    }
}
