package no.nav.helse.person.view

import no.nav.helse.utbetalingslinjer.UtbetalingView

data class PersonView(val arbeidsgivere: List<ArbeidsgiverView>)

data class ArbeidsgiverView(
    val organisasjonsnummer: String,
    val utbetalinger: List<UtbetalingView>,
)