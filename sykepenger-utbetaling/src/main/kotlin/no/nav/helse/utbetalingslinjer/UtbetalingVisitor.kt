package no.nav.helse.utbetalingslinjer

interface UtbetalingVisitor {
    fun visitUtbetaling(utbetaling: Utbetaling) {}
}