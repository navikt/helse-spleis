package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InntektsmeldingHendelseTest {

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "12345"
        private const val ORGNR = "987654321"
        private val INNTEKT_PR_MÅNED = 12340.månedlig
    }

    private lateinit var person: Person

    private val inspektør get() = TestArbeidsgiverInspektør(person)

    @BeforeEach
    internal fun opprettPerson() {
        person = Person(AKTØRID, UNG_PERSON_FNR_2018)
    }

    @Test
    internal fun `legger inn beregnet inntekt i inntekthistorikk`() {
        val inntekthistorikk = Inntekthistorikk()
        inntektsmelding(beregnetInntekt = INNTEKT_PR_MÅNED, førsteFraværsdag = 1.januar)
            .addInntekt(inntekthistorikk)
        assertEquals(INNTEKT_PR_MÅNED, inntekthistorikk.inntekt(1.januar))
    }

    @Test
    internal fun `førsteFraværsdag settes i vedtaksperiode når inntektsmelding håndteres`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding(førsteFraværsdag = 1.januar))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1.januar, inspektør.førsteFraværsdag(0))
    }

    @Test
    internal fun `inntektsmelding før søknad`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `inntektsmelding etter søknad`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100)))
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(6.januar,  20.januar, 100)))
        person.håndter(inntektsmelding())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_VILKÅRSPRØVING_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `søknad etter inntektsmelding`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        person.håndter(søknad(Søknad.Søknadsperiode.Sykdom(6.januar,  20.januar, 100)))
        assertFalse(inspektør.personLogg.hasErrors(), inspektør.personLogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_VILKÅRSPRØVING_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `Sykmelding med overlapp på en periode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        person.håndter(sykmelding(Sykmeldingsperiode(19.januar, 30.januar, 100)))
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `mangler sykmelding`() {
        person.håndter(inntektsmelding())
        assertTrue(inspektør.personLogg.hasErrors())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
    }

    @Test
    internal fun `flere inntektsmeldinger`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding())
        person.håndter(inntektsmelding())
        assertTrue(inspektør.personLogg.hasWarnings())
        assertFalse(inspektør.personLogg.hasErrors())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP, inspektør.sisteTilstand(0))
    }

    @Test
    internal fun `ferie i inntektsmelding vinner over sykedager i sykmelding`() {
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, INNTEKT_PR_MÅNED, emptyList()),
            orgnummer = ORGNR,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT_PR_MÅNED,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            ferieperioder = listOf(Periode(16.januar, 31.januar)),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )
        assertFalse(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasErrors())
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100)))
        person.håndter(inntektsmelding)
        assertEquals(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP, inspektør.sisteTilstand(0))
    }

    private fun inntektsmelding(
        beregnetInntekt: Inntekt = 1000.månedlig,
        førsteFraværsdag: LocalDate = 1.januar,
        virksomhetsnummer: String = ORGNR
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, beregnetInntekt, emptyList()),
            orgnummer = virksomhetsnummer,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnr: String = ORGNR) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018,
        aktørId = AKTØRID,
        orgnummer = orgnr,
        sykeperioder = listOf(*sykeperioder),
        mottatt = sykeperioder.map { it.fom }.min()?.atStartOfDay() ?: LocalDateTime.now()
    )

    private fun søknad(vararg perioder: Søknad.Søknadsperiode, orgnummer: String = ORGNR) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            harAndreInntektskilder = false,
            sendtTilNAV = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive.atStartOfDay(),
            permittert = false
        )
}
