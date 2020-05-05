package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal fun tilUtbetaltEvent(
    orgnummer: String,
    førsteFraværsdag: LocalDate,
    vedtaksperiodeId: UUID,
    sykdomshistorikk: Sykdomshistorikk,
    utbetaling: Utbetaling,
    periode: Periode,
    forbrukteSykedager: Int
) = UtbetaltEventBuilder(
    orgnummer = orgnummer,
    førsteFraværsdag = førsteFraværsdag,
    vedtaksperiodeId = vedtaksperiodeId,
    sykdomshistorikk = sykdomshistorikk,
    utbetaling = utbetaling,
    periode = periode,
    forbrukteSykedager = forbrukteSykedager
).build()

private class UtbetaltEventBuilder(
    private val orgnummer: String,
    private val førsteFraværsdag: LocalDate,
    private val vedtaksperiodeId: UUID,
    private val sykdomshistorikk: Sykdomshistorikk,
    private val utbetaling: Utbetaling,
    private val periode: Periode,
    private val forbrukteSykedager: Int
) : ArbeidsgiverVisitor {
    private lateinit var opprettet: LocalDateTime
    private var gjenståendeSykedager: Int? = null
    private val hendelser = mutableSetOf<UUID>()
    private val utbetalingslinjer = mutableListOf<PersonObserver.Utbetalingslinje>()

    internal fun build(): PersonObserver.UtbetaltEvent {
        sykdomshistorikk.accept(this)
        utbetaling.accept(this)

        return PersonObserver.UtbetaltEvent(
            førsteFraværsdag = førsteFraværsdag,
            hendelser = hendelser.toSet(),
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingslinjer = utbetalingslinjer.toList(),
            forbrukteSykedager = forbrukteSykedager,
            gjenståendeSykedager = gjenståendeSykedager,
            opprettet = opprettet
        )
    }

    override fun preVisitUtbetaling(utbetaling: Utbetaling, tidsstempel: LocalDateTime) {
        opprettet = tidsstempel
    }

    override fun visitGjenståendeSykedager(gjenståendeSykedager: Int?) {
        this.gjenståendeSykedager = gjenståendeSykedager
    }

    override fun preVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        tidsstempel: LocalDateTime
    ) {
        hendelser.add(id)
    }

    private fun PersonObserver.Utbetalingslinje.isAlmostEqualTo(
        navDag: Utbetalingstidslinje.Utbetalingsdag.NavDag
    ) =
        (tom.plusDays(1) == navDag.dato || (tom.dayOfWeek == DayOfWeek.FRIDAY && tom.plusDays(3) == navDag.dato))
            && grad == navDag.grad && dagsats == navDag.utbetaling && (enDelAvPeriode == navDag.dato in periode)


    override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
        utbetalingslinjer.lastOrNull()
            ?.takeIf { it.isAlmostEqualTo(dag) }
            ?.also { linje ->
                utbetalingslinjer.apply {
                    remove(linje)
                    add(linje.copy(tom = dag.dato))
                }
            }
            ?: utbetalingslinjer.add(
                PersonObserver.Utbetalingslinje(
                    fom = dag.dato,
                    tom = dag.dato,
                    dagsats = dag.dagsats,
                    beløp = dag.utbetaling,
                    grad = dag.grad,
                    enDelAvPeriode = dag.dato in periode,
                    mottaker = orgnummer,
                    konto = "SPREF"
                )
            )
    }
}
