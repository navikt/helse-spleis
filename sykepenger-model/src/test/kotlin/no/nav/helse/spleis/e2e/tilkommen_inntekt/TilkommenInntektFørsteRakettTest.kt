package no.nav.helse.spleis.e2e.tilkommen_inntekt

import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.februar
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.TilkommenInntekt
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.TilstandType
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TilkommenInntektFørsteRakettTest : AbstractDslTest() {

    @Test
    fun `permisjon skal ikke hensyntas av spleis i første rakett`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(
                Sykdom(1.februar, 28.februar,100.prosent),
                Permisjon(20.februar, 28.februar),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(1.februar, 28.februar, a2, 10000.månedlig)
                )
            )
            assertVarsel(Varselkode.RV_IV_9, 2.vedtaksperiode.filter())
            assertIngenVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.single().gjelder(a1))
            }
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
                    TilkommenInntekt(1.februar, 28.februar, a2, 10000.månedlig)
                )
            )
            assertVarsel(Varselkode.RV_IV_9, 2.vedtaksperiode.filter())
            assertIngenVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.single().gjelder(a1))
            }
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
                    TilkommenInntekt(1.februar, 28.februar, a2, 10000.månedlig)
                )
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertVarsel(Varselkode.RV_IV_9, 1.vedtaksperiode.filter())
            assertIngenVarsel(Varselkode.RV_SV_5, 1.vedtaksperiode.filter())
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.inntektsgrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.single().gjelder(a1))
            }
        }
    }

    @Test
    fun `andre ytelser skal ikke hensyntas av spleis i første rakett`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(
                Sykdom(1.februar, 28.februar,100.prosent),
                tilkomneInntekter = listOf(
                    TilkommenInntekt(1.februar, 28.februar, a2, 10000.månedlig)
                )
            )
            håndterYtelser(2.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(februar, 100)))
            assertForventetFeil(
                forklaring = "skal ikke hensynta andre ytelser i rakett 1",
                nå = {
                    assertVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
                },
                ønsket = {
                    assertVarsel(Varselkode.RV_IV_9, 2.vedtaksperiode.filter())
                    assertIngenVarsel(Varselkode.RV_SV_5, 2.vedtaksperiode.filter())
                }
            )
        }
    }

}