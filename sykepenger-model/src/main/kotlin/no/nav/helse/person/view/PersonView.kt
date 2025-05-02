package no.nav.helse.person.view

import no.nav.helse.feriepenger.FeriepengeutbetalingView
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.person.SykmeldingsperioderView
import no.nav.helse.person.VedtaksperiodeView
import no.nav.helse.person.VilkårsgrunnlagHistorikkView
import no.nav.helse.person.inntekt.InntektshistorikkView
import no.nav.helse.person.refusjon.RefusjonsservitørView
import no.nav.helse.sykdomstidslinje.SykdomshistorikkView
import no.nav.helse.utbetalingslinjer.UtbetalingView

internal data class PersonView(
    val arbeidsgivere: List<ArbeidsgiverView>,
    val vilkårsgrunnlaghistorikk: VilkårsgrunnlagHistorikkView
)

internal data class ArbeidsgiverView(
    val organisasjonsnummer: String,
    val yrkesaktivitetssporing: Behandlingsporing.Yrkesaktivitet,
    val sykdomshistorikk: SykdomshistorikkView,
    val utbetalinger: List<UtbetalingView>,
    val inntektshistorikk: InntektshistorikkView,
    val sykmeldingsperioder: SykmeldingsperioderView,
    val ubrukteRefusjonsopplysninger: RefusjonsservitørView,
    val feriepengeutbetalinger: List<FeriepengeutbetalingView>,
    val aktiveVedtaksperioder: List<VedtaksperiodeView>,
    val forkastetVedtaksperioder: List<VedtaksperiodeView>
)
