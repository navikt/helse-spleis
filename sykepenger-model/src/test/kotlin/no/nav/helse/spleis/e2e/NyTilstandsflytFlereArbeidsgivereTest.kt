package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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

    @Test
    fun `drawio -- PERIODE HOS AG1 STREKKER SEG OVER TO PERIODER HOS AG2 - Må vente på alle IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(20.januar, 30.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(2.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(20.januar, 30.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1)
        håndterInntektsmelding(listOf(2.januar til 17.januar), førsteFraværsdag = 2.januar, orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        håndterInntektsmelding(listOf(2.januar til 17.januar), førsteFraværsdag = 20.januar, orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)

        utbetalPeriode(1.vedtaksperiode, orgnummer = a2, 1.januar)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)


        utbetalPeriodeEtterVilkårsprøving(2.vedtaksperiode, orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    @Test
    fun `Burde slippe å vente på inntektsmelding fra forlengelsen for å gå videre med førstegangsbehandling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, orgnummer = a2)

        assertForventetFeil(
            forklaring = "Nå venter vi på innteksmeldingen til forlengelsen før vi behandler førstegangsbehandlingen",
            nå = {
                assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)
                assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)
                assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

            },
            ønsket = {
                assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)
                assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
                assertTilstand(2.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)
            }
        )
    }

    @Test
    fun `drawio -- Må vente på alle IM (forlengelse)`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(18.januar, 17.februar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(18.januar, 17.februar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)

        håndterSøknad(Sykdom(18.januar, 17.februar, 100.prosent), orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)

        håndterSøknad(Sykdom(18.januar, 17.februar, 100.prosent), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        håndterInntektsmelding(listOf(18.januar til 2.februar), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)
    }

    @Test
    fun `drawio -- Må vente på alle IM (gap)`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(25.januar, 17.februar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(25.januar, 17.februar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent), orgnummer = a1)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        assertForventetFeil(
            forklaring = "Vedtaksperioden kan gå videre til AvventerHistorikk siden vi har gap til neste periode",
            nå = { assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1) },
            ønsket = { assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1) }
        )

        håndterSøknad(Sykdom(25.januar, 17.februar, 100.prosent), orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a1)

        håndterSøknad(Sykdom(25.januar, 17.februar, 100.prosent), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)

        håndterInntektsmelding(listOf(25.januar til 9.februar), orgnummer = a1)
        håndterInntektsmelding(listOf(25.januar til 9.februar), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(2.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        utbetalPeriode(2.vedtaksperiode, orgnummer = a1, 25.januar)
        assertTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode, orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    @Test
    fun `Kort periode skal ikke blokkeres av mangelende søknad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar, 100.prosent), orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)
    }

    @Test
    fun `En arbeidsgiver uten sykdom, blir syk i forlengelsen - skal vente på inntektsmelding før den går til AvventerTidligereEllerOverlappendePerioder`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        håndterSykmelding(Sykmeldingsperiode(periode1.start, periode1.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode1.start, periode1.endInclusive, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar(2021) til 16.januar(2021)),
            førsteFraværsdag = 1.januar(2021),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2020) til 1.desember(2020) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt 1000.månedlig
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar(2021), INNTEKT.repeat(3)),
                    grunnlag(a2, 1.januar(2021), 1000.månedlig.repeat(3))
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
            )
        )
        håndterYtelser(1.vedtaksperiode, inntektshistorikk = emptyList(), orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)


        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent), orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        håndterInntektsmelding(
            listOf(1.februar(2021) til 16.februar(2021)),
            førsteFraværsdag = 1.februar(2021),
            orgnummer = a2
        )
        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)
    }

    @Disabled
    @Test
    fun `forlengelse av sykdom hos AG2 hvor sykdom startet hos AG1 en annen mnd enn skjæringstidspunkt, vedtaksperiode 2 for AG2 skal ikke vente på IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a2)

        assertTilstand(1.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)
        assertTilstand(2.vedtaksperiode, AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER, orgnummer = a2)
    }

    @Test
    fun `kort periode hos annen arbeidsgiver skal ikke blokkere videre behandling pga manglende IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
    }

    @Test
    fun `kort periode hos annen arbeidsgiver vi tidligere har utbetalt til skal ikke blokkere videre behandling pga manglende IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        utbetalPeriode(1.vedtaksperiode, a2, 1.januar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 16.mars, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), orgnummer = a1)
        håndterUtbetalingshistorikk(1.vedtaksperiode, orgnummer = a1)

        håndterSøknad(Sykdom(1.mars, 16.mars, 100.prosent), orgnummer = a2)
        håndterUtbetalingshistorikk(2.vedtaksperiode, orgnummer = a2)

        håndterInntektsmelding(listOf(1.mars til 16.mars), orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
    }

    @Test
    fun `inntektsmelding for arbeidsgiver 2 har ikke full refusjon - kan gå videre til utbetaling`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null),
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INGEN, null),
            orgnummer = a2
        )

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
    }

    @Test
    fun `en periode er fullstending om den venter på andre arbeidsgivere(speilbuilder)`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null),
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null),
            orgnummer = a2
        )

        vilkårsprøv(1.vedtaksperiode, a1, 1.januar)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val speilSnapshot = serializePersonForSpeil(person)
        assertTrue(speilSnapshot.arbeidsgivere[0].vedtaksperioder.single().fullstendig)
        assertTrue(speilSnapshot.arbeidsgivere[1].vedtaksperioder.single().fullstendig)
    }

    @Test
    fun `gjenopptaBehandling poker ikke neste arbeidsgiver til AvventerHistorikk før den blir kastet ut`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a2)

        person.invaliderAllePerioder(hendelselogg, null)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER,
            AVVENTER_HISTORIKK,
            TIL_INFOTRYGD,
            orgnummer = a1
        )
        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_TIDLIGERE_ELLER_OVERLAPPENDE_PERIODER,
            TIL_INFOTRYGD,
            orgnummer = a2
        )
    }

    @Test
    fun `bruker har satt inntektskilde til ANDRE_ARBEIDSFORHOLD hvor vi har sykmeldingsperioder for begge arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(
            Sykdom(1.januar, 31.januar, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD")),
            orgnummer = a1
        )
        håndterSøknad(
            Sykdom(1.januar, 31.januar, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD")),
            orgnummer = a2
        )

        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a2)
        assertForventetFeil(
            forklaring = "Vi må sjekke mot sykmeldingsperioder om vi forventer en søknad før vi kaster ut",
            nå = {
                assertWarning("Den sykmeldte har oppgitt å ha andre arbeidsforhold med sykmelding i søknaden.")
            },
            ønsket = {
                assertNoWarning("Den sykmeldte har oppgitt å ha andre arbeidsforhold med sykmelding i søknaden.")
            }
        )
    }

    @Test
    fun `bruker har satt inntektskilde til ANDRE_ARBEIDSFORHOLD men vi kjenner ikke til sykdom for mer enn en arbeidsgiver`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(
            Sykdom(1.januar, 31.januar, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANDRE_ARBEIDSFORHOLD")),
            orgnummer = a1
        )

        assertTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK, orgnummer = a1)
        assertWarning("Den sykmeldte har oppgitt å ha andre arbeidsforhold med sykmelding i søknaden.")
    }

    private fun utbetalPeriodeEtterVilkårsprøving(vedtaksperiode: IdInnhenter, orgnummer: String) {
        håndterYtelser(vedtaksperiode, orgnummer = orgnummer)
        håndterSimulering(vedtaksperiode, orgnummer = orgnummer)
        håndterUtbetalingsgodkjenning(vedtaksperiode, orgnummer = orgnummer)
        håndterUtbetalt(orgnummer = orgnummer)
    }

    private fun vilkårsprøv(vedtaksperiode: IdInnhenter, orgnummer: String, skjæringstidspunkt: LocalDate) {
        håndterYtelser(vedtaksperiode, orgnummer = orgnummer)
        håndterVilkårsgrunnlag(
            vedtaksperiode, orgnummer = orgnummer,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, skjæringstidspunkt, 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, skjæringstidspunkt, 31000.månedlig.repeat(12)),
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, skjæringstidspunkt, 31000.månedlig.repeat(3)),
                    grunnlag(a2, skjæringstidspunkt, 31000.månedlig.repeat(3)),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH)
            )
        )
    }

    private fun utbetalPeriode(vedtaksperiode: IdInnhenter, orgnummer: String, skjæringstidspunkt: LocalDate) {
        vilkårsprøv(vedtaksperiode, orgnummer, skjæringstidspunkt)
        utbetalPeriodeEtterVilkårsprøving(vedtaksperiode, orgnummer)
    }

}
