package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V203SpissetGjenopplivingAvTidligereForkastet: GjenopplivingAvTidligereForkastet(version = 203) {
    override fun finnPerioder(jsonNode: ObjectNode): Set<String> = vedtaksperioder

    private companion object {
        private val vedtaksperioder = setOf("33714304-d38e-4aa9-91bc-20c7f3b2a917")
    }
}