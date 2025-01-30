package no.nav.helse.spleis.e2e.tilkommen_inntekt

import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.TilkommenInntekt
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_5
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_9
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_5
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TilkommenInntektFørsteRakettTest : AbstractDslTest() {

    @Test
    fun `permisjon skal ikke hensyntas av spleis i første rakett`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                Permisjon(20.februar, 28.februar),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(1.februar, 28.februar, a2, 10000)
                )
            )
            assertForventetFeil(
                forklaring = "Støtter ikke tilkommen inntekt ennå",
                nå = { assertSisteTilstand(2.vedtaksperiode, TilstandType.TIL_INFOTRYGD) },
                ønsket = { assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK) }
            )
        }
    }

    @Test
    fun `ferie skal ikke hensyntas av spleis i første rakett`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                Ferie(20.februar, 28.februar),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(1.februar, 28.februar, a2, 10000)
                )
            )
            assertForventetFeil(
                forklaring = "Støtter ikke tilkommen inntekt ennå",
                nå = { assertSisteTilstand(2.vedtaksperiode, TilstandType.TIL_INFOTRYGD) },
                ønsket = { assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK) }
            )
        }
    }

    @Test
    fun `ferie og tilkommen inntekt ved korigert søknad`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                Ferie(20.januar, 31.januar),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(1.februar, 28.februar, a2, 10000)
                )
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertVarsler(1.vedtaksperiode, RV_IV_9)
            assertEquals(emptyList<TilkommenInntekt>(), inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.view().inntektsgrunnlag.tilkommendeInntekter)
        }
    }

    @Test
    fun `tilkommende inntekt ved korrigerende søknad`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(1.februar, 28.februar, a2, 10000)
                )
            )
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertVarsler(2.vedtaksperiode, RV_IV_9)
            assertEquals(emptyList<TilkommenInntekt>(), inspektør.vilkårsgrunnlag(1.januar)!!.view().inntektsgrunnlag.tilkommendeInntekter)
        }
    }

    @Test
    fun `andre ytelser skal ikke hensyntas av spleis i første rakett`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(
                Sykdom(1.februar, 28.februar, 100.prosent),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(1.februar, 28.februar, a2, 10000)
                )
            )
            assertForventetFeil(
                forklaring = "Støtter ikke tilkommen inntekt ennå",
                nå = { assertSisteTilstand(2.vedtaksperiode, TilstandType.TIL_INFOTRYGD) },
                ønsket = {
                    assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
                    håndterYtelser(2.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(februar, 100)))
                    assertVarsler(listOf(RV_SV_5, RV_AY_5, RV_IV_9), 2.vedtaksperiode.filter())
                }
            )
        }
    }
}
