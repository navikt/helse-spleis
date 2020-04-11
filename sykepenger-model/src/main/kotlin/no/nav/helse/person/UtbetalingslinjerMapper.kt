package no.nav.helse.person

import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingslinjer.Utbetalingslinjer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal fun tilUtbetaltEvent(
    aktørId: String,
    fødselsnummer: String,
    gruppeId: UUID,
    vedtaksperiodeId: UUID,
    utbetaling: Utbetaling,
    forbrukteSykedager: Int
) = UtbetalingslinjerMapper(
    aktørId = aktørId,
    fødselsnummer = fødselsnummer,
    gruppeId = gruppeId,
    vedtaksperiodeId = vedtaksperiodeId,
    utbetaling = utbetaling,
    forbrukteSykedager = forbrukteSykedager
).tilEvent()

private class UtbetalingslinjerMapper(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val gruppeId: UUID,
    private val vedtaksperiodeId: UUID,
    private val utbetaling: Utbetaling,
    private val forbrukteSykedager: Int
) : UtbetalingVisitor {
    private lateinit var opprettet: LocalDateTime
    private val utbetalingslinjeListe = mutableListOf<PersonObserver.Utbetalingslinje>()
    private val utbetalingslinjerListe = mutableListOf<PersonObserver.Utbetalingslinjer>()

    internal fun tilEvent(): PersonObserver.UtbetaltEvent {
        utbetaling.accept(this)

        return PersonObserver.UtbetaltEvent(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            gruppeId = gruppeId,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingslinjer = utbetalingslinjerListe.toList(),
            opprettet = opprettet,
            forbrukteSykedager = forbrukteSykedager
        )
    }

    override fun preVisitUtbetaling(utbetaling: Utbetaling, tidsstempel: LocalDateTime) {
        opprettet = tidsstempel
    }

    override fun preVisitArbeidsgiverUtbetalingslinjer(linjer: Utbetalingslinjer) {
        utbetalingslinjeListe.clear()
    }

    override fun postVisitArbeidsgiverUtbetalingslinjer(linjer: Utbetalingslinjer) {
        PersonObserver.Utbetalingslinjer(
            utbetalingsreferanse = linjer.referanse(),
            utbetalingslinjer = utbetalingslinjeListe.toList()
        )
            .takeIf { it.utbetalingslinjer.isNotEmpty() }
            ?.also { utbetalingslinjerListe.add(it) }
    }

    override fun preVisitPersonUtbetalingslinjer(linjer: Utbetalingslinjer) {
        utbetalingslinjeListe.clear()
    }

    override fun postVisitPersonUtbetalingslinjer(linjer: Utbetalingslinjer) {
        PersonObserver.Utbetalingslinjer(
            utbetalingsreferanse = linjer.referanse(),
            utbetalingslinjer = utbetalingslinjeListe.toList()
        )
            .takeIf { it.utbetalingslinjer.isNotEmpty() }
            ?.also { utbetalingslinjerListe.add(it) }
    }

    override fun visitUtbetalingslinje(
        utbetalingslinje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Double,
        delytelseId: Int,
        refDelytelseId: Int?
    ) {
        utbetalingslinjeListe.add(
            PersonObserver.Utbetalingslinje(
                fom = fom,
                tom = tom,
                dagsats = dagsats,
                grad = grad
            )
        )
    }
}
