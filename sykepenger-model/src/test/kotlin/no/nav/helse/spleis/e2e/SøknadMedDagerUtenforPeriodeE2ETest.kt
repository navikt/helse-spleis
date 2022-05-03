package no.nav.helse.spleis.e2e

import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.sykdomstidslinje.Dag.Feriedag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SøknadMedDagerUtenforPeriodeE2ETest: AbstractEndToEndTest() {

    @Test
    fun `eldgammel ferieperiode før sykdomsperioden klippes bort`() {
        håndterSykmelding(Sykmeldingsperiode(1.mars, 28.mars, 100.prosent))
        håndterSøknad(
            Sykdom(1.mars, 28.mars, 100.prosent),
            Ferie(1.juli(2015), 10.juli(2015)),
        )
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
    }

    @Test
    fun `søknad med arbeidsdager mellom to perioder bridger ikke de to periodene`(){
        nyttVedtak(1.januar, 19.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Arbeid(20.januar, 31.januar))
        håndterUtbetalingshistorikk(2.vedtaksperiode)
        assertWarning("Søknaden inneholder Arbeidsdager utenfor sykdomsvindu", AktivitetsloggFilter.person())
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
        )
    }

    @Test
    fun `feriedager som vi allerede vet om fra forrige periode, trimme bort ferie, ingen warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(25.januar, 31.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(25.januar, 31.januar))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertEquals(7, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller[Feriedag::class])
    }

    @Test
    fun `klipper bare ferie - ikke ferie i perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(
            Sykdom(1.februar, 28.februar, 100.prosent),
            Ferie(1.januar, 16.januar),
            Permisjon(17.januar, 25.januar),
            Ferie(26.januar, 31.januar)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertEquals(1.februar til 28.februar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
    }

    @Test
    fun `klipper bare ferie - ferie litt inn i perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(
            Sykdom(1.februar, 28.februar, 100.prosent),
            Ferie(1.januar, 16.januar),
            Permisjon(17.januar, 25.januar),
            Ferie(26.januar, 2.februar)
        )
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        assertEquals(1.februar til 28.februar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(2, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
    }

    @Test
    fun `feriedager som vi ikke vet om og ikke treffer forrige periode, trimme bort ferie, ingen warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(25.januar, 31.januar))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertEquals(1.januar til 22.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertNoWarnings()
    }

    @Test
    fun `feriedager som vi ikke vet om og bridger gapet til forrige periode, trimme bort ferie, ingen warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(23.januar, 31.januar))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertEquals(1.januar til 22.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller[Feriedag::class])
    }

    @Test
    fun `feriedager som vi vet om og treffer forrige periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent), Ferie(18.januar, 22.januar))
        håndterUtbetalingshistorikk(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(18.januar, 31.januar))
        håndterUtbetalingshistorikk(2.vedtaksperiode)

        assertEquals(1.januar til 22.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK)
        assertEquals(5, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller[Feriedag::class])
    }
}
