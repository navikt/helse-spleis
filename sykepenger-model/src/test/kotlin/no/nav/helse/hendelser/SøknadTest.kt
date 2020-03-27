package no.nav.helse.hendelser

import no.nav.helse.hendelser.Søknad.Periode
import no.nav.helse.hendelser.Søknad.Periode.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

internal class SøknadTest {

    private companion object {
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
        søknad(Sykdom(1.januar,  10.januar, 100, null))
        assertFalse(søknad.valider().hasErrors())
        assertEquals(10, søknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `søknad med ferie`() {
        søknad(Sykdom(1.januar,  10.januar, 100, null), Ferie(2.januar, 4.januar))
        assertFalse(søknad.valider().hasErrors())
        assertEquals(10, søknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `søknad med utdanning`() {
        søknad(Sykdom(1.januar,  10.januar, 100, null), Utdanning(5.januar, 10.januar))
        assertTrue(søknad.valider().hasBehov())
        assertEquals(10, søknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `sykdomsgrad under 100 støttes`() {
        søknad(Sykdom(1.januar,  10.januar, 50, null))
        assertFalse(søknad.valider().hasErrors())
    }

    @Test
    internal fun `sykdom faktiskgrad under 100 støttes`() {
        søknad(Sykdom(1.januar, 10.januar, 100, 50))
        assertFalse(søknad.valider().hasErrors())
    }

    @Test
    internal fun `ferie ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar,  10.januar, 100, null), Ferie(2.januar, 16.januar))
        assertTrue(søknad.valider().hasErrors())
    }

    @Test
    internal fun `utdanning ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar,  10.januar, 100, null), Utdanning(16.januar, 10.januar))
        assertTrue(søknad.valider().hasBehov())
    }

    @Test
    internal fun `permisjon ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar,  10.januar, 100, null), Permisjon(2.januar, 16.januar))
        assertTrue(søknad.valider().hasBehov())
    }

    @Test
    internal fun `arbeidag ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar,  10.januar, 100, null), Arbeid(2.januar, 16.januar))
        assertTrue(søknad.valider().hasErrors())
    }

    @Test
    internal fun `egenmelding ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(5.januar,  12.januar, 100, null), Egenmelding(2.januar, 3.januar))
        assertFalse(søknad.valider().hasErrors())
        assertEquals(11, søknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `egenmelding ligger langt utenfor sykdomsvindu`() {
        søknad(Sykdom(5.januar,  12.januar, 100, null), Egenmelding(19.desember(2017), 20.desember(2017)))
        assertFalse(søknad.valider().hasErrors())
        assertTrue(søknad.valider().hasWarnings())
        assertEquals(8, søknad.sykdomstidslinje().length())
    }

    @Test
    internal fun `søknad med andre inntektskilder`() {
        søknad(Sykdom(5.januar,  12.januar, 100, null), harAndreInntektskilder = true)
        assertTrue(søknad.valider().hasErrors())
    }

    @Test
    internal fun `søknad uten andre inntektskilder`() {
        søknad(Sykdom(5.januar,  12.januar, 100, null), harAndreInntektskilder = false)
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

    @Test
    internal fun `angitt arbeidsgrad kan ikke føre til sykegrad høyere enn graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar,  20, 79))
        søknad.valider()
        assertTrue(søknad.hasErrors())
    }

    @Test
    internal fun `angitt arbeidsgrad kan føre til lavere sykegrad enn graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar,  20, 81))
        søknad.valider()
        assertFalse(søknad.hasErrors())
    }

    @Test
    internal fun `angitt arbeidsgrad kan føre til lik sykegrad som graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar,  20, 80))
        søknad.valider()
        assertFalse(søknad.hasErrors())
    }

    private fun søknad(vararg perioder: Periode, harAndreInntektskilder: Boolean = false) {
        søknad = Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = "987654321",
            perioder = listOf(*perioder),
            harAndreInntektskilder = harAndreInntektskilder,
            sendtTilNAV = perioder.lastOrNull()?.tom?.atStartOfDay() ?: LocalDateTime.now()
        )
    }
}
