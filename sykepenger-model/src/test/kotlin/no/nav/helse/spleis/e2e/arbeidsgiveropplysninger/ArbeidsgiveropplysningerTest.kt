package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Arbeidsgiveropplysning.Begrunnelse.ManglerOpptjening
import no.nav.helse.hendelser.Arbeidsgiveropplysning.Begrunnelse.StreikEllerLockout
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeNyArbeidsgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeUtbetaltArbeidsgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittArbeidgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittRefusjon
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OpphørAvNaturalytelser
import no.nav.helse.hendelser.Arbeidsgiveropplysning.RedusertUtbetaltBeløpIArbeidsgiverperioden
import no.nav.helse.hendelser.Arbeidsgiveropplysning.UtbetaltDelerAvArbeidsgiverperioden
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_25
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.beløpstidslinje
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsgiveropplysningerTest : AbstractDslTest() {

    @Test
    fun `oppgir inntekt når vi allerede har skatt i inntektsgrunnlaget - syk fra ghost samme måned`() {
        listOf(a1).nyeVedtak(januar, inntekt = 20_000.månedlig, ghosts = listOf(a2))
        a1 {
            assertVarsler(1.vedtaksperiode, RV_VV_2)
        }
        a2 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(25_000.månedlig), OppgittRefusjon(25_000.månedlig, emptyList()))
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            inspektør.inntekt(1.vedtaksperiode).let {
                assertTrue(it is Inntektsmeldinginntekt)
                assertEquals(25_000.månedlig, it.beløp)
            }
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, 25_000.månedlig), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
        }
    }

    @Test
    fun `oppgir inntekt når vi allerede har skatt i inntektsgrunnlaget - syk fra ghost annen måned`() {
        listOf(a1).nyeVedtak(januar, inntekt = 20_000.månedlig, ghosts = listOf(a2))
        a1 {
            assertVarsler(1.vedtaksperiode, RV_VV_2)
        }
        a2 {
            håndterSøknad(februar)
            assertTrue(inspektør.inntekt(1.januar) is SkattSykepengegrunnlag)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(25_000.månedlig), OppgittRefusjon(25_000.månedlig, emptyList()))
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            inspektør.inntekt(1.vedtaksperiode).let {
                assertTrue(it is Inntektsmeldinginntekt)
                assertEquals(25_000.månedlig, it.beløp)
            }
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(februar, 25_000.månedlig), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
        }
    }

    @Test
    fun `oppgir at det ikke er noen ny arbeidsgiverperiode på lang periode`() {
        a1 {
            håndterSøknad(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeNyArbeidsgiverperiode)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_25), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at det ikke er noen ny arbeidsgiverperiode på kort periode`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 18.januar, 50.prosent), egenmeldinger = listOf(1.januar til 2.januar))
            assertEquals("SSSHH SSSSSHH SSSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeNyArbeidsgiverperiode)
            assertEquals("NSSHH SSSSSHH SSSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals(50.prosent, (inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje[3.januar] as Dag.SykedagNav).økonomi.grad)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_25), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de ikke har utbetalt arbeidsgiverperioden på lang periode`() {
        a1 {
            håndterSøknad(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeUtbetaltArbeidsgiverperiode(ManglerOpptjening))
            assertEquals("NNNNNHH NNNNNHH NNSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de ikke har utbetalt arbeidsgiverperioden på kort periode`() {
        a1 {
            håndterSøknad(Sykdom(3.januar, 18.januar, 69.prosent), egenmeldinger = listOf(1.januar til 2.januar))
            assertEquals("SSSHH SSSSSHH SSSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeUtbetaltArbeidsgiverperiode(ManglerOpptjening))
            assertEquals("NNNHH NNNNNHH NNNN", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertTrue(inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.filterIsInstance<Dag.SykedagNav>().all { it.økonomi.grad == 69.prosent })
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de ikke har utbetalt arbeidsgiverperioden når arbeidsgiverperiode er i flere perioder`() {
        a1 {
            håndterSøknad(1.januar til 10.januar)
            håndterSøknad(11.januar til 31.januar)
            assertEquals("SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals("SSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(2.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeUtbetaltArbeidsgiverperiode(ManglerOpptjening))
            assertEquals("SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals("NNHH NNSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de ikke har utbetalt arbeidsgiverperioden når arbeidsgiverperiode i sin helhet ligger i perioden før`() {
        a1 {
            håndterSøknad(1.januar til 16.januar)
            håndterSøknad(17.januar til 31.januar)
            assertEquals("SSSSSHH SSSSSHH SS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals("SSSHH SSSSSHH SSS", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(2.vedtaksperiode, OppgittInntekt(INNTEKT), OppgittRefusjon(INNTEKT, emptyList()), IkkeUtbetaltArbeidsgiverperiode(ManglerOpptjening))
            assertEquals("SSSSSHH SSSSSHH SS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals("SSSHH SSSSSHH SSS", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de har utbetalt redusert beløp i hullete arbeidsgiverperiode`() {
        a1 {
            håndterSøknad(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, emptyList()),
                OppgittArbeidgiverperiode(listOf(1.januar til 6.januar, 10.januar til 15.januar, 20.januar til 23.januar)),
                RedusertUtbetaltBeløpIArbeidsgiverperioden(ManglerOpptjening)
            )
            assertEquals("NNNNNHR AANNNHH NAAAAHH NNSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertEquals(20.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(listOf(20.januar, 10.januar, 1.januar), inspektør.skjæringstidspunkter(1.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at de kun har utbetalt deler av hullete arbeidsgiverperiode`() {
        a1 {
            håndterSøknad(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, emptyList()),
                OppgittArbeidgiverperiode(listOf(1.januar til 6.januar, 10.januar til 15.januar)),
                UtbetaltDelerAvArbeidsgiverperioden(ManglerOpptjening, 15.januar)
            )
            assertEquals("SSSSSHR AASSSHH SNNNNHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir at det er opphør av naturalytelser`() = Toggle.OpphørAvNaturalytelser.enable {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, emptyList()),
                OppgittArbeidgiverperiode(listOf(1.januar til 16.januar)),
                OpphørAvNaturalytelser,
            )
            if (Toggle.OpphørAvNaturalytelser.enabled) {
                assertVarsel(Varselkode.RV_IM_7, 1.vedtaksperiode.filter())
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            } else {
                assertFunksjonellFeil(Varselkode.RV_IM_7, 1.vedtaksperiode.filter())
                assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            }
        }
    }

    @Test
    fun `oppgir at de har utbetalt redusert beløp med arbeidsgiverperiode flyttet litt frem`() {
        a1 {
            håndterSøknad(januar)
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            håndterArbeidsgiveropplysninger(
                1.vedtaksperiode,
                OppgittInntekt(INNTEKT),
                OppgittRefusjon(INNTEKT, emptyList()),
                OppgittArbeidgiverperiode(listOf(5.januar til 20.januar)),
                RedusertUtbetaltBeløpIArbeidsgiverperioden(ManglerOpptjening)
            )
            assertEquals("AAAANHH NNNNNHH NNNNNHH SSSSSHH SSS", inspektør.vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `oppgir en begrunnelse vi ikke støtter`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(1.vedtaksperiode, IkkeUtbetaltArbeidsgiverperiode(StreikEllerLockout))
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertFunksjonellFeil(RV_IM_8, 1.vedtaksperiode.filter())
        }
    }
}
