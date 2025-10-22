package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class AndreInntektskilderTest: AbstractDslTest() {

    @Test
    fun `Andre inntektskilder out of order`(){
        a1 {
            håndterSøknad(februar, andreInntektskilder = false)
            håndterSøknad(januar, andreInntektskilder = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteForkastetTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Andre inntektskilder på en førstegangssøknad`() {
        a1 {
            håndterSøknad(februar, andreInntektskilder = true)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Andre inntektskilder på en forlengelse før inntektsmelding`() {
        a1 {
            håndterSøknad(januar, andreInntektskilder = false)
            håndterSøknad(februar, andreInntektskilder = true)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertVarsel(Varselkode.TilkommenInntekt.`Opplyst i søknaden om at hen har andre inntekskilder`, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Andre inntektskilder på en forlengelse etter inntektsmelding`() {
        a1 {
            håndterSøknad(januar, andreInntektskilder = false)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterSøknad(februar, andreInntektskilder = true)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsel(Varselkode.TilkommenInntekt.`Opplyst i søknaden om at hen har andre inntekskilder`, 2.vedtaksperiode.filter())
        }
    }

    private fun håndterSøknad(periode: Periode, andreInntektskilder: Boolean) =
        håndterSøknad(Sykdom(periode.start, periode.endInclusive, 100.prosent), andreInntektskilder = andreInntektskilder)
}
