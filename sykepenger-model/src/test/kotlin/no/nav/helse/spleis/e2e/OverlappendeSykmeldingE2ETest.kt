package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.TilstandType.*
import no.nav.helse.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OverlappendeSykmeldingE2ETest : AbstractEndToEndTest() {

    @BeforeEach
    fun setup() {
        Toggle.OverlappendeSykmelding.enable()
    }

    @AfterEach
    fun teardown() {
        Toggle.OverlappendeSykmelding.pop()
    }

    @Test
    fun `sykmelding overlapper i starten av eksisterende periode (out of order)`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.januar, 100.prosent))
        assertEquals(10.januar til 25.januar, inspektør.periode(1.vedtaksperiode))
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = {
                assertEquals(1, inspektør.vedtaksperiodeTeller)
                assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
            },
            ønsket = {
                assertEquals(2, inspektør.vedtaksperiodeTeller)
                assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
                assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
                assertEquals(3.januar til 9.januar, inspektør.periode(2.vedtaksperiode))
            }
        )
    }

    @Test
    fun `sykmelding overlapper på slutten av eksisterende periode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 100.prosent))
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = {
                assertEquals(1, inspektør.vedtaksperiodeTeller)
                assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
                assertEquals(3.januar til 15.januar, inspektør.periode(1.vedtaksperiode))
            },
            ønsket = {
                assertEquals(2, inspektør.vedtaksperiodeTeller)
                assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
                assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
                assertEquals(3.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
                assertEquals(10.januar til 25.januar, inspektør.periode(2.vedtaksperiode))
            }
        )
    }

    @Test
    fun `sykmelding overlapper på slutten av eksisterende periode - sykmelding skrevet samme dag - out of order`() {
        val sykmeldingSkrevet = 3.januar.atStartOfDay()
        håndterSykmelding(Sykmeldingsperiode(3.januar, 25.januar, 100.prosent), sykmeldingSkrevet = sykmeldingSkrevet)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.januar, 100.prosent), sykmeldingSkrevet = sykmeldingSkrevet)
        assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = {
                assertEquals(1, inspektør.vedtaksperiodeTeller)
            },
            ønsket = {
                assertEquals(2, inspektør.vedtaksperiodeTeller)
                assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
            }
        )
    }

    @Test
    fun `sykmelding overlapper på slutten av eksisterende periode - sykmelding skrevet samme dag`() {
        val sykmeldingSkrevet = 3.januar.atStartOfDay()
        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.januar, 100.prosent), sykmeldingSkrevet = sykmeldingSkrevet)
        håndterSykmelding(Sykmeldingsperiode(3.januar, 25.januar, 100.prosent), sykmeldingSkrevet = sykmeldingSkrevet)
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = {
                assertEquals(1, inspektør.vedtaksperiodeTeller)
                assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
            },
            ønsket = {
                assertEquals(2, inspektør.vedtaksperiodeTeller)
                assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
                assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
            }
        )
    }

    @Test
    fun `sykmelding overlapper inni eksisterende`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.januar, 12.januar, 90.prosent))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        inspektør.sykdomstidslinje.inspektør.also { sykdomstidslinjeInspektør ->
            val litenPeriode = 10.januar til 12.januar
            assertTrue((3.januar til 15.januar).filterNot { it in litenPeriode }.all { sykdomstidslinjeInspektør.grader[it] == 100 })
            assertTrue(litenPeriode.all { sykdomstidslinjeInspektør.grader[it] == 90 })
        }
    }

    @Test
    fun `sykmelding strekker seg både lengre tilbake og lengre frem i tid`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 12.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.januar, 90.prosent))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        inspektør.sykdomstidslinje.inspektør.also { sykdomstidslinjeInspektør ->
            assertTrue(sykdomstidslinjeInspektør.grader.all { it.value == 90 })
        }
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = { assertEquals(4.januar til 12.januar, inspektør.periode(1.vedtaksperiode)) },
            ønsket = { assertEquals(3.januar til 15.januar, inspektør.periode(1.vedtaksperiode)) }
        )
    }

    @Test
    fun `sykmelding overlapper med flere`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(7.januar, 8.januar, 90.prosent))
        håndterSykmelding(Sykmeldingsperiode(3.januar, 8.januar, 90.prosent))
        assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = { assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode)) },
            ønsket = { assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode)) }
        )
    }

    @Test
    fun `sykmelding overlapper med to perioder - lager forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(7.januar, 8.januar, 90.prosent))
        håndterSykmelding(Sykmeldingsperiode(4.januar, 8.januar, 90.prosent))
        assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = { assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode)) },
            ønsket = { assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode)) }
        )
    }

    @Test
    fun `sykmelding overlapper i begynnelsen - lager forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(7.januar, 8.januar, 90.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.januar, 8.januar, 90.prosent))
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = {
                assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
                assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
            },
            ønsket = {
                assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode))
                assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
            }
        )
    }

    @Test
    fun `sykmelding overlapper i slutten - lager forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 4.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(7.januar, 8.januar, 90.prosent))
        håndterSykmelding(Sykmeldingsperiode(4.januar, 6.januar, 90.prosent))
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertFalse(inspektør.periodeErForkastet(2.vedtaksperiode))
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = { assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode)) },
            ønsket = { assertFalse(inspektør.periodeErForkastet(1.vedtaksperiode)) }
        )
    }

    @Test
    fun `nyere sykmelding overskriver gammel`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.januar, 100.prosent), sykmeldingSkrevet = 3.januar.atStartOfDay())
        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 50.prosent), sykmeldingSkrevet = 10.januar.atStartOfDay())
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = { assertTrue(inspektør.sykdomstidslinje.inspektør.grader.isEmpty()) },
            ønsket = {
                inspektør.sykdomstidslinje.inspektør.also { sykdomstidslinjeInspektør ->
                    assertTrue((3.januar til 9.januar).all { sykdomstidslinjeInspektør.grader[it] == 100 })
                    assertTrue((10.januar til 25.januar).all { sykdomstidslinjeInspektør.grader[it] == 50 })
                }
            }
        )
    }

    @Test
    fun `søknad for eldre sykmeldinger overskriver ikke nyere sykmelding`() {
        val sykmelding1Skrevet = 3.januar.atStartOfDay()
        val sykmelding2Skrevet = 10.januar.atStartOfDay()
        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.januar, 100.prosent), mottatt = sykmelding1Skrevet)
        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 50.prosent), mottatt = sykmelding2Skrevet)
        håndterSøknad(Sykdom(3.januar, 15.januar, 100.prosent), sykmeldingSkrevet = sykmelding1Skrevet)
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = {
                assertEquals(1, inspektør.vedtaksperiodeTeller)
                assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
                assertEquals(3.januar til 15.januar, inspektør.periode(1.vedtaksperiode))
                assertTrue(inspektør.sykdomstidslinje.inspektør.grader.isEmpty())
            },
            ønsket = {
                assertEquals(2, inspektør.vedtaksperiodeTeller)
                assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP)
                assertEquals(3.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
                assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
                assertEquals(10.januar til 25.januar, inspektør.periode(2.vedtaksperiode))
                inspektør.sykdomstidslinje.inspektør.also { sykdomstidslinjeInspektør ->
                    assertTrue((3.januar til 9.januar).all { sykdomstidslinjeInspektør.grader[it] == 100 })
                    assertTrue((10.januar til 25.januar).all { sykdomstidslinjeInspektør.grader[it] == 50 })
                }
            }
        )
    }

    @Test
    fun `søknad for nyere sykmelding overskriver eldre sykmelding`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 50.prosent))
        håndterSøknad(Sykdom(10.januar, 25.januar, 40.prosent))
        assertForventetFeil(
            forklaring = "Avventer støtte for overlappende sykmeldinger",
            nå = {
                assertEquals(1, inspektør.vedtaksperiodeTeller)
                assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, TIL_INFOTRYGD)
                assertEquals(3.januar til 15.januar, inspektør.periode(1.vedtaksperiode))
                assertTrue(inspektør.sykdomstidslinje.inspektør.grader.isEmpty())
            },
            ønsket = {
                assertEquals(2, inspektør.vedtaksperiodeTeller)
                assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
                assertEquals(3.januar til 9.januar, inspektør.periode(1.vedtaksperiode))
                assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE)
                assertEquals(10.januar til 25.januar, inspektør.periode(2.vedtaksperiode))
                inspektør.sykdomstidslinje.inspektør.also { sykdomstidslinjeInspektør ->
                    assertTrue((3.januar til 9.januar).all { sykdomstidslinjeInspektør.grader[it] == 100 })
                    assertTrue((10.januar til 25.januar).all { sykdomstidslinjeInspektør.grader[it] == 40 })
                }
            }
        )
    }
}
