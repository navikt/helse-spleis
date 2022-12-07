package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V206GjenoppliveTidligereForkastet: GjenopplivingAvTidligereForkastet(version = 206) {
    override fun finnPerioder(jsonNode: ObjectNode): Set<String> {
        return finnPerioder(jsonNode) { vedtaksperiodensPeriode, utbetalingensPeriode ->
            vedtaksperiodensPeriode.overlapperMed(utbetalingensPeriode)
        }
    }
}