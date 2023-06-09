package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.tilAvventerSaksbehandler
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_SAKSBEHANDLER
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.Vedtaksperiode.AvventerSaksbehandler.AvventerSaksbehandlerÅrsak.MÅ_SKJØNNSMESSIG_FASTSETTE_SYKEPENGEGRUNNLAG
import no.nav.helse.person.aktivitetslogg.Varselkode
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.TjuefemprosentAvvik::class)
internal class AvventerSaksbehandlerTest: AbstractDslTest() {

    @Test
    fun `saksbehandler må skjønnsmessig fastsette`() {
        a1 {
            tilAvventerSaksbehandler(1.januar, 31.januar, fordi = MÅ_SKJØNNSMESSIG_FASTSETTE_SYKEPENGEGRUNNLAG)
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_SAKSBEHANDLER,
            )
            assertVarsel(Varselkode.RV_IV_2)
        }
    }

    @Test
    fun `saksbehandler må skjønnsmessig fastsette, men endrer på inntekt`() {
        a1 {
            tilAvventerSaksbehandler(1.januar, 31.januar, fordi = MÅ_SKJØNNSMESSIG_FASTSETTE_SYKEPENGEGRUNNLAG)
            assertVarsel(Varselkode.RV_IV_2)
            håndterOverstyrInntekt(1.januar, INNTEKT)
            assertForventetFeil(
                forklaring = "Må skjønnsmessig fastsette før man skal beregne utbetaling",
                nå = { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK) },
                ønsket = { assertSisteTilstand(1.vedtaksperiode, AVVENTER_SAKSBEHANDLER) }
            )
        }
    }

}