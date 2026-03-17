package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MinimumSykdomsgradVurdertFlereAGTest : AbstractDslTest() {

    @Test
    fun `Saksbehandler vurderer arbeidstid og avslår under 20 prosent på begge arbeidsgivere`() {
        a1 { nyPeriode(januar, grad = 15.prosent) }
        a2 { nyPeriode(januar, grad = 20.prosent) }
        a1 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = 20000.månedlig) }
        a2 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), beregnetInntekt = 40000.månedlig) }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            val avvistedager1 = inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.grad == 15.prosent })
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.totalGrad == 18 })
            assertEquals(11, avvistedager1.size)
            assertTrue(avvistedager1.all { it.begrunnelser == listOf(Begrunnelse.MinimumSykdomsgrad) })
            assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }

        a1 {
            håndterMinimumSykdomsgradVurdert(
                perioderMedMinimumSykdomsgradVurdertOK = emptyList(),
                perioderMedMinimumSykdomsgradVurdertIkkeOK = listOf(januar)
            )
            håndterYtelser(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.grad == 15.prosent })
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.totalGrad == 18 })
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            val avvistedager2 = inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager
            assertEquals(11, avvistedager2.size)
            assertTrue(avvistedager2.all { it.begrunnelser == listOf(Begrunnelse.MinimumSykdomsgrad) })
            assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter())
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.grad == 20.prosent })
            assertTrue(inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.totalGrad == 18 })
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
    }
}
