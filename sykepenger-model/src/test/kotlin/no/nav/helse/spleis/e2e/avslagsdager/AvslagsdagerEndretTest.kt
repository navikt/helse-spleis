package no.nav.helse.spleis.e2e.avslagsdager

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Avslagsdager
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Avslagstidslinje
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class AvslagsdagerEndretTest : AbstractDslTest() {
    @Test
    fun `avslag etter utbetaling gir revurdering`() {
        val vurdering = UUID.randomUUID()
        val avslått = 22.januar til 26.januar
        a1 {
            nyttVedtak(januar)
            håndterAvslagsdagerEndret(avslått.start)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

            håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, avslått))
            håndterSimulering(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertAvslåttForOppholdskrav(1.vedtaksperiode, avslått)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertEquals(setOf(vurdering), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagsdagerVurderingIder)
        }
    }

    @Test
    fun `omgjøring etter utbetaling fjerner avslaget`() {
        val vurdering1 = UUID.randomUUID()
        val vurdering2 = UUID.randomUUID()
        val avslått = 22.januar til 26.januar
        a1 {
            nyttVedtak(januar)
            håndterAvslagsdagerEndret(avslått.start)
            håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering1, avslått))
            håndterSimulering(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

            håndterAvslagsdagerEndret(avslått.start)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(1.vedtaksperiode, avslagsdager = Avslagsdager(setOf(vurdering2), emptyList()))
            håndterSimulering(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertEquals(Avslagstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagstidslinje)
            assertEquals(emptyList<LocalDate>(), inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedatoer)
            assertEquals(setOf(vurdering2), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagsdagerVurderingIder)
        }
    }

    @Test
    fun `revurdering fra første avslagsdag tar med senere perioder`() {
        val vurdering = UUID.randomUUID()
        a1 {
            nyttVedtak(januar)
            nyttVedtak(mars)
            håndterAvslagsdagerEndret(22.januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)

            håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, 22.januar til 23.januar))
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertAvslåttForOppholdskrav(1.vedtaksperiode, 22.januar til 23.januar)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())

            håndterYtelser(2.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, 22.mars til 23.mars))
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertAvslåttForOppholdskrav(2.vedtaksperiode, 22.mars til 23.mars)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `to arbeidsgivere får begge dagene avslått i revurderingen`() {
        val vurdering = UUID.randomUUID()
        val avslått = 22.januar til 26.januar
        listOf(a1, a2).nyeVedtak(januar)
        a1 {
            håndterAvslagsdagerEndret(avslått.start)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, avslått))
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertAvslåttForOppholdskrav(1.vedtaksperiode, avslått)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
        }
        a2 {
            assertAvslåttForOppholdskrav(1.vedtaksperiode, avslått)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertEquals(setOf(vurdering), inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.avslagsdagerVurderingIder)
        }
    }

    @Test
    fun `periode som ikke er beregnet går videre som vanlig og får avslaget når den beregnes`() {
        val vurdering = UUID.randomUUID()
        val avslått = 22.januar til 26.januar
        a1 {
            håndterSøknad(januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterAvslagsdagerEndret(avslått.start)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            håndterYtelser(1.vedtaksperiode, avslagsdager = oppholdskravIkkeOppfylt(vurdering, avslått))
            håndterSimulering(1.vedtaksperiode)
            assertAvslåttForOppholdskrav(1.vedtaksperiode, avslått)
        }
    }

    private fun TestPerson.TestArbeidsgiver.assertAvslåttForOppholdskrav(
        vedtaksperiodeId: UUID,
        periode: Periode,
    ) {
        assertEquals(
            Avslagstidslinje(periode to Avslagstidslinje.Avslagsdag(listOf(Begrunnelse.Oppholdskrav), Avslagsdager.KILDE)),
            inspektør.vedtaksperioder(vedtaksperiodeId).inspektør.avslagstidslinje,
        )
        with(inspektør.utbetalingstidslinjer(vedtaksperiodeId).inspektør) {
            assertEquals(periode.toList(), avvistedatoer)
            periode.forEach { assertEquals(listOf(Begrunnelse.Oppholdskrav), begrunnelse(it)) }
        }
    }

    private fun oppholdskravIkkeOppfylt(
        vurderingId: UUID,
        periode: Periode,
    ) = Avslagsdager(setOf(vurderingId), periode.map { Avslagsdager.Dag(vurderingId, it, Begrunnelse.Oppholdskrav) })
}
