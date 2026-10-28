package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class RettighetskategoriIdTest : AbstractDslTest() {
    @Test
    fun `nytt vilkårsgrunnlag får deterministisk rettighetskategoriId`() {
        a1 {
            nyttVedtak(januar)
            val grunnlag = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!
            assertEquals(deterministisk(grunnlag.vilkårsgrunnlagId), grunnlag.rettighetskategoriId)
        }
    }

    @Test
    fun `kopiert vilkårsgrunnlag beholder rettighetskategoriId`() {
        a1 {
            nyttVedtak(januar)
            val grunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!
            håndterOverstyrArbeidsforhold(1.januar, OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a1, deaktivert = true, "test"))
            val grunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!
            assertNotEquals(grunnlagFør.vilkårsgrunnlagId, grunnlagEtter.vilkårsgrunnlagId)
            assertEquals(grunnlagFør.rettighetskategoriId, grunnlagEtter.rettighetskategoriId)
        }
    }

    @Test
    fun `nytt skjæringstidspunkt gir ny rettighetskategoriId`() {
        a1 {
            nyttVedtak(januar)
            val grunnlagFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!

            håndterOverstyrTidslinje(
                listOf(
                    ManuellOverskrivingDag(1.januar, Dagtype.Arbeidsdag, 100),
                    ManuellOverskrivingDag(4.januar, Dagtype.Arbeidsdag, 100),
                ),
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            assertVarsel(RV_IV_7, 1.vedtaksperiode.filter())

            assertEquals(5.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            val grunnlagEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!
            assertNotEquals(grunnlagFør.rettighetskategoriId, grunnlagEtter.rettighetskategoriId)
            assertEquals(deterministisk(grunnlagEtter.vilkårsgrunnlagId), grunnlagEtter.rettighetskategoriId)
        }
    }

    @Test
    fun `migreringen gir samme rettighetskategoriId som Spleis lager for nye vilkårsgrunnlag`() {
        medJSONPerson("/personer/vedtak_januar_uten_opptjeningsvurderingid.json", 344)
        a1 {
            nyttVedtak(mars)
        }
        val historikk = dto().vilkårsgrunnlagHistorikk.historikk
        assertEquals(2, historikk.size)
        val alleGrunnlag = historikk.flatMap { it.vilkårsgrunnlag }
        assertEquals(listOf(1.januar, 1.mars, 1.januar), alleGrunnlag.map { it.skjæringstidspunkt })
        alleGrunnlag.forEach { grunnlag ->
            assertEquals(deterministisk(grunnlag.vilkårsgrunnlagId), grunnlag.rettighetskategoriId)
            assertEquals(
                VilkårsgrunnlagElement.deterministiskRettighetskategoriId(grunnlag.vilkårsgrunnlagId),
                grunnlag.rettighetskategoriId,
            )
        }
        assertGjenoppbygget(dto())
    }

    @Test
    fun `vilkårsgrunnlag fra Infotrygd får deterministisk rettighetskategoriId`() {
        medJSONPerson("/personer/infotrygdforlengelse.json", 334)
        a1 {
            nyttVedtak(april)
        }
        val alleGrunnlag = dto().vilkårsgrunnlagHistorikk.historikk.flatMap { it.vilkårsgrunnlag }
        assertTrue(alleGrunnlag.any { it is VilkårsgrunnlagUtDto.Infotrygd })
        alleGrunnlag.forEach { grunnlag ->
            assertEquals(deterministisk(grunnlag.vilkårsgrunnlagId), grunnlag.rettighetskategoriId)
        }
        assertGjenoppbygget(dto())
    }

    private fun deterministisk(vilkårsgrunnlagId: UUID) = UUID.nameUUIDFromBytes("$vilkårsgrunnlagId:Rettighetskategori".toByteArray())
}
