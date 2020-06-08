package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

internal fun tilUtbetaltEvent(
    aktørId: String,
    fødselnummer: String,
    orgnummer: String,
    sykepengegrunnlag: Double,
    sykdomshistorikk: Sykdomshistorikk,
    utbetaling: Utbetaling,
    periode: Periode,
    forbrukteSykedager: Int,
    gjenståendeSykedager: Int
) = UtbetaltEventBuilder(
    aktørId = aktørId,
    fødselnummer = fødselnummer,
    orgnummer = orgnummer,
    sykdomshistorikk = sykdomshistorikk,
    sykepengegrunnlag = sykepengegrunnlag,
    utbetaling = utbetaling,
    periode = periode,
    forbrukteSykedager = forbrukteSykedager,
    gjenståendeSykedager = gjenståendeSykedager
).result()

private class UtbetaltEventBuilder(
    private val aktørId: String,
    private val fødselnummer: String,
    private val orgnummer: String,
    sykdomshistorikk: Sykdomshistorikk,
    sykepengegrunnlag: Double,
    utbetaling: Utbetaling,
    private val periode: Periode,
    private val forbrukteSykedager: Int,
    private var gjenståendeSykedager: Int
) : ArbeidsgiverVisitor {
    private lateinit var opprettet: LocalDateTime
    private val dagsats = (sykepengegrunnlag / 260).roundToInt()
    private val hendelser = mutableSetOf<UUID>()
    private val oppdragListe = mutableListOf<PersonObserver.UtbetaltEvent.Utbetalt>()
    private val utbetalingslinjer = mutableListOf<PersonObserver.UtbetaltEvent.Utbetalt.Utbetalingslinje>()

    init {
        sykdomshistorikk.accept(this)
        utbetaling.accept(this)
    }

    internal fun result(): PersonObserver.UtbetaltEvent {
        return PersonObserver.UtbetaltEvent(
            aktørId = aktørId,
            fødselsnummer = fødselnummer,
            organisasjonsnummer = orgnummer,
            hendelser = hendelser.toSet(),
            oppdrag = oppdragListe.toList(),
            fom = periode.start,
            tom = periode.endInclusive,
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            opprettet = opprettet
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
            PersonObserver.UtbetaltEvent.Utbetalt(
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
            PersonObserver.UtbetaltEvent.Utbetalt(
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
        beløp: Int,
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
            PersonObserver.UtbetaltEvent.Utbetalt.Utbetalingslinje(
                fom = fom,
                tom = tom,
                dagsats = min(aktuellDagsinntekt, this.dagsats),
                beløp = beløp,
                grad = grad,
                sykedager = linje.filterNot { it.erHelg() }.count()
            )
        )
    }

    override fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {
        hendelser.add(id)
    }
}
