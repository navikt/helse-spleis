package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.selvstendig
import no.nav.helse.hendelser.ForsikringsvurderingResultat
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Klassekode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class JordbrukerTest : AbstractDslTest() {
    @Test
    fun `tar inn jordbruker, men forkaster perioden`() = Toggle.Jordbruker.disable {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.JORDBRUKER)
            assertInfo("Har ikke støtte for søknadstypen JORDBRUKER", 1.vedtaksperiode.filter())
            assertFunksjonellFeil(Varselkode.RV_SØ_39, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, SELVSTENDIG_START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `jordbruker har 100 prosent dekningsgrad og egen klassekode`() = Toggle.Jordbruker.enable {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.JORDBRUKER)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(
                1.vedtaksperiode,
                forsikringsvurderingResultat = ForsikringsvurderingResultat(
                    forsikringsvurderingId = UUID.randomUUID(),
                    harForsikring = true,
                    dekning = ForsikringsvurderingResultat.Dekning(grad = 100, iVentetid = false),
                    opphørsdato = null,
                    harIndividuellForsikring = false,
                    villeHattForsikringOmDenVarBetalt = false,
                )
            )
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)

            inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                assertEquals(1, utbetalinginspektør.personOppdrag.size)
                utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                    assertEquals(17.januar til 31.januar, linje.periode)
                    assertEquals(1771, linje.beløp)
                    assertEquals(Klassekode.SelvstendigNæringsdrivendeJordbrukOgSkogbruk, linje.klassekode)
                }
            }
            assertVarsler(1.vedtaksperiode, Varselkode.RV_SØ_55)
        }
    }

    @Test
    fun `jordbruker får riktig verdi i spesielleYrkesgrupper`() = Toggle.Jordbruker.enable {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.JORDBRUKER)
            assertTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_VILKÅRSPRØVING)

            val vilkårsprøvingEvent = observatør.trengerInformasjonTilVilkårsprøvingEventer.single()
            assertEquals(listOf("JORDBRUKER"), vilkårsprøvingEvent.spesielleYrkesgrupper)
        }
    }

    @Test
    fun `jordbruker med ekstra tegnet forsikring har ting på stell`() = Toggle.Jordbruker.enable {
        Toggle.SelvstendigForsikring.enable {
            selvstendig {
                håndterFørstegangssøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.JORDBRUKER)
                håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
                håndterYtelserSelvstendig(
                    1.vedtaksperiode,
                    forsikringsvurderingResultat = ForsikringsvurderingResultat(
                        forsikringsvurderingId = UUID.randomUUID(),
                        harForsikring = true,
                        dekning = ForsikringsvurderingResultat.Dekning(grad = 100, iVentetid = true),
                        opphørsdato = null,
                        harIndividuellForsikring = true,
                        villeHattForsikringOmDenVarBetalt = false,
                    )
                )
                håndterSimulering(1.vedtaksperiode)
                assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_GODKJENNING)

                inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.also { utbetalinginspektør ->
                    assertEquals(0, utbetalinginspektør.arbeidsgiverOppdrag.size)
                    assertEquals(1, utbetalinginspektør.personOppdrag.size)
                    utbetalinginspektør.personOppdrag.single().inspektør.also { linje ->
                        assertEquals(1.januar til 31.januar, linje.periode)
                        assertEquals(1771, linje.beløp)
                        assertEquals(Klassekode.SelvstendigNæringsdrivendeJordbrukOgSkogbruk, linje.klassekode)
                    }
                }
                assertVarsler(1.vedtaksperiode, Varselkode.RV_SØ_55, Varselkode.RV_AN_6)
            }
        }
    }
}
