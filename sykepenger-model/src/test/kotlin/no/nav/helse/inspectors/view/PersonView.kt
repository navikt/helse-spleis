package no.nav.helse.inspectors.view

import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.person.Person
import no.nav.helse.person.Yrkesaktivitet

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

internal fun Person.view() = PersonView(
    arbeidsgivere = yrkesaktiviteter.map { it.view() },
    vilkårsgrunnlaghistorikk = vilkårsgrunnlagHistorikk.view()
)

internal fun Yrkesaktivitet.view(): ArbeidsgiverView = ArbeidsgiverView(
    organisasjonsnummer = organisasjonsnummer,
    yrkesaktivitetssporing = yrkesaktivitetstype,
    sykdomshistorikk = sykdomshistorikk.view(),
    utbetalinger = utbetalinger.map { it.view },
    inntektshistorikk = inntektshistorikk.view(),
    sykmeldingsperioder = sykmeldingsperioder.view(),
    ubrukteRefusjonsopplysninger = ubrukteRefusjonsopplysninger.view(),
    feriepengeutbetalinger = feriepengeutbetalinger.map { it.view() },
    aktiveVedtaksperioder = vedtaksperioder.map { it.view() },
    forkastetVedtaksperioder = forkastede.map { it.view() }
)
