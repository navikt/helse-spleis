package no.nav.helse.person.view

import no.nav.helse.person.SykmeldingsperioderView
import no.nav.helse.person.VedtaksperiodeView
import no.nav.helse.person.VilkårsgrunnlagHistorikkView
import no.nav.helse.person.inntekt.InntektshistorikkView
import no.nav.helse.person.inntekt.RefusjonshistorikkView
import no.nav.helse.sykdomstidslinje.SykdomshistorikkView
import no.nav.helse.utbetalingslinjer.FeriepengeutbetalingView
import no.nav.helse.utbetalingslinjer.UtbetalingView

internal data class PersonView(
    val arbeidsgivere: List<ArbeidsgiverView>,
    val vilkårsgrunnlaghistorikk: VilkårsgrunnlagHistorikkView
)

internal data class ArbeidsgiverView(
    val organisasjonsnummer: String,
    val sykdomshistorikk: SykdomshistorikkView,
    val utbetalinger: List<UtbetalingView>,
    val inntektshistorikk: InntektshistorikkView,
    val sykmeldingsperioder: SykmeldingsperioderView,
    val refusjonshistorikk: RefusjonshistorikkView,
    val feriepengeutbetalinger: List<FeriepengeutbetalingView>,
    val aktiveVedtaksperioder: List<VedtaksperiodeView>,
    val forkastetVedtaksperioder: List<VedtaksperiodeView>
)