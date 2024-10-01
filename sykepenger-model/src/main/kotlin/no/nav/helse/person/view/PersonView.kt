package no.nav.helse.person.view

import no.nav.helse.person.SykmeldingsperioderView
import no.nav.helse.person.inntekt.InntektshistorikkView
import no.nav.helse.sykdomstidslinje.SykdomshistorikkView
import no.nav.helse.utbetalingslinjer.UtbetalingView

internal data class PersonView(val arbeidsgivere: List<ArbeidsgiverView>)

internal data class ArbeidsgiverView(
    val organisasjonsnummer: String,
    val sykdomshistorikk: SykdomshistorikkView,
    val utbetalinger: List<UtbetalingView>,
    val inntektshistorikk: InntektshistorikkView,
    val sykmeldingsperioder: SykmeldingsperioderView
)