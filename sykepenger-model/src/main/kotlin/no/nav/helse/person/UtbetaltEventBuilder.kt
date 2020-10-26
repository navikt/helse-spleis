package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.PersonObserver.UtbetaltEvent
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

internal fun tilUtbetaltEvent(
    aktørId: String,
    fødselnummer: String,
    orgnummer: String,
    sykepengegrunnlag: Inntekt,
    hendelseIder: List<UUID>,
    utbetaling: Utbetaling,
    utbetalingstidslinje: Utbetalingstidslinje,
    periode: Periode,
    forbrukteSykedager: Int,
    gjenståendeSykedager: Int,
    godkjentAv: String,
    automatiskBehandling: Boolean,
    maksdato: LocalDate
) = UtbetaltEventBuilder(
    aktørId = aktørId,
    fødselnummer = fødselnummer,
    orgnummer = orgnummer,
    hendelseIder = hendelseIder,
    sykepengegrunnlag = sykepengegrunnlag,
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
    private val aktørId: String,
    private val fødselnummer: String,
    private val orgnummer: String,
    private val hendelseIder: List<UUID>,
    private val sykepengegrunnlag: Inntekt,
    utbetaling: Utbetaling,
    utbetalingstidslinje: Utbetalingstidslinje,
    private val periode: Periode,
    private val forbrukteSykedager: Int,
    private val gjenståendeSykedager: Int,
    private val godkjentAv: String,
    private val automatiskBehandling: Boolean,
    private val maksdato: LocalDate
) : UtbetalingVisitor {
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
            aktørId = aktørId,
            fødselsnummer = fødselnummer,
            organisasjonsnummer = orgnummer,
            hendelser = hendelseIder.toSet(),
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
            maksdato = maksdato
        )
    }

    override fun preVisitUtbetaling(utbetaling: Utbetaling, tidsstempel: LocalDateTime) {
        opprettet = tidsstempel
    }

    override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        utbetalingslinjer.clear()
    }

    override fun postVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
        oppdragListe.add(
            UtbetaltEvent.Utbetalt(
                mottaker = orgnummer,
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
                mottaker = fødselnummer,
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
        beløp: Int?,
        aktuellDagsinntekt: Int,
        grad: Double,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?
    ) {
        if (linje.erOpphør()) return
        utbetalingslinjer.add(
            UtbetaltEvent.Utbetalt.Utbetalingslinje(
                fom = fom,
                tom = tom,
                dagsats = dagsats,
                beløp = beløp!!,
                grad = grad,
                sykedager = linje.filterNot { it.erHelg() }.count()
            )
        )
    }

    fun finnIkkeUtbetalteDager(utbetalingstidslinje: Utbetalingstidslinje): List<UtbetaltEvent.IkkeUtbetaltDag> {
        val ikkeUtbetalteDager = mutableListOf<UtbetaltEvent.IkkeUtbetaltDag>()

        utbetalingstidslinje.accept(object : UtbetalingsdagVisitor {
            override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag, dato: LocalDate, økonomi: Økonomi) {
                ikkeUtbetalteDager.add(
                    UtbetaltEvent.IkkeUtbetaltDag(
                        dato, when (dag.begrunnelse) {
                            Begrunnelse.SykepengedagerOppbrukt -> UtbetaltEvent.IkkeUtbetaltDag.Type.SykepengedagerOppbrukt
                            Begrunnelse.MinimumInntekt -> UtbetaltEvent.IkkeUtbetaltDag.Type.MinimumInntekt
                            Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode -> UtbetaltEvent.IkkeUtbetaltDag.Type.EgenmeldingUtenforArbeidsgiverperiode
                            Begrunnelse.MinimumSykdomsgrad -> UtbetaltEvent.IkkeUtbetaltDag.Type.MinimumSykdomsgrad
                        }
                    )
                )
            }

            override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
                ikkeUtbetalteDager.add(UtbetaltEvent.IkkeUtbetaltDag(dato, UtbetaltEvent.IkkeUtbetaltDag.Type.Fridag))
            }

            override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag, dato: LocalDate, økonomi: Økonomi) {
                if (!dato.isBefore(utbetalingstidslinje.førsteSykepengedag() ?: LocalDate.MIN)) {
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
