package no.nav.helse.spleis.e2e.revurdering

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.sykdomstidslinje.Dag.Feriedag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.Fridag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurderKorrigertSoknad::class)
internal class RevurderKorrigertSoknadTest : AbstractEndToEndTest() {

    @Test
    fun `Avsluttet periode får en korrigert søknad med perfekt overlapp - skal sette i gang en revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(17.januar, 18.januar))

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        håndterYtelser(1.vedtaksperiode)
        assertTrue(inspektør.sykdomstidslinje[17.januar] is Feriedag)
        assertTrue(inspektør.sykdomstidslinje[18.januar] is Feriedag)
        assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[17.januar] is Fridag)
        assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode)[18.januar] is Fridag)
    }

    @Test
    fun `Avsluttet periode får en overlappende søknad som strekker seg etter vedtaksperiode - skal ikke sette i gang revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(15.januar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(15.januar, 15.februar, 100.prosent))

        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertEquals(1.januar til 31.januar, inspektør.sykdomstidslinje.periode())
        assertFunksjonellFeil("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass", 1.vedtaksperiode.filter())
    }

    @Test
    fun `Avsluttet periode får en overlappende søknad som strekker seg før vedtaksperiode - skal ikke sette i gang revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(15.desember(2017), 15.januar, 100.prosent))
        håndterSøknad(Sykdom(15.desember(2017), 15.januar, 100.prosent))

        assertTilstand(1.vedtaksperiode, AVSLUTTET)
        assertEquals(1.januar til 31.januar, inspektør.sykdomstidslinje.periode())
        assertFunksjonellFeil("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass", 1.vedtaksperiode.filter())
    }

    @Test
    fun `Avsluttet periode får en overlappende søknad som starter etter og slutter før vedtaksperiode - skal sette i gang revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(17.januar, 25.januar, 50.prosent))
        håndterSøknad(Sykdom(17.januar, 25.januar, 50.prosent))

        assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertEquals(1.januar til 31.januar, inspektør.sykdomstidslinje.periode())
        håndterYtelser(1.vedtaksperiode)
        (1..16).forEach {
            assertEquals(0.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
        }
        (17..25).forEach {
            assertEquals(50.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
        }
        (26..31).forEach {
            assertEquals(100.prosent, inspektør.utbetalingstidslinjer(1.vedtaksperiode)[it.januar].økonomi.inspektør.grad)
        }
    }

    @Test
    fun `Korrigerende søknad setter ikke i gang en revurdering hvis det finnes en periode med et senere skjæringstidspunkt`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(17.januar, 18.januar))
        assertForventetFeil(
            forklaring = "ikke implementert enda",
            nå = {
                assertTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
                assertTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)

                assertEquals(21, inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[Sykedag::class])
                assertEquals(2, inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[Feriedag::class])
                assertEquals(8, inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[SykHelgedag::class])
            },
            ønsket = {
                assertTilstand(1.vedtaksperiode, AVSLUTTET)
                assertTilstand(2.vedtaksperiode, AVSLUTTET)

                assertEquals(23, inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[Sykedag::class])
                assertEquals(0, inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[Feriedag::class])
                assertEquals(8, inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[SykHelgedag::class])
            })
    }


    @Test
    fun `Avsluttet periode med en forlengelse får en korrigerende søknad - skal sette i gang revurdering`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(17.januar, 18.januar))

        assertTilstand(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        assertEquals(21, inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[Sykedag::class])
        assertEquals(2, inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[Feriedag::class])
        assertEquals(8, inspektør.sykdomstidslinje.subset(1.januar til 31.januar).inspektør.dagteller[SykHelgedag::class])

    }

    @Test
    fun `Avsluttet forlengelse får en overlappende søknad - skal sette i gang revurdering`() {

    }

    @Test
    fun `Overlappende søknad treffer førstegangsbehandling og forlengelse - skal ikke sette i gang revurdering`() {

    }

    @Test
    fun `Avsluttet forlengelse får en overlappende søknad som slutter etter - skal ikke sette i gang revurdering`() {

    }

    @Test
    fun `Avsluttet periode med forlengelse får en overlappende søknad som starter før - skal ikke sette i gang revurdering`() {

    }
}