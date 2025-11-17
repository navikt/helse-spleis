package no.nav.helse.spleis.e2e

import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.selvstendig
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt.ArbeistakerFaktaavklartInntektView
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class FaktaavklartInntektPåBehandlingTest : AbstractDslTest() {

    @Test
    fun `Ny vilkårsprøving etter bruk av inntekter fra aordningen`() = Toggle.BrukFaktaavklartInntektFraBehandling.enable {
        a1 {
            håndterSøknad(2.januar til 31.januar)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, flagg = setOf("ønskerInntektFraAOrdningen"))
            håndterSykepengegrunnlagForArbeidsgiver(
                vedtaksperiodeId = 1.vedtaksperiode,
                skjæringstidspunkt = 2.januar,
                inntekter = listOf(
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 12), INNTEKT, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 11), INNTEKT, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", ""),
                    ArbeidsgiverInntekt.MånedligInntekt(YearMonth.of(2017, 10), INNTEKT, ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT, "", "")
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertVarsler(1.vedtaksperiode, Varselkode.RV_IV_10)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            nullstillTilstandsendringer()
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.januar, Dagtype.Sykedag, 100)))

            assertForventetFeil(
                forklaring = "Periode kan ikke hoppe tilbake til AvventerInntektsmelding etter å ha vært til godkjenning. Skatt lagres ikke på behandlingen. I dag funker dette fordi inntekten lagres tilbake i historikken på ny dato (hack)",
                ønsket = { assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)},
                nå = { assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_INNTEKTSMELDING)}
            )
        }
    }

    @Test
    fun `korrigert inntekt blir med når skjæringstidspunktet flyttes`() = Toggle.BrukFaktaavklartInntektFraBehandling.enable {
        a1 {
            nyttVedtak(2.januar til 31.januar)
            håndterOverstyrArbeidsgiveropplysninger(2.januar, listOf(OverstyrtArbeidsgiveropplysning(orgnummer = "a1", inntekt = INNTEKT*1.1)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(INNTEKT, faktaavvklartArbeidstakerBeløp(1.vedtaksperiode))
            assertEquals(INNTEKT*1.1, korrigertInntektBeløp(1.vedtaksperiode))
            assertInntektsgrunnlag(2.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(
                    orgnummer = a1,
                    forventetFaktaavklartInntekt = INNTEKT,
                    forventetOmregnetÅrsinntekt = INNTEKT*1.1,
                    forventetKorrigertInntekt = INNTEKT*1.1
                )
            }

            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.januar, Dagtype.Sykedag, 100)))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(INNTEKT, faktaavvklartArbeidstakerBeløp(1.vedtaksperiode))
            assertEquals(INNTEKT*1.1, korrigertInntektBeløp(1.vedtaksperiode))
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(
                    orgnummer = a1,
                    forventetFaktaavklartInntekt = INNTEKT,
                    forventetOmregnetÅrsinntekt = INNTEKT*1.1,
                    forventetKorrigertInntekt = INNTEKT*1.1
                )
            }
            assertVarsler(1.vedtaksperiode, RV_IV_7)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `Flere korrigerende inntektsmeldinger, også flytter skjæringstidspunktet på seg`(brukFaktaavklartInntektFraBehandling: Boolean) {
        val test =  {
            a1 {
                nyttVedtak(2.januar til 20.januar, beregnetInntekt = INNTEKT*1.05)
                forlengVedtak(21.januar til 31.januar)
                forlengVedtak(februar)
                forlengVedtak(mars)
                håndterInntektsmelding(arbeidsgiverperioder = emptyList(), førsteFraværsdag = 1.mars, beregnetInntekt = INNTEKT*1.20)
                håndterInntektsmelding(arbeidsgiverperioder = emptyList(), førsteFraværsdag = 21.januar, beregnetInntekt = INNTEKT*1.10)
                håndterInntektsmelding(arbeidsgiverperioder = emptyList(), førsteFraværsdag = 1.februar, beregnetInntekt = INNTEKT*1.15)

                assertEquals(INNTEKT*1.05, faktaavvklartArbeidstakerBeløp(1.vedtaksperiode))
                assertEquals(INNTEKT*1.10, faktaavvklartArbeidstakerBeløp(2.vedtaksperiode))
                assertEquals(INNTEKT*1.15, faktaavvklartArbeidstakerBeløp(3.vedtaksperiode))
                assertEquals(INNTEKT*1.20, faktaavvklartArbeidstakerBeløp(4.vedtaksperiode))

                assertInntektsgrunnlag(2.januar, forventetAntallArbeidsgivere = 1) {
                    assertInntektsgrunnlag(a1, INNTEKT*1.10)
                }

                assertVarsler(2.vedtaksperiode, RV_IM_4)

                assertEquals(2.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
                håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(1.januar, Dagtype.Sykedag, 100)))
                assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))

                håndterVilkårsgrunnlag(1.vedtaksperiode)
                assertVarsler(1.vedtaksperiode, RV_IV_7)

                assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                    when (brukFaktaavklartInntektFraBehandling) {
                        true -> assertInntektsgrunnlag(a1, INNTEKT * 1.10) // Dette må jo være et bedre valg enn vi velger i dag 🎉
                        false -> assertInntektsgrunnlag(a1, INNTEKT * 1.05)
                    }
                }

                assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
                håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.desember(2017), Dagtype.Sykedag, 100)))
                assertEquals(31.desember(2017), inspektør.skjæringstidspunkt(1.vedtaksperiode))

                håndterVilkårsgrunnlag(1.vedtaksperiode)

                assertInntektsgrunnlag(31.desember(2017), forventetAntallArbeidsgivere = 1) {
                    when (brukFaktaavklartInntektFraBehandling) {
                        true -> assertInntektsgrunnlag(a1, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen) // Dette må jo være et bedre valg enn vi velger i dag 🎉
                        false -> assertInntektsgrunnlag(a1, INNTEKT * 1.05)
                    }
                }
            }
        }

        when (brukFaktaavklartInntektFraBehandling) {
            true -> Toggle.BrukFaktaavklartInntektFraBehandling.enable { test() }
            false -> Toggle.BrukFaktaavklartInntektFraBehandling.disable { test() }
        }
    }

    @Test
    fun `Selvstendig får faktaavklart inntekt fra søknaden`() {
        selvstendig {
            val søknadId = UUID.randomUUID()
            håndterFørstegangssøknadSelvstendig(januar, søknadId = søknadId)

            val faktaavklartInntekt = inspektør.faktaavklartInntekt(1.vedtaksperiode) as? SelvstendigFaktaavklartInntekt.SelvstendigFaktaavklartInntektView
            assertNotNull(faktaavklartInntekt)
            assertEquals(460589.årlig, faktaavklartInntekt.beløp)
            assertEquals(søknadId, faktaavklartInntekt.hendelseId)
        }
    }

    @Test
    fun `Selvstendig får faktaavklart inntekt fra korrigerende søknad`() {
        selvstendig {
            val søknadId = UUID.randomUUID()
            håndterFørstegangssøknadSelvstendig(januar, søknadId = søknadId)

            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? SelvstendigFaktaavklartInntekt.SelvstendigFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(460589.årlig, faktaavklartInntekt.beløp)
                assertEquals(søknadId, faktaavklartInntekt.hendelseId)
            }

            val korrigerendeSøknadId = UUID.randomUUID()
            håndterFørstegangssøknadSelvstendig(
                periode = januar,
                søknadId = korrigerendeSøknadId,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 500000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true)
                )
            )

            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? SelvstendigFaktaavklartInntekt.SelvstendigFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(460589.årlig, faktaavklartInntekt.beløp)
                assertForventetFeil(
                    forklaring = "Tar tydeligvis ikke inn korrigerende inntektsopplysninger fra selvstendigsøknad",
                    nå = {
                        assertEquals(460589.årlig, faktaavklartInntekt.beløp)
                        assertEquals(søknadId, faktaavklartInntekt.hendelseId)
                    },
                    ønsket = {
                        assertEquals(477319.årlig, faktaavklartInntekt.beløp)
                        assertEquals(korrigerendeSøknadId, faktaavklartInntekt.hendelseId)
                    }
                )
            }
        }
    }

    @Test
    fun `Arbeidstaker får faktaavklart inntekt fra arbeidsgiveropplysninger`() {
        a1 {
            håndterSøknad(januar)
            assertNull(inspektør.faktaavklartInntekt(1.vedtaksperiode))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)

            val faktaavklartInntekt = inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView
            assertNotNull(faktaavklartInntekt)
            assertEquals(INNTEKT, faktaavklartInntekt.beløp)
        }
    }

    @Test
    fun `Arbeidstaker får faktaavklart inntekt fra korrigerende arbeidsgiveropplysninger`() {
        a1 {
            håndterSøknad(januar)
            val hendelseIdArbeidsgiveropplysninger = håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(INNTEKT, faktaavklartInntekt.beløp)
                assertEquals(hendelseIdArbeidsgiveropplysninger, faktaavklartInntekt.hendelseId)
            }

            val hendelseIdKorrigerendeArbeidsgiveropplysninger = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, Arbeidsgiveropplysning.OppgittInntekt(INNTEKT * 1.1))
            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(INNTEKT * 1.1, faktaavklartInntekt.beløp)
                assertEquals(hendelseIdKorrigerendeArbeidsgiveropplysninger, faktaavklartInntekt.hendelseId)
            }
        }
    }

    @Test
    fun `Arbeidstaker får faktaavklart inntekt fra inntektsmelding`() {
        a1 {
            håndterSøknad(januar)
            assertNull(inspektør.faktaavklartInntekt(1.vedtaksperiode))
            val hendelseIdIM = håndterInntektsmelding(listOf(1.januar til 16.januar))

            val faktaavklartInntekt = inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView
            assertNotNull(faktaavklartInntekt)
            assertEquals(INNTEKT, faktaavklartInntekt.beløp)
            assertEquals(hendelseIdIM, faktaavklartInntekt.hendelseId)
        }
    }

    @Test
    fun `bruker ny faktaavklart inntekt fra korrigerende inntektsmelding`() {
        a1 {
            håndterSøknad(januar)
            val hendelseIdIM = håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(INNTEKT, faktaavklartInntekt.beløp)
                assertEquals(hendelseIdIM, faktaavklartInntekt.hendelseId)
            }

            val hendelseIdKorrigerendeIM = håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT * 1.1)
            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(INNTEKT * 1.1, faktaavklartInntekt.beløp)
                assertEquals(hendelseIdKorrigerendeIM, faktaavklartInntekt.hendelseId)
            }

            assertVarsel(RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `korrigerende inntektsmelding med samme inntekt som original inntektsmelding`() {
        a1 {
            håndterSøknad(januar)
            val hendelseIdIM = håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(INNTEKT, faktaavklartInntekt.beløp)
                assertEquals(hendelseIdIM, faktaavklartInntekt.hendelseId)
            }

            val korrigertInntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(INNTEKT, faktaavklartInntekt.beløp)
                assertEquals(korrigertInntektsmeldingId, faktaavklartInntekt.hendelseId)
            }
        }
    }

    @Test
    fun `lagrer IKKE faktaavklart inntekt fra a-ordningen`() {
        a1 {
            håndterSøknad(januar)
            assertNull(inspektør.faktaavklartInntekt(1.vedtaksperiode))
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, flagg = setOf("ønskerInntektFraAOrdningen"))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertNull(inspektør.faktaavklartInntekt(1.vedtaksperiode))
            assertVarsel(Varselkode.RV_IV_10, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Lagrer korrigert inntekt fra saksbehandler på behandlingen`() {
        a1 {
            nyttVedtak(januar)
            assertNull(inspektør.korrigertInntekt(1.vedtaksperiode))
            val overstyringId = håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1))).metadata.meldingsreferanseId.id
            inspektør.korrigertInntekt(1.vedtaksperiode)!!.apply {
                assertEquals(INNTEKT * 1.1, beløp)
                assertEquals(overstyringId, hendelseId)
            }
        }
    }

    @Test
    fun `Lagrer ikke korigert inntekt når det er den samme som var der fra før`() {
        a1 {
            nyttVedtak(januar)
            assertNull(inspektør.korrigertInntekt(1.vedtaksperiode))
            val overstyringId1 = håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1))).metadata.meldingsreferanseId.id
            inspektør.korrigertInntekt(1.vedtaksperiode)!!.apply {
                assertEquals(INNTEKT * 1.1, beløp)
                assertEquals(overstyringId1, hendelseId)
            }
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            nullstillTilstandsendringer()
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1))).metadata.meldingsreferanseId.id

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            inspektør.korrigertInntekt(1.vedtaksperiode)!!.apply {
                assertEquals(INNTEKT * 1.1, beløp)
                assertEquals(overstyringId1, hendelseId)
            }
        }
    }

    private fun faktaavvklartArbeidstakerInntekt(vedtaksperiodeId: UUID) = inspektør.faktaavklartInntekt(vedtaksperiodeId) as? ArbeistakerFaktaavklartInntektView
    private fun faktaavvklartArbeidstakerBeløp(vedtaksperiodeId: UUID) = (faktaavvklartArbeidstakerInntekt(vedtaksperiodeId))?.beløp
    private fun korrigertInntekt(vedtaksperiodeId: UUID) = inspektør.korrigertInntekt(vedtaksperiodeId)
    private fun korrigertInntektBeløp(vedtaksperiodeId: UUID) = korrigertInntekt(vedtaksperiodeId)?.beløp
}
