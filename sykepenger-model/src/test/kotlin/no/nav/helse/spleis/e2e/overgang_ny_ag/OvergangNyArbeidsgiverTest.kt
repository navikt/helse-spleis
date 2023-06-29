package no.nav.helse.spleis.e2e.overgang_ny_ag

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class OvergangNyArbeidsgiverTest : AbstractDslTest() {

    @Test
    fun `overgang til ny arbeidsgiver - innenfor agp - reduksjon oppgitt`() {
        // Inntektsmelding-signal corner case
        a1 {
            nyttVedtak(1.januar, 31.januar)
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 16.februar, 100.prosent))
            håndterInntektsmelding(emptyList(), førsteFraværsdag = 1.februar, begrunnelseForReduksjonEllerIkkeUtbetalt = "TidligereVirksomhet")
            assertFunksjonellFeil(Varselkode.RV_SV_2)
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `overgang til ny arbeidsgiver - utenfor agp`() {
        a1 {
            nyttVedtak(1.januar, 31.januar)
        }
        a2 {
            håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent))
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }
}