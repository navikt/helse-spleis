package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.inntekt.Inntektshistorikk
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
        assertEquals(INNTEKT_PR_MÅNED, inntekthistorikk.omregnetÅrsinntekt(1.januar, 1.januar, Arbeidsforholdhistorikk())?.omregnetÅrsinntekt())
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
        assertTrue(person.personLogg.harVarslerEllerVerre())
        assertFalse(person.personLogg.harFunksjonelleFeilEllerVerre())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.AVVENTER_VILKÅRSPRØVING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `ferie i inntektsmelding vinner over sykedager i sykmelding`() {
        val inntektsmelding = a1Hendelsefabrikk.lagInntektsmelding(
            refusjon = Inntektsmelding.Refusjon(INNTEKT_PR_MÅNED, null, emptyList()),
            førsteFraværsdag = 1.januar,
            beregnetInntekt = INNTEKT_PR_MÅNED,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )
        assertFalse(inntektsmelding.valider(Periode(1.januar, 31.januar), MaskinellJurist()).harFunksjonelleFeilEllerVerre())
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 20.januar, 100.prosent)))
        person.håndter(søknad(Sykdom(6.januar, 20.januar, 100.prosent)))
        person.håndter(inntektsmelding)
        assertEquals(TilstandType.AVVENTER_VILKÅRSPRØVING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    private fun inntektsmelding(
        beregnetInntekt: Inntekt = 1000.månedlig,
        førsteFraværsdag: LocalDate = 1.januar
    ) = a1Hendelsefabrikk.lagInntektsmelding(
            refusjon = Inntektsmelding.Refusjon(beregnetInntekt, null, emptyList()),
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode) = a1Hendelsefabrikk.lagSykmelding(
        sykeperioder = sykeperioder,
        sykmeldingSkrevet = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
        mottatt = Sykmeldingsperiode.periode(sykeperioder.toList())!!.endInclusive.atStartOfDay()
    )

    private fun søknad(vararg perioder: Søknadsperiode) = a1Hendelsefabrikk.lagSøknad(
        perioder = arrayOf(*perioder),
        andreInntektskilder = false,
        sendtTilNAVEllerArbeidsgiver = Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        permittert = false,
        merknaderFraSykmelding = emptyList(),
        sykmeldingSkrevet = LocalDateTime.now()
    )
}
