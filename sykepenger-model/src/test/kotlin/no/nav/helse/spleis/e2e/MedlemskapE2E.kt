package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus
import no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus.Ja
import no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus.Nei
import no.nav.helse.hendelser.Medlemskapsvurdering.Medlemskapstatus.UavklartMedBrukerspørsmål
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.vedtaksperiode
import no.nav.helse.utbetalingstidslinje.Begrunnelse.ManglerMedlemskap
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class MedlemskapE2E : AbstractDslTest() {
    @Test
    fun `søknad med arbeidUtenforNorge gir varsel om medlemskap`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), arbeidUtenforNorge = true)
            assertVarsel(Varselkode.RV_MV_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `søknad uten arbeidUtenforNorge gir ikke varsel om medlemskap`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), arbeidUtenforNorge = false)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `ikke medlem av folketrygden`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Nei)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(RV_MV_2, 1.vedtaksperiode.filter())
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            val forventetAvvisteDager =
                setOf(
                    17.januar,
                    18.januar,
                    19.januar,
                    22.januar,
                    23.januar,
                    24.januar,
                    25.januar,
                    26.januar,
                    29.januar,
                    30.januar,
                    31.januar,
                )
            val forventetAvvisteUtbetalingsdager =
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).drop(16).filterNot { it.dato.erHelg() }
            assertEquals(forventetAvvisteDager, forventetAvvisteUtbetalingsdager.map { it.dato }.toSet())

            forventetAvvisteUtbetalingsdager.forEach { utbetalingsdag ->
                assertTrue(utbetalingsdag is AvvistDag)
                assertEquals(ManglerMedlemskap, (utbetalingsdag as AvvistDag).begrunnelser.single())
            }
        }
    }

    @Test
    fun `medlemskapsvurderingresultat nei ved beregning avslår dager og gir varsel selv om vilkårsgrunnlaget sier ja`() =
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Ja)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = Nei)
            assertVarsel(RV_MV_2, 1.vedtaksperiode.filter())
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            val utbetalingsdager =
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).drop(16).filterNot { it.dato.erHelg() }
            assertTrue(utbetalingsdager.isNotEmpty())
            utbetalingsdager.forEach { utbetalingsdag ->
                assertTrue(utbetalingsdag is AvvistDag)
                assertEquals(ManglerMedlemskap, (utbetalingsdag as AvvistDag).begrunnelser.single())
            }
        }

    @Test
    fun `medlemskapsvurderingresultat ja ved beregning gir utbetaling og ikke varsel selv om vilkårsgrunnlaget sier nei`() =
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Nei)
            håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = Ja)

            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertFalse(inspektør.utbetalingstidslinjer(1.vedtaksperiode).any { it is AvvistDag })
        }

    @Test
    fun `medlemskapsvurderingresultat uavklart med brukerspørsmål ved beregning gir varsel`() =
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Ja)
            håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = UavklartMedBrukerspørsmål)

            assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())
            assertFalse(inspektør.utbetalingstidslinjer(1.vedtaksperiode).any { it is AvvistDag })
        }

    @Test
    fun `uten svar fra sp-medlemskap gir statusen Nei på vilkårsgrunnlaget varsel ved beregning`() =
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Nei)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)

            assertVarsel(RV_MV_2, 1.vedtaksperiode.filter())
        }

    @Test
    fun `uten svar fra sp-medlemskap gir statusen UavklartMedBrukerspørsmål på vilkårsgrunnlaget varsel ved beregning`() =
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = UavklartMedBrukerspørsmål)
            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)

            assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())
        }

    @Test
    fun `forlengelse av vedtak med uavklart medlemskap gir ikke nytt varsel`() =
        a1 {
            vedtakMedMedlemskap(UavklartMedBrukerspørsmål)
            assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())

            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode, medlemskapsvurderingResultat = UavklartMedBrukerspørsmål)

            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        }

    @Test
    fun `forlengelse av vedtak med medlemskap nei gir ikke nytt varsel`() =
        a1 {
            vedtakMedMedlemskap(Nei)
            assertVarsel(RV_MV_2, 1.vedtaksperiode.filter())

            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode, medlemskapsvurderingResultat = Nei)

            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
            assertTrue(inspektør.utbetalingstidslinjer(2.vedtaksperiode).filterNot { it.dato.erHelg() }.all { it is AvvistDag })
        }

    @Test
    fun `revurdering av vedtak med uavklart medlemskap gir ikke nytt varsel`() =
        a1 {
            vedtakMedMedlemskap(UavklartMedBrukerspørsmål)
            assertEquals(1, antallVarsler(RV_MV_1, 1.vedtaksperiode))

            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = UavklartMedBrukerspørsmål)

            assertEquals(1, antallVarsler(RV_MV_1, 1.vedtaksperiode))
            assertVarsler(listOf(RV_MV_1, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
        }

    @Test
    fun `ny medlemskapsvurdering som fortsatt er uavklart gir nytt varsel ved revurdering`() =
        a1 {
            vedtakMedMedlemskap(UavklartMedBrukerspørsmål)
            assertEquals(1, antallVarsler(RV_MV_1, 1.vedtaksperiode))

            håndterEndretMedlemskapsvurdering(1.januar, UUID.randomUUID())
            håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = UavklartMedBrukerspørsmål)

            assertEquals(2, antallVarsler(RV_MV_1, 1.vedtaksperiode))
            assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())
        }

    @Test
    fun `kopi av vilkårsgrunnlaget beholder medlemskapsvurderingId og gir ikke nytt varsel`() =
        a1 {
            vedtakMedMedlemskap(UavklartMedBrukerspørsmål)
            val før = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!

            håndterOverstyrInntekt(1.januar, 32000.månedlig)
            val etter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!
            assertNotEquals(før.vilkårsgrunnlagId, etter.vilkårsgrunnlagId)
            assertEquals(før.medlemskapsvurderingId, etter.medlemskapsvurderingId)

            håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = UavklartMedBrukerspørsmål)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(1, antallVarsler(RV_MV_1, 1.vedtaksperiode))
            assertVarsel(RV_MV_1, 1.vedtaksperiode.filter())

            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode, medlemskapsvurderingResultat = UavklartMedBrukerspørsmål)
            assertEquals(0, antallVarsler(RV_MV_1, 2.vedtaksperiode))
        }

    @Test
    fun `ny vilkårsprøving etter vedtak gir ny medlemskapsvurderingId og nytt varsel`() =
        a1 {
            vedtakMedMedlemskap(Nei)
            val før = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.medlemskapsvurderingId
            assertEquals(1, antallVarsler(RV_MV_2, 1.vedtaksperiode))

            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag)))
            håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = Nei)
            håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = Nei)

            assertNotEquals(før, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.medlemskapsvurderingId)
            assertEquals(2, antallVarsler(RV_MV_2, 1.vedtaksperiode))
            assertVarsler(listOf(RV_MV_2, Varselkode.RV_IV_7), 1.vedtaksperiode.filter())
        }

    private fun no.nav.helse.dsl.TestPerson.TestArbeidsgiver.vedtakMedMedlemskap(medlemskapstatus: Medlemskapstatus) {
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapstatus = medlemskapstatus)
        håndterYtelser(1.vedtaksperiode, medlemskapsvurderingResultat = medlemskapstatus)
        if (medlemskapstatus != Nei) håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        if (medlemskapstatus != Nei) håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
    }

    private fun antallVarsler(
        kode: Varselkode,
        vedtaksperiodeId: UUID,
    ) = testperson.personlogg.varsel.count { varsel ->
        varsel.kode == kode && varsel.kontekster.any { vedtaksperiode(vedtaksperiodeId).filtrer(it) }
    }
}
