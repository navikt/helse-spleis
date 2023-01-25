package no.nav.helse.spleis.e2e

import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ManglerVilkårsgrunnlagE2ETest : AbstractEndToEndTest() {

    @Test
    fun `Inntektsmelding opplyser om endret arbeidsgiverperiode - Avsluttet periode får nytt skjæringstidspunkt`() {
        nyttVedtak(2.januar, 31.januar)
        nyPeriode(12.februar til 28.februar)

        assertEquals(2.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar), førsteFraværsdag = 12.februar)

        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        assertNotNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        assertIllegalStateException("Fant ikke vilkårsgrunnlag for 2018-01-17. Må ha et vilkårsgrunnlag for å legge til utbetalingsopplysninger. Har vilkårsgrunnlag på skjæringstidspunktene [2018-02-12]") {
            håndterYtelser(2.vedtaksperiode)
        }
    }

    @Test
    fun `Inntektsmelding opplyser om endret arbeidsgiverperiode - AUU periode inneholder utbetalingsdag`() {
        nyPeriode(2.januar til 17.januar)
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(2.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

        nyPeriode(22.januar til 31.januar)
        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar), førsteFraværsdag = 22.januar)

        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        assertIllegalStateException("Fant ikke vilkårsgrunnlag for 2018-01-17. Må ha et vilkårsgrunnlag for å legge til utbetalingsopplysninger. Har vilkårsgrunnlag på skjæringstidspunktene [2018-01-22]") {
            håndterYtelser(2.vedtaksperiode)
        }
    }

    @Test
    fun `Infotrygd utbetaler periode i forkant - Skjæringstidspunktet flytter seg`() {
        nyttVedtak(1.februar, 28.februar)
        assertEquals(1.februar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

        nyPeriode(10.mars til 31.mars)
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, INNTEKT))
        håndterInntektsmelding(listOf(10.mars til 26.mars))
        håndterVilkårsgrunnlag(2.vedtaksperiode)

        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

        assertIllegalStateException("Fant ikke vilkårsgrunnlag for 2018-02-01. Må ha et vilkårsgrunnlag for å legge til utbetalingsopplysninger. Har vilkårsgrunnlag på skjæringstidspunktene [2018-03-10]") {
            håndterYtelser(2.vedtaksperiode)
        }
    }

    @Test
    fun `Inntektsmelding sletter vilkårsgrunnlag og trekker tilbake penger`() {
        createOvergangFraInfotrygdPerson()
        assertEquals(1.januar til 31.januar, person.inspektør.utbetaltIInfotrygd.single())
        assertEquals(1.februar til 28.februar, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.periode)

        val assertTilstandFørInnteksmeldingHensyntas: () -> Unit = {
            val førsteUtbetalingsdagIInfotrygd = 1.januar
            assertEquals(førsteUtbetalingsdagIInfotrygd, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
            assertTrue(inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.infotrygd)
        }

        assertTilstandFørInnteksmeldingHensyntas()

        håndterSykmelding(Sykmeldingsperiode(5.mars, 31.mars, 100.prosent))
        // Arbeidsgiver sender inntektsmelding for forlengelse i Mars _før_ vi møttar søknad.
        // Så lenge det ikke treffer noen vedtaksperiode i Spleis skjer det ingenting.
        // Personen vært frisk 1. & 2.Mars, så er nytt skjæringstidspunkt, men samme arbeidsgiverperiode
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(16.desember(2017) til 31.desember(2017)),
            førsteFraværsdag = 5.mars
        )
        assertTilstandFørInnteksmeldingHensyntas()

        // Når søknaden kommer replayes Inntektsmelding og nå puttes plutselig info fra Inntektsmlding på
        // arbeidsgiver, også lengre tilbake i tid enn vedtaksperioden som blir truffet.
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.mars, 31.mars, 100.prosent))

        assertForventetFeil(
            forklaring = "Inntektsmeldingen flytter skjæringstidspunkt på tidligere periode på arbeidsgiverperiode og sletter vilkårsgrunnlag",
            nå = {
                assertEquals(16.desember(2017), inspektør.skjæringstidspunkt(1.vedtaksperiode))
                assertNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
                assertTrue(inspektør.vilkårsgrunnlagHistorikkInnslag().first().inspektør.elementer.isEmpty())
            },
            ønsket = {
                assertTilstandFørInnteksmeldingHensyntas()
            }
        )

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        assertIllegalStateException("Fant ikke vilkårsgrunnlag for 2018-02-01. Må ha et vilkårsgrunnlag for å legge til utbetalingsopplysninger. Har vilkårsgrunnlag på skjæringstidspunktene [2018-03-05]") {
            håndterYtelser(2.vedtaksperiode)
        }
    }

    @Test
    fun `korrigert arbeidsgiverperiode under pågående revurdering`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        forlengVedtak(1.februar, 28.februar, 100.prosent)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 80.prosent))

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.februar til 16.februar), førsteFraværsdag = 1.februar)
        assertEquals(listOf(1.januar), person.skjæringstidspunkter())

        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
    }

    @Test
    fun `korrigert arbeidsgiverperiode under pågående revurdering - korrigert søknad for februar`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        forlengVedtak(1.februar, 28.februar, 100.prosent)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 80.prosent))

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.februar til 16.februar), førsteFraværsdag = 1.februar)
        assertEquals(listOf(1.januar), person.skjæringstidspunkter())

        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
    }


    private companion object {
        private fun assertIllegalStateException(melding: String, block: () -> Unit) {
            assertEquals(melding, assertThrows<IllegalStateException>(melding) { block() }.message)
        }

        private fun assertIllegalArgumentException(melding: String, block: () -> Unit) {
            assertEquals(melding, assertThrows<IllegalArgumentException>(melding) { block() }.message)
        }
    }
}