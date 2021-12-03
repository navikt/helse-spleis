package no.nav.helse.hendelser

import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.hentErrors
import no.nav.helse.hentWarnings
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

internal class SøknadTest {

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12029240045"
        private val EN_PERIODE = Periode(1.januar, 31.januar)
        val FYLLER_18_ÅR_2_NOVEMBER = "02110075045"
    }

    private lateinit var søknad: Søknad

    @Test
    fun `søknad med bare sykdom`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `tillater ikke andre inntektskilder dersom én arbeidsgiver`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD")))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertTrue(søknad.validerIkkeOppgittFlereArbeidsforholdMedSykmelding().hasErrorsOrWorse())
    }

    @Test
    fun `søknad med ferie`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Ferie(2.januar, 4.januar))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `17 år på søknadstidspunkt gir error`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), fnr = FYLLER_18_ÅR_2_NOVEMBER, sendtTilNav = 1.november.atStartOfDay())
        assertTrue(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `18 år på søknadstidspunkt gir ikke error`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), fnr = FYLLER_18_ÅR_2_NOVEMBER, sendtTilNav = 2.november.atStartOfDay())
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `søknad med utdanning`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Utdanning(5.januar, 10.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasWarningsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad med permisjon`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Permisjon(5.januar, 10.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasWarningsOrWorse())
        assertEquals(10, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad med papirsykmelding`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Papirsykmelding(11.januar, 16.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertEquals(16, søknad.sykdomstidslinje().count())
        assertEquals(6, søknad.sykdomstidslinje().filterIsInstance<ProblemDag>().size)
    }

    @Test
    fun `sykdomsgrad under 100 støttes`() {
        søknad(Sykdom(1.januar, 10.januar, 50.prosent))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `sykdom faktiskgrad under 100 støttes`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent, 50.prosent))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
    }

    @Test
    fun `ferie foran sykdomsvindu`() {
        søknad(Sykdom(1.februar, 10.februar, 100.prosent), Ferie(20.januar, 31.januar))
        assertFalse(søknad.valider(EN_PERIODE).hasWarningsOrWorse())
    }

    @Test
    fun `ferie etter sykdomsvindu - ikke et realistisk scenario`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Ferie(2.januar, 16.januar))
        assertFalse(søknad.valider(EN_PERIODE).hasWarningsOrWorse())
    }

    @Test
    fun `utdanning ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Utdanning(16.januar, 17.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasWarningsOrWorse())
    }

    @Test
    fun `permisjon ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Permisjon(2.januar, 16.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasWarningsOrWorse())
    }

    @Test
    fun `arbeidag ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(1.januar, 10.januar, 100.prosent), Arbeid(2.januar, 16.januar))
        assertTrue(søknad.valider(EN_PERIODE).hasWarningsOrWorse())
    }

    @Test
    fun `egenmelding ligger utenfor sykdomsvindu`() {
        søknad(Sykdom(5.januar, 12.januar, 100.prosent), Egenmelding(2.januar, 3.januar))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertEquals(8, søknad.sykdomstidslinje().count())
        assertEquals(6, søknad.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(2, søknad.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
    }

    @Test
    fun `egenmelding ligger langt utenfor sykdomsvindu`() {
        søknad(Sykdom(5.januar, 12.januar, 100.prosent), Egenmelding(19.desember(2017), 20.desember(2017)))
        assertFalse(søknad.valider(EN_PERIODE).hasErrorsOrWorse())
        assertEquals(8, søknad.sykdomstidslinje().count())
    }

    @Test
    fun `søknad uten andre inntektskilder`() {
        søknad(Sykdom(5.januar, 12.januar, 100.prosent), andreInntektskilder = emptyList())
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
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 79.prosent))
        søknad.valider(EN_PERIODE)
        assertTrue(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `angitt arbeidsgrad kan føre til lavere sykegrad enn graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 81.prosent))
        søknad.valider(EN_PERIODE)
        assertFalse(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `angitt arbeidsgrad kan føre til lik sykegrad som graden fra sykmelding`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent))
        søknad.valider(EN_PERIODE)
        assertFalse(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `søknad uten permittering får ikke warning`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent))
        søknad.valider(EN_PERIODE)
        assertFalse(søknad.hasWarningsOrWorse())
    }

    @Test
    fun `søknad med permittering får warning`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent), permittert = true)
        søknad.valider(EN_PERIODE)
        assertTrue(søknad.hasWarningsOrWorse())
    }

    @Test
    fun `søknad uten tilbakedateringmerknad får ikke warning`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent))
        søknad.valider(EN_PERIODE)
        assertFalse(søknad.hasWarningsOrWorse())
    }

    @Test
    fun `søknad med tilbakedateringmerknad får warning`() {
        søknad(Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent), merknaderFraSykmelding = listOf(Søknad.Merknad("UGYLDIG_TILBAKEDATERING", null)))
        søknad.valider(EN_PERIODE)
        assertTrue(søknad.hasWarningsOrWorse())
    }

    @Test
    fun `søknadsturnering for nye dagtyper`() {
        søknad(Arbeid(15.januar, 31.januar), Sykdom(1.januar, 31.januar, 100.prosent))

        assertEquals(10, søknad.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(4, søknad.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
        assertEquals(13, søknad.sykdomstidslinje().filterIsInstance<Arbeidsdag>().size)
        assertEquals(4, søknad.sykdomstidslinje().filterIsInstance<FriskHelgedag>().size)
    }

    @Test
    fun `turnering mellom arbeidsgiverdager og sykedager`() {
        søknad(Sykdom(1.januar, 31.januar, 100.prosent), Egenmelding(15.januar, 31.januar))

        assertEquals(23, søknad.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(8, søknad.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
    }

    @Test
    fun `legger på warning om søknad inneholder foreldete dager`() {
        søknad(Sykdom(1.januar, 1.mai, 100.prosent))
        søknad.valider(EN_PERIODE)
        assertEquals(1, søknad.kontekster().size)
        assertTrue(søknad.hasWarningsOrWorse())
        assertFalse(søknad.hasErrorsOrWorse())
    }

    @Test
    fun `inntektskilde med type ANNET skal gi warning istedenfor error`() {
        søknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANNET")))
        søknad.valider(EN_PERIODE)
        assertTrue(søknad.hentWarnings().contains("Det er oppgitt annen inntektskilde i søknaden. Vurder inntekt."))
        assertTrue(søknad.hentErrors().isEmpty())
    }

    @Test
    fun `inntektskilde med type FRILANSER skal gi error`() {
        søknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = listOf(Søknad.Inntektskilde(true, "FRILANSER")))
        søknad.valider(EN_PERIODE)
        assertTrue(søknad.hentErrors().contains("Søknaden inneholder andre inntektskilder enn ANDRE_ARBEIDSFORHOLD"))
    }

    private fun søknad(vararg perioder: Søknadsperiode, andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(), permittert: Boolean = false, merknaderFraSykmelding: List<Søknad.Merknad> = emptyList(), fnr: String = UNG_PERSON_FNR_2018, sendtTilNav: LocalDateTime? = null) {
        søknad = Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = fnr,
            aktørId = "12345",
            orgnummer = "987654321",
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            sendtTilNAV = sendtTilNav ?: Søknadsperiode.søknadsperiode(perioder.toList())?.endInclusive?.atStartOfDay() ?: LocalDateTime.now(),
            permittert = permittert,
            merknaderFraSykmelding = merknaderFraSykmelding,
            sykmeldingSkrevet = LocalDateTime.now()
        )
    }
}
