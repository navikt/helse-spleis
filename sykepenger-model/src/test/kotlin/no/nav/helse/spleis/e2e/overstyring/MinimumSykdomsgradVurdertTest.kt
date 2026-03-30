package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MinimumSykdomsgradVurdertTest : AbstractDslTest() {

    @Test
    fun `Saksbehandler overstyrer avslag pga minimum sykdomsgrad`() {
        a1 {
            settOppAvslagPåMinimumSykdomsgrad()

            val avvistedager = inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager

            assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertEquals(11, avvistedager.size)
            assertTrue(avvistedager.all { it.begrunnelser == listOf(Begrunnelse.MinimumSykdomsgrad) })
            assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())

            håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = listOf(januar))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)

            assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
            assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.totalGrad == 10 })
        }
    }

    @Test
    fun `Får søknad fra ghost etter at minimum sykdomsgrad er vurdert`() {
        a1 {
            settOppAvslagPåMinimumSykdomsgrad()

            håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = listOf(januar))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(10, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].økonomi.inspektør.totalGrad)

        }
        a2 {
            nyPeriode(januar, a2, grad = 10.prosent)
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = 81000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)

            assertEquals(19, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar].økonomi.inspektør.totalGrad)

            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            assertEquals(0, inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
        }
        a2 {
            assertEquals(0, inspektør(a2).utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
            assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Vurderer samme måned til å være både ok og ikke ok`() {
        a1 {
            settOppAvslagPåMinimumSykdomsgrad()
            assertThrows<IllegalStateException> {
                håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = listOf(januar), perioderMedMinimumSykdomsgradVurdertIkkeOK = listOf(januar))
            }
        }
    }

    @Test
    fun `Saksbehandler angrer vurdering`() {
        a1 {
            settOppAvslagPåMinimumSykdomsgrad()
            assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
            assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
            håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = listOf(januar))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter())
            assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
            assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
            håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = emptyList(), perioderMedMinimumSykdomsgradVurdertIkkeOK = listOf(januar))
            håndterYtelser(1.vedtaksperiode)
            assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
            assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
        }
    }

    @Test
    fun `bare enkeltdager i vedtaksperioden er vurdert ok`() {
        a1 {
            settOppAvslagPåMinimumSykdomsgrad()
            assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
            assertEquals(0, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
            håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = listOf(1.januar til 20.januar))
            håndterYtelser(1.vedtaksperiode)
            assertEquals(8, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
            assertEquals(3, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.size)
            assertVarsel(Varselkode.RV_VV_17, 1.vedtaksperiode.filter())
        }
    }

    private fun TestPerson.TestArbeidsgiver.settOppAvslagPåMinimumSykdomsgrad() {
        nyPeriode(januar, this.orgnummer)
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = 10000.månedlig,
            vedtaksperiodeId = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            skatteinntekter = listOf(a1 to 10000.månedlig, a2 to 81000.månedlig),
        )
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())
        håndterYtelser(1.vedtaksperiode)
        assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
    }
}
