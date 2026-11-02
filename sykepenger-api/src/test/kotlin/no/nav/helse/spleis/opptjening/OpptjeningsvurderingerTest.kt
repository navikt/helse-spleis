package no.nav.helse.spleis.opptjening

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.dto.serialisering.VilkårsgrunnlagInnslagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class OpptjeningsvurderingerTest {
    @Test
    fun `tom historikk gir ingen opptjeningsvurderinger`() {
        assertEquals(emptyList(), opptjeningsvurderinger(emptyList()))
    }

    @Test
    fun `hopper over rettighetskategorigrunnlag`() {
        val komplett = vilkårsgrunnlag()
        val rettighetskategorigrunnlag =
            VilkårsgrunnlagUtDto.Rettighetskategori(
                vilkårsgrunnlagId = UUID.randomUUID(),
                skjæringstidspunkt = LocalDate.of(2026, 1, 1),
                rettighetskategoriId = UUID.randomUUID(),
            )
        val historikk = listOf(historikkinnslag("2026-01-01T12:00:00", rettighetskategorigrunnlag, komplett))

        val resultat = opptjeningsvurderinger(historikk)

        assertEquals(listOf(komplett), resultat.map { it.vilkårsgrunnlag })
    }

    @Test
    fun `beholder unike opptjeningsvurderinger i opprinnelig rekkefølge`() {
        val første = vilkårsgrunnlag()
        val andre = vilkårsgrunnlag()
        val historikk =
            listOf(
                historikkinnslag("2026-01-02T12:00:00", første),
                historikkinnslag("2026-01-01T12:00:00", andre),
            )

        val resultat = opptjeningsvurderinger(historikk)

        assertEquals(listOf(første, andre), resultat.map { it.vilkårsgrunnlag })
    }

    @Test
    fun `bruker tidligste opprettet når opptjeningsvurderingen finnes flere ganger`() {
        val opptjeningsvurderingId = UUID.randomUUID()
        val nyesteVilkårsgrunnlag = vilkårsgrunnlag(opptjeningsvurderingId)
        val eldsteVilkårsgrunnlag = vilkårsgrunnlag(opptjeningsvurderingId)
        val historikk =
            listOf(
                historikkinnslag("2026-01-03T12:00:00", nyesteVilkårsgrunnlag),
                historikkinnslag("2026-01-01T12:00:00", eldsteVilkårsgrunnlag),
                historikkinnslag("2026-01-02T12:00:00", nyesteVilkårsgrunnlag),
            )

        val resultat = opptjeningsvurderinger(historikk).single()

        assertEquals(nyesteVilkårsgrunnlag, resultat.vilkårsgrunnlag)
        assertEquals(LocalDateTime.parse("2026-01-01T12:00:00"), resultat.opprettet)
    }

    @Test
    fun `beholder rekkefølgen og bruker tidligste opprettet for hver unike opptjeningsvurdering`() {
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

        val resultat = opptjeningsvurderinger(historikk)

        assertEquals(listOf(første, andre), resultat.map { it.vilkårsgrunnlag })
        assertEquals(
            listOf(
                LocalDateTime.parse("2026-01-02T12:00:00"),
                LocalDateTime.parse("2026-01-01T12:00:00"),
            ),
            resultat.map { it.opprettet },
        )
    }

    private fun vilkårsgrunnlag(opptjeningsvurderingId: UUID = UUID.randomUUID()) =
        mockk<VilkårsgrunnlagUtDto.Komplett> {
            every { this@mockk.opptjeningsvurderingId } returns opptjeningsvurderingId
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
