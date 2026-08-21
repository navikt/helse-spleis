package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.selvstendig
import no.nav.helse.hendelser.Vurdering
import no.nav.helse.januar
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EndretVurderingPåSkjæringstidspunktTest: AbstractDslTest() {

    @Test
    fun `ny opptjeningsvurdering`() {
        a1 {
            nyttVedtak(januar)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            val nyOpptjeningsvurderingId = UUID.randomUUID()
            håndterEndretVuderingPåSkjæringstidspunkt(1.januar, Vurdering.Opptjeningsvurdering(nyOpptjeningsvurderingId))
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertEquals(nyOpptjeningsvurderingId, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.opptjeningsvurderingId)
        }
    }

    @Test
    fun `samme opptjeningsvurdering som allerede er lagt til grunn`() {
        a1 {
            nyttVedtak(januar)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            val opptjeningsvurderingId = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.opptjeningsvurderingId
            håndterEndretVuderingPåSkjæringstidspunkt(1.januar, Vurdering.Opptjeningsvurdering(opptjeningsvurderingId))
            assertInfo("Opptjeningsvurderingen er allerede lagt til grunn")
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `ny forsikringsvurdering`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            val nyForsikringsvurderingId = UUID.randomUUID()
            håndterEndretVuderingPåSkjæringstidspunkt(1.januar, Vurdering.Forsikringsvurdering(nyForsikringsvurderingId))
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING)
            assertEquals(nyForsikringsvurderingId, (inspektør.vilkårsgrunnlag(1.vedtaksperiode) as? VilkårsgrunnlagHistorikk.Grunnlagsdata)!!.forsikringsvurderingId)
            assertEquals(nyForsikringsvurderingId, observatør.trengerInformasjonTilBeregningEventer.last().forsikringsvurderingId)
        }
    }

    @Test
    fun `samme forsikringsvurdering som allerede er lagt til grunn`() {
        selvstendig {
            val forsikringsvurderingId = UUID.randomUUID()
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode, forsikringsvurderingId = forsikringsvurderingId)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            val forsikringsvurderingIdFraVilkårsgrunnlag = (inspektør.vilkårsgrunnlag(1.vedtaksperiode) as? VilkårsgrunnlagHistorikk.Grunnlagsdata)!!.forsikringsvurderingId!!
            assertEquals(forsikringsvurderingId, forsikringsvurderingIdFraVilkårsgrunnlag)
            håndterEndretVuderingPåSkjæringstidspunkt(1.januar, Vurdering.Forsikringsvurdering(forsikringsvurderingId))

            assertInfo("Forsikringsvurderingen er allerede lagt til grunn")
            assertSisteTilstand(1.vedtaksperiode, SELVSTENDIG_AVSLUTTET)
            assertEquals(1, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        }
    }
}
