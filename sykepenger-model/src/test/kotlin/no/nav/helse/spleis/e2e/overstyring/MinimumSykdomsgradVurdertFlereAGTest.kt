package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterMinimumSykdomsgradVurdert
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MinimumSykdomsgradVurdertFlereAGTest : AbstractEndToEndTest() {

    @Test
    fun `Saksbehandler vurderer arbeidstid og avslår under 20 prosent på begge arbeidsgivere`() {
        nyPeriode(januar, orgnummer = a1, grad = 15.prosent)
        nyPeriode(januar, orgnummer = a2, grad = 20.prosent)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 20000.månedlig,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 40000.månedlig,
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@MinimumSykdomsgradVurdertFlereAGTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)

        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        val avvistedager1 = inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager
        assertTrue(inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.grad == 15.prosent })
        assertTrue(inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.totalGrad == 18 })
        assertEquals(11, avvistedager1.size)
        assertTrue(avvistedager1.all { it.begrunnelser == listOf(Begrunnelse.MinimumSykdomsgrad) })
        assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter(a1))

        håndterMinimumSykdomsgradVurdert(perioderMedMinimumSykdomsgradVurdertOK = emptyList(), perioderMedMinimumSykdomsgradVurdertIkkeOK = listOf(januar))
        this@MinimumSykdomsgradVurdertFlereAGTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING, orgnummer = a1)
        assertEquals(11, inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager.size)
        assertTrue(inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.grad == 15.prosent })
        assertTrue(inspektør(a1).utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.totalGrad == 18 })
        this@MinimumSykdomsgradVurdertFlereAGTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        this@MinimumSykdomsgradVurdertFlereAGTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        val avvistedager2 = inspektør(a2).utbetalingstidslinjer(1.vedtaksperiode).inspektør.avvistedager
        assertEquals(11, avvistedager2.size)
        assertTrue(avvistedager2.all { it.begrunnelser == listOf(Begrunnelse.MinimumSykdomsgrad) })
        assertVarsel(Varselkode.RV_VV_4, 1.vedtaksperiode.filter(a2))
        assertTrue(inspektør(a2).utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.grad == 20.prosent })
        assertTrue(inspektør(a2).utbetalingstidslinjer(1.vedtaksperiode).inspektør.navdager.all { it.økonomi.inspektør.totalGrad == 18 })

        this@MinimumSykdomsgradVurdertFlereAGTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }
}
