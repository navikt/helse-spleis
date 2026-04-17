package no.nav.helse.spleis.e2e.overstyring

import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.InfotrygdView
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.saksbehandler
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Endringskode.ENDR
import no.nav.helse.utbetalingslinjer.Endringskode.NY
import no.nav.helse.utbetalingslinjer.Endringskode.UEND
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OverstyrArbeidsgiveropplysningerTest : AbstractDslTest() {

    @Test
    fun `endre inntekt når faktaavklart inntekt er 0 kr`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = INGEN, vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertVarsel(Varselkode.RV_SV_1, 1.vedtaksperiode.filter())
            håndterOverstyrArbeidsgiveropplysninger(
                1.januar,
                listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT, emptyList()))
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INGEN, INNTEKT, forventetKorrigertInntekt = INNTEKT)
            }

            inspektør.utbetaling(1).also { utbetaling ->
                assertEquals(15741, utbetaling.nettobeløp)
            }
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).forEach { utbetalingsdag ->
                assertEquals(INNTEKT, utbetalingsdag.økonomi.inspektør.aktuellDagsinntekt)
                assertEquals(INGEN, utbetalingsdag.økonomi.inspektør.arbeidsgiverbeløp)
            }
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).subset(17.januar til 31.januar)
                .filterNot { it.dato.erHelg() }
                .forEach { utbetalingsdag ->
                    assertEquals(1431.daglig, utbetalingsdag.økonomi.inspektør.personbeløp)
                }
        }
    }

    @Test
    fun `Overstyring av refusjon skal gjelde også på forlengelser`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            håndterOverstyrArbeidsgiveropplysninger(
                1.januar, listOf(
                OverstyrtArbeidsgiveropplysning(
                    a1, INNTEKT, listOf(
                    Triple(1.januar, null, INNTEKT / 2),
                )
                )
            )
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(INNTEKT / 2, inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje[1.februar].beløp)
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Kun periodene med endring i refusjon revurderes`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            håndterOverstyrArbeidsgiveropplysninger(
                1.januar, listOf(
                OverstyrtArbeidsgiveropplysning(
                    a1, INNTEKT, listOf(
                    Triple(1.januar, 31.januar, INNTEKT),
                    Triple(1.februar, null, INNTEKT / 2)
                )
                )
            )
            )
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        }
    }

    @Test
    fun `overstyrer inntekt og refusjon`() {
        a1 {
            nyttVedtak(januar)
            nullstillTilstandsendringer()
            val nyInntekt = INNTEKT * 2
            val overstyringId = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(
                1.januar, listOf(
                OverstyrtArbeidsgiveropplysning(
                    a1, nyInntekt, listOf(
                    Triple(1.januar, null, nyInntekt)
                )
                )
            ), meldingsreferanseId = overstyringId
            )
            håndterYtelser(1.vedtaksperiode)
            val førsteUtbetaling = inspektør.utbetaling(0)
            val revurdering = inspektør.utbetaling(1)
            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
            assertEquals(0, revurdering.personOppdrag.size)
            revurdering.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(17.januar, oppdrag[0].inspektør.fom)
                assertEquals(31.januar, oppdrag[0].inspektør.tom)
                assertEquals(2161, oppdrag[0].inspektør.beløp)
            }

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, nyInntekt, forventetKorrigertInntekt = nyInntekt)
            }
            assertEquals(1, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.arbeidsgiverInntektsopplysninger.size)
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, nyInntekt, overstyringId.saksbehandler), inspektør.refusjon(1.vedtaksperiode))

            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING
            )
        }
    }

    @Test
    fun `ny inntektsmelding etter saksbehandleroverstyrt inntekt`() {
        a1 {
            nyttVedtak(januar)
            val nySaksbehandlerInntekt = INNTEKT * 2
            val nyIMInntekt = INNTEKT * 3
            val overstyringId = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(
                1.januar, listOf(
                OverstyrtArbeidsgiveropplysning(
                    a1, nySaksbehandlerInntekt, listOf(
                    Triple(1.januar, null, nySaksbehandlerInntekt)
                )
                )
            ), meldingsreferanseId = overstyringId
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, nySaksbehandlerInntekt, forventetKorrigertInntekt = nySaksbehandlerInntekt)
            }

            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = nyIMInntekt
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, nyIMInntekt)
            }
        }
    }

    @Test
    fun `overstyrer inntekt og refusjon til samme som før`() {
        a1 {
            nyttVedtak(januar)
            nullstillTilstandsendringer()
            val nyInntekt = INNTEKT * 2
            val overstyringId = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                arbeidsgiveropplysninger = listOf(
                    OverstyrtArbeidsgiveropplysning(a1, nyInntekt, listOf(Triple(1.januar, null, nyInntekt)))
                ),
                meldingsreferanseId = overstyringId
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            nullstillTilstandsendringer()
            val overstyring2Id = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                arbeidsgiveropplysninger = listOf(
                    OverstyrtArbeidsgiveropplysning(a1, nyInntekt, listOf(Triple(1.januar, null, nyInntekt)))
                ),
                meldingsreferanseId = overstyring2Id
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            val førsteUtbetaling = inspektør.utbetaling(0)
            val revurdering = inspektør.utbetaling(1)
            assertEquals(førsteUtbetaling.korrelasjonsId, revurdering.korrelasjonsId)
            assertEquals(0, revurdering.personOppdrag.size)
            revurdering.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(17.januar, oppdrag[0].inspektør.fom)
                assertEquals(31.januar, oppdrag[0].inspektør.tom)
                assertEquals(2161, oppdrag[0].inspektør.beløp)
            }

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, nyInntekt, forventetKorrigertInntekt = nyInntekt)
            }
            assertEquals(1, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.arbeidsgiverInntektsopplysninger.size)
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, nyInntekt, overstyringId.saksbehandler), inspektør.refusjon(1.vedtaksperiode))

            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        }
    }

    @Test
    fun `overstyring av refusjon skal starte revurdering fom første dato med endring`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            forlengVedtak(mars)
            nullstillTilstandsendringer()
            val overstyringId = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                arbeidsgiveropplysninger = listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT, listOf(Triple(1.mars, null, INNTEKT / 2)))),
                meldingsreferanseId = overstyringId
            )
            håndterYtelser(3.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 3.vedtaksperiode.filter())
            håndterSimulering(3.vedtaksperiode)

            val førsteMarsUtbetaling = inspektør.utbetaling(2)
            val revurderingMarsUtbetaling = inspektør.utbetaling(3)
            assertEquals(førsteMarsUtbetaling.korrelasjonsId, revurderingMarsUtbetaling.korrelasjonsId)
            assertEquals(1, revurderingMarsUtbetaling.personOppdrag.size)
            revurderingMarsUtbetaling.arbeidsgiverOppdrag.also { oppdrag ->
                assertEquals(1.mars, oppdrag[0].inspektør.fom)
                assertEquals(31.mars, oppdrag[0].inspektør.tom)
                assertEquals(715, oppdrag[0].inspektør.beløp)
                assertEquals(NY, oppdrag[0].inspektør.endringskode)
            }
            revurderingMarsUtbetaling.personOppdrag.also { oppdrag ->
                assertEquals(1.mars, oppdrag[0].inspektør.fom)
                assertEquals(31.mars, oppdrag[0].inspektør.tom)
                assertEquals(715, oppdrag[0].inspektør.beløp)
                assertEquals(NY, oppdrag[0].inspektør.endringskode)
            }
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)

            assertEquals(1, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.inntektsgrunnlag.arbeidsgiverInntektsopplysninger.size)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(februar, INNTEKT), inspektør.refusjon(2.vedtaksperiode), ignoreMeldingsreferanseId = true)
            assertBeløpstidslinje(Beløpstidslinje.fra(mars, INNTEKT / 2, overstyringId.saksbehandler), inspektør.refusjon(3.vedtaksperiode))

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING)
        }
    }

    @Test
    fun `overstyrer inntekt på Infotrygdvilkårsgrunnlag`() {
        medJSONPerson("/personer/infotrygdforlengelse.json", 334)
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
            assertTrue(inspektør.vilkårsgrunnlag(1.vedtaksperiode)?.view() is InfotrygdView)
            val antallHistorikkInnslagFør = inspektør.vilkårsgrunnlagHistorikkInnslag().size
            val nyInntekt = INNTEKT * 2
            nullstillTilstandsendringer()
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                arbeidsgiveropplysninger = listOf(OverstyrtArbeidsgiveropplysning(a1, nyInntekt, emptyList()))
            )
            assertEquals(antallHistorikkInnslagFør, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT)
            }
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `skal være idempotente greier`() {
        a1 {
            nyttVedtak(januar)

            val overstyr: () -> Unit = {
                håndterOverstyrArbeidsgiveropplysninger(
                    skjæringstidspunkt = 1.januar,
                    meldingsreferanseId = UUID.randomUUID(),
                    arbeidsgiveropplysninger = listOf(
                        OverstyrtArbeidsgiveropplysning(a1, INNTEKT / 2, refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT / 2)))
                    )
                )
            }

            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            overstyr()
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)

            nullstillTilstandsendringer()

            repeat(10) { overstyr() }
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        }
    }

    @Test
    fun `overstyrer arbeidsgiveropplysninger på flere arbeidsgivere`() {
        val inntektPerArbeidsgiver = 19000.månedlig
        (a1 og a2 og a3).nyeVedtak(januar, inntekt = inntektPerArbeidsgiver)
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a3 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }

        val vilkårsgrunnlagHistorikkInnslagFørOverstyring = inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size
        val overstyringId = UUID.randomUUID()
        val a3RefusjonsopplysningerFørOverstyring = a3 { inspektør.refusjon(1.vedtaksperiode) }
        a3 { assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, inntektPerArbeidsgiver), a3RefusjonsopplysningerFørOverstyring, ignoreMeldingsreferanseId = true) }

        håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            meldingsreferanseId = overstyringId,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    orgnummer = a1,
                    inntekt = inntektPerArbeidsgiver,
                    refusjonsopplysninger = listOf(
                        Triple(1.januar, 20.januar, inntektPerArbeidsgiver),
                        Triple(21.januar, null, INGEN),
                    )
                ), OverstyrtArbeidsgiveropplysning(
                orgnummer = a2,
                inntekt = inntektPerArbeidsgiver * 1.25,
                refusjonsopplysninger = listOf(
                    Triple(1.januar, null, inntektPerArbeidsgiver * 1.25)
                )
            ), OverstyrtArbeidsgiveropplysning(
                orgnummer = a3,
                inntekt = inntektPerArbeidsgiver * 1.5,
                refusjonsopplysninger = listOf(
                    Triple(1.januar, null, inntektPerArbeidsgiver)
                )
            )
            )
        )

        // a1
        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())

            assertEquals(vilkårsgrunnlagHistorikkInnslagFørOverstyring + 1, inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 3) {
                assertInntektsgrunnlag(a1, inntektPerArbeidsgiver)
                assertInntektsgrunnlag(a2, inntektPerArbeidsgiver, inntektPerArbeidsgiver * 1.25, forventetKorrigertInntekt = inntektPerArbeidsgiver * 1.25)
                assertInntektsgrunnlag(a3, inntektPerArbeidsgiver, inntektPerArbeidsgiver * 1.5, forventetKorrigertInntekt = inntektPerArbeidsgiver * 1.5)
            }
            assertBeløpstidslinje(
                ARBEIDSGIVER.beløpstidslinje(1.januar til 20.januar, inntektPerArbeidsgiver) + SAKSBEHANDLER.beløpstidslinje(21.januar til 31.januar, INGEN),
                inspektør.refusjon(1.vedtaksperiode),
                ignoreMeldingsreferanseId = true
            )
        }

        // a2
        a2 { assertBeløpstidslinje(Beløpstidslinje.fra(januar, inntektPerArbeidsgiver * 1.25, overstyringId.saksbehandler), inspektør.refusjon(1.vedtaksperiode)) }

        // a3
        a3 { assertBeløpstidslinje(a3RefusjonsopplysningerFørOverstyring, inspektør.refusjon(1.vedtaksperiode)) }
    }

    @Test
    fun `to arbeidsgivere uten endring`() {
        val inntektPerArbeidsgiver = 19000.månedlig
        (a1 og a2).nyeVedtak(januar, inntekt = inntektPerArbeidsgiver)
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }

        val vilkårsgrunnlagHistorikkInnslagFørOverstyring = inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size
        val overstyringId = UUID.randomUUID()

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntektPerArbeidsgiver)
                assertInntektsgrunnlag(a2, inntektPerArbeidsgiver)
            }
        }

        håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            meldingsreferanseId = overstyringId,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    orgnummer = a1,
                    inntekt = inntektPerArbeidsgiver,
                    refusjonsopplysninger = listOf(
                        Triple(1.januar, null, inntektPerArbeidsgiver),
                    )
                ), OverstyrtArbeidsgiveropplysning(
                orgnummer = a2,
                inntekt = inntektPerArbeidsgiver,
                refusjonsopplysninger = listOf(Triple(1.januar, null, inntektPerArbeidsgiver))
            )
            )
        )

        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        assertEquals(vilkårsgrunnlagHistorikkInnslagFørOverstyring, inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size)

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntektPerArbeidsgiver)
                assertInntektsgrunnlag(a2, inntektPerArbeidsgiver)
            }
        }
    }

    @Test
    fun `to arbeidsgivere kun endring på den ene`() {
        val inntektPerArbeidsgiver = 19000.månedlig
        (a1 og a2).nyeVedtak(januar, inntekt = inntektPerArbeidsgiver)
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }

        val vilkårsgrunnlagHistorikkInnslagFørOverstyring = inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size
        val overstyringId = UUID.randomUUID()
        val a1RefusjonFørOverstyring = a1 { inspektør.refusjon(1.vedtaksperiode) }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntektPerArbeidsgiver)
                assertInntektsgrunnlag(a2, inntektPerArbeidsgiver)
            }
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, inntektPerArbeidsgiver), a1RefusjonFørOverstyring, ignoreMeldingsreferanseId = true)
        }

        håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            meldingsreferanseId = overstyringId,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    orgnummer = a1,
                    inntekt = inntektPerArbeidsgiver,
                    refusjonsopplysninger = listOf(
                        Triple(1.januar, null, inntektPerArbeidsgiver),
                    )
                ), OverstyrtArbeidsgiveropplysning(
                orgnummer = a2,
                inntekt = inntektPerArbeidsgiver * 1.5,
                refusjonsopplysninger = listOf(
                    Triple(1.januar, 20.januar, inntektPerArbeidsgiver),
                    Triple(21.januar, null, inntektPerArbeidsgiver * 1.5)
                )
            )
            )
        )

        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertEquals(vilkårsgrunnlagHistorikkInnslagFørOverstyring + 1, inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size)
        }

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntektPerArbeidsgiver)
                assertInntektsgrunnlag(a2, inntektPerArbeidsgiver, inntektPerArbeidsgiver * 1.5, forventetKorrigertInntekt = inntektPerArbeidsgiver * 1.5)
            }
            assertBeløpstidslinje(a1RefusjonFørOverstyring, inspektør.refusjon(1.vedtaksperiode))
        }

        a2 {
            assertBeløpstidslinje(
                ARBEIDSGIVER.beløpstidslinje(1.januar til 20.januar, inntektPerArbeidsgiver) + SAKSBEHANDLER.beløpstidslinje(21.januar til 31.januar, inntektPerArbeidsgiver * 1.5),
                inspektør.refusjon(1.vedtaksperiode),
                ignoreMeldingsreferanseId = true
            )
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
    }

    @Test
    fun `to arbeidsgivere kun refusjonsendring på den ene og endring av inntekt på andre`() {
        val inntektPerArbeidsgiver = 19000.månedlig
        (a1 og a2).nyeVedtak(januar, inntekt = inntektPerArbeidsgiver)
        (a1 og a2).forlengVedtak(februar)
        a1 { assertSisteTilstand(2.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(2.vedtaksperiode, AVSLUTTET) }

        val refusjonsopplysningerFørOverstyringA1 = a1 { inspektør.refusjon(1.vedtaksperiode) + inspektør.refusjon(2.vedtaksperiode) }
        a1 { assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(1.januar til 28.februar, inntektPerArbeidsgiver), refusjonsopplysningerFørOverstyringA1, ignoreMeldingsreferanseId = true) }

        val overstyringId = UUID.randomUUID()
        håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            meldingsreferanseId = overstyringId,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    orgnummer = a1,
                    inntekt = inntektPerArbeidsgiver * 1.5,
                    refusjonsopplysninger = listOf(
                        Triple(1.januar, null, inntektPerArbeidsgiver),
                    )
                ), OverstyrtArbeidsgiveropplysning(
                orgnummer = a2,
                inntekt = inntektPerArbeidsgiver,
                refusjonsopplysninger = listOf(
                    Triple(1.januar, 31.januar, inntektPerArbeidsgiver),
                    Triple(1.februar, null, inntektPerArbeidsgiver / 2)
                )
            )
            )
        )

        a1 {
            håndterYtelser(1.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntektPerArbeidsgiver, inntektPerArbeidsgiver * 1.5, forventetKorrigertInntekt = inntektPerArbeidsgiver * 1.5)
                assertInntektsgrunnlag(a2, inntektPerArbeidsgiver)
            }
            assertBeløpstidslinje(refusjonsopplysningerFørOverstyringA1, inspektør.refusjon(1.vedtaksperiode) + inspektør.refusjon(2.vedtaksperiode))
        }

        a2 {
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, inntektPerArbeidsgiver), inspektør.refusjon(1.vedtaksperiode), ignoreMeldingsreferanseId = true)
            assertBeløpstidslinje(Beløpstidslinje.fra(februar, inntektPerArbeidsgiver / 2, overstyringId.saksbehandler), inspektør.refusjon(2.vedtaksperiode))
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `flere arbeidsgivere får rett utbetaling etter nye opplysninger på begge arbeidsgivere`() {
        val inntekt = 10000.månedlig
        (a1 og a2).nyeVedtak(januar, inntekt = inntekt)
        assertEquals(1, inspektør(a1).antallUtbetalinger)
        assertEquals(1, inspektør(a2).antallUtbetalinger)

        håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    orgnummer = a1,
                    inntekt = inntekt * 1.5,
                    refusjonsopplysninger = listOf(
                        Triple(1.januar, null, inntekt),
                    )
                ), OverstyrtArbeidsgiveropplysning(
                orgnummer = a2,
                inntekt = inntekt,
                refusjonsopplysninger = listOf(
                    Triple(1.januar, 20.januar, inntekt),
                    Triple(21.januar, null, INGEN)
                )
            )
            )
        )

        a1 { håndterYtelser(1.vedtaksperiode) }

        assertEquals(2, inspektør(a1).antallUtbetalinger)
        assertEquals(2, inspektør(a2).antallUtbetalinger)

        assertEquals(inspektør(a1).utbetaling(0).korrelasjonsId, inspektør(a1).sisteUtbetaling().korrelasjonsId)
        assertEquals(inspektør(a2).utbetaling(0).korrelasjonsId, inspektør(a2).sisteUtbetaling().korrelasjonsId)

        inspektør(a1).utbetaling(0).let { opprinneligUtbetaling ->
            assertEquals(0, opprinneligUtbetaling.personOppdrag.size)
            assertEquals(1, opprinneligUtbetaling.arbeidsgiverOppdrag.size)
            opprinneligUtbetaling.arbeidsgiverOppdrag[0].let { utbetalingslinje ->
                assertEquals(17.januar, utbetalingslinje.inspektør.fom)
                assertEquals(31.januar, utbetalingslinje.inspektør.tom)
                assertEquals(462, utbetalingslinje.inspektør.beløp)
                assertEquals(NY, utbetalingslinje.inspektør.endringskode)
            }
        }

        inspektør(a1).sisteUtbetaling().let { revurdering ->
            assertEquals(1, revurdering.personOppdrag.size)
            assertEquals(1, revurdering.arbeidsgiverOppdrag.size)
            revurdering.arbeidsgiverOppdrag[0].let { utbetalingslinje ->
                assertEquals(17.januar, utbetalingslinje.inspektør.fom)
                assertEquals(31.januar, utbetalingslinje.inspektør.tom)
                assertEquals(462, utbetalingslinje.inspektør.beløp)
                assertEquals(UEND, utbetalingslinje.inspektør.endringskode)
            }
            revurdering.personOppdrag[0].let { utbetalingslinje ->
                assertEquals(17.januar, utbetalingslinje.inspektør.fom)
                assertEquals(31.januar, utbetalingslinje.inspektør.tom)
                assertEquals(231, utbetalingslinje.inspektør.beløp)
                assertEquals(NY, utbetalingslinje.inspektør.endringskode)
            }
        }

        inspektør(a2).utbetaling(0).let { opprinneligUtbetaling ->
            assertEquals(0, opprinneligUtbetaling.personOppdrag.size)
            assertEquals(1, opprinneligUtbetaling.arbeidsgiverOppdrag.size)
            opprinneligUtbetaling.arbeidsgiverOppdrag[0].let { utbetalingslinje ->
                assertEquals(17.januar, utbetalingslinje.inspektør.fom)
                assertEquals(31.januar, utbetalingslinje.inspektør.tom)
                assertEquals(462, utbetalingslinje.inspektør.beløp)
                assertEquals(NY, utbetalingslinje.inspektør.endringskode)
            }
        }
        inspektør(a2).sisteUtbetaling().let { revurdering ->
            assertEquals(1, revurdering.personOppdrag.size)
            assertEquals(1, revurdering.arbeidsgiverOppdrag.size)
            revurdering.arbeidsgiverOppdrag[0].let { utbetalingslinje ->
                assertEquals(17.januar, utbetalingslinje.inspektør.fom)
                assertEquals(21.januar, utbetalingslinje.inspektør.tom)
                assertEquals(462, utbetalingslinje.inspektør.beløp)
                assertEquals(ENDR, utbetalingslinje.inspektør.endringskode)
            }
            revurdering.personOppdrag[0].let { utbetalingslinje ->
                assertEquals(22.januar, utbetalingslinje.inspektør.fom)
                assertEquals(31.januar, utbetalingslinje.inspektør.tom)
                assertEquals(462, utbetalingslinje.inspektør.beløp)
                assertEquals(NY, utbetalingslinje.inspektør.endringskode)
            }
        }
    }

    @Test
    fun `overstyrer arbeidsgiver som ikke er i sykepengegrunnlaget`() {
        a1 {
            nyttVedtak(januar)
            val nyInntekt = INNTEKT * 1.25

            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                arbeidsgiveropplysninger = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = nyInntekt,
                        refusjonsopplysninger = listOf(
                            Triple(1.januar, null, nyInntekt)
                        )
                    ), OverstyrtArbeidsgiveropplysning(
                    orgnummer = a2,
                    inntekt = nyInntekt,
                    refusjonsopplysninger = listOf(
                        Triple(1.januar, null, nyInntekt)
                    )
                )
                )
            )

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT, nyInntekt, nyInntekt, nyInntekt)
            }
        }
    }

    @Test
    fun `overstyrer kun enkelte arbeidsgivere i sykepengegrunnlaget`() {
        val inntektPerArbeidsgiver = 19000.månedlig
        (a1 og a2).nyeVedtak(januar, inntekt = inntektPerArbeidsgiver)
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }

        val vilkårsgrunnlagHistorikkInnslagFørOverstyring = inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size
        val overstyringId = UUID.randomUUID()
        val a1RefusjonFørOverstyring = a1 { inspektør.refusjon(1.vedtaksperiode) }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntektPerArbeidsgiver)
                assertInntektsgrunnlag(a2, inntektPerArbeidsgiver)
            }
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(januar, inntektPerArbeidsgiver), a1RefusjonFørOverstyring, ignoreMeldingsreferanseId = true)
        }

        håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            meldingsreferanseId = overstyringId,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    orgnummer = a2,
                    inntekt = inntektPerArbeidsgiver * 1.5,
                    refusjonsopplysninger = listOf(
                        Triple(1.januar, 20.januar, inntektPerArbeidsgiver),
                        Triple(21.januar, null, inntektPerArbeidsgiver * 1.5)
                    )
                )
            )
        )

        a1 {
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            assertEquals(vilkårsgrunnlagHistorikkInnslagFørOverstyring + 1, inspektør(a1).vilkårsgrunnlagHistorikkInnslag().size)
            assertSame(a1RefusjonFørOverstyring, a1 { inspektør.refusjon(1.vedtaksperiode) })

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntektPerArbeidsgiver)
                assertInntektsgrunnlag(a2, inntektPerArbeidsgiver, inntektPerArbeidsgiver * 1.5, forventetKorrigertInntekt = inntektPerArbeidsgiver * 1.5)
            }
        }
        a2 {
            assertBeløpstidslinje(
                ARBEIDSGIVER.beløpstidslinje(1.januar til 20.januar, inntektPerArbeidsgiver) + SAKSBEHANDLER.beløpstidslinje(21.januar til 31.januar, inntektPerArbeidsgiver * 1.5),
                inspektør.refusjon(1.vedtaksperiode),
                ignoreMeldingsreferanseId = true
            )
        }
    }

    @Test
    fun `Innteksmelding overstyrer saksbehandlerinntekt`() {
        (a1 og a2).nyeVedtak(januar, inntekt = INNTEKT)
        håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            meldingsreferanseId = UUID.randomUUID(),
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(
                    orgnummer = a1,
                    inntekt = INNTEKT * 1.5,
                    refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT * 1.5))
                ),
                OverstyrtArbeidsgiveropplysning(
                    orgnummer = a2,
                    inntekt = INNTEKT * 1.5,
                    refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT * 1.5))
                )
            )
        )
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT, INNTEKT * 1.5, forventetKorrigertInntekt = INNTEKT * 1.5)
                assertInntektsgrunnlag(a2, INNTEKT, INNTEKT * 1.5, forventetKorrigertInntekt = INNTEKT * 1.5)
            }

            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT
            )
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT, INNTEKT * 1.5, forventetKorrigertInntekt = INNTEKT * 1.5)
            }
        }
    }

    @Test
    fun `Legge til refusjonsopplysninger tilbake i tid`() {
        val im1 = UUID.randomUUID()
        a1 {
            val vedtaksperiode = nyPeriode(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, id = im1)
            håndterVilkårsgrunnlag(vedtaksperiode)
            håndterYtelser(vedtaksperiode)
            håndterSimulering(vedtaksperiode)
            håndterUtbetalingsgodkjenning(vedtaksperiode)
            håndterUtbetalt()

            nyPeriode(5.februar til 28.februar)
            val im2 = håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                førsteFraværsdag = 7.februar
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)

            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT, im1.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
            assertBeløpstidslinje(Beløpstidslinje.fra(5.februar til 28.februar, INNTEKT, im2.arbeidsgiver), inspektør.refusjon(2.vedtaksperiode))

            nullstillTilstandsendringer()

            val overstyringId = UUID.randomUUID()
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 5.februar,
                meldingsreferanseId = overstyringId,
                arbeidsgiveropplysninger = listOf(
                    OverstyrtArbeidsgiveropplysning(a1, INNTEKT, listOf(Triple(5.februar, null, INNTEKT)))
                )
            )

            assertBeløpstidslinje(Beløpstidslinje.fra(5.februar til 28.februar, INNTEKT, im2.arbeidsgiver), inspektør.refusjon(2.vedtaksperiode))

            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
            assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING)
        }
    }
}
