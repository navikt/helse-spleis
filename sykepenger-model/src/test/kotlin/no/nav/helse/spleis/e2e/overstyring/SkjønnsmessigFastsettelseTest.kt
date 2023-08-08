package no.nav.helse.spleis.e2e.overstyring

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.Toggle.Companion.disable
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SKJØNNSMESSIG_FASTSETTELSE
import no.nav.helse.person.TilstandType.AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_2
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import no.nav.helse.person.inntekt.Sykepengegrunnlag.AvventerFastsettelseEtterSkjønn
import no.nav.helse.person.inntekt.Sykepengegrunnlag.FastsattEtterHovedregel
import no.nav.helse.person.inntekt.Sykepengegrunnlag.FastsattEtterSkjønn
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SkjønnsmessigFastsettelseTest: AbstractDslTest() {

    @Test
    fun `korrigere inntekten på noe som allerede har blitt skjønnsmessig fastsatt`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertEquals(FastsattEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))
        inspektør.inntektsopplysningISykepengegrunnlaget(1.januar).let {
            assertTrue(it is SkjønnsmessigFastsatt)
            assertEquals(INNTEKT * 2, it.inspektør.beløp)
        }
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 3)))
        inspektør.inntektsopplysningISykepengegrunnlaget(1.januar).let {
            assertTrue(it is SkjønnsmessigFastsatt)
            assertEquals(INNTEKT * 3, it.inspektør.beløp)
        }
    }

    @Test
    fun `skjønnsmessig fastsatt inntekt skal ikke ha avviksvurdering`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        val sykepengegrunnlag = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.sykepengegrunnlag.inspektør
        val inntektsopplysning = inspektør.inntektsopplysningISykepengegrunnlaget(1.januar)
        assertTrue(inntektsopplysning is SkjønnsmessigFastsatt)
        assertEquals(0, sykepengegrunnlag.avviksprosent)
        assertEquals(INNTEKT * 2, sykepengegrunnlag.beregningsgrunnlag)
        assertEquals(INNTEKT, sykepengegrunnlag.omregnetÅrsinntekt)
    }

    @Test
    fun `alle inntektene må skjønnsfastsettes ved overstyring`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        a1 {
            assertThrows<IllegalStateException> {
                håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
            }
        }
    }

    @Test
    fun `saksbehandler-inntekt overstyres av en skjønnsmessig med samme beløp`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2, forklaring = "forklaring")))
        assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is Saksbehandler)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        assertEquals(3, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is SkjønnsmessigFastsatt)
    }

    @Test
    fun `skjønnsmessig fastsettelse overstyres av en skjønnsmessig med samme beløp`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
    }

    @Test
    fun `skjønnsmessig fastsettelse overstyres av en inntektmelding med samme beløp`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2)))
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT * 2)
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is SkjønnsmessigFastsatt)
    }

    @Test
    fun `korrigert IM etter skjønnsfastsettelse på flere AG`() {
        (a1 og a2 og a3).nyeVedtak(1.januar til 31.januar)
        håndterOverstyrArbeidsgiveropplysninger(
            1.januar,
            listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a3, inntekt = INNTEKT * 3, forklaring = "ogga bogga"))
        )
        a3 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a3) is Saksbehandler) }
        håndterSkjønnsmessigFastsettelse(
            1.januar,
            listOf(
                OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2),
                OverstyrtArbeidsgiveropplysning(orgnummer = a2, inntekt = INNTEKT * 2),
                OverstyrtArbeidsgiveropplysning(orgnummer = a3, inntekt = INNTEKT * 2)
            )
        )
        a1 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a1) is SkjønnsmessigFastsatt) }
        a2 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a2) is SkjønnsmessigFastsatt) }
        a3 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a3) is SkjønnsmessigFastsatt) }

        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT) }

        a1 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a1) is Inntektsmelding) }
        a2 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a2) is Inntektsmelding) }
        a3 { assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar, a3) is Saksbehandler) }
    }

    @Test
    fun `skjønnsmessig fastsettelse overstyres av en inntektmelding med ulikt beløp`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSkjønnsmessigFastsettelse(
            1.januar,
            listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = INNTEKT * 2))
        )
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT * 3)
        assertEquals(3, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is Inntektsmelding)
    }

    @Test
    fun `førstegangsbehandling med mer enn 25% avvik`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            nyPeriode(1.januar til 31.januar, a1)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 2)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt = INNTEKT)
        }
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE)

    }

    @Test
    fun `endring til avvik`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            nullstillTilstandsendringer()
            håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT * 2)
            assertVarsel(RV_IV_2)
            assertEquals(AvventerFastsettelseEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))
            assertTilstander(
                1.vedtaksperiode,
                AVVENTER_GODKJENNING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_SKJØNNSMESSIG_FASTSETTELSE
            )
        }
    }

    @Test
    fun `endring til avvik før vi støtter skjønnsmessig fastsettelse`() {
        a1 {
            tilGodkjenning(1.januar, 31.januar)
            nullstillTilstandsendringer()
            håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT * 2)
            assertVarsel(RV_IV_2)
            assertForkastetPeriodeTilstander(
                1.vedtaksperiode,
                AVVENTER_GODKJENNING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_SKJØNNSMESSIG_FASTSETTELSE,
                TIL_INFOTRYGD
            )
        }
    }

    @Test
    fun `avvik i utgangspunktet - men så overstyres inntekt`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            nyPeriode(1.januar til 31.januar, a1)
            håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT * 2)
            håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt = INNTEKT)
            assertVarsel(RV_IV_2)
            assertEquals(AvventerFastsettelseEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE)
            assertEquals(100, inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.sykepengegrunnlag.inspektør.avviksprosent)

            håndterOverstyrArbeidsgiveropplysninger(
                1.januar, listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        forklaring = "forklaring",
                        refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT))
                    )
                )
            )
            assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is Saksbehandler)
            assertEquals(0, inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.sykepengegrunnlag.inspektør.avviksprosent)
            assertEquals(FastsattEtterHovedregel, inspektør.tilstandPåSykepengegrunnlag(1.januar))
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `skjønnsmessig fastsatt - men så skulle det være etter hovedregel`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 2)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertEquals(AvventerFastsettelseEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))
            assertEquals(100, inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.sykepengegrunnlag.inspektør.avviksprosent)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE)

            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(
                orgnummer = a1,
                inntekt = INNTEKT * 2
            )))

            assertEquals(FastsattEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))
            assertEquals(100, inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.sykepengegrunnlag.inspektør.avviksprosent)

            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(
                orgnummer = a1,
                inntekt = INNTEKT,
                forklaring = "forklaring"
            )))

            assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is Saksbehandler)
            assertEquals(0, inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.sykepengegrunnlag.inspektør.avviksprosent)
            assertEquals(FastsattEtterHovedregel, inspektør.tilstandPåSykepengegrunnlag(1.januar))
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
        }
    }

    @Test
    fun `revurdering med avvik går gjennom AvventerSkjønnsmessigFastsettelse`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            nullstillTilstandsendringer()
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 2)
            assertEquals(AvventerFastsettelseEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
            )
            assertVarsel(RV_IV_2)
        }
    }

    @Test
    fun `Tidligere perioder revurderes mens nyere skjønnsmessig fastsettes`() = Toggle.TjuefemprosentAvvik.enable {
        a1 {
            nyttVedtak(1.januar, 31.januar)
            nyPeriode(1.mars til 31.mars, a1)
            håndterInntektsmelding(listOf(1.mars til 16.mars), beregnetInntekt = INNTEKT * 2)
            håndterVilkårsgrunnlag(2.vedtaksperiode, inntekt = INNTEKT)
            nullstillTilstandsendringer()
            håndterOverstyrTidslinje(
                listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag, 100))
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(2.vedtaksperiode, AVVENTER_SKJØNNSMESSIG_FASTSETTELSE)
        }

    }

    @Test
    fun `Overstyre refusjon etter skjønnsmessig fastasatt`() {
        val inntektsmeldingInntekt = INNTEKT
        val skjønnsfastsattInntekt = INNTEKT * 2

        a1 {
            // Normal behandling med Inntektsmelding
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntektsmeldingInntekt, refusjon = Refusjon(inntektsmeldingInntekt, null, emptyList()))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is Inntektsmelding)
            assertEquals(listOf(Refusjonsopplysning(inntektsmeldingId, 1.januar, null, inntektsmeldingInntekt)), inspektør.refusjonsopplysningerFraVilkårsgrunnlag().inspektør.refusjonsopplysninger)
            assertEquals(FastsattEtterHovedregel, inspektør.tilstandPåSykepengegrunnlag(1.januar))


            // Saksbehandler skjønnsmessig fastsetter
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = skjønnsfastsattInntekt, refusjonsopplysninger = emptyList())))
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertEquals(0, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.sykepengegrunnlag.inspektør.avviksprosent)
            assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is SkjønnsmessigFastsatt)
            assertEquals(listOf(Refusjonsopplysning(inntektsmeldingId, 1.januar, null, inntektsmeldingInntekt)), inspektør.refusjonsopplysningerFraVilkårsgrunnlag().inspektør.refusjonsopplysninger)
            assertEquals(FastsattEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))


            // Saksbehandler endrer kun refusjon, men beholder inntekt
            val overstyrInntektOgRefusjonId = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = a1, inntekt = skjønnsfastsattInntekt, forklaring = "forklaring", refusjonsopplysninger = listOf(Triple(1.januar, null, skjønnsfastsattInntekt)))), hendelseId = overstyrInntektOgRefusjonId)
            assertEquals(3, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertTrue(inspektør.inntektsopplysningISykepengegrunnlaget(1.januar) is SkjønnsmessigFastsatt)
            assertEquals(listOf(Refusjonsopplysning(overstyrInntektOgRefusjonId, 1.januar, null, skjønnsfastsattInntekt)), inspektør.refusjonsopplysningerFraVilkårsgrunnlag().inspektør.refusjonsopplysninger)
            assertEquals(FastsattEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))
        }
    }

    @Test
    fun `Hindrer tilstandsendring hvis avvikssak som trenger fastsettelse ved skjønn godkjennes`() = Toggle.TjuefemprosentAvvik.disable {
        a1 {
            nyttVedtak(1.januar, 31.januar, beregnetInntekt = 20000.månedlig)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 9000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertVarsel(RV_IV_2, 1.vedtaksperiode.filter())
            assertEquals(AvventerFastsettelseEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))
            assertThrows<IllegalStateException> { håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true) }
        }
    }

    @Test
    fun `Hindrer tilstandsendring hvis avvikssak som trenger fastsettelse ved skjønn godkjennes - med senere uferdige periode`() = Toggle.TjuefemprosentAvvik.disable {
        nyttVedtak(1.januar, 31.januar, beregnetInntekt = 20000.månedlig)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Arbeid(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 9000.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertEquals(AvventerFastsettelseEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))

        assertVarsel(RV_IV_2, 1.vedtaksperiode.filter())
        assertThrows<IllegalStateException> { håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true) }
    }


    @Test
    fun `Hindrer tilstandsendring hvis avvikssak som trenger fastsettelse ved skjønn godkjennes - med senere uferdige periode med toggle disabled`() = listOf(Toggle.TjuefemprosentAvvik, Toggle.RevurdereAgpFraIm).disable  {
        nyttVedtak(1.januar, 31.januar, beregnetInntekt = 20000.månedlig)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Arbeid(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 9000.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertEquals(AvventerFastsettelseEtterSkjønn, inspektør.tilstandPåSykepengegrunnlag(1.januar))

        assertVarsel(RV_IV_2, 1.vedtaksperiode.filter())
        assertIngenVarsel(RV_IV_2, 3.vedtaksperiode.filter())
        assertThrows<IllegalStateException> { håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = true) }
    }

    private fun TestArbeidsgiverInspektør.inntektsopplysningISykepengegrunnlaget(skjæringstidspunkt: LocalDate, orgnr: String = a1) =
        vilkårsgrunnlag(skjæringstidspunkt)!!.inspektør.sykepengegrunnlag.inspektør.arbeidsgiverInntektsopplysninger.single { it.gjelder(orgnr) }.inspektør.inntektsopplysning
    private fun TestArbeidsgiverInspektør.tilstandPåSykepengegrunnlag(skjæringstidspunkt: LocalDate) =
        vilkårsgrunnlag(skjæringstidspunkt)!!.inspektør.sykepengegrunnlag.inspektør.tilstand
}