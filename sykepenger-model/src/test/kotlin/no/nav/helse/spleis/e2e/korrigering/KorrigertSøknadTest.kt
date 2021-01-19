package no.nav.helse.spleis.e2e.korrigering

import no.nav.helse.Toggles
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.*
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.testhelpers.SykdomstidslinjeInspektør
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class KorrigertSøknadTest : AbstractEndToEndTest() {

    @Test
    fun `Arbeidsdag i søknad nr 2 kaster ut perioden`() {
        Toggles.KorrigertSøknadToggle.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 30.januar, 100.prosent), Arbeid(31.januar, 31.januar))
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Korrigerer feriedag til sykedag før vi henter historikk`() {
        Toggles.KorrigertSøknadToggle.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(31.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            SykdomstidslinjeInspektør(inspektør.sykdomstidslinje).also {
                assertTrue(it[31.januar] is Sykedag)
            }
            assertTilstander(1.vedtaksperiode, START, MOTTATT_SYKMELDING_FERDIG_GAP, AVVENTER_GAP)
        }
    }

    @Test
    fun `Går tilbake til AVVENTER_HISTORIKK når søknaden kommer inn i AVVENTER_SIMULERING`() {
        Toggles.KorrigertSøknadToggle.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(31.januar, 31.januar))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

            SykdomstidslinjeInspektør(inspektør.sykdomstidslinje).also {
                assertTrue(it[31.januar] is Sykedag)
            }
            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_GAP,
                AVVENTER_VILKÅRSPRØVING_GAP,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_HISTORIKK
            )
        }
    }
}
