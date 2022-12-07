package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V205SpissetGjenopplivingAvTidligereForkastet: GjenopplivingAvTidligereForkastet(version = 205) {
    override fun finnPerioder(jsonNode: ObjectNode) = vedtaksperioder

    private companion object {
        private val vedtaksperioder = setOf("966cfaf3-0c6b-4324-ba53-d6736571cf8e", "d3576642-48e1-4689-a7b0-86aa70529c5b")
    }
}