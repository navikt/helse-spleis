package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittRefusjon
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeNyArbeidsgiverperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_25
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
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
}
