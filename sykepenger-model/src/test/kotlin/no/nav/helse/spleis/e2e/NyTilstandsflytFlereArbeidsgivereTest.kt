package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.Toggle
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER
import no.nav.helse.person.TilstandType.START
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class NyTilstandsflytFlereArbeidsgivereTest : AbstractEndToEndTest() {
    @BeforeEach
    fun setup() {
        Toggle.NyTilstandsflyt.enable()
    }

    @AfterEach
    fun tearDown() {
        Toggle.NyTilstandsflyt.disable()
    }

    @Test
    fun `En periode i AvventerTidligerEllerOverlappendePerioder for hver arbeidsgiver - kun en periode skal gå videre`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)
    }

    @Test
    fun `To overlappende vedtaksperioder for forskjellige arbeidsgivere - skal ikke gå videre uten at begge har IM og søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER,
            orgnummer = a1
        )
    }

    @Test
    fun `To overlappende vedtaksperioder med en forlengelse - vedtaksperiode for ag2 dytter vedtaksperiode for ag1 videre`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER,
            AVVENTER_HISTORIKK,
            orgnummer = a1
        )
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)
    }

    @Test
    fun `drawio -- MANGLER SØKNAD`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        assertEquals(emptyList<Periode>(), inspektør(a1).sykmeldingsperioder())

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, a1)

        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a2)
        utbetalPeriode(1.vedtaksperiode, a1, 1.mars)
        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, a2)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, a2)
    }

    @Test
    fun `drawio -- ULIK LENGDE PÅ SYKEFRAVÆR`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(5.januar, 5.februar, 100.prosent), orgnummer = a2)
        assertEquals(listOf(1.januar til 31.januar), inspektør(a1).sykmeldingsperioder())
        assertEquals(listOf(5.januar til 5.februar), inspektør(a2).sykmeldingsperioder())

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        assertEquals(emptyList<Periode>(), inspektør(a1).sykmeldingsperioder())

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        assertEquals(listOf(1.februar til 28.februar), inspektør(a1).sykmeldingsperioder())

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, a1)

        håndterSøknad(Sykdom(5.januar, 5.februar, 100.prosent), orgnummer = a2)
        assertEquals(emptyList<Periode>(), inspektør(a2).sykmeldingsperioder())

        håndterInntektsmelding(listOf(5.januar til 20.januar), orgnummer = a2)

        utbetalPeriode(1.vedtaksperiode, a1, 1.januar)

        assertTilstand(1.vedtaksperiode, AVSLUTTET, a1)

        håndterSykmelding(Sykmeldingsperiode(6.februar, 26.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, a2)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        håndterSøknad(Sykdom(6.februar, 26.februar, 100.prosent), orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)

        utbetalPeriodeEtterVilkårsprøving(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        utbetalPeriodeEtterVilkårsprøving(2.vedtaksperiode, orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
    }

    @Test
    fun `drawio -- BURDE BLOKKERE PGA MANGLENDE IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)

        utbetalPeriode(1.vedtaksperiode, a1, 1.januar)
        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, a2)

        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    private fun utbetalPeriodeEtterVilkårsprøving(vedtaksperiode: IdInnhenter, orgnummer: String) {
        håndterYtelser(vedtaksperiode, orgnummer = orgnummer)
        håndterSimulering(vedtaksperiode, orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(vedtaksperiode, orgnummer = orgnummer)
        håndterUtbetalt(orgnummer = orgnummer)
    }

    private fun utbetalPeriode(vedtaksperiode: IdInnhenter, orgnummer: String, skjæringstidspunkt: LocalDate) {
        håndterYtelser(vedtaksperiode, orgnummer = orgnummer)
        håndterVilkårsgrunnlag(vedtaksperiode, orgnummer = orgnummer, inntektsvurdering = Inntektsvurdering(listOf(
            sammenligningsgrunnlag(a1, skjæringstidspunkt, 31000.månedlig.repeat(12)),
            sammenligningsgrunnlag(a2, skjæringstidspunkt, 31000.månedlig.repeat(12)),
        )))
        utbetalPeriodeEtterVilkårsprøving(vedtaksperiode, orgnummer)
    }

}
