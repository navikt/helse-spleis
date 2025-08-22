package no.nav.helse.inspectors

import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.VedtaksperiodeView
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat.Bestemmelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal val VedtaksperiodeView.inspektør get() = VedtaksperiodeInspektør(this)

internal class VedtaksperiodeInspektør(view: VedtaksperiodeView) {
    internal val id = view.id
    internal val periode = view.periode
    internal val oppdatert = view.oppdatert
    internal val skjæringstidspunkt = view.skjæringstidspunkt
    internal val skjæringstidspunkter = view.skjæringstidspunkter
    internal val førsteFraværsdag = view.førsteFraværsdag

    internal val utbetalingstidslinje: Utbetalingstidslinje get() = behandlinger.last().endringer.last().utbetalingstidslinje
    internal val behandlinger = view.behandlinger.behandlinger.map { it.inspektør.behandling }
    internal val egenmeldingsperioder = view.egenmeldingsdager

    internal val arbeidsgiverperiode get() = behandlinger.last().endringer.last().arbeidsgiverperiode
    internal val ventetid get() = behandlinger.last().endringer.last().ventetid

    internal val sykdomstidslinje get() = behandlinger.last().endringer.last().sykdomstidslinje
    internal val inntektsendringer get() = behandlinger.last().endringer.last().inntektsendringer

    internal val maksdatoer = view.behandlinger.behandlinger
        .flatMap { it.endringer.map { it.maksdatoresultat } }
        .filter { it.bestemmelse != Bestemmelse.IKKE_VURDERT }

    internal val utbetalinger = view.behandlinger.behandlinger
        .flatMap { it.endringer.mapNotNull { it.utbetaling } }

    internal val hendelser: Set<Dokumentsporing> = view.behandlinger.hendelser

    internal val hendelseIder get() = hendelser.map { it.id }.toSet()
}
