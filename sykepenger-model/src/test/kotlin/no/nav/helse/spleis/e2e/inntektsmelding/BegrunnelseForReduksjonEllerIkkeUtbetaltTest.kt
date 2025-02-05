package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BegrunnelseForReduksjonEllerIkkeUtbetaltTest : AbstractDslTest() {

    @Test
    fun `arbeidsgiverperioden strekker seg over to perioder og inntektsmelding kommer etter søknadene`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            håndterSøknad(Sykdom(11.januar, 17.januar, 100.prosent))
            assertEquals("SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
            assertEquals(listOf(1.januar til 10.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(listOf(11.januar til 16.januar), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
            assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_8, 2.vedtaksperiode.filter())
            assertEquals("SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        }
    }

    @Test
    fun `arbeidsgiverperioden strekker seg over to perioder og inntektsmelding kommer før siste søknad`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            assertEquals("SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
            assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
            assertEquals(listOf(1.januar til 10.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals("SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            håndterSøknad(Sykdom(11.januar, 17.januar, 100.prosent))
            assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
            assertForventetFeil(
                forklaring = "Når IM skal lage SykNav-dager kommer før vi har mottatt søknad blir det ikke lagt inn som SykNav når søknaden kommer",
                nå = {
                    assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
                },
                ønsket = {
                    assertEquals(listOf(11.januar til 16.januar), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
                }
            )
        }
    }

    @Test
    fun `Vedtaksperiode blir strukket med UkjentDag`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 25.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFullStillingsandel")
            assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
            assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
            assertEquals(25.januar, inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().sykdomstidslinje.inspektør.førsteIkkeUkjenteDag)
            (25.januar til 31.januar).let { periode ->
                assertEquals(periode, inspektør.periode(2.vedtaksperiode))
                assertEquals(periode, inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().sykdomstidslinje.periode())
            }
        }
    }

    @Test
    fun `Varsel havner på feil periode når første fraværsdag er i forlengelsen`() {
        a1 {
            nyPeriode(1.januar til 17.januar)
            nyPeriode(18.januar til 31.januar)
            håndterInntektsmelding(emptyList(), begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening", førsteFraværsdag = 19.januar)
            assertEquals(listOf<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(listOf(19.januar.somPeriode()), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_8, 2.vedtaksperiode.filter())
        }
    }
}
