package no.nav.helse.hendelser

import no.nav.helse.hendelser.Søknad.Periode
import no.nav.helse.hendelser.Søknad.Periode.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

internal class SøknadTest {

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var søknad: Søknad
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    internal fun `søknad med bare sykdom`() {
        søknad(Sykdom(1.januar, 10.januar, 100))
        assertFalse(søknad.valider().hasErrors())
        assertEquals(10, søknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `sykdomsgrad under 100% støttes ikke`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Sykdom(12.januar, 16.januar, 50))
        assertTrue(søknad.valider().hasErrors())
    }

    @Test
    internal fun `søknad med ferie`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Ferie(2.januar, 4.januar))
        assertFalse(søknad.valider().hasErrors())
        assertEquals(10, søknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `søknad med utdanning`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Utdanning(5.januar, 10.januar))
        assertTrue(søknad.valider().hasNeeds())
        assertEquals(10, søknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `sykdomsgrad ikke 100`() {
        søknad(Sykdom(1.januar, 10.januar, 50))
        assertTrue(søknad.valider().hasErrors())
    }

    @Test
    internal fun `sykdom faktiskgrad ikke 100`() {
        søknad(Sykdom(1.januar, 10.januar, 100, 50.0))
        assertTrue(søknad.valider().hasErrors())
    }

    @Test
    internal fun `ferie ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Ferie(2.januar, 16.januar))
        assertTrue(søknad.valider().hasErrors())
    }

    @Test
    internal fun `utdanning ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Utdanning(16.januar, 10.januar))
        assertTrue(søknad.valider().hasNeeds())
    }

    @Test
    internal fun `permisjon ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Permisjon(2.januar, 16.januar))
        assertTrue(søknad.valider().hasNeeds())
    }

    @Test
    internal fun `arbeidag ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Arbeid(2.januar, 16.januar))
        assertTrue(søknad.valider().hasErrors())
    }

    @Test
    internal fun `egenmelding ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(5.januar, 12.januar, 100), Egenmelding(2.januar, 3.januar))
        assertFalse(søknad.valider().hasErrors())
        assertEquals(11, søknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `søknad med andre inntektskilder`() {
        søknad(Sykdom(5.januar, 12.januar, 100), harAndreInntektskilder = true)
        assertTrue(søknad.valider().hasErrors())
    }

    @Test
    internal fun `søknad uten andre inntektskilder`() {
        søknad(Sykdom(5.januar, 12.januar, 100), harAndreInntektskilder = false)
        assertFalse(søknad.valider().hasErrors())
    }

    @Test
    internal fun `må ha perioder`() {
        assertThrows<Aktivitetslogg.AktivitetException> { søknad() }
    }

    @Test
    internal fun `må ha sykdomsperioder`() {
        assertThrows<Aktivitetslogg.AktivitetException> { søknad(Ferie(2.januar, 16.januar)) }
    }

    private fun søknad(vararg perioder: Periode, harAndreInntektskilder: Boolean = false) {
        søknad = Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = SykmeldingTest.UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = "987654321",
            perioder = listOf(*perioder),
            harAndreInntektskilder = harAndreInntektskilder
        )
    }
}
