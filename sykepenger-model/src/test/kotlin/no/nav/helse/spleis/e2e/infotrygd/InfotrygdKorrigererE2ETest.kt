package no.nav.helse.spleis.e2e.infotrygd

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.dsl.TestPerson
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.readResource
import no.nav.helse.serde.SerialisertPerson
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.AUUSomFørstegangsbehandling::class)
internal class InfotrygdKorrigererE2ETest : AbstractEndToEndTest() {

    @Test
    fun `infotrygd korrigerer arbeid gjenopptatt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Arbeid(29.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
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
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, besvart = LocalDateTime.now().minusDays(2))
        håndterSimulering(2.vedtaksperiode)
        nullstillTilstandsendringer()

        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING)
        håndterUtbetalingshistorikk(2.vedtaksperiode, ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 2.januar, 2.januar, 100.prosent, INNTEKT))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, TIL_INFOTRYGD)
    }

    @Test
    fun `to sykefraværstilfeller blir til en, starter med AUU`() {
        createDobbelutbetalingPerson()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Dagtype.Feriedag)))
        håndterYtelser(2.vedtaksperiode, besvart = LocalDate.EPOCH.atStartOfDay())
        håndterSimulering(2.vedtaksperiode)
        håndterPåminnelse(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalingshistorikk(2.vedtaksperiode, Friperiode(1.februar, 28.februar))
        håndterUtbetalt()

        assertForkastetPeriodeTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, TIL_INFOTRYGD)
    }

    @Test
    fun `to sykefraværstilfeller blir til en, starter med Avsluttet`() {
        createAuuBlirMedIRevureringPerson()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Dagtype.Feriedag)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterYtelser(2.vedtaksperiode)
        assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, TIL_INFOTRYGD)
        håndterYtelser(3.vedtaksperiode)

        inspektør.utbetaling(2).inspektør.also {
            assertEquals(it.korrelasjonsId, inspektør.utbetaling(0).inspektør.korrelasjonsId)
            assertEquals(it.arbeidsgiverOppdrag.inspektør.fagsystemId(), inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.fagsystemId())
            assertEquals(2, it.arbeidsgiverOppdrag.size)
            assertEquals(Endringskode.ENDR, it.arbeidsgiverOppdrag[0].inspektør.endringskode)
            assertEquals(17.januar, it.arbeidsgiverOppdrag[0].inspektør.fom)
            assertEquals(18.januar, it.arbeidsgiverOppdrag[0].inspektør.tom)
            assertEquals(Endringskode.NY, it.arbeidsgiverOppdrag[1].inspektør.endringskode)
            assertEquals(20.januar, it.arbeidsgiverOppdrag[1].inspektør.fom)
            assertEquals(31.januar, it.arbeidsgiverOppdrag[1].inspektør.tom)
        }

        inspektør.utbetaling(3).inspektør.also {
            assertEquals(it.korrelasjonsId, inspektør.utbetaling(1).inspektør.korrelasjonsId)
            assertEquals(it.arbeidsgiverOppdrag.inspektør.fagsystemId(), inspektør.utbetaling(1).inspektør.arbeidsgiverOppdrag.fagsystemId())
            assertEquals(1, it.arbeidsgiverOppdrag.size)
            assertEquals(Endringskode.UEND, it.arbeidsgiverOppdrag[0].inspektør.endringskode)
            assertEquals(17.mai, it.arbeidsgiverOppdrag[0].inspektør.fom)
            assertEquals(31.mai, it.arbeidsgiverOppdrag[0].inspektør.tom)
        }
    }


    private fun createDobbelutbetalingPerson() = createTestPerson { jurist ->
        SerialisertPerson("/personer/dobbelutbetaling.json".readResource()).deserialize(jurist)
    }

    private fun createOverlappendeFraInfotrygdPerson() = createTestPerson { jurist ->
        SerialisertPerson("/personer/infotrygd-overlappende-utbetaling.json".readResource()).deserialize(jurist).also { person ->
            person.håndter(
                Utbetalingshistorikk(
                    UUID.randomUUID(), "", "", ORGNUMMER, UUID.randomUUID().toString(),
                    InfotrygdhistorikkElement.opprett(
                        LocalDateTime.now(),
                        UUID.randomUUID(),
                        listOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.mars, 30.april, 100.prosent, TestPerson.INNTEKT)),
                        listOf(Inntektsopplysning(ORGNUMMER, 1.januar, TestPerson.INNTEKT, true)),
                        emptyMap(),
                        emptyList(),
                        false
                    ),
                ),
            )
        }
    }

    private fun createAuuBlirMedIRevureringPerson() = createTestPerson { jurist ->
        SerialisertPerson("/personer/auu-blir-med-i-revurdering.json".readResource()).deserialize(jurist).also { person ->
            person.håndter(
                Utbetalingshistorikk(
                    UUID.randomUUID(), "", "", ORGNUMMER, UUID.randomUUID().toString(),
                    InfotrygdhistorikkElement.opprett(
                        oppdatert = LocalDateTime.now(),
                        hendelseId = UUID.randomUUID(),
                        perioder = listOf(
                            Friperiode(fom = 1.februar, tom = 28.februar)
                        ),
                        inntekter = listOf(Inntektsopplysning(ORGNUMMER, 1.januar, TestPerson.INNTEKT, true)),
                        arbeidskategorikoder = emptyMap(),
                        ugyldigePerioder = emptyList(),
                        harStatslønn = false
                    ),
                ),
            )
        }
    }
}
