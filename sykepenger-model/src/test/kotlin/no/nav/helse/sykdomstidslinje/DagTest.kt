package no.nav.helse.sykdomstidslinje

import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class DagTest {

    @Test
    fun `gir siste dag`() {
        val tidligereSøknad = TestEvent.søknad
        val senereSøknad = TestEvent.søknad
        val tidligere = Dag.Sykedag(1.januar, Økonomi.ikkeBetalt(), tidligereSøknad)
        val senere = Dag.Sykedag(1.januar, Økonomi.ikkeBetalt(), senereSøknad)
        assertNotEquals(tidligere, senere)
        assertEquals(senere, tidligere.sisteAv(senere))
        assertEquals(senere, senere.sisteAv(tidligere))
    }

    @Test
    fun `gir ny dag dersom samme tidspunkt`() {
        val tidspunkt = LocalDateTime.now()
        val tidligereSøknad = TestEvent.Søknad(tidspunkt).kilde
        val nySøknad = TestEvent.Søknad(tidspunkt).kilde
        val tidligere = Dag.Sykedag(1.januar, Økonomi.ikkeBetalt(), tidligereSøknad)
        val senere = Dag.Sykedag(1.januar, Økonomi.ikkeBetalt(), nySøknad)
        assertNotEquals(tidligere, senere)
        assertEquals(senere, tidligere.sisteAv(senere))
        assertEquals(senere, senere.sisteAv(tidligere))
    }
}
