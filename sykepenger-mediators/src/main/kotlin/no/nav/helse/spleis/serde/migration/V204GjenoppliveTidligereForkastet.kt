package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V204GjenoppliveTidligereForkastet: GjenopplivingAvTidligereForkastet(version = 204) {
    override fun finnPerioder(jsonNode: ObjectNode): Set<String> {
        return finnPerioder(jsonNode) { vedtaksperiodensPeriode, utbetalingensPeriode ->
            vedtaksperiodensPeriode in utbetalingensPeriode
        }
    }
}