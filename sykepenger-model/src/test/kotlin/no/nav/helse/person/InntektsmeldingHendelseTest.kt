package no.nav.helse.person

import no.nav.helse.Organisasjonsnummer
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InntektsmeldingHendelseTest : AbstractPersonTest() {

    private companion object {
        private val INNTEKT_PR_MÅNED = 12340.månedlig
    }

    @Test
    fun `legger inn beregnet inntekt i inntekthistorikk`() {
        val inntekthistorikk = Inntektshistorikk()
        inntektsmelding(beregnetInntekt = INNTEKT_PR_MÅNED, førsteFraværsdag = 1.januar)
            .addInntekt(inntekthistorikk, 1.januar)
        assertEquals(INNTEKT_PR_MÅNED, inntekthistorikk.grunnlagForSykepengegrunnlag(1.januar, 1.januar)?.grunnlagForSykepengegrunnlag())
    }

    @Test
    fun `skjæringstidspunkt oppdateres i vedtaksperiode når inntektsmelding håndteres`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100.prosent)))
        assertEquals(6.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        person.håndter(inntektsmelding(førsteFraværsdag = 1.januar))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
    }

    @Test
    fun `inntektsmelding før søknad`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100.prosent)))
        person.håndter(inntektsmelding())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `søknad etter inntektsmelding`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100.prosent)))
        person.håndter(inntektsmelding())
        person.håndter(søknad(Søknadsperiode.Sykdom(6.januar,  20.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse(), inspektør.personLogg.toString())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `mangler sykmelding`() {
        person.håndter(inntektsmelding())
        assertTrue(inspektør.personLogg.hasWarningsOrWorse())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
    }

    @Test
    fun `flere inntektsmeldinger`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100.prosent)))
        person.håndter(inntektsmelding())
        person.håndter(inntektsmelding())
        assertTrue(inspektør.personLogg.hasWarningsOrWorse())
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `ferie i inntektsmelding vinner over sykedager i sykmelding`() {
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(INNTEKT_PR_MÅNED, null, emptyList()),
            orgnummer = ORGNUMMER.toString(),
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            aktørId = AKTØRID,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT_PR_MÅNED,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )
        assertFalse(inntektsmelding.valider(Periode(1.januar, 31.januar)).hasErrorsOrWorse())
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100.prosent)))
        person.håndter(inntektsmelding)
        assertEquals(TilstandType.AVVENTER_SØKNAD_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    private fun inntektsmelding(
        beregnetInntekt: Inntekt = 1000.månedlig,
        førsteFraværsdag: LocalDate = 1.januar,
        virksomhetsnummer: Organisasjonsnummer = ORGNUMMER
    ) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
            orgnummer = virksomhetsnummer.toString(),
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnr: Organisasjonsnummer = ORGNUMMER) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018.toString(),
        aktørId = AKTØRID,
        orgnummer = orgnr.toString(),
        sykeperioder = sykeperioder.toList(),
        sykmeldingSkrevet = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
        mottatt = Sykmeldingsperiode.periode(sykeperioder.toList())!!.endInclusive.atStartOfDay()
    )

    private fun søknad(vararg perioder: Søknadsperiode, orgnummer: Organisasjonsnummer = ORGNUMMER) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018.toString(),
            aktørId = "12345",
            orgnummer = orgnummer.toString(),
            perioder = listOf(*perioder),
            andreInntektskilder = emptyList(),
            sendtTilNAVEllerArbeidsgiver = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
        )
}
