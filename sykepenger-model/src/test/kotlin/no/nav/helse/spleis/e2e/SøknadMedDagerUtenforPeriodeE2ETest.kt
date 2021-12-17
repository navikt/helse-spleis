package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.*
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Dag.Feriedag
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juli
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SøknadMedDagerUtenforPeriodeE2ETest: AbstractEndToEndTest() {

    @Test
    fun `søppelbøtter dersom ny søknad inneholder permisjon som overlapper med tidligere sykmelding`(){
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Permisjon(20.januar, 31.januar))
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
            TIL_INFOTRYGD
        )
    }

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
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Arbeid(20.januar, 31.januar))
        assertWarningTekst(inspektør, "Søknaden inneholder Arbeidsdager utenfor sykdomsvindu")
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
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
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP
        )
    }

    @Test
    fun `søknad med permisjon mellom to perioder bridger ikke de to periodene`() {
        nyttVedtak(1.januar, 19.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Permisjon(20.januar, 31.januar))
        assertWarningTekst(
            inspektør,
            "Søknaden inneholder Permisjonsdager utenfor sykdomsvindu",
            "Permisjon oppgitt i perioden i søknaden."
        )
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
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
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
            AVVENTER_HISTORIKK
        )
    }

    @Test
    fun `feriedager som vi allerede vet om fra forrige periode, trimme bort ferie, ingen warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(25.januar, 31.januar))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(25.januar, 31.januar))

        assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
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
        assertEquals(17.januar til 28.februar, inspektør.periode(1.vedtaksperiode) )
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
        assertEquals(17.januar til 28.februar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(2, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
    }

    @Test
    fun `feriedager som vi ikke vet om fra forrige periode, trimme bort ferie, warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(25.januar, 31.januar))

        assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertWarnings(inspektør)
    }

    @Test
    fun `feriedager som vi ikke vet om midt i forrige periode, trimme bort ferie, warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(25.januar, 30.januar))

        assertEquals(1.januar til 31.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertWarningTekst(inspektør, "Det er oppgitt ny informasjon om ferie i søknaden som det ikke har blitt opplyst om tidligere. Tidligere periode må revurderes.")
    }

    @Test
    fun `feriedager som vi ikke vet om og ikke treffer forrige periode, trimme bort ferie, ingen warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(25.januar, 31.januar))

        assertEquals(1.januar til 22.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller[Feriedag::class])
    }

    @Test
    fun `feriedager som vi ikke vet om og bridger gapet til forrige periode, trimme bort ferie, ingen warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(23.januar, 31.januar))

        assertEquals(1.januar til 22.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller[Feriedag::class])
    }

    @Test
    fun `feriedager som vi ikke vet om og treffer forrige periode, trimme bort ferie, warning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(18.januar, 31.januar))

        assertEquals(1.januar til 22.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertWarnings(inspektør)
    }

    @Test
    fun `feriedager som vi vet om og treffer forrige periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent), Ferie(18.januar, 22.januar))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(18.januar, 31.januar))

        assertEquals(1.januar til 22.januar, inspektør.periode(1.vedtaksperiode) )
        assertEquals(1.februar til 28.februar, inspektør.periode(2.vedtaksperiode) )
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_INNTEKTSMELDING_UFERDIG_GAP)
        assertEquals(5, inspektør.vedtaksperiodeSykdomstidslinje(1.vedtaksperiode).inspektør.dagteller[Feriedag::class])
        assertEquals(null, inspektør.vedtaksperiodeSykdomstidslinje(2.vedtaksperiode).inspektør.dagteller[Feriedag::class])
    }
}
