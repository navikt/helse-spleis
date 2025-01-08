package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import org.junit.jupiter.api.Test

internal class KorrigerteArbeidsigveropplysningerTest : AbstractDslTest() {

    @Test
    fun `opplyser om korrigerert inntekt på en allerede utbetalt periode`() {
        a1 {
            nyttVedtak(januar)
            håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, Arbeidsgiveropplysning.OppgittInntekt(INNTEKT * 1.25))
            assertForventetFeil(
                forklaring = "Vi har jo ikke lagd dette",
                nå = { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) },
                ønsket = { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING) }
            )
        }
    }
}
