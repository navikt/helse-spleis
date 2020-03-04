package no.nav.helse.e2e

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class FlereInntektsmeldingerTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private const val INNTEKT = 31000.00
    }

    private lateinit var person: Person
    private lateinit var observatør: TestObservatør
    private val inspektør get() = TestPersonInspektør(person)
    private lateinit var hendelselogg: ArbeidstakerHendelse
    private var forventetEndringTeller = 0

    @BeforeEach
    internal fun setup() {
        person = Person(UNG_PERSON_FNR_2018, AKTØRID)
        observatør = TestObservatør().also { person.addObserver(it) }
    }

    @Test
    internal fun `ignorer inntektsmeldinger på påfølgende perioder`() {
        håndterSykmelding(Triple(3.januar, 26.januar, 100))
        håndterSykmelding(Triple(29.januar, 16.februar, 100))
        håndterInntektsmelding(3.januar, listOf(Periode(3.januar, 26.januar)))
        val inntektsmelding = inntektsmelding(arbeidsgiverperioder = listOf(Periode(29.januar, 16.februar)), førsteFraværsdag = 29.januar, beregnetInntekt = Double.POSITIVE_INFINITY, refusjonBeløp = Double.POSITIVE_INFINITY)
        assertIngenEndring { person.håndter(inntektsmelding) }
        inspektør.also {
            assertFalse(it.personLogg.hasErrors())
            assertTrue(it.personLogg.hasWarnings())
            assertEquals(INNTEKT.toBigDecimal(), it.inntektshistorikk.inntekt(3.januar))
            assertEquals(INNTEKT.toBigDecimal(), it.inntektshistorikk.inntekt(29.januar))
            assertEquals(1, it.inntekter[0]?.size)
            assertEquals(1, it.førsteFraværsdager.size)
            assertEquals(3.januar, it.førsteFraværsdager[0])
        }
        assertTrue(inntektsmelding.hasWarnings())
        assertTilstander(0, START, MOTTATT_SYKMELDING, AVVENTER_SØKNAD)
        assertTilstander(1, START, MOTTATT_SYKMELDING)
    }

    private fun assertEndringTeller() {
        forventetEndringTeller += 1
        assertEquals(forventetEndringTeller, observatør.endreTeller)
    }

    private fun assertIngenEndring(block: () -> Unit) {
        block()
        assertEquals(forventetEndringTeller, observatør.endreTeller)
    }

    private fun assertTilstander(indeks: Int, vararg tilstander: TilstandType) {
        assertEquals(tilstander.asList(), observatør.tilstander[indeks])
    }

    private fun håndterSykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>) {
        person.håndter(sykmelding(*sykeperioder))
        assertEndringTeller()
    }

    private fun håndterInntektsmelding(førsteFraværsdag: LocalDate, arbeidsgiverperioder: List<Periode>) {
        person.håndter(inntektsmelding(arbeidsgiverperioder = arbeidsgiverperioder, førsteFraværsdag = førsteFraværsdag))
        assertEndringTeller()
    }

    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(*sykeperioder)
        ).apply {
            hendelselogg = this
        }
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        refusjonBeløp: Double = INNTEKT,
        beregnetInntekt: Double = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 31.desember,  // Employer paid
        endringerIRefusjon: List<LocalDate> = emptyList()
    ): Inntektsmelding {
        return Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder
        ).apply {
            hendelselogg = this
        }
    }
}
