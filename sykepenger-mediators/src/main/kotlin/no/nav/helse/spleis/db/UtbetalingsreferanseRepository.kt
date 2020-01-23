package no.nav.helse.spleis.db

interface UtbetalingsreferanseRepository {

    fun hentUtbetaling(utbetalingsreferanse: String): Utbetalingsreferanse?
}
