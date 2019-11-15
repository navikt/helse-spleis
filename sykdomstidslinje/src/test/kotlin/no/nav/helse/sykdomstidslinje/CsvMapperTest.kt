package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CsvMapperTest {
    @Test
    internal fun `kan hente ut nøkkel fra dag`(){
        val nøkkel = ImplisittDag(Uke(1).mandag, Testhendelse()).nøkkel()
        assertEquals(Dag.Nøkkel.I, nøkkel)
    }
}
