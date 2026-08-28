package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BegrunnelseForReduksjonEllerIkkeUtbetaltTest : AbstractDslTest() {

    @Test
    fun `AI fjerner gammel IM - En miks av inntektsmeldinger med og uten begrunnelseForReduksjonEllerIkkeUtbetalt`() {
        a1 {
            val agp = 1.januar til 16.januar
            håndterSøknad(agp)
            håndterSøknad(20.januar til 31.januar)
            nullstillTilstandsendringer()
            håndterArbeidsgiveropplysninger(listOf(agp), begrunnelseForReduksjonEllerIkkeUtbetalt = "LovligFravaer", vedtaksperiodeId = 2.vedtaksperiode)
            assertEquals(listOf(agp), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
            håndterKorrigerteArbeidsgiveropplysninger(listOf(agp))
            assertEquals(listOf(agp), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsel(Varselkode.RV_IM_4, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - arbeidsgiver betviler arbeidsuførhet i korrigert inntektsmelding, forkaster periode`() {
        a1 {
            tilGodkjenning(januar)
            håndterKorrigerteArbeidsgiveropplysninger(listOf(1.januar til 16.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "BetvilerArbeidsufoerhet")

            assertEquals(emptyList<Periode>(), inspektør.dagerNavOvertarAnsvar(1.vedtaksperiode))
            assertVarsler(listOf(Varselkode.RV_IM_4, RV_IM_8), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `arbeidsgiverperioden strekker seg over to perioder og inntektsmelding kommer før siste søknad`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            assertEquals("SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
            håndterSelvbestemtArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
            assertVarsel(Varselkode.RV_AO_3, 1.vedtaksperiode.filter())
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
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
    fun `AI fjerner gammel IM - Vedtaksperiode blir strukket med UkjentDag`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterSøknad(Sykdom(25.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "IkkeFullStillingsandel")
            assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(25.januar, inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().sykdomstidslinje.inspektør.førsteIkkeUkjenteDag)
            (25.januar til 31.januar).let { periode ->
                assertEquals(periode, inspektør.periode(2.vedtaksperiode))
                assertEquals(periode, inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().sykdomstidslinje.periode())
            }
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `AI fjerner gammel IM - Varsel havner på feil periode når første fraværsdag er i forlengelsen`() {
        a1 {
            nyPeriode(1.januar til 17.januar)
            nyPeriode(18.januar til 31.januar)
            håndterArbeidsgiveropplysninger(emptyList(), begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening", vedtaksperiodeId = 1.vedtaksperiode)
            assertEquals(listOf(1.januar til 16.januar), inspektør.vedtaksperioder(1.vedtaksperiode).dagerNavOvertarAnsvar)
            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(2.vedtaksperiode).dagerNavOvertarAnsvar)
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
            assertVarsel(RV_IM_8, 1.vedtaksperiode.filter())
        }
    }
}
