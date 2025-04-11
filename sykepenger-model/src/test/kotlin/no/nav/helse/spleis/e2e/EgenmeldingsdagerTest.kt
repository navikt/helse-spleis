package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EgenmeldingsdagerTest: AbstractDslTest() {

    @Test
    fun `Nullstille alle egenmeldingsdager innenfor arbeidsgiverperioden`() {
        a1 {
            håndterSøknad(Sykdom(5.januar, 9.januar, 100.prosent), egenmeldinger = listOf(1.januar til 2.januar))
            håndterSøknad(Sykdom(15.januar, 19.januar, 100.prosent))
            håndterSøknad(Sykdom(25.januar, 29.januar, 100.prosent), egenmeldinger = listOf(24.januar til 24.januar))
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
