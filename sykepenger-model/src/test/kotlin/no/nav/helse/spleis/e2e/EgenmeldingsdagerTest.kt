package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EgenmeldingsdagerTest: AbstractDslTest() {

    @Test
    fun `egenmeldingsdager på forlengelsen av en auu`() {
        a1 {
            håndterSøknad(3.januar til 18.januar)
            nullstillTilstandsendringer()
            håndterSøknad(19.januar til 31.januar, egenmeldinger = listOf(1.januar til 3.januar))
            assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(2.vedtaksperiode))

            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `Fjerner egenmeldingsdager fra søknaden når inntektmelding kommer`() {
        a1 {
            håndterSøknad(3.januar til 17.januar, egenmeldinger = listOf(1.januar til 2.januar))

            håndterSøknad(5.mai til 9.mai, egenmeldinger = listOf(1.mai til 2.mai))
            håndterSøknad(15.mai til 19.mai)
            håndterSøknad(25.mai til 29.mai, egenmeldinger = listOf(24.mai til 24.mai))
            with(4.vedtaksperiode) {
                assertEquals(listOf(1.mai til 2.mai, 5.mai til 9.mai, 15.mai til 19.mai, 24.mai til 27.mai), inspektør.arbeidsgiverperiode(this))
            }

            håndterInntektsmelding(arbeidsgiverperioder = listOf(15.mai til 19.mai, 25.mai til 29.mai))
            with(1.vedtaksperiode) {
                assertEquals(listOf(1.januar til 2.januar), inspektør.egenmeldingsdager(this))
                assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(this))
            }
            with(2.vedtaksperiode) {
                assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(this))
            }
            with(3.vedtaksperiode) {
                assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(this))
            }
            with(4.vedtaksperiode) {
                assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(this))
                assertEquals(listOf(5.mai til 9.mai, 15.mai til 19.mai, 25.mai til 29.mai), inspektør.arbeidsgiverperiode(this))
            }
        }
    }

    @Test
    fun `Fjerner egenmeldingsdager fra søknaden når inntektmelding med tom agp håndteres`() {
        a1 {
            håndterSøknad(3.januar til 9.januar, egenmeldinger = listOf(1.januar til 2.januar))
            håndterSøknad(15.januar til 31.januar, egenmeldinger = listOf(14.januar til 14.januar))

            with(2.vedtaksperiode) {
                assertEquals(listOf(1.januar til 9.januar, 14.januar til 20.januar), inspektør.arbeidsgiverperiode(this))
            }

            håndterInntektsmelding(arbeidsgiverperioder = emptyList(), førsteFraværsdag = 15.januar)
            with(1.vedtaksperiode) {
                assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(this))
                assertEquals(listOf(3.januar til 9.januar, 15.januar til 23.januar), inspektør.arbeidsgiverperiode(this))
            }
            with(2.vedtaksperiode) {
                assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(this))
                assertEquals(listOf(3.januar til 9.januar, 15.januar til 23.januar), inspektør.arbeidsgiverperiode(this))
            }
        }
    }

    @Test
    fun `Nullstille alle egenmeldingsdager innenfor arbeidsgiverperioden`() {
        a1 {
            håndterSøknad(5.januar til 9.januar, egenmeldinger = listOf(1.januar til 2.januar))
            håndterSøknad(15.januar til 19.januar)
            håndterSøknad(25.januar til 29.januar, egenmeldinger = listOf(24.januar til 24.januar))
            nullstillTilstandsendringer()

            with(1.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING)
                assertEquals(listOf(1.januar til 2.januar), inspektør.egenmeldingsdager(this))
            }

            with(2.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING)
                assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(this))
            }

            with(3.vedtaksperiode) {
                assertTilstander(this, AVVENTER_INNTEKTSMELDING)
                assertEquals(listOf(24.januar til 24.januar), inspektør.egenmeldingsdager(this))
                assertEquals(listOf(1.januar til 2.januar, 5.januar til 9.januar, 15.januar til 19.januar, 24.januar til 27.januar), inspektør.arbeidsgiverperiode(this))
            }

            håndterPåminnelse(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, flagg = setOf("nullstillEgenmeldingsdager"))

            with(1.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
                assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(this))
            }

            with(2.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
                assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(this))
            }

            with(3.vedtaksperiode) {
                assertTilstander(this, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
                assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(this))
                assertEquals(listOf(5.januar til 9.januar, 15.januar til 19.januar, 25.januar til 29.januar), inspektør.arbeidsgiverperiode(this))
            }
        }
    }
}
