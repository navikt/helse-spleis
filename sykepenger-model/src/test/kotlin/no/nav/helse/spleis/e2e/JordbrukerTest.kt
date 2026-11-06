package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.selvstendig
import no.nav.helse.hendelser.ForsikringsvurderingResultat
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Klassekode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class JordbrukerTest : AbstractDslTest() {
    @Test
    fun `tar inn jordbruker, men forkaster perioden når togglen er av`() =
        Toggle.Jordbruker.disable {
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.JORDBRUKER)
                assertInfo("Har ikke støtte for søknadstypen JORDBRUKER", 1.vedtaksperiode.filter())
                assertFunksjonellFeil(Varselkode.RV_SØ_39, 1.vedtaksperiode.filter())
                assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
            }
        }

    @Test
    fun `jordbruker behandles som selvstendig næringsdrivende med varsel når togglen er på`() =
        Toggle.Jordbruker.enable {
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.JORDBRUKER)
                assertVarsel(Varselkode.RV_SØ_55, 1.vedtaksperiode.filter())
                assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

                håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
                håndterYtelserSelvstendig(
                    1.vedtaksperiode,
                    forsikringsvurderingResultat =
                        ForsikringsvurderingResultat(
                            forsikringsvurderingId = UUID.randomUUID(),
                            dekning = ForsikringsvurderingResultat.Dekning(grad = 100, iVentetid = false),
                            opphørsdato = null,
                            harIndividuellForsikring = false,
                            villeHattForsikringOmDenVarBetalt = false,
                            harForsikringSomIkkePasserMedSøknadstype = false,
                        ),
                )
                håndterSimulering(1.vedtaksperiode)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)

                inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                    assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                    assertEquals(1, utbetalinginspektør.personOppdrag.size)
                    utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                        assertEquals(17.januar til 31.januar, linje.periode)
                        assertEquals(1771, linje.beløp)
                        assertEquals(Klassekode.SelvstendigNæringsdrivendeOppgavepliktig, linje.klassekode)
                    }
                }
                assertVarsler(1.vedtaksperiode, Varselkode.RV_SØ_55)
            }
        }
}
