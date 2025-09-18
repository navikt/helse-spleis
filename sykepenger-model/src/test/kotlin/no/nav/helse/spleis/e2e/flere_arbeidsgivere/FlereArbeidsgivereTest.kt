package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.april
import no.nav.helse.den
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mandag
import no.nav.helse.mars
import no.nav.helse.onsdag
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SY_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.arbeidsgiveropplysninger.TrengerArbeidsgiveropplysningerTest.Companion.assertEtterspurt
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.til
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereTest : AbstractDslTest() {

    @Test
    fun `a1 med periode tidligere utenfor AGP, men innenfor senere, skal få opprettet utbetaling av a2`() {
        a1  {
            nyPeriode(2.januar til 10.januar)
            nyPeriode(11.januar til 17.januar)
            nyPeriode(18.januar til 31.januar)
        }
        a2 {
            nyPeriode(januar)
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a2 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a2 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()
        }

        // endrer arbeidsgiverperioden slik at 2.vedtaksperiode er innenfor AGP
        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag)))
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a1 {
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)

            assertEquals(listOf(Utbetalingstatus.UTBETALT, Utbetalingstatus.IKKE_UTBETALT), inspektør.utbetalinger(2.vedtaksperiode).map { it.status })
            assertEquals(listOf(Utbetalingstatus.UTBETALT), inspektør.utbetalinger(3.vedtaksperiode).map { it.status })
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            håndterYtelser(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Periode som starter før en annen, men med senere skjæringstidspunkt - da må de behandles i rett rekkefølge`() {
        a1 { nyttVedtak(januar) } // Ignorer denne, den må bare på plass til å lage rett rekkefølge på ting
        a2 {
            håndterSøknad(16.januar(2025) til 24.januar(2025))
            håndterSøknad(25.januar(2025) til 9.februar(2025))
            håndterSøknad(17.februar(2025) til 24.februar(2025))
        }
        a1 {
            håndterSøknad(17.februar(2025) til 24.februar(2025))
            assertEquals(17.februar(2025) til 24.februar(2025), inspektør.periode(2.vedtaksperiode))
            håndterInntektsmelding(listOf(16.januar(2025) til 31.januar(2025)), førsteFraværsdag = 17.februar(2025))
            // Blir strukket en måned i snuten
            assertEquals(16.januar(2025) til 24.februar(2025), inspektør.periode(2.vedtaksperiode))
        }
        a2 {
            håndterInntektsmelding(listOf(16.januar(2025) til 31.januar(2025)))
            håndterInntektsmelding(listOf(16.januar(2025) til 31.januar(2025)), førsteFraværsdag = 17.februar(2025))
            assertEquals(16.januar(2025), inspektør.skjæringstidspunkt(2.vedtaksperiode))
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(16.januar(2025), inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertVarsler(2.vedtaksperiode, RV_VV_2)
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(listOf(17.februar(2025), 16.januar(2025)), inspektør.skjæringstidspunkter(2.vedtaksperiode))
        }
        a2 {
            assertEquals(17.februar(2025), inspektør.skjæringstidspunkt(3.vedtaksperiode))
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()
        }
    }

    @Test
    fun `En kort AUU på annen arbeidsgiver på eget skjæringstidspunkt, så kommer søknad på opprinnelig arbeidsgiver som gjør at hen er på samme skjæringstidspunkt`() {
        a1 { nyttVedtak(januar) }
        a2 { håndterSøknad(10.februar til 20.februar) }
        a1 {
            forlengVedtak(februar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `når vi har ventet 3 måneder på søknad på andre arbeisgivere går vi videre med varsel`() {
        a1 { håndterSykmelding(januar) }
        (a2 og a3) {
            håndterSykmelding(januar)
            håndterSykmelding(februar)
        }
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertVenterPåSøknad(1.vedtaksperiode)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertVenterPåSøknad(1.vedtaksperiode)
        }
        (a2 og a3) {
            assertEquals(listOf(januar, februar), inspektør.sykmeldingsperioder())
        }
        a1 {
            val ikkeLengeNok = LocalDateTime.now().minusMonths(3).plusWeeks(1)
            val lengeNok = LocalDateTime.now().minusMonths(3).minusDays(1)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, tilstandsendringstidspunkt = ikkeLengeNok)
            assertVenterPåSøknad(1.vedtaksperiode)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, tilstandsendringstidspunkt = lengeNok)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(1.vedtaksperiode, RV_SY_4)
        }
        (a2 og a3) {
            assertEquals(listOf(februar), inspektør.sykmeldingsperioder())
        }
    }

    private fun TestPerson.TestArbeidsgiver.assertVenterPåSøknad(vedtaksperiodeId: UUID) {
        assertSisteTilstand(vedtaksperiodeId, AVVENTER_BLOKKERENDE_PERIODE)
        val venterPå = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == vedtaksperiodeId }.venterPå.venteårsak.hva
        assertEquals("SØKNAD", venterPå)
    }

    @Test
    fun `en gjenskapning fra virkeligheten med foreldrepenger, ferie og en spenstig IM blant annet`() {
        (a1 og a2).nyeVedtak(januar)
        (a1 og a2).forlengVedtak(februar)
        a1 { håndterOverstyrTidslinje((1..28).map { ManuellOverskrivingDag(it.februar, Dagtype.Foreldrepengerdag) }) }
        a2 { håndterOverstyrTidslinje((1..28).map { ManuellOverskrivingDag(it.februar, Dagtype.Foreldrepengerdag) }) }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), Ferie(1.mars, 31.mars))
            håndterYtelser(3.vedtaksperiode)
        }
        a2 {
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), Ferie(1.mars, 14.mars))
            håndterInntektsmelding(emptyList(), førsteFraværsdag = 15.februar, begrunnelseForReduksjonEllerIkkeUtbetalt = "fox")
            assertEquals(15.mars, inspektør.skjæringstidspunkt(3.vedtaksperiode))
            assertEquals(listOf(15.mars), inspektør.skjæringstidspunkter(3.vedtaksperiode))
            assertVarsel(Varselkode.RV_IM_8, 3.vedtaksperiode.filter())
        }
        a1 {
            håndterSøknad(april)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `Forsøker å beregne seg forbi det punktet vi venter på inntektsmelding hos annen arbeidsgiver`() {
        a1 { håndterSøknad(1.januar til 20.januar) }
        a2 {
            håndterSøknad(1.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }

        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, a3)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 3) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT)
                assertInntektsgrunnlag(a3, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a3 {
            håndterSøknad(5.januar til 10.januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a3 {
            håndterSøknad(21.januar til 21.februar)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            val venterPå = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == 1.vedtaksperiode }.venterPå
            assertEquals("a3", venterPå.yrkesaktivitetssporing.somOrganisasjonsnummer)
            assertEquals("INNTEKTSMELDING", venterPå.venteårsak.hva)
            assertNull(venterPå.venteårsak.hvorfor)
        }
    }

    @Test
    fun `forbrukte og gjenstående sykedager blir riktig også om arbeidsgiver som ikke beregner utbetalinger strekker seg lengre enn den som beregner`() {
        a1 { håndterSøknad(1.januar til 20.januar) }
        a2 { håndterSøknad(januar) }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            inspektør.sisteMaksdato(1.vedtaksperiode).also {
                assertEquals(3, it.antallForbrukteDager)
                assertEquals(245, it.gjenståendeDager)
                assertEquals(28.desember, it.maksdato)
            }
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            inspektør.sisteMaksdato(1.vedtaksperiode).also {
                assertEquals(11, it.antallForbrukteDager)
                assertEquals(237, it.gjenståendeDager)
                assertEquals(28.desember, it.maksdato)
            }
        }
    }

    @Test
    fun `En AUU som åpnes opp, men vil tilbake til AUU bør ikke trenge å vente på en eventuell overlappende søknad`() {
        a1 { håndterSykmelding(1.januar til 16.januar) }
        a2 { håndterSykmelding(januar) }

        a1 {
            håndterSøknad(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

            nullstillTilstandsendringer()
            observatør.vedtaksperiodeVenter.clear()
            håndterInntektsmelding(listOf(1.januar til 16.januar))

            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertEquals(0, observatør.vedtaksperiodeVenter.size)
        }
    }

    @Test
    fun `tre arbeidsgivere med flere perioder som overlapper`() {
        a1 {
            nyPeriode(januar)
        }
        a2 {
            nyPeriode(januar)
        }
        a3 {
            nyPeriode(2.januar til 20.januar)
            nyPeriode(21.januar til 31.januar)
        }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a3 { håndterInntektsmelding(listOf(2.januar til 17.januar)) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 3) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT)
                assertInntektsgrunnlag(a3, INNTEKT)
            }
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a3 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 { assertEquals(1, inspektør.antallUtbetalinger) }
        a2 { assertEquals(1, inspektør.antallUtbetalinger) }
        a3 { assertEquals(1, inspektør.antallUtbetalinger) }
    }

    @Test
    fun `tre arbeidsgivere med flere perioder som overlapper - første periode hos a3 skal i auu`() {
        a1 {
            nyPeriode(januar)
        }
        a2 {
            nyPeriode(januar)
        }
        a3 {
            nyPeriode(2.januar til 18.januar)
            nyPeriode(19.januar til 31.januar)
        }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a3 { håndterInntektsmelding(listOf(2.januar til 17.januar)) }
        a3 {
            håndterSøknad(Sykdom(2.januar, 18.januar, 100.prosent), Ferie(18.januar, 18.januar))
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 3) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT)
                assertInntektsgrunnlag(a3, INNTEKT)
            }
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a3 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 { assertEquals(1, inspektør.antallUtbetalinger) }
        a2 { assertEquals(1, inspektør.antallUtbetalinger) }
        a3 { assertEquals(2, inspektør.antallUtbetalinger) }
    }

    @Test
    fun `mangler refusjonsopplysninger etter overstyring av saksbehander - ikke fravær`() {
        a1 {
            nyPeriode(7.januar til 31.januar)
            nyPeriode(februar)
        }
        a2 {
            nyPeriode(februar)
        }
        a1 {
            håndterInntektsmelding(listOf(7.januar til 22.januar))
        }
        a2 {
            håndterInntektsmelding(listOf(7.januar til 22.januar), førsteFraværsdag = 1.februar)
            // denne inntektsmeldingen lagrer refusjonsopplysninger uten første fraværsdag. Uten denne IMen så er testen useless
            håndterInntektsmelding(listOf(7.januar til 22.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFravaer")
            assertVarsler(listOf(Varselkode.RV_IM_24, Varselkode.RV_IM_8), 1.vedtaksperiode.filter())
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertInntektsgrunnlag(7.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }

        a1 {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterOverstyrTidslinje((7.januar til 31.januar).map { ManuellOverskrivingDag(it, Dagtype.Arbeidsdag) })
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
    }

    @Test
    fun `mangler refusjonsopplysninger etter at skjæringstidspunktet flyttes - da gjenbruker vi tidsnære opplysninger`() {
        a1 {
            nyPeriode(3.januar til onsdag den 17.januar)
        }
        a2 {
            nyPeriode(8.januar til onsdag den 17.januar)
            nyPeriode(mandag den 22.januar til 23.januar)
        }
        a1 {
            håndterSøknad(
                Sykdom(mandag den 22.januar, 23.januar, 100.prosent),
                egenmeldinger = listOf(fredag den 19.januar til fredag den 19.januar)
            )
            håndterInntektsmelding(listOf(3.januar til 17.januar, 19.januar.somPeriode()), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertInntektsgrunnlag(19.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        a1 {
            nyPeriode(24.januar til 31.januar)
        }
        a2 {
            nyPeriode(24.januar til 31.januar)
            nullstillTilstandsendringer()
            håndterInntektsmelding(
                listOf(
                    8.januar til onsdag den 17.januar,
                    // nå blir helgen 20. januar - 21.januar tolket som frisk,
                    // og flytter i praksis skjæringstidspunktet fra 19. januar til 22. januar
                    mandag den 22.januar til 25.januar
                )
            )
            assertVarsel(RV_IM_3, 3.vedtaksperiode.filter())
        }
        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstander(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `replay av inntektsmelding medfører at to perioder går til godkjenning samtidig`() {
        a1 {
            nyPeriode(1.januar til 16.januar)
            nyPeriode(20.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 20.januar, beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
        }
        a2 {
            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 20.januar, beregnetInntekt = INNTEKT)
            nyPeriode(20.januar til 31.januar)
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }

        a1 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `out of order på ghost`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a2 {
            håndterSøknad(17.januar til 31.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a1 {
            håndterSøknad(17.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 {
            håndterSøknad(1.januar til 16.januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `Periode med snutete egenmeldinger som har sklidd gjennom på forlengelse`() {
        listOf(a1, a2).nyeVedtak(januar)
        a1 {
            håndterSykmelding(februar)
        }
        a2 {
            håndterSykmelding(februar)
            håndterSøknad(Sykdom(3.februar, 28.februar, 100.prosent), egenmeldinger = listOf(1.februar til 1.februar))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `out of order ag1 - binder sammen skjæringstidspunkt mellom ag1 og ag2 - da gjenbruker vi tidsnære opplysninger`() {
        val inntekt = 20000.månedlig
        listOf(a1).nyeVedtak(januar, inntekt = inntekt, ghosts = listOf(a2))
        a1 {
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
        }
        listOf(a1).forlengVedtak(februar)
        a2 {
            tilGodkjenning(april, beregnetInntekt = inntekt, ghosts = listOf(a1))
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
        }
        listOf(a1).forlengVedtak(mars)

        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `Beholder skatt i sykepengegrunnlag ved inntektsmelding i annen måned enn skjæringstidspunktet`() {
        val a2Inntektsmelding = UUID.randomUUID()

        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }

        a2 {
            håndterSøknad(februar)
            håndterInntektsmelding(listOf(1.februar til 16.februar), førsteFraværsdag = 1.februar, beregnetInntekt = INNTEKT, id = a2Inntektsmelding)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

            assertBeløpstidslinje(Beløpstidslinje.fra(februar, INNTEKT, a2Inntektsmelding.arbeidsgiver), inspektør(a2).refusjon(1.vedtaksperiode))

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `forskjellige arbeidsgiverperioder i kombinasjon med 6G og forskjellige refusjon`() {
        val a1Inntekt = 44000.månedlig
        val a2Inntekt = 12000.månedlig

        a1 { håndterSykmelding(10.januar til 31.januar) }
        a2 { håndterSykmelding(10.januar til 31.januar) }

        a1 {
            håndterSøknad(10.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 1.januar, 8.januar til 22.januar), beregnetInntekt = a1Inntekt)
            assertEquals(listOf(1.januar til 1.januar, 8.januar til 22.januar), inspektør.arbeidsgiverperioder { 1.vedtaksperiode })
        }

        a2 {
            håndterSøknad(10.januar til 31.januar)
            håndterInntektsmelding(listOf(8.januar til 23.januar), beregnetInntekt = a2Inntekt, refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()))
            assertEquals(listOf(8.januar til 23.januar), inspektør.arbeidsgiverperiode { 1.vedtaksperiode })
        }

        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }

        a1 {
            val `23Januar` = inspektør.utbetalingstidslinjer(1.vedtaksperiode)[23.januar]
            assertTrue(`23Januar` is NavDag)
            assertEquals(1698, `23Januar`.arbeidsgiverbeløp)
            assertEquals(0, `23Januar`.personbeløp)

            val `24Januar` = inspektør.utbetalingstidslinjer(1.vedtaksperiode)[24.januar]
            assertTrue(`24Januar` is NavDag)
            assertEquals(2031, `24Januar`.arbeidsgiverbeløp)
            assertEquals(0, `24Januar`.personbeløp)
        }

        a2 {
            val `23Januar` = inspektør.utbetalingstidslinjer(1.vedtaksperiode)[23.januar]
            assertTrue(`23Januar` is ArbeidsgiverperiodeDag)

            val `24Januar` = inspektør.utbetalingstidslinjer(1.vedtaksperiode)[24.januar]
            assertTrue(`24Januar` is NavDag)
            assertEquals(0, `24Januar`.arbeidsgiverbeløp)
            assertEquals(130, `24Januar`.personbeløp)
        }
    }

    @Test
    fun `kort sykdom hos ag2 med eksisterende vedtak`() {
        a1 { nyttVedtak(januar, 100.prosent) }
        nyPeriode(1.februar til 14.februar, a1, a2)
        a1 {
            assertEquals(1.januar, inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.skjæringstidspunkt)
            assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        }
        a2 {
            assertEquals(1.januar, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.skjæringstidspunkt)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
        a2 {
            assertFunksjonellFeil(Varselkode.RV_SV_2, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `kort sykdom hos ag2`() {
        nyPeriode(1.januar til 14.januar, a1, a2)
        a1 {
            håndterSykmelding(15.januar til 31.januar)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a1 {
            håndterSøknad(15.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)

            assertEquals(2, inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.view().inntektsgrunnlag.arbeidsgiverInntektsopplysninger.size)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
    }

    @Test
    fun `vedtaksperioder atskilt med betydelig tid`() {
        a1 {
            nyttVedtak(januar)
            assertIngenFunksjonelleFeil()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            nyttVedtak(mars)
            assertIngenFunksjonelleFeil()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Tillater førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding er på samme dato`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        a1 { håndterSykmelding(periode) }
        a2 { håndterSykmelding(periode) }

        a1 {
            håndterSøknad(periode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }

        a2 { håndterSøknad(periode) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }

        a1 {
            håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)))
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }

        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }

        a1 {
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, TIL_UTBETALING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterYtelser(1.vedtaksperiode)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
            håndterSimulering(1.vedtaksperiode)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, TIL_UTBETALING)
            håndterUtbetalt()
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
    }

    @Test
    fun `bruddstykker hos arbeidsgivere skal ikke medføre at alle perioder må ha inntekt`()  {
        a1 {
            håndterSykmelding(1.januar til 20.januar)
            håndterSykmelding(25.januar til 10.februar)
        }
        a2 {
            håndterSykmelding(januar)
            håndterSykmelding(5.februar til 15.februar)
        }

        a1 {
            håndterSøknad(1.januar til 20.januar)
        }
        a2 {
            håndterSøknad(januar)
        }
        a1 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT
            )
            håndterSøknad(25.januar til 10.februar)
            observatør.assertEtterspurt(2.vedtaksperiode, PersonObserver.Refusjon::class)
        }
        a2 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT
            )
            håndterSøknad(5.februar til 15.februar)
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }

        // sender im på mursteinspølse hos a1
        a1 {
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 2.vedtaksperiode
            )
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }

        // sender im på siste mursteinspølse på a2
        a2 {
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 2.vedtaksperiode
            )
        }

        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `Tillater førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding ikke er på samme dato - så lenge de er i samme måned`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        a1 { håndterSykmelding(periode) }
        a2 { håndterSykmelding(periode) }

        a1 {
            håndterSøknad(periode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }

        a2 {
            håndterSøknad(periode)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }

        a1 {
            håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 3.januar(2021), 6.januar(2021) til 18.januar(2021)),
                beregnetInntekt = 1000.månedlig
            )
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertInntektsgrunnlag(1.januar(2021), forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, 1000.månedlig)
            }
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `Tillater førstegangsbehandling av ag2 dersom inntektsmelding ikke er i samme måned som skjæringstidspunkt - uten skatteinntekter for ag2`() {
        val periode = 31.desember(2020) til 31.januar(2021)
        a1 { håndterSykmelding(periode) }
        a2 { håndterSykmelding(periode) }
        a1 {
            håndterSøknad(periode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a2 { håndterSøknad(periode) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a1 {
            håndterInntektsmelding(
                listOf(31.desember(2020) til 15.januar(2021))
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 3.januar(2021), 6.januar(2021) til 18.januar(2021)),
                beregnetInntekt = 1000.månedlig
            )
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)

        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }

        inspiser(personInspektør).also { inspektør ->
            val vilkårsgrunnlagInnslag = inspektør.vilkårsgrunnlagHistorikk.vilkårsgrunnlagHistorikkInnslag()
            assertEquals(1, vilkårsgrunnlagInnslag.size)
        }
    }

    @Test
    fun `Tillater førstegangsbehandling av ag2 dersom inntektsmelding ikke er i samme måned som skjæringstidspunkt - med skatteinntekter for ag2`() {
        val periode = 31.desember(2020) til 31.januar(2021)
        a1 { håndterSykmelding(periode) }
        a2 { håndterSykmelding(periode) }
        a1 {
            håndterSøknad(periode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a2 { håndterSøknad(periode) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a1 {
            håndterInntektsmelding(
                listOf(31.desember(2020) til 15.januar(2021))
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 3.januar(2021), 6.januar(2021) til 18.januar(2021)),
                beregnetInntekt = 1000.månedlig
            )
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)

        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `tillater to arbeidsgivere med korte perioder, og forlengelse av disse`() {
        val periode = 1.januar(2021) til 14.januar(2021)
        val forlengelseperiode = 15.januar(2021) til 31.januar(2021)
        nyPeriode(periode, a1, a2)
        nyPeriode(forlengelseperiode, a1, a2)
        a1 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),

                )
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),

                )
        }
        a1 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
        a2 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)

            håndterUtbetalt()
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            håndterUtbetalt()
        }
        a1 { assertSisteTilstand(2.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(2.vedtaksperiode, AVSLUTTET) }
    }

    @Test
    fun `medlemskap ikke oppfyllt i vilkårsgrunnlag, avviser perioden riktig for begge arbeidsgivere`() {
        val periode = januar
        nyPeriode(periode, a1, a2)
        a1 {
            håndterInntektsmelding(listOf(Periode(periode.start, periode.start.plusDays(15))))
        }
        a2 {
            håndterInntektsmelding(listOf(Periode(periode.start, periode.start.plusDays(15))))
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Nei
            )
            assertVarsel(Varselkode.RV_MV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
        a1 {
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }
        a2 {
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.arbeidsgiverperiodeDagTeller)
                assertEquals(4, tidslinjeInspektør.navHelgDagTeller)
                assertEquals(11, tidslinjeInspektør.avvistDagTeller)
            }
        }
    }

    @Test
    fun `To arbeidsgivere med sykdom gir ikke warning for flere inntekter de siste tre månedene`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        a1 { håndterSykmelding(periode) }
        a2 { håndterSykmelding(periode) }
        a1 {
            håndterSøknad(periode)
        }
        a2 { håndterSøknad(periode) }
        a1 { håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021))) }
        a2 { håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021))) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `to AG - to perioder på hver - siste periode på første AG til godkjenning, siste periode på andre AG avventer første AG`() {
        nyPeriode(januar, a1, a2)
        a1 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
                beregnetInntekt = 20000.månedlig,

                )
        }
        a2 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
                beregnetInntekt = 20000.månedlig,

                )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        nyPeriode(februar, a1, a2)
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
        }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertIngenFunksjonelleFeil()
            assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        a2 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING
            )
            assertIngenFunksjonelleFeil()
            assertEquals(1, inspektør.avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(0, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(0, inspektør.avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(1, inspektør.ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun `Går ikke direkte til AVVENTER_HISTORIKK dersom inntektsmelding kommer før søknad`() {
        a1 { håndterSykmelding(januar) }
        a2 { håndterSykmelding(januar) }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterSøknad(januar)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,

                )
        }
    }

    @Test
    fun `Siste arbeidsgiver som går til AVVENTER_BLOKKERENDE_PERIODE sparker første tilbake til AVVENTER_HISTORIKK når inntektsmelding kommer før søknad`() {
        a1 { håndterSykmelding(januar) }
        a2 { håndterSykmelding(januar) }
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a2 {
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,

                )
        }
        a2 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,

                )
        }
    }

    @Test
    fun `Skal ikke ha noen avviste dager ved ulik startdato selv om arbeidsgiverperiodedag og navdag overlapper og begge har sykdomsgrad på 20 prosent eller høyere`() {
        a1 { håndterSykmelding(januar) }
        a2 { håndterSykmelding(17.januar til 16.februar) }
        a1 { håndterSøknad(Sykdom(1.januar, 31.januar, 20.prosent)) }
        a2 { håndterSøknad(Sykdom(17.januar, 16.februar, 20.prosent)) }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(17.januar til 1.februar)) }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            assertEquals(0, a1.inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistDagTeller)
        }
    }

    @Test
    fun `Sykmelding og søknad kommer for to perioder før inntektsmelding kommer - skal fortsatt vilkårsprøve kun én gang`() {
        nyPeriode(1.januar til 18.januar, a1, a2)
        a1 { håndterSykmelding(20.januar til 31.januar) }
        a2 { håndterSykmelding(22.januar til 31.januar) }
        a1 { håndterSøknad(20.januar til 31.januar) }
        a2 { håndterSøknad(22.januar til 31.januar) }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 20.januar) }
        // Sender med en annen inntekt enn i forrige IM for å kunne asserte på at det er denne vi bruker
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 32000.månedlig, førsteFraværsdag = 22.januar) }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 31000.månedlig) }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(20.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
        a2 {
            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(20.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `Burde ikke håndtere sykmelding dersom vi har forkastede vedtaksperioder i andre arbeidsforhold`() {
        a1 { nyPeriode(2.januar til 1.februar) }
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
        }
        a2 {
            nyPeriode(januar)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `kastes ikke ut pga manglende inntekt etter inntektsmelding`() {
        a2 { håndterSykmelding(3.januar til 18.januar) }
        a1 { håndterSykmelding(4.januar til 18.januar) }
        a2 {
            håndterSøknad(3.januar til 18.januar)
        }
        a1 {
            håndterSøknad(4.januar til 18.januar)
            håndterSykmelding(19.januar til 22.januar)
            håndterSøknad(19.januar til 22.januar)
            håndterInntektsmelding(listOf(4.januar til 19.januar))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            assertIngenFunksjonelleFeil(2.vedtaksperiode.filter())
            assertInntektsgrunnlag(3.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INGEN, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
        }
        a2 {
            val vilkårsgrunnlag = a2.inspektør.vilkårsgrunnlag(1.vedtaksperiode)
            assertNotNull(vilkårsgrunnlag)
            assertTrue(vilkårsgrunnlag.inspektør.vurdertOk)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING) }
        a1 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING) }
    }

    @Test
    fun `går til AVVENTER_BLOKKERENDE_PERIODE ved IM dersom vi har vedtaksperioder som ikke overlapper, men har samme skjæringstidspunkt som nåværende`() {
        a1 { håndterSykmelding(januar) }
        a2 { håndterSykmelding(januar) }
        a3 { håndterSykmelding(februar) }

        a1 { håndterSøknad(januar) }
        a2 { håndterSøknad(januar) }

        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }

        a1 { assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
        a2 { assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `forlengelse av AVSLUTTET_UTEN_UTBETALING skal ikke gå til AVVENTER_HISTORIKK ved flere arbeidsgivere om IM kommer først`() {
        a1 { håndterSykmelding(1.januar til 16.januar) }
        a2 { håndterSykmelding(1.januar til 16.januar) }
        a1 { håndterSøknad(1.januar til 16.januar) }
        a2 { håndterSøknad(1.januar til 16.januar) }
        a1 { håndterSykmelding(17.januar til 31.januar) }
        a2 { håndterSykmelding(17.januar til 31.januar) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a1 { håndterSøknad(17.januar til 31.januar) }
        a2 { håndterSøknad(17.januar til 31.januar) }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVSLUTTET_UTEN_UTBETALING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVSLUTTET_UTEN_UTBETALING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVSLUTTET_UTEN_UTBETALING
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING
            )
        }
        a2 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVSLUTTET_UTEN_UTBETALING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVSLUTTET_UTEN_UTBETALING
            )
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `GjenopptaBehandling poker ikke fremtidig periode for en annen arbeidsgiver videre ved tidligere uferdige perioder`() {
        a1 {
            nyPeriode(januar)
            nyPeriode(februar)
        }
        a2 {
            nyPeriode(mai)
            håndterInntektsmelding(listOf(1.mai til 16.mai))
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 { assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
        }
        a1 { assertSisteTilstand(2.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING) }
    }

    @Test
    fun `inntektsmelding skal kun treffe sammenhengende vedtaksperioder, ikke alle med samme skjæringstidspunkt`() {
        // Vedtaksperiode for AG 1 skal bare koble sammen to vedtaksperioder for AG 2 så de får samme skjæringstidspunkt
        a1 { håndterSykmelding(januar) }
        a2 {
            håndterSykmelding(1.januar til 20.januar)
            håndterSykmelding(25.januar til 31.januar)
            håndterSøknad(1.januar til 20.januar)
            håndterSøknad(25.januar til 31.januar)
        }
        a1 { håndterSøknad(januar) }
        a2 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            observatør.assertEtterspurt(2.vedtaksperiode, PersonObserver.Refusjon::class)
        }
        a1 { assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `andre inntektskilder på a2 før vilkårsprøving - error på a1 og a2`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
        }
        a2 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = true)
        }
        a1 {
            assertTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_SØ_10, AktivitetsloggFilter.Alle)
        }
        a2 {
            assertTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertFunksjonellFeil(Varselkode.RV_SØ_10, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `andre inntektskilder på a2 etter vilkårsprøving på a1 - kun warning på a2`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterSykmelding(15.januar til 15.februar)
            håndterSøknad(Sykdom(15.januar, 15.februar, 100.prosent), andreInntektskilder = true)
        }
        a1 {
            assertTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertIngenFunksjonelleFeil()
        }
        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertIngenFunksjonelleFeil()
            assertVarsel(Varselkode.RV_SØ_10, 1.vedtaksperiode.filter())
            håndterInntektsmelding(listOf(15.januar til 30.januar))
            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
        }

        a2 {
            assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `inntektsmelding for ag2 strekker perioden tilbake til å bli først`() {
        a1 {
            nyPeriode(3.januar til 18.januar)
            nyPeriode(19.januar til 31.januar)
        }

        a2 {
            håndterInntektsmelding(listOf(1.februar til 16.februar), beregnetInntekt = INNTEKT, refusjon = Inntektsmelding.Refusjon(INNTEKT, null))
            nyPeriode(februar)
        }

        a1 {
            håndterInntektsmelding(listOf(3.januar til 18.januar))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertVarsel(RV_VV_2, 2.vedtaksperiode.filter())
        }

        a2 {
            håndterInntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 1.februar, beregnetInntekt = INNTEKT, refusjon = Inntektsmelding.Refusjon(INNTEKT, null), begrunnelseForReduksjonEllerIkkeUtbetalt = "TidligereVirksomhet")
            assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_24, 1.vedtaksperiode.filter())
            assertEquals(3.januar til 28.februar, inspektør.periode(1.vedtaksperiode))
            assertEquals(listOf(1.februar til 1.februar), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
        }

        a1 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING) }
    }

    @Test
    fun `skal ikke ha to vedtaksperioder til godkjenning samtidig`() {
        a1 {
            nyPeriode(12.januar til 16.januar)
            håndterSøknad(17.januar til 21.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 {
            nyPeriode(30.januar til 31.januar)
            //Overlappende vedtaksperiode men med senere skjæringstidspunkt enn a1
            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 30.januar)
        }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
        }
        a1 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    private val Utbetalingsdag.arbeidsgiverbeløp get() = this.økonomi.inspektør.arbeidsgiverbeløp?.dagligInt ?: 0
    private val Utbetalingsdag.personbeløp get() = this.økonomi.inspektør.personbeløp?.dagligInt ?: 0
}
