package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelse.TestHendelser.sendtSøknad
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CsvMapperTest {
    @Test
    internal fun `kan hente ut nøkkel fra dag`(){
        val nøkkel = ImplisittDag(LocalDate.of(2019,10,10), sendtSøknad()).nøkkel()
        assertEquals(Dag.Nøkkel.WD_I, nøkkel)
    }
}