package no.nav.helse.utbetalingslinjer

enum class Utbetalingstatus {
    NY,
    IKKE_UTBETALT,
    IKKE_GODKJENT,
    GODKJENT,
    SENDT,
    OVERFØRT,
    UTBETALT,
    GODKJENT_UTEN_UTBETALING,
    UTBETALING_FEILET,
    ANNULLERT,
    FORKASTET;
}
