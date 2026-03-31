package no.nav.helse.spleis.e2e.brukerutbetaling

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsmelding.Refusjon.EndringIRefusjon
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.assertInntektshistorikkForDato
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DelvisRefusjonTest : AbstractDslTest() {

    @Test
    fun `Full refusjon til en arbeidsgiver med RefusjonPerDag på`() {
        a1 {
            nyttVedtak(januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))

            assertTrue(inspektør.sisteUtbetaling().arbeidsgiverOppdrag.isNotEmpty())
            inspektør.sisteUtbetaling().arbeidsgiverOppdrag.forEach { assertEquals(1431, it.beløp) }
            assertTrue(inspektør.sisteUtbetaling().personOppdrag.isEmpty())
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
        }
    }

    @Test
    fun `Full refusjon til en arbeidsgiver med forlengelse og opphørsdato treffer ferie`() {
        a1 {
            nyttVedtak(januar, refusjon = Inntektsmelding.Refusjon(INNTEKT, 27.februar, emptyList()))

            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(27.februar, 28.februar))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING
            )
            assertTrue(inspektør.sisteUtbetaling().arbeidsgiverOppdrag.isNotEmpty())
            inspektør.sisteUtbetaling().arbeidsgiverOppdrag.forEach { assertEquals(1431, it.beløp) }
            assertTrue(inspektør.sisteUtbetaling().personOppdrag.isEmpty())
            assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431, subset = 1.februar til 26.februar)
            assertUtbetalingsbeløp(2.vedtaksperiode, 0, 1431, subset = 27.februar til 27.februar)
            assertUtbetalingsbeløp(2.vedtaksperiode, 0, 0, subset = 28.februar til 28.februar)
        }
    }

    @Test
    fun `Refusjonsbeløpet er forskjellig fra beregnet inntekt i inntektsmeldingen`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 30000.månedlig,
                refusjon = Inntektsmelding.Refusjon(25000.månedlig, null, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING
            )
        }
    }

    @Test
    fun `arbeidsgiver refunderer ikke`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING
            )
        }
    }

    @Test
    fun `tidligere vedtaksperiode har opphør i refusjon`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(INNTEKT, 20.januar, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )

            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            assertTilstander(
                2.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING
            )


            assertTrue(inspektør.sisteUtbetaling().arbeidsgiverOppdrag.isNotEmpty())
            inspektør.sisteUtbetaling().arbeidsgiverOppdrag.forEach { assertEquals(1431, it.beløp) }
            assertTrue(inspektør.sisteUtbetaling().personOppdrag.isEmpty())
            assertUtbetalingsbeløp(2.vedtaksperiode, 0, 1431, subset = 1.mars til 16.mars)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1431, 1431, subset = 17.mars til 31.mars)
        }
    }

    @Test
    fun `kaster ikke ut vedtaksperiode når refusjonopphører`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(INNTEKT, 20.januar, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING
            )
        }
    }

    @Test
    fun `ikke kast ut vedtaksperiode ved endring i refusjon`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(INNTEKT, null, listOf(EndringIRefusjon(15000.månedlig, 20.januar))),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING
            )
        }
    }

    @Test
    fun `kaster ikke ut vedtaksperiode hvor endring i refusjon er etter perioden`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(INNTEKT, null, listOf(EndringIRefusjon(15000.månedlig, 1.februar))),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING
            )
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
        }
    }

    @Test
    fun `ikke kast ut vedtaksperiode ved ferie i slutten av perioden`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(25.januar, 31.januar))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(INNTEKT, 24.januar, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING
            )

            assertTrue(inspektør.sisteUtbetaling().arbeidsgiverOppdrag.isNotEmpty())
            inspektør.sisteUtbetaling().arbeidsgiverOppdrag.forEach { assertEquals(1431, it.beløp) }
            assertTrue(inspektør.sisteUtbetaling().personOppdrag.isEmpty())

            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 24.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 0, subset = 25.januar til 31.januar)
        }
    }

    @Test
    fun `to arbeidsgivere med ulik fom hvor den første har utbetalingsdager før arbeisdgiverperioden til den andre, ingen felles utbetalingsdager`() {
        a1 {
            håndterSykmelding(januar)
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(21.januar, 10.februar))
        }
        a1 {
            håndterSøknad(januar)
        }
        a2 {
            håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent))
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(21.januar til 5.februar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeId = 1.vedtaksperiode, a1, a2, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a1 {
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 1.januar til 16.januar,

                )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 17.januar til 31.januar
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 1.februar til 10.februar
            )
        }
        a2 {
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                subset = 1.januar til 20.januar
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 21.januar til 5.februar,

                )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 6.februar til 10.februar,

                )
        }
    }

    @Test
    fun `to arbeidsgivere med ulik fom hvor den første har utbetalingsdager før arbeisdgiverperioden til den andre`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.februar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(21.januar, 10.februar))
        }
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.februar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent))
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(21.januar til 5.februar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeId = 1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a1 {

            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 1.januar til 16.januar
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 17.januar til 5.februar
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 6.februar til 10.februar
            )
        }
        a2 {
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                subset = 1.januar til 20.januar
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 21.januar til 5.februar
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 6.februar til 10.februar
            )
        }
    }

    @Test
    fun `to arbeidsgivere med ulik fom hvor den andre har utbetalingsdager før arbeidsgiverperioden til den første`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(21.januar, 10.februar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 10.februar))
        }

        a1 {
            håndterSøknad(Sykdom(21.januar, 10.februar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(1.januar, 10.februar, 100.prosent))

            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(21.januar til 5.februar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

            håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeId = 1.vedtaksperiode, a1, a2, orgnummer = a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
        a1 {
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 21.januar til 5.februar,
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 6.februar til 10.februar,
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                subset = 1.januar til 20.januar,
            )
        }
        a2 {
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 1.januar til 16.januar,
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 17.januar til 20.januar,
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 21.januar til 5.februar,
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1431,
                subset = 6.februar til 10.februar,
            )
        }
    }

    @Test
    fun `gradert sykmelding med en arbeidsgiver`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING
            )
        }
    }

    @Test
    fun `korrigerende inntektsmelding endrer på refusjonsbeløp`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 50.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            val im2 = håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT + 100.månedlig,
                refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList())
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING
            )
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(a1, INNTEKT + 100.månedlig)
            }
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT / 2, im2.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `to arbeidsgivere hvor andre arbeidsgiver har delvis refusjon`() {
        a1 {
            håndterSykmelding(januar)
        }
        a2 {
            håndterSykmelding(januar)
        }
        a1 {
            håndterSøknad(januar)
        }
        a2 {
            håndterSøknad(januar)
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(
                    INNTEKT, 20.januar
                ),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(1, inspektør(a2).antallUtbetalinger)
        }
        a1 {
            inspektør(a1).utbetaling(0).also { utbetaling ->
                assertEquals(2, utbetaling.arbeidsgiverOppdrag.size)
                utbetaling.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                    assertEquals(1080, linje.beløp)
                    assertEquals(17.januar til 21.januar, linje.fom til linje.tom)
                }
                utbetaling.arbeidsgiverOppdrag[1].inspektør.also { linje ->
                    assertEquals(1431, linje.beløp)
                    assertEquals(22.januar til 31.januar, linje.fom til linje.tom)
                }
                assertTrue(utbetaling.personOppdrag.isEmpty())
            }
        }
        a2 {
            assertEquals(1, inspektør(a2).antallUtbetalinger)
            inspektør(a2).utbetaling(0).also { utbetaling ->
                assertEquals(1, utbetaling.arbeidsgiverOppdrag.size)
                utbetaling.arbeidsgiverOppdrag[0].inspektør.also { linje ->
                    assertEquals(1080, linje.beløp)
                    assertEquals(17.januar til 21.januar, linje.fom til linje.tom)
                }

                assertEquals(1, utbetaling.personOppdrag.size)
                utbetaling.personOppdrag[0].inspektør.also { linje ->
                    assertEquals(730, linje.beløp)
                    assertEquals(22.januar til 31.januar, linje.fom til linje.tom)
                }
            }
        }
    }

    @Test
    fun `Første utbetalte dag er før første fraværsdag`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            val inntektsmeldingId = håndterInntektsmelding(
                listOf(),
                førsteFraværsdag = 17.januar
            )

            assertInntektshistorikkForDato(INNTEKT, 17.januar, inspektør)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT, inntektsmeldingId.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))
        }
    }

    @Test
    fun `Korrigerende inntektsmelding med feil skjæringstidspunkt går til manuell behandling på grunn av warning`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)

            håndterInntektsmelding(emptyList(), førsteFraværsdag = 1.januar)

            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
    }

    @Test
    fun `arbeidsgiver sender unødvendig inntektsmelding ved forlengelse før sykmelding`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterInntektsmelding(
                emptyList(),
                førsteFraværsdag = 1.februar
            )
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(februar)
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
    }

    @Test
    fun `to arbeidsgivere, hvor den andre har opphør i refusjon`() {
        a1 {
            håndterSykmelding(januar)
        }
        a2 {
            håndterSykmelding(januar)
        }
        a1 {
            håndterSøknad(januar)
        }

        a2 {
            håndterSøknad(januar)
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(
                    INNTEKT, 15.januar, emptyList()
                ),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            assertEquals(1, inspektør(a1).antallUtbetalinger)
            inspektør(a1).utbetaling(0).also { utbetaling ->
                val linje = utbetaling.arbeidsgiverOppdrag[0].inspektør
                assertEquals(1431, linje.beløp)
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                assertTrue(utbetaling.personOppdrag.isEmpty())
            }
        }
        a2 {
            assertEquals(1, inspektør(a2).antallUtbetalinger)
            inspektør(a2).utbetaling(0).also { utbetaling ->
                val linje = utbetaling.personOppdrag[0].inspektør
                assertEquals(730, linje.beløp)
                assertEquals(17.januar til 31.januar, linje.fom til linje.tom)
                assertTrue(utbetaling.arbeidsgiverOppdrag.isEmpty())
            }

        }
    }
}
