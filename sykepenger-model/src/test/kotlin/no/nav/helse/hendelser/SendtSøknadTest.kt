package no.nav.helse.hendelser

import no.nav.helse.hendelser.SendtSøknad.Periode
import no.nav.helse.hendelser.SendtSøknad.Periode.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

internal class SendtSøknadTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var sendtSøknad: SendtSøknad
    private lateinit var aktivitetslogger: Aktivitetslogger
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogger = Aktivitetslogger()
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    internal fun `sendt søknad med bare sykdom`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100))
        assertFalse(sendtSøknad.valider().hasErrorsOld())
        assertEquals(10, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `sykdomsgrad under 100% støttes ikke`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Sykdom(12.januar, 16.januar, 50))
        assertTrue(sendtSøknad.valider().hasErrorsOld())
    }

    @Test
    internal fun `søknad med ferie`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Ferie(2.januar, 4.januar))
        assertFalse(sendtSøknad.valider().hasErrorsOld())
        assertEquals(10, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `søknad med utdanning`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Utdanning(5.januar, 10.januar))
        assertTrue(sendtSøknad.valider().hasNeedsOld())
        assertEquals(10, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `sykdomsgrad ikke 100`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 50))
        assertTrue(sendtSøknad.valider().hasErrorsOld())
    }

    @Test
    internal fun `sykdom faktiskgrad ikke 100`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100, 50.0))
        assertTrue(sendtSøknad.valider().hasErrorsOld())
    }

    @Test
    internal fun `ferie ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Ferie(2.januar, 16.januar))
        assertTrue(sendtSøknad.valider().hasErrorsOld())
    }

    @Test
    internal fun `utdanning ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Utdanning(16.januar, 10.januar))
        assertTrue(sendtSøknad.valider().hasNeedsOld())
    }

    @Test
    internal fun `permisjon ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Permisjon(2.januar, 16.januar))
        assertTrue(sendtSøknad.valider().hasNeedsOld())
    }

    @Test
    internal fun `arbeidag ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(1.januar, 10.januar, 100), Arbeid(2.januar, 16.januar))
        assertTrue(sendtSøknad.valider().hasErrorsOld())
    }

    @Test
    internal fun `egenmelding ligger utenfor sykdomsvindu`() {
        sendtSøknad(Sykdom(5.januar, 12.januar, 100), Egenmelding(2.januar, 3.januar))
        assertFalse(sendtSøknad.valider().hasErrorsOld())
        assertEquals(11, sendtSøknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `søknad med andre inntektskilder`() {
        sendtSøknad(Sykdom(5.januar, 12.januar, 100), harAndreInntektskilder = true)
        assertTrue(sendtSøknad.valider().hasErrorsOld())
    }

    @Test
    internal fun `søknad uten andre inntektskilder`() {
        sendtSøknad(Sykdom(5.januar, 12.januar, 100), harAndreInntektskilder = false)
        assertFalse(sendtSøknad.valider().hasErrorsOld())
    }

    @Test
    internal fun `må ha perioder`() {
        assertThrows<Aktivitetslogger.AktivitetException> { sendtSøknad() }
    }

    @Test
    internal fun `må ha sykdomsperioder`() {
        assertThrows<Aktivitetslogger.AktivitetException> { sendtSøknad(Ferie(2.januar, 16.januar)) }
    }

    private fun sendtSøknad(vararg perioder: Periode, harAndreInntektskilder: Boolean = false) {
        sendtSøknad = SendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = NySøknadTest.UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = "987654321",
            sendtNav = LocalDateTime.now(),
            perioder = listOf(*perioder),
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg,
            harAndreInntektskilder = harAndreInntektskilder
        )
    }
}
