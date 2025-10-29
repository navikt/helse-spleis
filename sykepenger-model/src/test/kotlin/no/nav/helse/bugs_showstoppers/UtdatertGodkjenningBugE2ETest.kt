package no.nav.helse.bugs_showstoppers

import java.util.UUID
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class UtdatertGodkjenningBugE2ETest : AbstractEndToEndTest() {

    @Test
    fun `Håndterer løsning på godkjenningsbehov der utbetalingid på løsningen matcher med periodens gjeldende utbetaling`() {
        tilGodkjenning(januar, 100.prosent)
        this@UtdatertGodkjenningBugE2ETest.håndterUtbetalingsgodkjenning()
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
    }

    @Test
    fun `Ignorerer løsning på godkjenningsbehov dersom utbetalingid på løsningen ikke samsvarer med periodens gjeldende utbetaling`() {
        tilGodkjenning(januar, 100.prosent)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent)) // reberegner vedtaksperioden
        håndterYtelser()
        håndterSimulering()
        assertThrows<IllegalStateException> {
            håndterUtbetalingsgodkjenning(utbetalingId = UUID.randomUUID())
        }
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
    }

    @Test
    fun `Blir stående i TIL_UTBETALING ved påminnelse, dersom utbetalingen er in transit (ikke ubetalt)`() {
        tilGodkjenning(januar, 100.prosent)
        this@UtdatertGodkjenningBugE2ETest.håndterUtbetalingsgodkjenning()
        this@UtdatertGodkjenningBugE2ETest.håndterPåminnelse(1.vedtaksperiode, TIL_UTBETALING)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING
        )
    }
}
