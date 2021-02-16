package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.hendelser.Utbetalingshistorikk.Infotrygdperiode.RefusjonTilArbeidsgiver
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.person.Inntektskilde.FLERE_ARBEIDSGIVERE
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
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

    @BeforeEach
    fun setup() {
        Toggles.FlereArbeidsgivereOvergangITEnabled.enable()
    }

    @AfterEach
    fun tearDown() {
        Toggles.FlereArbeidsgivereOvergangITEnabled.pop()
    }

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
        prosessperiode(1.januar til 31.januar, a1)
        assertNoErrors(a1.inspektør)
        assertTilstand(a1, AVSLUTTET)

        prosessperiode(1.mars til 31.mars, a2)
        assertNoErrors(a2.inspektør)
        assertTilstand(a1, AVSLUTTET)
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
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertInntektskilde(a1, EN_ARBEIDSGIVER)
        assertInntektskilde(a2, EN_ARBEIDSGIVER)

        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
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

        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
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
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_HISTORIKK)
        assertInntektskilde(a1, EN_ARBEIDSGIVER)
        assertInntektskilde(a2, EN_ARBEIDSGIVER)

        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE)

        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
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

        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
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
    fun `Tillater ikke flere arbeidsgivere hvis ingen er overgang fra Infotrygd`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 25.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 25.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, TIL_INFOTRYGD)
        assertTilstand(a2, TIL_INFOTRYGD)
    }

    @Test
    fun `Tillater ikke flere arbeidsgivere hvis ikke alle er overgang fra Infotrygd`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        assertTilstand(a1, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        val inntektshistorikk = listOf(
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 25.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_HISTORIKK)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertTilstand(a1, TIL_INFOTRYGD)
        assertTilstand(a2, TIL_INFOTRYGD)
    }

    @Test
    fun `Tillater forlengelse av overgang fra infotrygd for flere arbeidsgivere - a1 kommer til AVVENTER_HISTORIKK først`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
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

        håndterYtelser(2.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, MOTTATT_SYKMELDING_FERDIG_FORLENGELSE, 2)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)
        assertInntektskilde(a1, EN_ARBEIDSGIVER, 2)
        assertInntektskilde(a2, EN_ARBEIDSGIVER, 2)

        håndterYtelser(2.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)
        assertInntektskilde(a1, FLERE_ARBEIDSGIVERE, 2)
        assertInntektskilde(a2, FLERE_ARBEIDSGIVERE, 2)

        håndterYtelser(2.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
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

        håndterYtelser(2.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
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
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
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
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
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
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(2.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
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
    fun `forlenger forkastet periode med flere arbeidsgivere`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(2.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
        assertTilstand(a3, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
        assertTilstand(a3, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        håndterUtbetalingshistorikk(1.vedtaksperiode(a3), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a3)
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
    fun `forlenger forkastet periode med flere arbeidsgivere hvor alle arbeidsgivernes perioder blir forlenget`() {
        val periode = 27.januar(2021) til 31.januar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)

        val inntektshistorikk = listOf(
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)

        //Her starter forlengelsen
        val forlengelseperiode = 1.februar(2021) til 10.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(2.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(2.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(2.vedtaksperiode(a1), orgnummer = a1)
        håndterYtelser(2.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
        assertTilstand(a3, MOTTATT_SYKMELDING_FERDIG_GAP)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(forlengelseperiode.start, forlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
        assertTilstand(a3, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)

        håndterUtbetalingshistorikk(1.vedtaksperiode(a3), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a3)
        assertTilstand(a3, TIL_INFOTRYGD)

        //Her starter forlengelsen av forlengelsen
        val forlengelsesforlengelseperiode = 11.februar(2021) til 20.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(forlengelsesforlengelseperiode.start, forlengelsesforlengelseperiode.endInclusive, 100.prosent), orgnummer = a3)
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
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a1, true),
            Utbetalingshistorikk.Inntektsopplysning(20.januar(2021), INNTEKT, a2, true)
        )

        val utbetalinger = arrayOf(
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a1),
            RefusjonTilArbeidsgiver(20.januar(2021), 26.januar(2021), INNTEKT, 100.prosent, a2)
        )

        håndterUtbetalingshistorikk(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)

        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterYtelser(1.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a1), orgnummer = a1)
        håndterUtbetalt(1.vedtaksperiode(a1), orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 1)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterYtelser(2.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_HISTORIKK, 1)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterYtelser(1.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_ARBEIDSGIVERE, 2)
        assertTilstand(a2, AVVENTER_SIMULERING, 1)
        assertTilstand(a2, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE, 2)

        håndterSimulering(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(1.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVVENTER_HISTORIKK, 2)

        håndterYtelser(2.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVVENTER_HISTORIKK, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVVENTER_ARBEIDSGIVERE, 2)

        håndterYtelser(2.vedtaksperiode(a1), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a1)
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

        håndterYtelser(2.vedtaksperiode(a2), *utbetalinger, inntektshistorikk = inntektshistorikk, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode(a2), orgnummer = a2)
        håndterUtbetalt(2.vedtaksperiode(a2), orgnummer = a2)
        assertTilstand(a1, AVSLUTTET, 1)
        assertTilstand(a1, AVSLUTTET, 2)
        assertTilstand(a2, AVSLUTTET, 1)
        assertTilstand(a2, AVSLUTTET, 2)
    }

    @Test
    @Disabled("WIP")
    fun `⸘en kul test‽`() {
        val periode = 1.januar til 31.januar
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterPåminnelse(1.vedtaksperiode(orgnummer = a1), AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP, 1.januar(2017).atStartOfDay(), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 20.februar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(
            1.vedtaksperiode(orgnummer = a2),
            RefusjonTilArbeidsgiver(1.januar, 31.januar, INNTEKT, 100.prosent, a2),
            inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(1.januar, INNTEKT, a2, true)),
            orgnummer = a2
        )
        håndterYtelser(
            1.vedtaksperiode(orgnummer = a2),
            RefusjonTilArbeidsgiver(1.januar, 31.januar, INNTEKT, 100.prosent, a2),
            inntektshistorikk = listOf(Utbetalingshistorikk.Inntektsopplysning(1.januar, INNTEKT, a2, true)),
            orgnummer = a2
        )

    }

    private fun assertTilstand(
        orgnummer: String,
        tilstand: TilstandType,
        vedtaksperiodeIndeks: Int = 1
    ) {
        assertEquals(tilstand, inspektør(orgnummer).sisteTilstand(vedtaksperiodeIndeks.vedtaksperiode(orgnummer)))
    }

    private fun assertInntektskilde(
        orgnummer: String,
        inntektskilde: Inntektskilde,
        vedtaksperiodeIndeks: Int = 1
    ) {
        assertEquals(inntektskilde, orgnummer.inspektør.inntektskilde(vedtaksperiodeIndeks.vedtaksperiode(orgnummer)))
    }

    private fun prosessperiode(periode: Periode, orgnummer: String, sykedagstelling: Int = 0) {
        gapPeriode(periode, orgnummer)
        historikk(orgnummer, sykedagstelling)
        betale(orgnummer)
    }

    private fun gapPeriode(periode: Periode, orgnummer: String) {
        nyPeriode(periode, orgnummer)
        person.håndter(
            inntektsmelding(
                UUID.randomUUID(),
                arbeidsgiverperioder = listOf(Periode(periode.start, periode.start.plusDays(15))),
                førsteFraværsdag = periode.start,
                orgnummer = orgnummer
            )
        )
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

    private fun utbetalinger(dagTeller: Int, orgnummer: String): List<RefusjonTilArbeidsgiver> {
        if (dagTeller == 0) return emptyList()
        val førsteDato = 2.desember(2017).minusDays(
            (
                (dagTeller / 5 * 7) + dagTeller % 5
                ).toLong()
        )
        return listOf(
            RefusjonTilArbeidsgiver(
                førsteDato, 1.desember(2017), 100.daglig,
                100.prosent,
                orgnummer
            )
        )
    }
}
