package no.nav.helse.inspectors.view

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType

internal data class VedtaksperiodeView(
    val id: UUID,
    val periode: Periode,
    val tilstand: TilstandType,
    val oppdatert: LocalDateTime,
    val skjæringstidspunkt: LocalDate,
    val skjæringstidspunkter: List<LocalDate>,
    val egenmeldingsdager: List<Periode>,
    val behandlinger: BehandlingerView,
    val førsteFraværsdag: LocalDate?,
    val annulleringskandidater: Set<Vedtaksperiode>
) {
    val sykdomstidslinje = behandlinger.behandlinger.last().endringer.last().sykdomstidslinje
    val refusjonstidslinje = behandlinger.behandlinger.last().endringer.last().refusjonstidslinje
    val avslagstidslinje = behandlinger.behandlinger.last().endringer.last().avslagstidslinje
    val dagerNavOvertarAnsvar = behandlinger.behandlinger.last().endringer.last().dagerNavOvertarAnsvar
}

internal fun Vedtaksperiode.view() = VedtaksperiodeView(
    id = id,
    periode = periode,
    tilstand = tilstand.type,
    oppdatert = oppdatert,
    skjæringstidspunkt = skjæringstidspunkt,
    skjæringstidspunkter = behandlinger.skjæringstidspunkter(),
    egenmeldingsdager = behandlinger.egenmeldingsdager(),
    behandlinger = behandlinger.view(),
    førsteFraværsdag = førsteFraværsdag,
    annulleringskandidater = yrkesaktivitet.finnAnnulleringskandidater(this.id)
)

internal fun ForkastetVedtaksperiode.view() = vedtaksperiode.view()
