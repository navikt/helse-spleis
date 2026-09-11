package no.nav.helse.inspectors.view

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.Behandlinger
import no.nav.helse.person.Behandlinger.Behandling.Tilstand
import no.nav.helse.person.DagerUtenNavAnsvaravklaring
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

internal data class BehandlingerView(
    val behandlinger: List<BehandlingView>,
    val hendelser: Set<Dokumentsporing>
)

internal data class BehandlingView(
    val id: UUID,
    val periode: Periode,
    val vedtakFattet: LocalDateTime?,
    val avsluttet: LocalDateTime?,
    val kilde: BehandlingkildeView,
    val tilstand: TilstandView,
    val endringer: List<BehandlingendringView>,
    val faktaavklartInntekt: FaktaavklartInntektView?,
    val korrigertInntekt: SaksbehandlerView?
) {
    enum class TilstandView {
        ANNULLERT_PERIODE, AVSLUTTET_UTEN_VEDTAK,
        BEREGNET, BEREGNET_OMGJØRING, BEREGNET_REVURDERING,
        REVURDERT_VEDTAK_AVVIST,
        TIL_INFOTRYGD, UBEREGNET, UBEREGNET_OMGJØRING, UBEREGNET_REVURDERING,
        VEDTAK_FATTET, VEDTAK_IVERKSATT, UBEREGNET_ANNULLERING, OVERFØRT_ANNULLERING
    }
}

internal data class BehandlingendringView(
    val id: UUID,
    val sykmeldingsperiode: Periode,
    val periode: Periode,
    val sykdomstidslinje: Sykdomstidslinje,
    val grunnlagsdata: VilkårsgrunnlagElement?,
    val utbetaling: UtbetalingView?,
    val dokumentsporing: Dokumentsporing,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val refusjonstidslinje: Beløpstidslinje,
    val avslagstidslinje: Avslagstidslinje,
    val skjæringstidspunkt: LocalDate,
    val skjæringstidspunkter: List<LocalDate>,
    val dagerNavOvertarAnsvar: List<Periode>,
    val dagerUtenNavAnsvar: DagerUtenNavAnsvaravklaring,
    val egenmeldingsdager: List<Periode>,
    val maksdatoresultat: Maksdatoresultat,
    val beregningId: UUID
)

internal data class BehandlingkildeView(
    val meldingsreferanseId: MeldingsreferanseId,
    val innsendt: LocalDateTime,
    val registert: LocalDateTime,
    val avsender: Avsender
)

internal fun Behandlinger.view() = BehandlingerView(
    behandlinger = behandlinger().map { it.view() },
    hendelser = hendelseIder()
)

internal fun Behandlinger.Behandlingkilde.view() = BehandlingkildeView(meldingsreferanseId, innsendt, registert, avsender)

internal fun Behandlinger.Behandling.view() = BehandlingView(
    id = id,
    periode = periode,
    vedtakFattet = vedtakFattet,
    avsluttet = avsluttet,
    kilde = kilde.view(),
    tilstand = when (tilstand) {
        Tilstand.AnnullertPeriode -> BehandlingView.TilstandView.ANNULLERT_PERIODE
        Tilstand.AvsluttetUtenVedtak -> BehandlingView.TilstandView.AVSLUTTET_UTEN_VEDTAK
        Tilstand.Beregnet -> BehandlingView.TilstandView.BEREGNET
        Tilstand.BeregnetOmgjøring -> BehandlingView.TilstandView.BEREGNET_OMGJØRING
        Tilstand.BeregnetRevurdering -> BehandlingView.TilstandView.BEREGNET_REVURDERING
        Tilstand.RevurdertVedtakAvvist -> BehandlingView.TilstandView.REVURDERT_VEDTAK_AVVIST
        Tilstand.TilInfotrygd -> BehandlingView.TilstandView.TIL_INFOTRYGD
        Tilstand.Uberegnet -> BehandlingView.TilstandView.UBEREGNET
        Tilstand.UberegnetOmgjøring -> BehandlingView.TilstandView.UBEREGNET_OMGJØRING
        Tilstand.UberegnetRevurdering -> BehandlingView.TilstandView.UBEREGNET_REVURDERING
        Tilstand.VedtakFattet -> BehandlingView.TilstandView.VEDTAK_FATTET
        Tilstand.VedtakIverksatt -> BehandlingView.TilstandView.VEDTAK_IVERKSATT
        Tilstand.UberegnetAnnullering -> BehandlingView.TilstandView.UBEREGNET_ANNULLERING
        Tilstand.OverførtAnnullering -> BehandlingView.TilstandView.OVERFØRT_ANNULLERING
    },
    endringer = endringer().map { it.view() },
    faktaavklartInntekt = when (val fi = faktaavklartInntekt) {
        is SelvstendigFaktaavklartInntekt -> fi.view()
        is ArbeidstakerFaktaavklartInntekt -> fi.view()
        null -> null
    },
    korrigertInntekt = korrigertInntekt?.view()
)

internal fun Behandlinger.Behandling.Endring.view() = BehandlingendringView(
    id = id,
    sykmeldingsperiode = sykmeldingsperiode,
    periode = periode,
    sykdomstidslinje = sykdomstidslinje,
    grunnlagsdata = grunnlagsdata,
    utbetaling = utbetaling?.view,
    dokumentsporing = dokumentsporing,
    utbetalingstidslinje = utbetalingstidslinje,
    refusjonstidslinje = refusjonstidslinje,
    skjæringstidspunkt = skjæringstidspunkt,
    skjæringstidspunkter = skjæringstidspunkter,
    dagerUtenNavAnsvar = dagerUtenNavAnsvar,
    egenmeldingsdager = egenmeldingsdager,
    dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
    maksdatoresultat = maksdatoresultat,
    beregningId = beregningId,
    avslagstidslinje = avslagstidslinje
)
