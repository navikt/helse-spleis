package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V207SpissetGjenopplivingAvTidligereForkastet: GjenopplivingAvTidligereForkastet(version = 207) {
    override fun finnPerioder(jsonNode: ObjectNode) = vedtaksperioder

    private companion object {
        private val vedtaksperioder = setOf("ef43ca59-52ce-4962-9806-a9f8a89ca994", "97c56b88-c81d-432b-a3ab-e1c24f3cbf4d")
    }
}