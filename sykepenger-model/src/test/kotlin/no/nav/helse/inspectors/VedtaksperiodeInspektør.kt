package no.nav.helse.inspectors

import no.nav.helse.person.Behandlinger
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat.Bestemmelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal val Vedtaksperiode.inspektør get() = VedtaksperiodeInspektør(this)
internal val ForkastetVedtaksperiode.inspektør get() = VedtaksperiodeInspektør(this.vedtaksperiode)

internal class VedtaksperiodeInspektør(vedtaksperiode: Vedtaksperiode) {
    internal val id = vedtaksperiode.id
    internal val periode = vedtaksperiode.periode
    internal val oppdatert = vedtaksperiode.oppdatert
    internal val skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt
    internal val skjæringstidspunkter = vedtaksperiode.behandlinger.skjæringstidspunkter()
    internal val førsteFraværsdag = vedtaksperiode.førsteFraværsdag
    internal val tilstand = vedtaksperiode.tilstand.type

    internal val behandlinger: List<Behandlinger.Behandling> = vedtaksperiode.behandlinger.behandlinger().toList()
    internal val utbetalingstidslinje: Utbetalingstidslinje get() = behandlinger.last().endringer().last().utbetalingstidslinje
    internal val egenmeldingsperioder = vedtaksperiode.behandlinger.egenmeldingsdager()

    internal val dagerUtenNavAnsvar get() = behandlinger.last().endringer().last().dagerUtenNavAnsvar.dager
    internal val dagerNavOvertarAnsvar get() = behandlinger.last().endringer().last().dagerNavOvertarAnsvar

    internal val sykdomstidslinje get() = behandlinger.last().endringer().last().sykdomstidslinje
    internal val refusjonstidslinje get() = behandlinger.last().endringer().last().refusjonstidslinje
    internal val avslagstidslinje get() = behandlinger.last().endringer().last().avslagstidslinje

    internal val faktaavklartInntekt = behandlinger.last().faktaavklartInntekt
    internal val korrigertInntekt = behandlinger.last().korrigertInntekt

    internal val maksdatoer = behandlinger
        .flatMap { it.endringer().map { endring -> endring.maksdatoresultat } }
        .filter { it.bestemmelse != Bestemmelse.IKKE_VURDERT }

    internal val utbetalinger = behandlinger
        .flatMap { it.endringer().mapNotNull { endring -> endring.utbetaling } }

    internal val hendelser: Set<Dokumentsporing> = vedtaksperiode.behandlinger.hendelseIder()

    internal val hendelseIder get() = hendelser.map { it.id }.toSet()

    internal val annulleringskandidater = vedtaksperiode.yrkesaktivitet.finnAnnulleringskandidater(vedtaksperiode.id)
}
