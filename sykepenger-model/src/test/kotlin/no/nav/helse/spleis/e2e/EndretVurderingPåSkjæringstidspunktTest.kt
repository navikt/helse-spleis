package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.selvstendig
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus.Ja
import no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus.Nei
import no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus.UavklartMedBrukerspørsmål
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Begrunnelse.ManglerMedlemskap
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class EndretVurderingPåSkjæringstidspunktTest : AbstractDslTest() {
    @Test
    fun `ny opptjeningsvurdering`() {
        a1 {
            nyttVedtak(januar)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            val nyOpptjeningsvurderingId = UUID.randomUUID()
            håndterEndretOpptjeningsvurdering(1.januar, nyOpptjeningsvurderingId)
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertEquals(nyOpptjeningsvurderingId, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.opptjeningsvurderingId)
        }
    }

    @Test
    fun `samme opptjeningsvurdering som allerede er lagt til grunn`() {
        a1 {
            nyttVedtak(januar)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            val opptjeningsvurderingId = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.opptjeningsvurderingId
            håndterEndretOpptjeningsvurdering(1.januar, opptjeningsvurderingId)
            assertInfo("Opptjeningsvurderingen er allerede lagt til grunn")
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `ny forsikringsvurdering`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            val nyForsikringsvurderingId = UUID.randomUUID()
            håndterEndretForsikringsvurdering(1.januar, nyForsikringsvurderingId)
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertEquals(
                nyForsikringsvurderingId,
                (inspektør.vilkårsgrunnlag(1.vedtaksperiode) as? VilkårsgrunnlagHistorikk.Grunnlagsdata)!!.forsikringsvurderingId,
            )
            assertEquals(nyForsikringsvurderingId, observatør.trengerInformasjonTilBeregningEventer.last().forsikringsvurderingId)
        }
    }

    @Test
    fun `samme forsikringsvurdering som allerede er lagt til grunn`() {
        selvstendig {
            val forsikringsvurderingId = UUID.randomUUID()
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode, forsikringsvurderingId = forsikringsvurderingId)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            val forsikringsvurderingIdFraVilkårsgrunnlag =
                (
                    inspektør.vilkårsgrunnlag(
                        1.vedtaksperiode,
                    ) as? VilkårsgrunnlagHistorikk.Grunnlagsdata
                )!!.forsikringsvurderingId!!
            assertEquals(forsikringsvurderingId, forsikringsvurderingIdFraVilkårsgrunnlag)
            håndterEndretForsikringsvurdering(1.januar, forsikringsvurderingId)

            assertInfo("Forsikringsvurderingen er allerede lagt til grunn")
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        }
    }

    @Test
    fun `saksbehandler vurderer uavklart medlemskap til ja`() =
        Toggle.MedlemskapsvurderingResultatBehov.enable {
            a1 {
                håndterSøknad(januar)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = UavklartMedBrukerspørsmål)
                håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = UavklartMedBrukerspørsmål)
                assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())
                håndterSimulering(1.vedtaksperiode)
                håndterUtbetalingsgodkjenning(1.vedtaksperiode)
                håndterUtbetalt()
                assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

                val gammelMedlemskapsvurderingId = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.medlemskapsvurderingId
                val nyMedlemskapsvurderingId = UUID.randomUUID()
                håndterEndretMedlemskapsvurdering(1.januar, nyMedlemskapsvurderingId)

                assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
                assertNotEquals(gammelMedlemskapsvurderingId, nyMedlemskapsvurderingId)
                assertEquals(nyMedlemskapsvurderingId, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.medlemskapsvurderingId)
                assertEquals(nyMedlemskapsvurderingId, observatør.trengerInformasjonTilBeregningEventer.last().medlemskapsvurderingId)

                håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = Ja)
                assertFalse(inspektør.utbetalingstidslinjer(1.vedtaksperiode).any { it is AvvistDag })
            }
        }

    @Test
    fun `saksbehandler vurderer uavklart medlemskap til nei`() =
        Toggle.MedlemskapsvurderingResultatBehov.enable {
            a1 {
                håndterSøknad(januar)
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = UavklartMedBrukerspørsmål)
                håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = UavklartMedBrukerspørsmål)
                assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())
                håndterSimulering(1.vedtaksperiode)
                håndterUtbetalingsgodkjenning(1.vedtaksperiode)
                håndterUtbetalt()
                assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)

                val nyMedlemskapsvurderingId = UUID.randomUUID()
                håndterEndretMedlemskapsvurdering(1.januar, nyMedlemskapsvurderingId)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
                assertEquals(nyMedlemskapsvurderingId, observatør.trengerInformasjonTilBeregningEventer.last().medlemskapsvurderingId)

                håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = Nei)
                assertVarsel(RV_MV_2, 1.vedtaksperiode.filter())
                assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
                val utbetalingsdager = inspektør.utbetalingstidslinjer(1.vedtaksperiode).drop(16).filterNot { it.dato.erHelg() }
                assertTrue(utbetalingsdager.isNotEmpty())
                utbetalingsdager.forEach { utbetalingsdag ->
                    assertTrue(utbetalingsdag is AvvistDag)
                    assertEquals(ManglerMedlemskap, (utbetalingsdag as AvvistDag).begrunnelser.single())
                }
                håndterSimulering(1.vedtaksperiode)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            }
        }

    @Test
    fun `samme medlemskapsvurdering som allerede er lagt til grunn`() {
        a1 {
            nyttVedtak(januar)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            val medlemskapsvurderingId = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.medlemskapsvurderingId
            håndterEndretMedlemskapsvurdering(1.januar, medlemskapsvurderingId)
            assertInfo("Medlemskapsvurderingen er allerede lagt til grunn")
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `endret medlemskapsvurdering beholder medlemskapstatus og andre vurderinger på grunnlaget`() {
        a1 {
            nyttVedtak(januar)
            val før = inspektør.vilkårsgrunnlag(1.vedtaksperiode) as VilkårsgrunnlagHistorikk.Grunnlagsdata
            val nyMedlemskapsvurderingId = UUID.randomUUID()
            håndterEndretMedlemskapsvurdering(1.januar, nyMedlemskapsvurderingId)
            val etter = inspektør.vilkårsgrunnlag(1.vedtaksperiode) as VilkårsgrunnlagHistorikk.Grunnlagsdata
            assertNotEquals(før.vilkårsgrunnlagId, etter.vilkårsgrunnlagId)
            assertEquals(nyMedlemskapsvurderingId, etter.medlemskapsvurderingId)
            assertEquals(før.medlemskapstatus, etter.medlemskapstatus)
            assertEquals(før.opptjeningsvurderingId, etter.opptjeningsvurderingId)
            assertEquals(før.forsikringsvurderingId, etter.forsikringsvurderingId)
        }
    }
}
