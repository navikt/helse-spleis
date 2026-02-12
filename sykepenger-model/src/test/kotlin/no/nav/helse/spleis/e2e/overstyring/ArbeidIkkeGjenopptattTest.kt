package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.ArbeidIkkeGjenopptattDag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_11
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ArbeidIkkeGjenopptattTest: AbstractDslTest() {

    @Test
    fun `Ingen varsel om flere skjærsingstidspunkt ved ved aig-strekk uten gap mellom periodene`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(mars)
            håndterInntektsmelding(emptyList(), førsteFraværsdag = 1.mars)
            håndterOverstyrTidslinje(februar.map { ManuellOverskrivingDag(it, ArbeidIkkeGjenopptattDag) })
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(listOf(1.mars, 1.januar), inspektør.skjæringstidspunkter(2.vedtaksperiode))
            // Her er det ikke varsler pga. reglene for når det blir varsel eller ei
        }
    }

    @Test
    fun `Varsel om flere skjærsingstidspunkt ved ved aig-strekk med gap mellom periodene`() {
        a1 {
            nyttVedtak(januar)

            nyttVedtak(10.februar til 28.februar, arbeidsgiverperiode = emptyList())

            håndterSøknad(april)
            håndterInntektsmelding(emptyList(), førsteFraværsdag = 1.april)
            håndterOverstyrTidslinje(mars.map { ManuellOverskrivingDag(it, ArbeidIkkeGjenopptattDag) })
            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()
            assertEquals(listOf(1.april, 10.februar), inspektør.skjæringstidspunkter(3.vedtaksperiode))
            // Vi trenger ikke dette varselet her, den har nok kommet som en konsekvens av måten vi nå beregner skjæringstidspunkt
            assertVarsler(3.vedtaksperiode, RV_IV_11)
        }
    }
}
