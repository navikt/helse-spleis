package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.januar
import no.nav.helse.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OpptjeningsvurderingIdTest : AbstractDslTest() {

    @Test
    fun `deterministisk opptjeningsvurderingId migreres inn i V345-migreringen, mens det legges random id på nye vilkårsgrunnlag`() {
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
                    assertEquals(UUID.nameUUIDFromBytes("${grunnlag.vilkårsgrunnlagId}:Opptjening".toByteArray()),
                        (grunnlag as VilkårsgrunnlagUtDto.Spleis).opptjeningsvurderingId)
                }
                it.vilkårsgrunnlag[1].also { grunnlag ->
                    assertEquals(1.mars, grunnlag.skjæringstidspunkt)
                    assertNotEquals(UUID.nameUUIDFromBytes("${grunnlag.vilkårsgrunnlagId}:Opptjening".toByteArray()),
                        (grunnlag as VilkårsgrunnlagUtDto.Spleis).opptjeningsvurderingId)
                }
            }
            historikk[1].also {
                assertEquals(1, it.vilkårsgrunnlag.size)
                it.vilkårsgrunnlag[0].also { grunnlag ->
                    assertEquals(1.januar, grunnlag.skjæringstidspunkt)
                    assertEquals(UUID.nameUUIDFromBytes("${grunnlag.vilkårsgrunnlagId}:Opptjening".toByteArray()),
                        (grunnlag as VilkårsgrunnlagUtDto.Spleis).opptjeningsvurderingId)
                }
            }
        }
        assertGjenoppbygget(dto())
    }

    @Test
    fun `infotrygdgrunnlag får også en deterministisk opptjeningsvurderingId`() {
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
                    assertEquals(UUID.nameUUIDFromBytes("${grunnlag.vilkårsgrunnlagId}:Opptjening".toByteArray()),
                        (grunnlag as VilkårsgrunnlagUtDto.Infotrygd).opptjeningsvurderingId)
                }
                it.vilkårsgrunnlag[1].also { grunnlag ->
                    assertEquals(1.april, grunnlag.skjæringstidspunkt)
                    assertNotEquals(UUID.nameUUIDFromBytes("${grunnlag.vilkårsgrunnlagId}:Opptjening".toByteArray()),
                        (grunnlag as VilkårsgrunnlagUtDto.Spleis).opptjeningsvurderingId)
                }
            }
            historikk[1].also {
                assertEquals(1, it.vilkårsgrunnlag.size)
                it.vilkårsgrunnlag[0].also { grunnlag ->
                    assertTrue(grunnlag is VilkårsgrunnlagUtDto.Infotrygd)
                    assertEquals(1.januar, grunnlag.skjæringstidspunkt)
                }
            }
            historikk[2].also {
                assertEquals(1, it.vilkårsgrunnlag.size)
                it.vilkårsgrunnlag[0].also { grunnlag ->
                    assertTrue(grunnlag is VilkårsgrunnlagUtDto.Infotrygd)
                    assertEquals(1.januar, grunnlag.skjæringstidspunkt)
                }
            }
        }

        assertGjenoppbygget(dto())
    }

}
