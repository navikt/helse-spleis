package no.nav.helse.utbetalingslinjer

interface Beløpkilde {
    fun arbeidsgiverbeløp(): Int
    fun personbeløp(): Int
}