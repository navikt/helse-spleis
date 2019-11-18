package no.nav.helse.spleis

interface UtbetalingsreferanseRepository {

    fun hentUtbetaling(utbetalingsreferanse: String): Utbetalingsreferanse?
}
