package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.etterlevelse.`§ 8-17 ledd 1 bokstav a - arbeidsgiversøknad`
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.behandlingkilde
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
        val utbetalingstidslinje = lagArbeidsgiverberegning(listOf(vedtaksperiode))
            .single {
                val vedtaksperiodeYrkesaktivitet = vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype
                when (val ya = it.yrkesaktivitet) {
                    Arbeidsgiverberegning.Yrkesaktivitet.Arbeidsledig -> vedtaksperiodeYrkesaktivitet is Behandlingsporing.Yrkesaktivitet.Arbeidsledig
                    is Arbeidsgiverberegning.Yrkesaktivitet.Arbeidstaker -> vedtaksperiodeYrkesaktivitet is Behandlingsporing.Yrkesaktivitet.Arbeidstaker && ya.organisasjonsnummer == vedtaksperiode.yrkesaktivitet.organisasjonsnummer
                    Arbeidsgiverberegning.Yrkesaktivitet.Frilans -> vedtaksperiodeYrkesaktivitet is Behandlingsporing.Yrkesaktivitet.Frilans
                    Arbeidsgiverberegning.Yrkesaktivitet.Selvstendig -> vedtaksperiodeYrkesaktivitet is Behandlingsporing.Yrkesaktivitet.Selvstendig
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

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.behandlinger.sikreNyBehandling(
            with (vedtaksperiode) { eventBus.behandlingEventBus },
            vedtaksperiode.yrkesaktivitet,
            revurdering.hendelse.metadata.behandlingkilde,
            vedtaksperiode.person.skjæringstidspunkter,
            vedtaksperiode.yrkesaktivitet.perioderUtenNavAnsvar
        )
        if (vedtaksperiode.skalBehandlesISpeil()) {
            revurdering.inngåSomEndring(vedtaksperiode, aktivitetslogg)
            revurdering.loggDersomKorrigerendeSøknad(
                aktivitetslogg,
                "Startet omgjøring grunnet korrigerende søknad"
            )
            vedtaksperiode.videreførEksisterendeRefusjonsopplysninger(
                eventBus = eventBus,
                behandlingkilde = revurdering.hendelse.metadata.behandlingkilde,
                dokumentsporing = null,
                aktivitetslogg = aktivitetslogg
            )
            aktivitetslogg.info("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden")
            if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) {
                aktivitetslogg.info("mangler nødvendige opplysninger fra arbeidsgiver")
                return vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerInntektsmelding)
            }
        }
        vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerBlokkerendePeriode)
    }

    override fun håndterKorrigerendeInntektsmelding(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterDager(eventBus, dager, aktivitetslogg)

        if (!aktivitetslogg.harFunksjonelleFeil()) return
        if (!vedtaksperiode.kanForkastes()) return
        vedtaksperiode.forkast(eventBus, dager.hendelse, aktivitetslogg)
    }
}
