package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RK_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class KonsistenssjekkRettighetskategoriTest : AbstractDslTest() {
    @Test
    fun `forlengelse av vilkårsgrunnlag fra før fase 1 slår opp hovedkategorien med deterministisk ID`() {
        medJSONPerson("/personer/vedtak_januar_uten_opptjeningsvurderingid.json", 344)
        a1 {
            håndterSøknad(februar)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
            val vilkårsgrunnlagId = inspektør.vilkårsgrunnlag(2.vedtaksperiode)!!.vilkårsgrunnlagId
            assertEquals(
                UUID.nameUUIDFromBytes("$vilkårsgrunnlagId:Rettighetskategori".toByteArray()),
                rettighetskategoriIdIBeregningsbehov(2.vedtaksperiode),
            )
            håndterYtelser(2.vedtaksperiode, rettighetskategoriResultat = Hovedkategori.ARBEIDSTAKER)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
            assertIngenFunksjonelleFeil(2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `forlengelse forkastes når hovedkategorien ikke stemmer med yrkesaktivitetstypen`() {
        medJSONPerson("/personer/vedtak_januar_uten_opptjeningsvurderingid.json", 344)
        a1 {
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode, rettighetskategoriResultat = Hovedkategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            assertFunksjonellFeil(RV_RK_2, 2.vedtaksperiode.filter())
            assertSisteForkastetTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `revurdering der hovedkategorien ikke stemmer gir varsel og forkastes ikke`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode, rettighetskategoriResultat = Hovedkategori.SELVSTENDIG_NÆRINGSDRIVENDE)
            assertVarsler(listOf(RV_RK_2, RV_UT_23), 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        }
    }
}
