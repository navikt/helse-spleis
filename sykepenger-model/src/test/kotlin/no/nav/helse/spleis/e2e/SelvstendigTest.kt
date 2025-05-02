package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.Arbeidstakerkilde
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.selvstendig
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.utbetalingslinjer.Klassekode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
            håndterSøknad(januar)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 1) {
                assertInntektsgrunnlag(selvstendig, INNTEKT, forventetkilde = Arbeidstakerkilde.AOrdningen)
            }
            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(januar, linje.periode)
                    assertEquals(1431, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeOppgavepliktig, linje.klassekode)
                }
            }
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        }
    }

    @Test
    fun `To selvstendigsøknader`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(januar)
            håndterSøknad(mars)
            
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_BLOKKERENDE_PERIODE)
            assertEquals(emptyList<Nothing>(), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
        }
    }
}
