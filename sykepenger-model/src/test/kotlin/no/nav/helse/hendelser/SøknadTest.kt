package no.nav.helse.hendelser

import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag.*
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
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private val EN_PERIODE = Periode(1.januar, 31.januar)
    }

    private lateinit var søknad: Søknad
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    internal fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `søknad med bare sykdom`() {
        søknad(Sykdom(1.januar,  10.januar, 100))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad med ferie`() {
        søknad(Sykdom(1.januar,  10.januar, 100), Ferie(2.januar, 4.januar))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad med utdanning`() {
        søknad(Sykdom(1.januar,  10.januar, 100), Utdanning(5.januar, 10.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad med papirsykmelding`() {
        søknad(Sykdom(1.januar,  10.januar, 100), Papirsykmelding(11.januar, 16.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertEquals(16, søknad.sykdomstidslinje().count())
        assertEquals(6, søknad.sykdomstidslinje().filterIsInstance<ProblemDag>().size)
    }

    @Test
    fun `sykdomsgrad under 100 støttes`() {
        søknad(Sykdom(1.januar, 10.januar, 50))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `sykdom faktiskgrad under 100 støttes`() {
        søknad(Sykdom(1.januar, 10.januar, 100, 50))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `ferie ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Ferie(2.januar, 16.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `utdanning ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Utdanning(16.januar, 17.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `permisjon ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Permisjon(2.januar, 16.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `arbeidag ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100), Arbeid(2.januar, 16.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `egenmelding ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(5.januar, 12.januar, 100), Egenmelding(2.januar, 3.januar))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertEquals(8, søknad.sykdomstidslinje().count())
        assertEquals(6, søknad.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(2, søknad.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
    }

    @Test
    fun `egenmelding ligger langt utenfor sykdomsvindu`() {
        søknad(Sykdom(5.januar,  12.januar, 100), Egenmelding(19.desember(2017), 20.desember(2017)))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse()) { aktivitetslogg.toString() }
        assertEquals(8, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad med andre inntektskilder`() {
        søknad(Sykdom(5.januar, 12.januar, 100), harAndreInntektskilder = true)
        assertTrue(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `søknad uten andre inntektskilder`() {
        søknad(Sykdom(5.januar, 12.januar, 100), harAndreInntektskilder = false)
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `må ha perioder`() {
        assertThrows<Aktivitetslogg.AktivitetException> { søknad() }
    }

    @Test
    fun `må ha sykdomsperioder`() {
        assertThrows<Aktivitetslogg.AktivitetException> { søknad(Ferie(2.januar, 16.januar)) }
    }

    @Test
    fun `angitt arbeidsgrad kan ikke føre til sykegrad høyere enn graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar,  20, 21))
        søknad.valider(EN_PERIODE)
        assertTrue(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `angitt arbeidsgrad kan føre til lavere sykegrad enn graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar,  20, 19))
        søknad.valider(EN_PERIODE)
        assertFalse(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `angitt arbeidsgrad kan føre til lik sykegrad som graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar,  20, 20))
        søknad.valider(EN_PERIODE)
        assertFalse(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `søknad uten permittering får ikke warning`() {
        søknad(Sykdom(1.januar, 31.januar,  20, 20))
        søknad.valider(EN_PERIODE)
        assertFalse(søknad.hasWarningsOrWorse())
    }

    @Test
    fun `søknad med permittering får warning`() {
        søknad(Sykdom(1.januar, 31.januar,  20, 20), permittert = true)
        søknad.valider(EN_PERIODE)
        assertTrue(søknad.hasWarningsOrWorse())
    }

    @Test
    fun `søknadsturnering for nye dagtyper`() {
        søknad(Arbeid(15.januar, 31.januar), Sykdom(1.januar, 31.januar,  100))

        assertEquals(10, søknad.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(4, søknad.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
        assertEquals(13, søknad.sykdomstidslinje().filterIsInstance<Arbeidsdag>().size)
        assertEquals(4, søknad.sykdomstidslinje().filterIsInstance<FriskHelgedag>().size)
    }

    @Test
    fun `turnering mellom arbeidsgiverdager og sykedager`() {
        søknad(Sykdom(1.januar, 31.januar,  100), Egenmelding(15.januar, 31.januar))

        assertEquals(23, søknad.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(8, søknad.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
    }

    private fun søknad(vararg perioder: Søknadsperiode, harAndreInntektskilder: Boolean = false, permittert: Boolean = false) {
        søknad = Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = "987654321",
            perioder = listOf(*perioder),
            harAndreInntektskilder = harAndreInntektskilder,
            sendtTilNAV = Søknadsperiode.søknadsperiode(perioder.toList())?.endInclusive?.atStartOfDay() ?: LocalDateTime.now(),
            permittert = permittert
        )
    }
}
