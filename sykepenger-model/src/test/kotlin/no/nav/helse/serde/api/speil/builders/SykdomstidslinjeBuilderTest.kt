package no.nav.helse.serde.api.speil.builders

import no.nav.helse.serde.api.dto.SykdomstidslinjedagType.ARBEID_IKKE_GJENOPPTATT_DAG
import no.nav.helse.testhelpers.J
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SykdomstidslinjeBuilderTest() {

    @BeforeEach
    fun reset() {
        resetSeed()
    }

    @Test
    fun `ferie uten sykmelding`() {
        val builder = SykdomstidslinjeBuilder(7.J)
        val build = builder.build()
        assertEquals(7, build.size)
        build.forEach { assertEquals(ARBEID_IKKE_GJENOPPTATT_DAG, it.type) }
    }
}

