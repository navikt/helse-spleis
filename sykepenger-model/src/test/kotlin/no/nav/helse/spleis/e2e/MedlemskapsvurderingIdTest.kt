package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class MedlemskapsvurderingIdTest : AbstractDslTest() {
    @Test
    fun `medlemskapsvurderingId fra sp-medlemskap blir lagt på`() {
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            val medlemskapsvurderingId = UUID.randomUUID()
            håndterVilkårsgrunnlag(1.vedtaksperiode, medlemskapsvurderingId = medlemskapsvurderingId)
            assertEquals(medlemskapsvurderingId, inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.medlemskapsvurderingId)
        }
    }

    @Test
    fun `Overstyring av arbeidsforhold beholder medlemskapsvurderingId`() {
        a1 {
            nyttVedtak(januar)
            val medlemskapsvurderingIdFør = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.medlemskapsvurderingId
            håndterOverstyrArbeidsforhold(1.januar, OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(a1, deaktivert = true, "test"))
            val medlemskapsvurderingIdEtter = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.medlemskapsvurderingId
            assertEquals(medlemskapsvurderingIdFør, medlemskapsvurderingIdEtter)
        }
    }

    @Test
    fun `nytt skjæringstidspunkt gir ny medlemskapsvurderingId`() {
        a1 {
            nyttVedtak(januar)
            assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
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
            assertNotEquals(grunnlagFør.vilkårsgrunnlagId, grunnlagEtter.vilkårsgrunnlagId)
            assertNotEquals(grunnlagFør.medlemskapsvurderingId, grunnlagEtter.medlemskapsvurderingId)
        }
    }

    @Test
    fun `deterministisk medlemskapsvurderingId migreres inn i V348-migreringen, mens det legges random id på nye vilkårsgrunnlag`() {
        medJSONPerson("/personer/vedtak_januar_uten_opptjeningsvurderingid.json", 344)
        a1 {
            nyttVedtak(mars)
        }
        dto().vilkårsgrunnlagHistorikk.historikk.also { historikk ->
            assertEquals(2, historikk.size)
            historikk[0].also {
                assertEquals(2, it.vilkårsgrunnlag.size)
                it.vilkårsgrunnlag[0].also { grunnlag ->
                    assertEquals(1.januar, grunnlag.skjæringstidspunkt)
                    assertEquals(
                        UUID.nameUUIDFromBytes("${grunnlag.vilkårsgrunnlagId}:Medlemskap".toByteArray()),
                        (grunnlag as VilkårsgrunnlagUtDto.Spleis).medlemskapsvurderingId,
                    )
                }
                it.vilkårsgrunnlag[1].also { grunnlag ->
                    assertEquals(1.mars, grunnlag.skjæringstidspunkt)
                    assertNotEquals(
                        UUID.nameUUIDFromBytes("${grunnlag.vilkårsgrunnlagId}:Medlemskap".toByteArray()),
                        (grunnlag as VilkårsgrunnlagUtDto.Spleis).medlemskapsvurderingId,
                    )
                }
            }
            historikk[1].also {
                assertEquals(1, it.vilkårsgrunnlag.size)
                it.vilkårsgrunnlag[0].also { grunnlag ->
                    assertEquals(1.januar, grunnlag.skjæringstidspunkt)
                    assertEquals(
                        UUID.nameUUIDFromBytes("${grunnlag.vilkårsgrunnlagId}:Medlemskap".toByteArray()),
                        (grunnlag as VilkårsgrunnlagUtDto.Spleis).medlemskapsvurderingId,
                    )
                }
            }
        }
        assertGjenoppbygget(dto())
    }

    @Test
    fun `infotrygdgrunnlag får også en deterministisk medlemskapsvurderingId`() {
        medJSONPerson("/personer/infotrygdforlengelse.json", 334)

        a1 {
            nyttVedtak(april)
        }
        dto().vilkårsgrunnlagHistorikk.historikk.also { historikk ->
            assertEquals(3, historikk.size)
            historikk[0].also {
                assertEquals(2, it.vilkårsgrunnlag.size)
                it.vilkårsgrunnlag[0].also { grunnlag ->
                    assertTrue(grunnlag is VilkårsgrunnlagUtDto.Infotrygd)
                    assertEquals(1.januar, grunnlag.skjæringstidspunkt)
                    assertEquals(
                        UUID.nameUUIDFromBytes("${grunnlag.vilkårsgrunnlagId}:Medlemskap".toByteArray()),
                        (grunnlag as VilkårsgrunnlagUtDto.Infotrygd).medlemskapsvurderingId,
                    )
                }
                it.vilkårsgrunnlag[1].also { grunnlag ->
                    assertEquals(1.april, grunnlag.skjæringstidspunkt)
                    assertNotEquals(
                        UUID.nameUUIDFromBytes("${grunnlag.vilkårsgrunnlagId}:Medlemskap".toByteArray()),
                        (grunnlag as VilkårsgrunnlagUtDto.Spleis).medlemskapsvurderingId,
                    )
                }
            }
        }

        assertGjenoppbygget(dto())
    }
}
