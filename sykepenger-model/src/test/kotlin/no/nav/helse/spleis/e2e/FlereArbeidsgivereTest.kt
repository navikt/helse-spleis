package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.SøknadArbeidsgiver.Søknadsperiode
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.person.Inntektskilde.FLERE_ARBEIDSGIVERE
import no.nav.helse.person.Periodetype
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.serde.reflection.castAsList
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class FlereArbeidsgivereTest : AbstractEndToEndTest() {
    private companion object {
        private const val a1 = "arbeidsgiver 1"
        private const val a2 = "arbeidsgiver 2"
        private const val a3 = "arbeidsgiver 3"
        private const val a4 = "arbeidsgiver 4"
    }

    private val String.inspektør get() = inspektør(this)

    @Test
    fun `Sammenligningsgrunnlag for flere arbeidsgivere`() {
        val periodeA1 = 1.januar til 31.januar
        nyPeriode(1.januar til 31.januar, a1)
        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(Periode(periodeA1.start, periodeA1.start.plusDays(15))),
                beregnetInntekt = 30000.månedlig,
                førsteFraværsdag = periodeA1.start,
                refusjon = Triple(null, 30000.månedlig, emptyList()),
                orgnummer = a1
            )
        )
        person.håndter(ytelser(1.vedtaksperiode(a1), orgnummer = a1, inntektshistorikk = emptyList()))
        person.håndter(vilkårsgrunnlag(a1.id(0), orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.januar(2017) til 1.juni(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 5000.månedlig
                }
                1.august(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 17000.månedlig
                    a2 inntekt 3500.månedlig
                }
            }
        )))

        val periodeA2 = 15.januar til 15.februar
        nyPeriode(periodeA2, a2)
        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(Periode(periodeA2.start, periodeA2.start.plusDays(15))),
                beregnetInntekt = 10000.månedlig,
                førsteFraværsdag = periodeA2.start,
                refusjon = Triple(null, 10000.månedlig, emptyList()),
                orgnummer = a2
            )
        )

        assertEquals(318500.årlig, person.sammenligningsgrunnlag(1.januar))
    }

    @Test
    fun `Sammenligningsgrunnlag for flere arbeidsgivere med flere sykeperioder`() {
        nyPeriode(15.januar til 5.februar, a1)
        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(15.januar til 28.januar, 2.februar til 3.februar),
                beregnetInntekt = 30000.månedlig,
                førsteFraværsdag = 2.februar,
                refusjon = Triple(null, 30000.månedlig, emptyList()),
                orgnummer = a1
            )
        )
        person.håndter(ytelser(1.vedtaksperiode(a1), orgnummer = a1, inntektshistorikk = emptyList()))
        person.håndter(vilkårsgrunnlag(a1.id(0), orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.januar(2017) til 1.juni(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 5000.månedlig
                }
                1.august(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 17000.månedlig
                    a2 inntekt 3500.månedlig
                }
            }
        )))

        val periodeA2 = 2.februar til 20.februar
        nyPeriode(periodeA2, a2)
        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(Periode(periodeA2.start, periodeA2.start.plusDays(15))),
                beregnetInntekt = 10000.månedlig,
                førsteFraværsdag = periodeA2.start,
                refusjon = Triple(null, 10000.månedlig, emptyList()),
                orgnummer = a2
            )
        )

        assertEquals(282500.årlig, person.sammenligningsgrunnlag(2.februar))
    }

    @Test
    @Disabled("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `Sammenligningsgrunnlag for flere arbeidsgivere som overlapper hverandres sykeperioder`() {
        nyPeriode(15.januar til 5.februar, a1)
        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(15.januar til 28.januar, 2.februar til 3.februar),
                beregnetInntekt = 30000.månedlig,
                førsteFraværsdag = 2.februar,
                refusjon = Triple(null, 30000.månedlig, emptyList()),
                orgnummer = a1
            )
        )
        val periodeA2 = 15.januar til 15.februar
        nyPeriode(periodeA2, a2)

        person.håndter(vilkårsgrunnlag(a1.id(0), orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.januar(2017) til 1.juni(2017) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt 5000.månedlig
                }
                1.august(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 17000.månedlig
                    a2 inntekt 3500.månedlig
                }
            }
        )))

        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(Periode(periodeA2.start, periodeA2.start.plusDays(15))),
                beregnetInntekt = 10000.månedlig,
                førsteFraværsdag = periodeA2.start,
                refusjon = Triple(null, 10000.månedlig, emptyList()),
                orgnummer = a2
            )
        )

        assertEquals(318500.årlig, person.sammenligningsgrunnlag(15.januar))
    }

    @Test
    @Disabled("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `overlappende arbeidsgivere ikke sendt til infotrygd`() {
        gapPeriode(1.januar til 31.januar, a1)
        gapPeriode(15.januar til 15.februar, a2)
        assertNoErrors(a1.inspektør)
        assertNoErrors(a2.inspektør)

        historikk(a1)
        assertNoErrors(a1.inspektør)
        assertNoErrors(a2.inspektør)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
    }

    @Test
    fun `vedtaksperioder atskilt med betydelig tid`() {
        Toggles.FlereArbeidsgivereFørstegangsbehandling.enable {
            prosessperiode(1.januar til 31.januar, a1)
            assertNoErrors(a1.inspektør)
            assertTilstand(a1, AVSLUTTET)

            prosessperiode(1.mars til 31.mars, a2)
            assertNoErrors(a2.inspektør)
            assertTilstand(a1, AVSLUTTET)
        }
    }

    @Test
    @Disabled("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `Tre overlappende perioder med en ikke-overlappende periode`() {
        gapPeriode(1.januar til 31.januar, a1)
        gapPeriode(15.januar til 15.mars, a2)
        gapPeriode(1.februar til 28.februar, a3)
        gapPeriode(15.april til 15.mai, a4)

        historikk(a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVVENTER_HISTORIKK)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a3)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        betale(a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_HISTORIKK)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a3)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_SIMULERING)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        betale(a3)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVSLUTTET)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)
        assertTilstand(a3, AVSLUTTET)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        betale(a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVSLUTTET)
        assertTilstand(a4, AVVENTER_HISTORIKK)

        historikk(a4)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVSLUTTET)
        assertTilstand(a4, AVVENTER_SIMULERING)

        betale(a4)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVSLUTTET)
        assertTilstand(a4, AVSLUTTET)
    }

    @Test
    @Disabled("Trenger inntekt fra Inntektskomponenten før disse virker (§8-28)")
    fun `Tre paralelle perioder`() {
        gapPeriode(3.januar til 31.januar, a1)
        gapPeriode(1.januar til 31.januar, a2)
        gapPeriode(2.januar til 31.januar, a3)

        historikk(a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVVENTER_HISTORIKK)

        historikk(a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_HISTORIKK)

        historikk(a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_HISTORIKK)

        historikk(a3)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)

        historikk(a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)

        betale(a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)

        historikk(a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)
        assertTilstand(a3, AVVENTER_ARBEIDSGIVERE)

        betale(a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVVENTER_HISTORIKK)

        historikk(a3)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVVENTER_SIMULERING)

        betale(a3)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
        assertTilstand(a3, AVSLUTTET)
    }

    @Test
    fun `Tillater overgang fra infotrygd for flere arbeidsgivere - a1 kommer til AVVENTER_HISTORIKK først`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertInntektskilde(a1, EN_ARBEIDSGIVER)
        assertInntektskilde(a2, EN_ARBEIDSGIVER)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)
        assertEquals("FLERE_ARBEIDSGIVERE", a1.inspektør.sisteBehov(1.vedtaksperiode(a1)).detaljer()["inntektskilde"])

        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, TIL_UTBETALING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)

        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_GODKJENNING)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)
        assertEquals("FLERE_ARBEIDSGIVERE", a2.inspektør.sisteBehov(1.vedtaksperiode(a2)).detaljer()["inntektskilde"])

        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, TIL_UTBETALING)

        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
    }

    @Test
    fun `Tillater overgang fra infotrygd for flere arbeidsgivere - a2 kommer til AVVENTER_HISTORIKK først`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertInntektskilde(a1, EN_ARBEIDSGIVER)
        assertInntektskilde(a2, EN_ARBEIDSGIVER)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, TIL_UTBETALING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)

        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_GODKJENNING)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, TIL_UTBETALING)

        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
    }

    @Test
    fun `Tillater ikke flere arbeidsgivere hvis ingen er overgang fra Infotrygd`() = Toggles.FlereArbeidsgivereFørstegangsbehandling.disable {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 25.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 25.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, TIL_INFOTRYGD)
        assertTilstand(a2, TIL_INFOTRYGD)
    }

    @Test
    fun `Tillater ikke flere arbeidsgivere hvis ikke alle er overgang fra Infotrygd`() = Toggles.FlereArbeidsgivereFørstegangsbehandling.disable {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 25.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, TIL_INFOTRYGD)
        assertTilstand(a2, TIL_INFOTRYGD)
    }

    @Disabled("Det finnes ikke inntekt for skjæringstidspunktet (4. januar)")
    @Test
    fun `Tillater flere arbeidsgivere selv om ikke alle har samme periodetype`() = Toggles.FlereArbeidsgivereFørstegangsbehandling.enable {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 25.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        håndterInntektsmelding(listOf(4.januar(2021) til 19.januar(2021)), førsteFraværsdag = 27.januar(2021), orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, TIL_UTBETALING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)

        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_GODKJENNING)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, TIL_UTBETALING)

        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
    }

    @Test
    fun `Tillater forlengelse av overgang fra infotrygd for flere arbeidsgivere - a1 kommer til AVVENTER_HISTORIKK først`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)
        assertInntektskilde(a1, EN_ARBEIDSGIVER, 2)
        assertInntektskilde(a2, EN_ARBEIDSGIVER, 2)

        håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE, 2)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE, 2)

        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE, 2)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE, 2)

        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, TIL_UTBETALING, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_SIMULERING, 2)

        håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_GODKJENNING, 2)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE, 2)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE, 2)

        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, TIL_UTBETALING, 2)

        håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVSLUTTET, 2)
    }

    @Test
    fun `Tillater ikke forlengelse av overgang fra infotrygd for flere arbeidsgivere hvis sykmelding og søknad for en arbeidsgiver kommer før sykmelding for den andre arbeidsgiveren`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, TIL_INFOTRYGD, 2)

        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, TIL_INFOTRYGD, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, TIL_INFOTRYGD, 2)
        assertTilstand(a2, TIL_INFOTRYGD, 2)
    }

    @Test
    fun `Tillater ikke forlengelse av overgang fra infotrygd for flere arbeidsgivere hvis sykmelding og søknad for en arbeidsgiver kommer før sykmelding for den andre arbeidsgiveren2`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)

        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING, 1)
        assertTilstand(a1, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, TIL_INFOTRYGD, 1)
        assertTilstand(a1, TIL_INFOTRYGD, 2)
        assertTilstand(a2, TIL_INFOTRYGD, 1)
    }

    @Test
    fun `Tillater ikke forlengelse av forlengelse av overgang fra infotrygd for flere arbeidsgivere hvis sykmelding og søknad for en arbeidsgiver kommer før sykmelding for den andre arbeidsgiveren`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)

        //Her starter forlengelsen av forlengelsen
        val forlengelsesforlengelseperiode = 11.februar(2021) til 20.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 3)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a1
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)

        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP, 3)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a2
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
    }

    @Test
    fun `forlenger forkastet periode med flere arbeidsgivere`() = Toggles.FlereArbeidsgivereFørstegangsbehandling.disable {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
        assertTilstand(a3, TIL_INFOTRYGD)

        //Her starter forlengelsen av forlengelsen
        val forlengelsesforlengelseperiode = 11.februar(2021) til 20.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP, 3)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP, 3)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a1
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)

        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
            orgnummer = a2
        )
        assertTilstand(a1, TIL_INFOTRYGD, 3)
        assertTilstand(a2, TIL_INFOTRYGD, 3)
    }

    @Test
    fun `Kaster ut en plutselig tredje arbeidsgiver og passer på at senere forlengelser også forkastes`() =
        Toggles.FlereArbeidsgivereFørstegangsbehandling.enable {
            val periode = 27.januar(2021) til 31.januar(2021)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

            val inntektshistorikk = listOf(
                Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
                Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
            )

            val utbetalinger = arrayOf(
                Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
                Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
            )

            håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

            //Her starter forlengelsen
            val forlengelseperiode = 1.februar(2021) til 28.februar(2021)
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
            håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
            håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
            håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
            håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
            håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)

            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
            assertTilstand(a3, TIL_INFOTRYGD)

            //Her starter forlengelsen av forlengelsen
            val forlengelsesforlengelseperiode = 1.mars(2021) til 31.mars(2021)
            håndterSykmelding(
                Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a1
            )
            håndterSykmelding(
                Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a2
            )
            assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP, 3)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP, 3)

            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a1
            )
            assertTilstand(a1, TIL_INFOTRYGD, 3)
            assertTilstand(a2, TIL_INFOTRYGD, 3)

            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a2
            )
            assertTilstand(a1, TIL_INFOTRYGD, 3)
            assertTilstand(a2, TIL_INFOTRYGD, 3)
        }

    @Test
    fun `forlenger forkastet periode med flere arbeidsgivere hvor alle arbeidsgivernes perioder blir forlenget`() =
        Toggles.FlereArbeidsgivereFørstegangsbehandling.disable {
            val periode = 27.januar(2021) til 31.januar(2021)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

            val inntektshistorikk = listOf(
                Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
                Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
            )

            val utbetalinger = arrayOf(
                Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
                Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
            )

            håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

            //Her starter forlengelsen
            val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
            håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
            håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
            håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
            håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
            håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)

            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
            assertTilstand(a3, TIL_INFOTRYGD)

            //Her starter forlengelsen av forlengelsen
            val forlengelsesforlengelseperiode = 11.februar(2021) til 20.februar(2021)
            håndterSykmelding(
                Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a1
            )
            håndterSykmelding(
                Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a2
            )
            håndterSykmelding(
                Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a3
            )
            assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP, 3)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP, 3)
            assertTilstand(a3, MOTTATT_SYKMELDING_FERDIG_GAP, 2)

            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a1
            )
            assertTilstand(a1, TIL_INFOTRYGD, 3)
            assertTilstand(a2, TIL_INFOTRYGD, 3)
            assertTilstand(a3, TIL_INFOTRYGD, 2)

            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a2
            )
            assertTilstand(a1, TIL_INFOTRYGD, 3)
            assertTilstand(a2, TIL_INFOTRYGD, 3)
            assertTilstand(a3, TIL_INFOTRYGD, 2)

            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a3
            )
            assertTilstand(a1, TIL_INFOTRYGD, 3)
            assertTilstand(a2, TIL_INFOTRYGD, 3)
            assertTilstand(a3, TIL_INFOTRYGD, 2)
        }

    @Test
    fun `forlenger forkastet periode med flere arbeidsgivere hvor alle arbeidsgivernes perioder blir forlenget 2`() =
        Toggles.FlereArbeidsgivereFørstegangsbehandling.enable {
            val periode = 27.januar(2021) til 31.januar(2021)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

            val inntektshistorikk = listOf(
                Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
                Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
            )

            val utbetalinger = arrayOf(
                Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
                Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
            )

            håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
            håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

            //Her starter forlengelsen
            val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
            håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
            håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
            håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
            håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
            håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
            håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)

            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
            assertTilstand(a3, TIL_INFOTRYGD)

            //Her starter forlengelsen av forlengelsen
            val forlengelsesforlengelseperiode = 11.februar(2021) til 20.februar(2021)
            håndterSykmelding(
                Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a1
            )
            håndterSykmelding(
                Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a2
            )
            håndterSykmelding(
                Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a3
            )
            assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP, 3)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP, 3)
            assertTilstand(a3, MOTTATT_SYKMELDING_FERDIG_GAP, 2)

            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a1
            )
            assertTilstand(a1, TIL_INFOTRYGD, 3)
            assertTilstand(a2, TIL_INFOTRYGD, 3)
            assertTilstand(a3, TIL_INFOTRYGD, 2)

            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a2
            )
            assertTilstand(a1, TIL_INFOTRYGD, 3)
            assertTilstand(a2, TIL_INFOTRYGD, 3)
            assertTilstand(a3, TIL_INFOTRYGD, 2)

            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent),
                orgnummer = a3
            )
            assertTilstand(a1, TIL_INFOTRYGD, 3)
            assertTilstand(a2, TIL_INFOTRYGD, 3)
            assertTilstand(a3, TIL_INFOTRYGD, 2)
        }

    @Test
    fun `Håndterer søknader i ikke-kronologisk rekkefølge for flere arbeidsgivere`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 1)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 1)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_SIMULERING, 1)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_SIMULERING, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVSLUTTET, 2)

        assertAlleBehovBesvart()
    }

    @Test
    fun `vedtaksperioder i AVVENTER_ARBEIDSGIVERE får også utbetalingstidslinje`() {
        val periode = 1.februar til 28.februar
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, 25.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, 25.februar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode(orgnummer = a1),
            Utbetalingsperiode(a1, 1.januar til 31.januar, 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 1.januar til 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.januar, INNTEKT, true),
                Inntektsopplysning(a2, 1.januar, INNTEKT, true)
            ),
            orgnummer = a1
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode(orgnummer = a2),
            Utbetalingsperiode(a1, 1.januar til 31.januar, 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 1.januar til 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.januar, INNTEKT, true),
                Inntektsopplysning(a2, 1.januar, INNTEKT, true)
            ),
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode(orgnummer = a1), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(orgnummer = a2), orgnummer = a2
        )
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 1)
        assertTilstand(a2, AVVENTER_SIMULERING, 1)

        //Utbetalingstidslinjen blir like lang som den som går til AvventerGodkjenning
        assertEquals(25, a1.inspektør.utbetalingstidslinjer(1.vedtaksperiode(a1)).size)

        assertEquals(25, a2.inspektør.utbetalingstidslinjer(1.vedtaksperiode(a2)).size)
    }

    @Test
    fun `Bygger ikke utbetalingstidlinjer for arbeidsgivere med kun forkastede perioder`() {
        val periode = 1.januar til 31.januar
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterPåminnelse(1.vedtaksperiode(orgnummer = a1), AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, 1.januar(2017).atStartOfDay(), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode(orgnummer = a2),
            Utbetalingsperiode(a2, 1.januar til 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(Inntektsopplysning(a2, 1.januar, INNTEKT, true)),
            orgnummer = a2
        )
        håndterYtelser(1.vedtaksperiode(orgnummer = a2), orgnummer = a2)
    }

    @Test
    fun `flere arbeidsgivere med inntekter på forskjellige skjæringstidspunkt skal til infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        var infotrygdPerioder = arrayOf(
            Utbetalingsperiode(a1, 1.januar til 31.januar, 100.prosent, INNTEKT)
        )
        val inntektshistorikk = mutableListOf(Inntektsopplysning(a1, 1.januar, INNTEKT, true))
        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *infotrygdPerioder, inntektshistorikk = inntektshistorikk, orgnummer = a1, besvart = LocalDateTime.now().minusHours(24))
        håndterYtelser(1.vedtaksperiode(a1), *infotrygdPerioder, inntektshistorikk = inntektshistorikk, orgnummer = a1, besvart = LocalDateTime.now().minusHours(24))
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        infotrygdPerioder += Utbetalingsperiode(a2, 1.mars til 31.mars, 100.prosent, INNTEKT)
        inntektshistorikk += Inntektsopplysning(a2, 1.mars, INNTEKT, true)

        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.april, 30.april, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *infotrygdPerioder, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertForkastetPeriodeTilstander(
            a2,
            1.vedtaksperiode(a2),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Kaster ut perioder hvis ikke alle forlengelser fra IT har fått sykmeldinger`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        håndterUtbetalingshistorikk(
            1.vedtaksperiode(orgnummer = a1),
            Utbetalingsperiode(a1, 1.januar til 31.januar, 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 1.januar til 31.januar, 100.prosent, INNTEKT),
            inntektshistorikk = listOf(
                Inntektsopplysning(a1, 1.januar, INNTEKT, true),
                Inntektsopplysning(a2, 1.januar, INNTEKT, true)
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode(orgnummer = a1), orgnummer = a1)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode(a1),
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Orgnummer, vedtaksperiodeIder og vedtaksperiodetyper ligger på godkjenningsbehov`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)

        val aktiveVedtaksperioder = inspektør.sisteBehov(Behovtype.Godkjenning).detaljer()["aktiveVedtaksperioder"].castAsList<Map<Any, Any>>()
        assertTrue(
            aktiveVedtaksperioder.containsAll(
                listOf(
                    mapOf("orgnummer" to a1, "vedtaksperiodeId" to 1.vedtaksperiode(a1).toString(), "periodetype" to Periodetype.OVERGANG_FRA_IT.name),
                    mapOf("orgnummer" to a2, "vedtaksperiodeId" to 1.vedtaksperiode(a2).toString(), "periodetype" to Periodetype.OVERGANG_FRA_IT.name)
                )
            )
        )
    }

    @Test
    fun `forkast alle aktive vedtaksperioder for alle arbeidsgivere dersom en periode til godkjenning avises`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), false, a1)

        assertSisteForkastetPeriodeTilstand(a1, 1.vedtaksperiode(a1), TIL_INFOTRYGD)
        assertSisteForkastetPeriodeTilstand(a2, 1.vedtaksperiode(a2), TIL_INFOTRYGD)

    }

    @Test
    fun `forkast ettergølgende vedtaksperioder for alle arbeidsgivere dersom en periode til godkjenning avises`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), true, a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 15.februar, 100.prosent), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), false, a2)

        assertSisteForkastetPeriodeTilstand(a1, 1.vedtaksperiode(a1), AVSLUTTET)
        assertSisteForkastetPeriodeTilstand(a2, 1.vedtaksperiode(a2), TIL_INFOTRYGD)
        assertSisteForkastetPeriodeTilstand(a1, 2.vedtaksperiode(a1), TIL_INFOTRYGD)
    }

    @Test
    fun `forkast periode som overlapper med annen arbeidsgiver i infotrygd`() {
        val periode = 22.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)

        assertSisteForkastetPeriodeTilstand(a1, 1.vedtaksperiode(a1), TIL_INFOTRYGD)
    }

    @Test
    fun `Sykefravær rett etter periode i infotrygd på annen arbeidsgiver forkastes`() {
        val periode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a2, 20.januar(2021) til 31.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterInntektsmelding(listOf(1.februar(2021) til 16.februar(2021)), førsteFraværsdag = 1.februar(2021), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.februar(2020) til 1.januar(2021) inntekter {
                    a1 inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)

        assertSisteForkastetPeriodeTilstand(a1, 1.vedtaksperiode(a1), TIL_INFOTRYGD)
    }

    @Test
    fun `Tillater førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding er på samme dato`() {
        Toggles.FlereArbeidsgivereFørstegangsbehandling.enable {
            val periode = 1.januar(2021) til 31.januar(2021)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

            håndterUtbetalingshistorikk(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
            assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

            håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a1)
            assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
            assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

            håndterInntektsmelding(listOf(1.januar(2021) til 16.januar(2021)), førsteFraværsdag = 1.januar(2021), orgnummer = a2)
            assertTilstand(a1, AVVENTER_HISTORIKK)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

            håndterYtelser(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            assertTilstand(a1, AVVENTER_VILKÅRSPRØVING)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

            håndterVilkårsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt INNTEKT
                    }
                }
            ))
            assertTilstand(a1, AVVENTER_HISTORIKK)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
            håndterYtelser(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            assertTilstand(a1, AVVENTER_SIMULERING)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            assertTilstand(a1, AVVENTER_GODKJENNING)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), true, a1)
            assertTilstand(a1, TIL_UTBETALING)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
            assertTilstand(a1, AVSLUTTET)
            assertTilstand(a2, AVVENTER_HISTORIKK)

            håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)
            assertTilstand(a1, AVSLUTTET)
            assertTilstand(a2, AVVENTER_SIMULERING)
            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            assertTilstand(a1, AVSLUTTET)
            assertTilstand(a2, AVVENTER_GODKJENNING)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), true, a2)
            assertTilstand(a1, AVSLUTTET)
            assertTilstand(a2, TIL_UTBETALING)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)
            assertTilstand(a1, AVSLUTTET)
            assertTilstand(a2, AVSLUTTET)
        }
    }

    @Test
    fun `Tillater ikke førstegangsbehandling av flere arbeidsgivere der inntekt i inntektsmelding ikke er på samme dato`() {
        Toggles.FlereArbeidsgivereFørstegangsbehandling.enable {
            val periode = 1.januar(2021) til 31.januar(2021)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

            håndterUtbetalingshistorikk(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
            assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
                førsteFraværsdag = 1.januar(2021),
                orgnummer = a1
            )
            assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
            assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 3.januar(2021), 6.januar(2021) til 18.januar(2021)),
                førsteFraværsdag = 6.januar(2021),
                refusjon = Triple(null, 1000.månedlig, emptyList()),
                orgnummer = a2
            )
            assertTilstand(a1, AVVENTER_HISTORIKK)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
            håndterYtelser(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            håndterVilkårsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt 1000.månedlig
                    }
                }
            ))
            håndterYtelser(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            assertTilstand(a1, TIL_INFOTRYGD)
            assertTilstand(a2, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Tillater ikke forlengelse av flere arbeidsgivere hvis sykmelding og søknad for en arbeidsgiver kommer før sykmelding for den andre arbeidsgiveren`() {
        Toggles.FlereArbeidsgivereFørstegangsbehandling.enable {
            val periode = 1.januar(2021) til 31.januar(2021)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterUtbetalingshistorikk(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
                førsteFraværsdag = 1.januar(2021),
                orgnummer = a1
            )
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
                førsteFraværsdag = 1.januar(2021),
                orgnummer = a2
            )
            håndterYtelser(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            håndterVilkårsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt INNTEKT
                    }
                }
            ))
            håndterYtelser(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), true, a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)
            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), true, a2)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

            //Forlengelsen starter her
            val forlengelseperiode = 1.februar(2021) til 28.februar(2021)
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
            assertTilstand(a1, TIL_INFOTRYGD, 2)
        }
    }

    @Test
    fun `Tillater forlengelse av flere arbeidsgivere`() {
        Toggles.FlereArbeidsgivereFørstegangsbehandling.enable {
            val periode = 1.januar(2021) til 31.januar(2021)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterUtbetalingshistorikk(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
                førsteFraværsdag = 1.januar(2021),
                orgnummer = a1
            )
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
                førsteFraværsdag = 1.januar(2021),
                orgnummer = a2
            )
            håndterYtelser(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            håndterVilkårsgrunnlag(1.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt INNTEKT
                    }
                }
            ))
            håndterYtelser(1.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), true, a1)
            håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
            håndterYtelser(1.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)
            håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), true, a2)
            håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

            //Forlengelsen starter her
            val forlengelseperiode = 1.februar(2021) til 28.februar(2021)
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
            assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

            håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
            assertTilstand(a1, AVVENTER_HISTORIKK, 2)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

            håndterYtelser(2.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
            assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

            håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
            assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
            assertTilstand(a2, AVVENTER_HISTORIKK, 2)

            håndterYtelser(2.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)
            assertTilstand(a1, AVVENTER_HISTORIKK, 2)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

            håndterYtelser(2.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
            assertTilstand(a1, AVVENTER_SIMULERING, 2)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

            håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
            assertTilstand(a1, AVVENTER_GODKJENNING, 2)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

            håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), true, a1)
            assertTilstand(a1, TIL_UTBETALING, 2)
            assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)
            håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
            assertTilstand(a1, AVSLUTTET, 2)
            assertTilstand(a2, AVVENTER_HISTORIKK, 2)

            håndterYtelser(2.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)
            assertTilstand(a1, AVSLUTTET, 2)
            assertTilstand(a2, AVVENTER_SIMULERING, 2)
            håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
            assertTilstand(a1, AVSLUTTET, 2)
            assertTilstand(a2, AVVENTER_GODKJENNING, 2)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), true, a2)
            assertTilstand(a1, AVSLUTTET, 2)
            assertTilstand(a2, TIL_UTBETALING, 2)
            håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)
            assertTilstand(a1, AVSLUTTET, 2)
            assertTilstand(a2, AVSLUTTET, 2)
        }
    }

    @Test
    fun `Periode som forlenger annen arbeidsgiver, men ikke seg selv, forkastes`() {
        val periode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 1.januar(2021), INNTEKT, true)
        )
        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 1.januar(2021) til 31.januar(2021), 100.prosent, INNTEKT)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), true, a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        val periode2 = 1.mars(2021) til 31.mars(2021)
        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.april(2021), 30.april(2021), 100.prosent), orgnummer = a2)
        assertTilstand(a1, TIL_INFOTRYGD, 2)
        assertTilstand(a2, TIL_INFOTRYGD, 1)
    }

    @Test
    fun `Tillater førstegangsbehandling hos annen arbeidsgiver, hvis gap til foregående`() = Toggles.FlereArbeidsgivereFørstegangsbehandling.enable {
        val periode = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 1.januar(2021), INNTEKT, true)
        )
        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 1.januar(2021) til 31.januar(2021), 100.prosent, INNTEKT)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), true, a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        val periode2 = 1.mars(2021) til 31.mars(2021)
        val a2Periode = 2.april(2021) til 30.april(2021)
        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(a2Periode.start, a2Periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP, 1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a1), orgnummer = a1)
        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), true, a1)
        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(a2Periode.start, a2Periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(2.april(2021) til 17.april(2021)),
            førsteFraværsdag = 2.april(2021),
            orgnummer = a2
        )

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterVilkårsgrunnlag(1.vedtaksperiode(a2), orgnummer = a2, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.april(2020) til 1.mars(2021) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INGEN
                }
            }
        ))
        assertWarnings(a2.inspektør)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), true, a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)
    }

    @Test
    fun `tillater to arbeidsgivere med korte perioder, og forlengelse av disse`() = Toggles.FlereArbeidsgivereFørstegangsbehandling.enable {
        val periode = 1.januar(2021) til 14.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknadArbeidsgiver(Søknadsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknadArbeidsgiver(Søknadsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        val forlengelseperiode = 15.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a1
        )

        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE, 2)

        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a2
        )

        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterYtelser(2.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)

        håndterVilkårsgrunnlag(2.vedtaksperiode(a1), orgnummer = a1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.januar(2020) til 1.desember(2020) inntekter {
                    a1 inntekt INNTEKT
                    a2 inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(2.vedtaksperiode(a1), inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), true, a1)
        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)

        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode(a2), inntektshistorikk = emptyList(), orgnummer = a2)
        håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), true, a2)
        håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)

        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVSLUTTET, 2)
    }

    @Test
    fun `tillater ikke overlappende periode hos annen arbeidsgiver - begge sykmeldinger først`() = Toggles.FlereArbeidsgivereFørstegangsbehandling.disable {
        val periode = 1.januar(2021) til 14.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknadArbeidsgiver(Søknadsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, TIL_INFOTRYGD)
        assertTilstand(a2, TIL_INFOTRYGD)
    }

    @Test
    fun `tillater ikke overlappende periode hos annen arbeidsgiver - sykmelding og søknad fra én arbeidsgiver først`() {
        Toggles.FlereArbeidsgivereFørstegangsbehandling.disable {
            val periode = 1.januar(2021) til 14.januar(2021)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSøknadArbeidsgiver(Søknadsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknadArbeidsgiver(Søknadsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            assertTilstand(a1, AVSLUTTET_UTEN_UTBETALING)
            assertTilstand(a2, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `tillater ikke overlappende periode hos annen arbeidsgiver - begge har uferdig periode foran gap`() {
        Toggles.FlereArbeidsgivereFørstegangsbehandling.disable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
            val periode = 1.mars til 14.mars
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSøknadArbeidsgiver(Søknadsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknadArbeidsgiver(Søknadsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

            assertForkastetPeriodeTilstander(a1, 1.vedtaksperiode(a1), START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(a1, 2.vedtaksperiode(a1), START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVSLUTTET_UTEN_UTBETALING)
            assertForkastetPeriodeTilstander(a2, 1.vedtaksperiode(a2), START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(a2, 2.vedtaksperiode(a2), START, MOTTATT_SYKMELDING_UFERDIG_GAP, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `tillater ikke overlappende periode hos annen arbeidsgiver - begge har uferdig periode foran gap og mottar sykmelding samtidig`() {
        Toggles.FlereArbeidsgivereFørstegangsbehandling.disable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
            val periode = 1.mars til 14.mars
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
            håndterSøknadArbeidsgiver(Søknadsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

            assertForkastetPeriodeTilstander(a1, 1.vedtaksperiode(a1), START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(a1, 2.vedtaksperiode(a1), START, MOTTATT_SYKMELDING_UFERDIG_GAP, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(a2, 1.vedtaksperiode(a2), START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(a2, 2.vedtaksperiode(a2), START, MOTTATT_SYKMELDING_UFERDIG_GAP, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `går ikke i loop mellom AVVENTER_ARBEIDSGIVERE og AVVENTER_HISTORIKK`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Inntektsopplysning(a1, 20.januar(2021), INNTEKT, true),
            Inntektsopplysning(a2, 20.januar(2021), INNTEKT, true)
        )

        val utbetalinger = arrayOf(
            Utbetalingsperiode(a1, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT),
            Utbetalingsperiode(a2, 20.januar(2021) til 26.januar(2021), 100.prosent, INNTEKT)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterPåminnelse(1.vedtaksperiode(a1), orgnummer = a1, påminnetTilstand = AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertInntektskilde(a1, EN_ARBEIDSGIVER)
        assertInntektskilde(a2, EN_ARBEIDSGIVER)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_SIMULERING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)
        assertEquals("FLERE_ARBEIDSGIVERE", a1.inspektør.sisteBehov(1.vedtaksperiode(a1)).detaljer()["inntektskilde"])

        håndterPåminnelse(1.vedtaksperiode(a2), orgnummer = a2, påminnetTilstand = AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, TIL_UTBETALING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_HISTORIKK)

        håndterYtelser(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_SIMULERING)

        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVVENTER_GODKJENNING)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)
        assertEquals("FLERE_ARBEIDSGIVERE", a2.inspektør.sisteBehov(1.vedtaksperiode(a2)).detaljer()["inntektskilde"])

        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, TIL_UTBETALING)

        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET)
        assertTilstand(a2, AVSLUTTET)
    }


    @Test
    fun `medlemskap ikke oppfyllt i vilkårsgrunnlag, avviser perioden riktig for begge arbeidsgivere`() {
        val periode = Periode(1.januar, 31.januar)
        nyPeriode(periode, a1)
        nyPeriode(periode, a2)

        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
                førsteFraværsdag = periode.start,
                orgnummer = a1
            )
        )

        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
                førsteFraværsdag = periode.start,
                orgnummer = a2
            )
        )

        historikk(a1)
        person.håndter(
            vilkårsgrunnlag(
                a1.id(0), orgnummer = a1, inntektsvurdering = Inntektsvurdering(
                    inntekter = inntektperioder {
                        inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                        1.januar(2017) til 1.desember(2017) inntekter {
                            a1 inntekt INNTEKT
                        }
                        1.januar(2017) til 1.desember(2017) inntekter {
                            a2 inntekt INNTEKT
                        }
                    },
                ), medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Nei
            )
        )
        historikk(a1)

        assertTilstand(a1, AVVENTER_GODKJENNING)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        a1.inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            TestTidslinjeInspektør(it.utbetalingstidslinjer(1.vedtaksperiode(a1))).also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag::class])
                assertEquals(4, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.NavHelgDag::class])
                assertEquals(11, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.AvvistDag::class])
            }
        }
        a2.inspektør.also {
            assertTrue(it.personLogg.hasWarningsOrWorse())
            TestTidslinjeInspektør(it.utbetalingstidslinjer(1.vedtaksperiode(a2))).also { tidslinjeInspektør ->
                assertEquals(16, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag::class])
                assertEquals(4, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.NavHelgDag::class])
                assertEquals(11, tidslinjeInspektør.dagtelling[Utbetalingstidslinje.Utbetalingsdag.AvvistDag::class])
            }
        }

    }

    private fun assertTilstand(
        orgnummer: String,
        tilstand: TilstandType,
        vedtaksperiodeIndeks: Int = 1
    ) {
        assertEquals(tilstand, inspektør(orgnummer).sisteTilstand(vedtaksperiodeIndeks.vedtaksperiode(orgnummer))) {
            inspektør.personLogg.toString()
        }
    }

    private fun assertInntektskilde(
        orgnummer: String,
        inntektskilde: Inntektskilde,
        vedtaksperiodeIndeks: Int = 1
    ) {
        assertEquals(inntektskilde, orgnummer.inspektør.inntektskilde(vedtaksperiodeIndeks.vedtaksperiode(orgnummer)))
    }

    private fun prosessperiode(periode: Periode, orgnummer: String, sykedagstelling: Int = 0) {
        gapPeriode(periode, orgnummer, sykedagstelling)
        historikk(orgnummer, sykedagstelling)
        betale(orgnummer)
    }

    private fun gapPeriode(periode: Periode, orgnummer: String, sykedagstelling: Int = 0) {
        nyPeriode(periode, orgnummer)
        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
                førsteFraværsdag = periode.start,
                orgnummer = orgnummer
            )
        )
        historikk(orgnummer, sykedagstelling)
        person.håndter(vilkårsgrunnlag(orgnummer.id(0), orgnummer = orgnummer, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                1.januar(2017) til 1.desember(2017) inntekter {
                    orgnummer inntekt INNTEKT
                }
            }
        )))
    }

    private fun nyPeriode(periode: Periode, orgnummer: String) {
        person.håndter(
            sykmelding(
                UUID.randomUUID(),
                Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent),
                orgnummer = orgnummer
            )
        )
        person.håndter(
            søknad(
                UUID.randomUUID(),
                Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent),
                orgnummer = orgnummer
            )
        )
    }

    private fun historikk(orgnummer: String, sykedagstelling: Int = 0) {
        person.håndter(
            ytelser(
                orgnummer.id(0),
                utbetalinger = utbetalinger(sykedagstelling, orgnummer),
                orgnummer = orgnummer
            )
        )
    }

    private fun betale(orgnummer: String) {
        person.håndter(simulering(orgnummer.id(0), orgnummer = orgnummer))
        person.håndter(
            utbetalingsgodkjenning(
                orgnummer.id(0),
                true,
                orgnummer = orgnummer,
                automatiskBehandling = false
            )
        )
        person.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018,
                orgnummer = orgnummer,
                fagsystemId = orgnummer.inspektør.fagsystemId(orgnummer.id(0)),
                utbetalingId = hendelselogg.behov().first { it.type == Behovtype.Utbetaling }.kontekst().getValue("utbetalingId"),
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        person.håndter(
            utbetaling(
                orgnummer.inspektør.fagsystemId(orgnummer.id(0)),
                status = AKSEPTERT,
                orgnummer = orgnummer
            )
        )
    }

    private fun utbetalinger(dagTeller: Int, orgnummer: String): List<Utbetalingsperiode> {
        if (dagTeller == 0) return emptyList()
        val førsteDato = 2.desember(2017).minusDays(
            (
                (dagTeller / 5 * 7) + dagTeller % 5
                ).toLong()
        )
        return listOf(
            Utbetalingsperiode(
                orgnummer, førsteDato til 1.desember(2017), 100.prosent, 100.daglig
            )
        )
    }
}
