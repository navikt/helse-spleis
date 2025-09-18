package no.nav.helse.spleis.e2e.infotrygd

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.gjenopprettFraJSON
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class InfotrygdKorrigererE2ETest : AbstractEndToEndTest() {

    @Test
    fun `skjæringstidspunkt endres som følge av infotrygdperiode`()  {
        nyPeriode(1.januar til 1.januar, a1)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar))
        håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(
            listOf(3.januar til 18.januar),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@InfotrygdKorrigererE2ETest.håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        nullstillTilstandsendringer()

        this@InfotrygdKorrigererE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 2.januar, 2.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@InfotrygdKorrigererE2ETest.håndterYtelser(2.vedtaksperiode)
        assertVarsel(Varselkode.RV_IT_14, 2.vedtaksperiode.filter())

        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
    }

    @Test
    fun `to sykefraværstilfeller blir til en, starter med AUU`() {
        createDobbelutbetalingPerson()

        this@InfotrygdKorrigererE2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Dagtype.Feriedag)))
        this@InfotrygdKorrigererE2ETest.håndterYtelser(2.vedtaksperiode)
        assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
        håndterSimulering(2.vedtaksperiode)
        this@InfotrygdKorrigererE2ETest.håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        this@InfotrygdKorrigererE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(Friperiode(1.februar, 28.februar))
        håndterUtbetalt()

        assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `Infotrygdutbetaling mens perioden er til revurdering`() {
        nyttVedtak(januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 80.prosent))
        this@InfotrygdKorrigererE2ETest.håndterYtelser()
        assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        håndterSimulering()
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)

        this@InfotrygdKorrigererE2ETest.håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 2.januar, 2.januar))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `to sykefraværstilfeller blir til en, starter med Avsluttet`() {
        createAuuBlirMedIRevureringPerson()

        this@InfotrygdKorrigererE2ETest.håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Dagtype.Feriedag)))
        this@InfotrygdKorrigererE2ETest.håndterYtelser(1.vedtaksperiode)
        assertVarsler(listOf(Varselkode.RV_UT_23, Varselkode.RV_IT_1), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        this@InfotrygdKorrigererE2ETest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertTilstander(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
    }

    private fun createDobbelutbetalingPerson() = createTestPerson { jurist ->
        gjenopprettFraJSON("/personer/dobbelutbetaling.json", jurist)
    }

    private fun createAuuBlirMedIRevureringPerson() = createTestPerson { jurist ->
        gjenopprettFraJSON("/personer/auu-blir-med-i-revurdering.json", jurist)
    }.also {
        UtbetalingshistorikkEtterInfotrygdendring(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            element = InfotrygdhistorikkElement.opprett(
                oppdatert = LocalDateTime.now(),
                hendelseId = MeldingsreferanseId(UUID.randomUUID()),
                perioder = listOf(
                    Friperiode(fom = 1.februar, tom = 28.februar)
                )
            ),
            besvart = LocalDateTime.now()
        ).håndter(Person::håndterUtbetalingshistorikkEtterInfotrygdendring)
    }
}
