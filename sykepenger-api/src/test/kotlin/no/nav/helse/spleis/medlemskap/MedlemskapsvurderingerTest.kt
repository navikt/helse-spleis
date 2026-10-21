package no.nav.helse.spleis.medlemskap

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.dto.serialisering.VilkårsgrunnlagInnslagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class MedlemskapsvurderingerTest {
    @Test
    fun `tom historikk gir ingen medlemskapsvurderinger`() {
        assertEquals(emptyList(), medlemskapsvurderinger(emptyList()))
    }

    @Test
    fun `beholder unike medlemskapsvurderinger i opprinnelig rekkefølge`() {
        val første = vilkårsgrunnlag()
        val andre = vilkårsgrunnlag()
        val historikk =
            listOf(
                historikkinnslag("2026-01-02T12:00:00", første),
                historikkinnslag("2026-01-01T12:00:00", andre),
            )

        val resultat = medlemskapsvurderinger(historikk)

        assertEquals(listOf(første, andre), resultat.map { it.vilkårsgrunnlag })
    }

    @Test
    fun `bruker tidligste opprettet når medlemskapsvurderingen finnes flere ganger`() {
        val medlemskapsvurderingId = UUID.randomUUID()
        val nyesteVilkårsgrunnlag = vilkårsgrunnlag(medlemskapsvurderingId)
        val eldsteVilkårsgrunnlag = vilkårsgrunnlag(medlemskapsvurderingId)
        val historikk =
            listOf(
                historikkinnslag("2026-01-03T12:00:00", nyesteVilkårsgrunnlag),
                historikkinnslag("2026-01-01T12:00:00", eldsteVilkårsgrunnlag),
                historikkinnslag("2026-01-02T12:00:00", nyesteVilkårsgrunnlag),
            )

        val resultat = medlemskapsvurderinger(historikk).single()

        assertEquals(nyesteVilkårsgrunnlag, resultat.vilkårsgrunnlag)
        assertEquals(LocalDateTime.parse("2026-01-01T12:00:00"), resultat.opprettet)
    }

    @Test
    fun `beholder rekkefølgen og bruker tidligste opprettet for hver unike medlemskapsvurdering`() {
        val førsteId = UUID.randomUUID()
        val andreId = UUID.randomUUID()
        val første = vilkårsgrunnlag(førsteId)
        val andre = vilkårsgrunnlag(andreId)
        val historikk =
            listOf(
                historikkinnslag("2026-01-04T12:00:00", første, andre),
                historikkinnslag("2026-01-01T12:00:00", andre),
                historikkinnslag("2026-01-02T12:00:00", første),
            )

        val resultat = medlemskapsvurderinger(historikk)

        assertEquals(listOf(første, andre), resultat.map { it.vilkårsgrunnlag })
        assertEquals(
            listOf(
                LocalDateTime.parse("2026-01-02T12:00:00"),
                LocalDateTime.parse("2026-01-01T12:00:00"),
            ),
            resultat.map { it.opprettet },
        )
    }

    private fun vilkårsgrunnlag(medlemskapsvurderingId: UUID = UUID.randomUUID()) =
        mockk<VilkårsgrunnlagUtDto> {
            every { this@mockk.medlemskapsvurderingId } returns medlemskapsvurderingId
        }

    private fun historikkinnslag(
        opprettet: String,
        vararg vilkårsgrunnlag: VilkårsgrunnlagUtDto,
    ) = VilkårsgrunnlagInnslagUtDto(
        id = UUID.randomUUID(),
        opprettet = LocalDateTime.parse(opprettet),
        vilkårsgrunnlag = vilkårsgrunnlag.toList(),
    )
}
