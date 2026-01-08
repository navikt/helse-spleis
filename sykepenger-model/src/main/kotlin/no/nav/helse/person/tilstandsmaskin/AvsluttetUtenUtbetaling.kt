package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.etterlevelse.`§ 8-17 ledd 1 bokstav a - arbeidsgiversøknad`
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.lagArbeidsgiverberegning
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning

internal data object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
    override val type = TilstandType.AVSLUTTET_UTEN_UTBETALING
    override val erFerdigBehandlet = true

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        avsluttUtenVedtak(vedtaksperiode, eventBus, aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    private fun avsluttUtenVedtak(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        val utbetalingstidslinje = lagArbeidsgiverberegning(vedtaksperiode.periode,listOf(vedtaksperiode)).first
            .single {
                val vedtaksperiodeYrkesaktivitet = vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype
                when (val ya = it.inntektskilde) {
                    is Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker -> vedtaksperiodeYrkesaktivitet is Behandlingsporing.Yrkesaktivitet.Arbeidstaker && ya.organisasjonsnummer == vedtaksperiode.yrkesaktivitet.organisasjonsnummer

                    Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidsledig,
                    Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Frilans,
                    Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Selvstendig,
                    is Arbeidsgiverberegning.Inntektskilde.AnnenInntektskilde -> false
                }
            }
            .vedtaksperioder
            .single()

        val sisteBehandling = vedtaksperiode.behandlinger.avsluttUtenVedtak(with (vedtaksperiode) { eventBus.behandlingEventBus }, vedtaksperiode.yrkesaktivitet, utbetalingstidslinje.utbetalingstidslinje, emptyMap())
        val dekkesAvArbeidsgiverperioden = vedtaksperiode.behandlinger.ventedager().dagerUtenNavAnsvar.periode?.inneholder(vedtaksperiode.periode) != false

        if (dekkesAvArbeidsgiverperioden) {
            vedtaksperiode.subsumsjonslogg.logg(`§ 8-17 ledd 1 bokstav a - arbeidsgiversøknad`(vedtaksperiode.periode, vedtaksperiode.sykdomstidslinje.subsumsjonsformat()))
        }
        eventBus.avsluttetUtenVedtak(
            EventSubscription.AvsluttetUtenVedtakEvent(
                yrkesaktivitetssporing = vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype,
                vedtaksperiodeId = vedtaksperiode.id,
                behandlingId = sisteBehandling.id,
                periode = vedtaksperiode.periode,
                hendelseIder = vedtaksperiode.behandlinger.hendelseIder().ider(),
                skjæringstidspunkt = sisteBehandling.skjæringstidspunkt,
                avsluttetTidspunkt = sisteBehandling.avsluttet!!
            )
        )
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftÅpenBehandling(vedtaksperiode.yrkesaktivitet)
    }
}
