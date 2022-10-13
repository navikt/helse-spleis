package no.nav.helse.spleis.e2e.infotrygd

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertSisteForkastetPeriodeTilstand
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InfotrygdKorrigererE2ETest : AbstractEndToEndTest() {

    @Test
    fun `infotrygd korrigerer arbeid gjenopptatt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Arbeid(29.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterSykmelding(Sykmeldingsperiode(1.februar, 16.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 16.februar, 100.prosent))
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 29.januar, 31.januar, 100.prosent, INNTEKT), inntektshistorikk = listOf(
            Inntektsopplysning(ORGNUMMER, 29.januar, INNTEKT, true)
        ))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertSisteForkastetPeriodeTilstand(ORGNUMMER, 2.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `skjæringstidspunkt endres som følge av infotrygdperiode`() {
        nyPeriode(1.januar til 1.januar, ORGNUMMER)
        håndterUtbetalingshistorikk(1.vedtaksperiode, besvart = LocalDateTime.now().minusDays(2))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterYtelser(2.vedtaksperiode, besvart = LocalDateTime.now().minusDays(2))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode,  besvart = LocalDateTime.now().minusDays(2))
        håndterSimulering(2.vedtaksperiode)
        nullstillTilstandsendringer()

        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING)
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 2.januar, 2.januar, 100.prosent, INNTEKT))

        assertForventetFeil(
            forklaring = "Første fraværsdag hensyntar ikke infotrygdhistorikken",
            nå = {
                assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
            },
            ønsket = {
                assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
                assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            }
        )
    }

    @Test
    fun `dobbelutbetaling når to sykefraværstilfeller blir til en`() {
        createDobbelutbetalingPerson()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Dagtype.Feriedag)))
        håndterYtelser(2.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterSimulering(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING_REVURDERING)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalingshistorikk(2.vedtaksperiode, Friperiode(1.februar, 28.februar))
        håndterUtbetalt()

        håndterYtelser(3.vedtaksperiode)

        assertEquals(3, inspektør.utbetalinger.size)
        val forrigeUtbetaling = inspektør.utbetaling(1)
        val nyUtbetaling = inspektør.utbetaling(2)

        assertForventetFeil(
            forklaring = "Lager en helt ny utbetaling som fører til dobbelutbetaling av første periode",
            nå = {
                Assertions.assertNotEquals(
                    forrigeUtbetaling.inspektør.korrelasjonsId,
                    nyUtbetaling.inspektør.korrelasjonsId
                )
                assertEquals(
                    listOf(Endringskode.NY, Endringskode.NY, Endringskode.NY),
                    nyUtbetaling.inspektør.arbeidsgiverOppdrag.inspektør.endringskoder()
                )
            },
            ønsket = {
                assertEquals(
                    forrigeUtbetaling.inspektør.korrelasjonsId,
                    nyUtbetaling.inspektør.korrelasjonsId
                )
                assertEquals(
                    listOf(Endringskode.UEND, Endringskode.UEND, Endringskode.NY),
                    nyUtbetaling.inspektør.arbeidsgiverOppdrag.inspektør.endringskoder()
                )
            }
        )
    }
}
