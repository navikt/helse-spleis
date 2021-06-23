package no.nav.helse.bugs_showstoppers

import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.TilstandType.*
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test
import java.util.*

internal class UtdatertGodkjenningBugE2ETest: AbstractEndToEndTest() {

    @Test
    fun `Håndterer løsning på godkjenningsbehov der utbetalingid på løsningen matcher med periodens gjeldende utbetaling`() {
        tilGodkjenning(1.januar, 31.januar, 100.prosent, 1.januar)
        håndterUtbetalingsgodkjenning()
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
    }

    @Test
    fun `Ignorerer løsning på godkjenningsbehov dersom utbetalingid på løsningen ikke samsvarer med periodens gjeldende utbetaling`() {
        tilGodkjenning(1.januar, 31.januar, 100.prosent, 1.januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent)) // reberegner vedtaksperioden
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning(utbetalingId = UUID.randomUUID())
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `Blir stående i TIL_UTBETALING ved påminnelse, dersom utbetalingen er in transit (ikke ubetalt)`() {
        tilGodkjenning(1.januar, 31.januar, 100.prosent, 1.januar)
        håndterUtbetalingsgodkjenning()
        håndterPåminnelse(1.vedtaksperiode, TIL_UTBETALING)
        assertTilstander(
            1.vedtaksperiode,
            START,
            MOTTATT_SYKMELDING_FERDIG_GAP,
            AVVENTER_SØKNAD_FERDIG_GAP,
            AVVENTER_UTBETALINGSGRUNNLAG,
            AVVENTER_HISTORIKK,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
    }
}
