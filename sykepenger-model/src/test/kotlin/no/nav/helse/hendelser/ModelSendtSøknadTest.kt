package no.nav.helse.hendelser

import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.hendelser.ModelSendtSøknad.Periode.Ferie
import no.nav.helse.hendelser.ModelSendtSøknad.Periode.Sykdom
import no.nav.helse.person.Problems
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class ModelSendtSøknadTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var sendtSøknad: ModelSendtSøknad
    private lateinit var problems: Problems

    @BeforeEach
    internal fun setup() {
        problems = Problems()
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
        assertEquals(16, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `søknad med ferie`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Ferie(2.januar, 4.januar))
        assertFalse(sendtSøknad.valider().hasErrors())
        assertEquals(10, sendtSøknad.sykdomstidslinje().length())
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
