package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittRefusjon
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeNyArbeidsgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeUtbetaltArbeidsgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeUtbetaltArbeidsgiverperiode.Begrunnelse.ManglerOpptjening
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_25
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ArbeidsgiveropplysningerTest: AbstractDslTest() {

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
}
