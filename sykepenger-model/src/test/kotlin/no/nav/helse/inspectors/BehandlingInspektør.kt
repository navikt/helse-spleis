package no.nav.helse.inspectors

import no.nav.helse.person.Behandlinger

internal val Behandlinger.Behandling.inspektør get() = BehandlingInspektør(this)

internal class BehandlingInspektør(internal val behandling: Behandlinger.Behandling) {
    internal val dagerUtenNavAnsvar get() = behandling.endringer().last().dagerUtenNavAnsvar.dager
    internal val utbetalingstidslinje get() = behandling.endringer().last().utbetalingstidslinje
}
