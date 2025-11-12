package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import org.junit.jupiter.api.Test

internal class EgenmeldingsdagerTest: AbstractDslTest() {

    @Test
    fun `egenmeldingsdager på forlengelsen av en auu`() {
        a1 {
            håndterSøknad(3.januar til 18.januar)
            nullstillTilstandsendringer()
            håndterSøknad(19.januar til 31.januar, egenmeldinger = listOf(1.januar til 3.januar))
            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 3.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 3.januar, listOf(1.januar til 16.januar), listOf(1.januar til 3.januar))

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

            assertSkjæringstidspunktOgVenteperiode(4.vedtaksperiode, 25.mai, listOf(1.mai til 2.mai, 5.mai til 9.mai, 15.mai til 19.mai, 24.mai til 27.mai), listOf(24.mai til 24.mai))

            håndterInntektsmelding(arbeidsgiverperioder = listOf(15.mai til 19.mai, 25.mai til 29.mai))

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 3.januar, listOf(1.januar til 16.januar), listOf(1.januar til 2.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 5.mai, listOf(5.mai til 9.mai, 15.mai til 19.mai, 25.mai til 29.mai))
            assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 15.mai, listOf(5.mai til 9.mai, 15.mai til 19.mai, 25.mai til 29.mai))
            assertSkjæringstidspunktOgVenteperiode(4.vedtaksperiode, 25.mai, listOf(5.mai til 9.mai, 15.mai til 19.mai, 25.mai til 29.mai))
        }
    }

    @Test
    fun `Fjerner egenmeldingsdager fra søknaden når inntektmelding med tom agp håndteres`() {
        a1 {
            håndterSøknad(3.januar til 9.januar, egenmeldinger = listOf(1.januar til 2.januar))
            håndterSøknad(15.januar til 31.januar, egenmeldinger = listOf(14.januar til 14.januar))

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 3.januar, listOf(1.januar til 9.januar), listOf(1.januar til 2.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 15.januar, listOf(1.januar til 9.januar, 14.januar til 20.januar), listOf(14.januar til 14.januar))

            håndterInntektsmelding(arbeidsgiverperioder = emptyList(), førsteFraværsdag = 15.januar)

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 3.januar, listOf(3.januar til 9.januar, 15.januar til 23.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 15.januar, listOf(3.januar til 9.januar, 15.januar til 23.januar))
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
                assertSkjæringstidspunktOgVenteperiode(this, 5.januar, listOf(1.januar til 2.januar, 5.januar til 9.januar), listOf(1.januar til 2.januar))
            }

            with(2.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING)
                assertSkjæringstidspunktOgVenteperiode(this, 15.januar, listOf(1.januar til 2.januar, 5.januar til 9.januar, 15.januar til 19.januar))
            }

            with(3.vedtaksperiode) {
                assertTilstander(this, AVVENTER_INNTEKTSMELDING)
                assertSkjæringstidspunktOgVenteperiode(this, 25.januar, listOf(1.januar til 2.januar, 5.januar til 9.januar, 15.januar til 19.januar, 24.januar til 27.januar), listOf(24.januar til 24.januar))
            }

            håndterPåminnelse(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING, flagg = setOf("nullstillEgenmeldingsdager"))

            with(1.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
                assertSkjæringstidspunktOgVenteperiode(this, 5.januar, listOf(5.januar til 9.januar, 15.januar til 19.januar, 25.januar til 29.januar))
            }

            with(2.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
                assertSkjæringstidspunktOgVenteperiode(this, 15.januar, listOf(5.januar til 9.januar, 15.januar til 19.januar, 25.januar til 29.januar))
            }

            with(3.vedtaksperiode) {
                assertTilstander(this, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
                assertSkjæringstidspunktOgVenteperiode(this, 25.januar, listOf(5.januar til 9.januar, 15.januar til 19.januar, 25.januar til 29.januar))
            }
        }
    }

    @Test
    fun `arbeidsgiverperioden justeres for alle perioder når egenmeldingsdager fjernes`() {
        a1 {
            håndterSøknad(5.januar til 14.januar, egenmeldinger = listOf(1.januar til 4.januar))
            håndterSøknad(15.januar til 16.januar)
            håndterSøknad(17.januar til 29.januar)
            nullstillTilstandsendringer()

            with(1.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING)
                assertSkjæringstidspunktOgVenteperiode(this, 5.januar, listOf(1.januar til 14.januar), listOf(1.januar til 4.januar))
            }

            with(2.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING)
                assertSkjæringstidspunktOgVenteperiode(this, 5.januar, listOf(1.januar til 16.januar))
            }

            with(3.vedtaksperiode) {
                assertTilstander(this, AVVENTER_INNTEKTSMELDING)
                assertSkjæringstidspunktOgVenteperiode(this, 5.januar, listOf(1.januar til 16.januar))
            }

            håndterPåminnelse(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, flagg = setOf("nullstillEgenmeldingsdager"))

            with(1.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
                assertSkjæringstidspunktOgVenteperiode(this, 5.januar, listOf(5.januar til 20.januar))
            }

            with(2.vedtaksperiode) {
                assertTilstander(this, AVSLUTTET_UTEN_UTBETALING, AVVENTER_AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
                assertSkjæringstidspunktOgVenteperiode(this, 5.januar, listOf(5.januar til 20.januar))
            }

            with(3.vedtaksperiode) {
                assertTilstander(this, AVVENTER_INNTEKTSMELDING)
                assertSkjæringstidspunktOgVenteperiode(this, 5.januar, listOf(5.januar til 20.januar))
            }
        }
    }
}
