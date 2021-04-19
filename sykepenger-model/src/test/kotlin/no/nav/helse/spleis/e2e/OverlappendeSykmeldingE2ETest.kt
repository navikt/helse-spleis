package no.nav.helse.spleis.e2e

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.SykdomstidslinjeInspektør
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OverlappendeSykmeldingE2ETest : AbstractEndToEndTest() {

    @BeforeAll
    fun setup() {
        Toggles.OverlappendeSykmelding.enable()
    }

    @AfterAll
    fun teardown() {
        Toggles.OverlappendeSykmelding.disable()
    }

    @Test
    fun `overlappende sykmelding med ulik grad`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.januar, 25.januar, 50.prosent))
        assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP)
        assertTilstander(2.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE)
        assertEquals(3.januar til 15.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(16.januar til 25.januar, inspektør.periode(2.vedtaksperiode))

        SykdomstidslinjeInspektør(inspektør.sykdomstidslinje).also { sykdomstidslinjeInspektør ->
            assertTrue((3.januar til 9.januar).all { sykdomstidslinjeInspektør.grader[it] == 100 })
            assertTrue((10.januar til 25.januar).all { sykdomstidslinjeInspektør.grader[it] == 50 })
        }
    }
}
