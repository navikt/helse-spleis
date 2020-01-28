package no.nav.helse.hendelser

import no.nav.helse.fixtures.januar
import no.nav.helse.hendelser.ModelSendtSøknad.Periode
import no.nav.helse.hendelser.ModelSendtSøknad.Periode.*
import no.nav.helse.person.Aktivitetslogger
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
    private lateinit var aktivitetslogger: Aktivitetslogger

    @BeforeEach
    internal fun setup() {
        aktivitetslogger = Aktivitetslogger()
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
    }

    @Test
    internal fun `søknad med ferie`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Ferie(2.januar, 4.januar))
        assertFalse(sendtSøknad.valider().hasErrors())
        assertEquals(10, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `søknad med utdanning`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Utdanning(5.januar, 10.januar))
        assertTrue(sendtSøknad.valider().hasErrors())
        assertEquals(10, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `sykdomsgrad ikke 100`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 50))
        assertTrue(sendtSøknad.valider().hasErrors())
    }

    @Test
    internal fun `sykdom faktiskgrad ikke 100`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100, 50.0))
        assertTrue(sendtSøknad.valider().hasErrors())
    }

    @Test
    internal fun `ferie ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Ferie(2.januar, 16.januar))
        assertTrue(sendtSøknad.valider().hasErrors())
    }

    @Test
    internal fun `utdanning ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Utdanning(16.januar, 10.januar))
        assertTrue(sendtSøknad.valider().hasErrors())
    }

    @Test
    internal fun `permisjon ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Permisjon(2.januar, 16.januar))
        assertTrue(sendtSøknad.valider().hasErrors())
    }

    @Test
    internal fun `arbeidag ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Arbeid(2.januar, 16.januar))
        assertTrue(sendtSøknad.valider().hasErrors())
    }

    @Test
    internal fun `egenmelding ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(5.januar, 12.januar, 100), Egenmelding(2.januar, 3.januar))
        assertFalse(sendtSøknad.valider().hasErrors())
        assertEquals(11, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `må ha perioder`() {
        assertThrows<Aktivitetslogger.AktivitetException> { sendtSøknad() }
    }

    @Test
    internal fun `må ha sykdomsperioder`() {
        assertThrows<Aktivitetslogger.AktivitetException> { sendtSøknad(Ferie(2.januar, 16.januar)) }
    }

    private fun sendtSøknad(vararg perioder: Periode) {
        sendtSøknad = ModelSendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = ModelNySøknadTest.UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = "987654321",
            rapportertdato = LocalDateTime.now(),
            perioder = listOf(*perioder),
            originalJson = "{}",
            aktivitetslogger = aktivitetslogger
        )
    }
}
