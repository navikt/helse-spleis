package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.selvstendig
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SelvstendigTest : AbstractDslTest() {

    @Test
    fun `selvstendigsøknad gir error`() = Toggle.SelvstendigNæringsdrivende.disable {
        selvstendig {
            håndterSøknad(januar)
            assertFunksjonelleFeil()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Ta inn selvstendigsøknad`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            assertThrows<Aktivitetslogg.AktivitetException> {
                håndterSøknad(januar)
            }
            assertForventetFeil(
                "Har ikke implementert dette enda",
                {
                    assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
                },
                {
                    assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
                    assertIngenFunksjonelleFeil()
                }
            )

        }
    }
}
