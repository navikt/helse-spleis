package no.nav.helse.spleis.e2e.infotrygd

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
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
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InfotrygdKorrigererE2ETest : AbstractDslTest() {

    @Test
    fun `skjæringstidspunkt endres som følge av infotrygdperiode`() {
        a1 {
            nyPeriode(1.januar til 1.januar, a1)
            håndterSykmelding(Sykmeldingsperiode(3.januar, 31.januar))
            håndterSøknad(Sykdom(3.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(3.januar til 18.januar),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            nullstillTilstandsendringer()

            assertEquals(listOf(1.januar.somPeriode(), 3.januar til 17.januar), inspektør.venteperiode(2.vedtaksperiode))
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 2.januar, 2.januar))
            assertEquals(listOf(1.januar.somPeriode()), inspektør.venteperiode(2.vedtaksperiode))

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            assertVarsel(RV_IV_7, 2.vedtaksperiode.filter())

            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_IT_14, 2.vedtaksperiode.filter())

            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `to sykefraværstilfeller blir til en, starter med AUU`() {
        createDobbelutbetalingPerson()

        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Dagtype.Feriedag)))
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalingshistorikkEtterInfotrygdendring(Friperiode(1.februar, 28.februar))
            håndterUtbetalt()

            assertTilstander(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `Infotrygdutbetaling mens perioden er til revurdering`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 80.prosent))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)

            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 2.januar, 2.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `to sykefraværstilfeller blir til en, starter med Avsluttet`() {
        createAuuBlirMedIRevureringPerson()

        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                utbetalinger = listOf(
                    Friperiode(fom = 1.februar, tom = 28.februar)
                )
            )
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(19.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_UT_23, Varselkode.RV_IT_1), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)

        }
    }

    private fun createDobbelutbetalingPerson() =
        medJSONPerson("/personer/dobbelutbetaling.json", 334)

    private fun createAuuBlirMedIRevureringPerson() =
        medJSONPerson("/personer/auu-blir-med-i-revurdering.json", 334)
}
