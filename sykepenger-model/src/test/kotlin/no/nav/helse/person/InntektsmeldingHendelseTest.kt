package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InntektsmeldingHendelseTest : AbstractPersonTest() {

    private companion object {
        private val INNTEKT_PR_MÅNED = 12340.månedlig
    }

    @Test
    fun `legger inn beregnet inntekt i inntekthistorikk`() {
        val inntekthistorikk = Inntektshistorikk()
        inntektsmelding(beregnetInntekt = INNTEKT_PR_MÅNED, førsteFraværsdag = 1.januar)
            .addInntekt(inntekthistorikk, 1.januar, MaskinellJurist())
        assertEquals(INNTEKT_PR_MÅNED, inntekthistorikk.grunnlagForSykepengegrunnlag(1.januar, 1.januar)?.grunnlagForSykepengegrunnlag())
    }

    @Test
    fun `skjæringstidspunkt oppdateres i vedtaksperiode når inntektsmelding håndteres`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100.prosent)))
        person.håndter(søknad(Sykdom(6.januar, 20.januar, 100.prosent)))
        assertEquals(6.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        person.håndter(inntektsmelding(førsteFraværsdag = 1.januar))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
    }


    @Test
    fun `flere inntektsmeldinger`() {
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100.prosent)))
        person.håndter(søknad(Sykdom(6.januar, 20.januar, 100.prosent)))
        person.håndter(inntektsmelding())
        person.håndter(inntektsmelding())
        assertTrue(person.personLogg.hasWarningsOrWorse())
        assertFalse(person.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `ferie i inntektsmelding vinner over sykedager i sykmelding`() {
        val inntektsmelding = Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(INNTEKT_PR_MÅNED, null, emptyList()),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            aktørId = AKTØRID,
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT_PR_MÅNED,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        )
        assertFalse(inntektsmelding.valider(Periode(1.januar, 31.januar), MaskinellJurist()).hasErrorsOrWorse())
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100.prosent)))
        person.håndter(søknad(Sykdom(6.januar, 20.januar, 100.prosent)))
        person.håndter(inntektsmelding)
        assertEquals(TilstandType.AVVENTER_HISTORIKK, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    private fun inntektsmelding(
        beregnetInntekt: Inntekt = 1000.månedlig,
        førsteFraværsdag: LocalDate = 1.januar,
        virksomhetsnummer: String = ORGNUMMER
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

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnr: String = ORGNUMMER) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = UNG_PERSON_FNR_2018.toString(),
        aktørId = AKTØRID,
        orgnummer = orgnr,
        sykeperioder = sykeperioder.toList(),
        sykmeldingSkrevet = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
        mottatt = Sykmeldingsperiode.periode(sykeperioder.toList())!!.endInclusive.atStartOfDay()
    )

    private fun søknad(vararg perioder: Søknadsperiode, orgnummer: String = ORGNUMMER) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018.toString(),
            aktørId = "12345",
            orgnummer = orgnummer,
            perioder = listOf(*perioder),
            andreInntektskilder = emptyList(),
            sendtTilNAVEllerArbeidsgiver = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
        )
}
