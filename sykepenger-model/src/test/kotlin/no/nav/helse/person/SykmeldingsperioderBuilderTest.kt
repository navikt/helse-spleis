package no.nav.helse.person

import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.serde.JsonBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykmeldingsperioderBuilderTest() {

    @Test
    fun `serialiserer Sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(1.mars til 31.mars)

        val sykmeldingsperioderMap = mutableListOf<Map<String, Any>>()
        val sykmeldingsperioderState = JsonBuilder.SykmeldingsperioderState(sykmeldingsperioderMap)

        sykmeldingsperioder.accept(sykmeldingsperioderState)
        assertEquals(
            listOf(mapOf("fom" to 1.januar, "tom" to 31.januar), mapOf("fom" to 1.mars, "tom" to 31.mars)),
            sykmeldingsperioderMap
        )
    }

}
