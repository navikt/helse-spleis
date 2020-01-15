package no.nav.helse.hendelser

import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.hendelser.ModelSendtSøknad.Periode.*
import no.nav.helse.person.Problemer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

internal class ModelSendtSøknadTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var sendtSøknad: ModelSendtSøknad
    private lateinit var problems: Problemer

    @BeforeEach
    internal fun setup() {
        problems = Problemer()
    }

    @Test
    internal fun `sendt søknad med bare sykdom`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100))
        assertFalse(sendtSøknad.valider().hasErrors())
        assertEquals(10, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `sykdomsgrad under 100% støttes ikke`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Sykdom(12.januar, 16.januar, 50))
        assertTrue(sendtSøknad.valider().hasErrors())
        assertThrows<Problemer>{sendtSøknad.sykdomstidslinje()}
    }

    @Test
    internal fun `søknad med ferie`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Ferie(2.januar, 4.januar))
        assertFalse(sendtSøknad.valider().hasErrors())
        assertEquals(10, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `søknad med utdanning`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Utdanning(2.januar))
        assertFalse(sendtSøknad.valider().hasErrors())
        assertEquals(10, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `ferie ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Ferie(2.januar, 16.januar))
        assertTrue(sendtSøknad.valider().hasErrors())
        assertThrows<Problemer>{sendtSøknad.sykdomstidslinje()}
    }

    @Test
    internal fun `utdanning ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Utdanning(2.januar, 16.januar))
        assertTrue(sendtSøknad.valider().hasErrors())
        assertThrows<Problemer>{sendtSøknad.sykdomstidslinje()}
    }

    @Test
    internal fun `må ha perioder`() {
        assertThrows<Problemer>{sendtSøknad()}
    }

    @Test
    internal fun `må ha sykdomsperioder`() {
        assertThrows<Problemer>{sendtSøknad(Ferie(2.januar, 16.januar))}
    }



    private fun sendtSøknad(vararg perioder: Periode) {
        sendtSøknad = ModelSendtSøknad(
            UUID.randomUUID(),
            ModelNySøknadTest.UNG_PERSON_FNR_2018,
            "12345",
            "987654321",
            LocalDateTime.now(),
            listOf(*perioder),
            problems
        )
    }
}
