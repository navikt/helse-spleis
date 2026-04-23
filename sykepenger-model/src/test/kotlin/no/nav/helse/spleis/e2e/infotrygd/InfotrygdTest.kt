package no.nav.helse.spleis.e2e.infotrygd

import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Behovsoppsamler
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.EventSubscription.VedtaksperiodeVenterEvent
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_14
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_37
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.saksbehandler
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.enesteGodkjenningsbehovSomFølgeAv
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InfotrygdTest : AbstractDslTest() {

    @Test
    fun `Saksbehandler legger til en utbetaling i forkant av en periode som er lagt til grunn ved infotrygdovergang`() {
        medJSONPerson("/personer/infotrygdforlengelse.json", 334)
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            assertEquals(emptyList<VedtaksperiodeVenterEvent>(), observatør.vedtaksperiodeVenter)
            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            val refusjonFør = inspektør.refusjon(1.vedtaksperiode)

            val eksisterendeUtbetaling = ArbeidsgiverUtbetalingsperiode("a1", 1.januar, 31.januar)
            val nyUtbetaling = ArbeidsgiverUtbetalingsperiode("a1", 20.desember(2017), 31.desember(2017))
            håndterUtbetalingshistorikkEtterInfotrygdendring(eksisterendeUtbetaling, nyUtbetaling)

            assertEquals(20.desember(2017), inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertEquals("VILKÅRSPRØVING", observatør.vedtaksperiodeVenter.single().venterPå.venteårsak.hva)
            nullstillTilstandsendringer()

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
            assertVarsler(listOf(RV_IT_14, RV_IV_10), 1.vedtaksperiode.filter())
            assertEquals(refusjonFør, inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `Legger på en tag når perioden til godkjenning overlapper med en periode i Infotrygd`() {
        a1 {
            nyttVedtak(januar)
            assertEquals(1, observatør.utkastTilVedtakEventer.size)
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            håndterYtelser(1.vedtaksperiode)
            val behov1 = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterSimulering(1.vedtaksperiode)
            }
            assertTrue("OverlapperMedInfotrygd" in behov1.event.tags)

            assertVarsler(listOf(RV_IT_3), 1.vedtaksperiode.filter())
            assertEquals(2, observatør.utkastTilVedtakEventer.size)


            håndterUtbetalingshistorikkEtterInfotrygdendring()
            val behov2 = enesteGodkjenningsbehovSomFølgeAv({1.vedtaksperiode}) {
                håndterYtelser(1.vedtaksperiode)
            }
            assertFalse("OverlapperMedInfotrygd" in behov2.event.tags)
        }
    }

    @Test
    fun `Arbeidsgiverperiode utført i Infotrygd med kort gap til periode i Spleis som utbetales i Infotrygd mens den står til godkjenning`() {
        a1 {
            nyttVedtak(10.februar til 28.februar)
            val februarKorrelasjonsId = gjeldendeKorrelasjonsId(1.vedtaksperiode)
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 10.februar, listOf(10.februar til 25.februar))

            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 10.februar, emptyList())

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(februarKorrelasjonsId, gjeldendeKorrelasjonsId(1.vedtaksperiode))
            assertVarsler(listOf(RV_IT_37), 1.vedtaksperiode.filter())

            håndterSøknad(10.mars til 31.mars)
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = emptyList(),
                beregnetInntekt = INNTEKT,
                vedtaksperiodeId = 2.vedtaksperiode,
                refusjon = Inntektsmelding.Refusjon(INNTEKT, null)
            )
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 10.mars, emptyList())
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)

            // Mens Mars står til godkjenning utbetales den i Infotrygd
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar), ArbeidsgiverUtbetalingsperiode(a1, 10.mars, 31.mars))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 10.mars, emptyList())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertNotEquals(februarKorrelasjonsId, gjeldendeKorrelasjonsId(2.vedtaksperiode))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
            assertVarsler(listOf(RV_IT_3), 2.vedtaksperiode.filter())

            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            håndterYtelser(2.vedtaksperiode)
        }
    }

    @Test
    fun `Når perioden utbetales i Infotrygd kan det medføre at vi feilaktig annullerer tidligere utebetalte perioder`() {
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            nyttVedtak(mars)
            nyttVedtak(mai)
            val korrelasjonsIdMars = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId

            håndterSøknad(juli)
            håndterArbeidsgiveropplysninger(listOf(1.juli til 16.juli), vedtaksperiodeId = 3.vedtaksperiode)
            håndterVilkårsgrunnlag(3.vedtaksperiode)

            assertEquals(listOf(1.juli til 16.juli), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.dagerUtenNavAnsvar)

            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar), ArbeidsgiverUtbetalingsperiode(a1, 1.juli, 31.juli))

            assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.dagerUtenNavAnsvar)

            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            val korrelasjonsIdJuli = inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId
            håndterUtbetalt()

            assertNotEquals(korrelasjonsIdMars, korrelasjonsIdJuli)
            assertTrue(inspektør.utbetalinger.none { it.erAnnullering })
            assertVarsler(listOf(RV_IT_3), 3.vedtaksperiode.filter())

            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)

            val nyKorrelasjonsIdJuli = inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId
            assertNotEquals(korrelasjonsIdMars, nyKorrelasjonsIdJuli)
            assertEquals(listOf(1.juli til 16.juli), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.dagerUtenNavAnsvar)
            assertVarsler(listOf(RV_UT_23, RV_IT_3), 3.vedtaksperiode.filter())
        }
    }

    @Test
    fun `En uheldig bivirkning av å behandle perioder uten AGP`() {
        a1 {
            nyttVedtak(1.januar(2017) til 31.januar(2017))
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.mars(2017), 10.mars(2017)))
            nyttVedtak(februar)
            inspektør.utbetalinger(2.vedtaksperiode).last().inspektør.korrelasjonsId
            nyttVedtak(april)
            inspektør.utbetalinger(3.vedtaksperiode).last().inspektør.korrelasjonsId

            håndterSøknad(4.juni til 6.juni)
            assertSisteTilstand(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterInntektsmelding(emptyList(), førsteFraværsdag = 4.juni, begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")
            håndterVilkårsgrunnlag(4.vedtaksperiode)
            håndterYtelser(4.vedtaksperiode)
            håndterSimulering(4.vedtaksperiode)
            håndterOverstyrTidslinje((4.juni til 6.juni).map { ManuellOverskrivingDag(it, Dagtype.Pleiepengerdag) })
            assertSkjæringstidspunktOgVenteperiode(4.vedtaksperiode, 4.juni, emptyList())
            assertSisteTilstand(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertVarsler(listOf(RV_IM_8), 4.vedtaksperiode.filter())
        }
    }

    @Test
    fun `infotrygd flytter skjæringstidspunkt`() {
        a1 {
            nyttVedtak(januar)
            nyttVedtak(10.februar til 28.februar)
            håndterUtbetalingshistorikkEtterInfotrygdendring(Friperiode(1.februar, 9.februar))
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().first().vilkårsgrunnlag.size)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `Infotrygdhistorikk som er nærme`() {
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 30.januar))
            håndterSykmelding(februar)
            håndterSøknad(februar)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Infotrygdhistorikk som ikke medfører utkasting`() {
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 30.januar))
            håndterSøknad(Sykdom(20.februar, 28.mars, 100.prosent))
            assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `eksisterende infotrygdforlengelse`() {
        medJSONPerson("/personer/infotrygdforlengelse.json", 334)
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            nyPeriode(mars)
            håndterYtelser(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
            assertIngenFunksjonelleFeil()
        }
    }

    @Test
    fun `Kan ikke overstyre inntekt på Infotrygd-sykepengegrunnlag`() {
        medJSONPerson("/personer/infotrygdforlengelse.json", 334)
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))

            val antallInnslagFør = inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size

            håndterOverstyrArbeidsgiveropplysninger(
                1.januar, listOf(
                OverstyrtArbeidsgiveropplysning(a1, 15000.månedlig, emptyList())
            )
            )
            assertEquals(antallInnslagFør, inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
        }
    }

    @Test
    fun `Kan endre refusjonsopplysninger på Infotrygd-sykepengegrunnlag, men inntekten ignoreres`() {
        medJSONPerson("/personer/infotrygdforlengelse.json", 334)
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            val antallInnslagFør = inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size

            val meldingsreferanse = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                arbeidsgiveropplysninger = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        a1, 15000.månedlig,
                        listOf(Triple(1.januar, null, 15000.månedlig))
                    )
                ),
                meldingsreferanseId = meldingsreferanse
            )
            assertEquals(antallInnslagFør, inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size)

            assertBeløpstidslinje(Beløpstidslinje.fra(februar, 15000.månedlig, meldingsreferanse.saksbehandler), inspektør.refusjon(1.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
        }
    }

    private fun gjeldendeKorrelasjonsId(vedtaksperiodeId: UUID) =
        inspektør(a1).vedtaksperioder(vedtaksperiodeId).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId
}
