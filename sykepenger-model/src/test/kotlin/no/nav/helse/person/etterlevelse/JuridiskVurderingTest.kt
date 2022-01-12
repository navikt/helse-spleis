package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Bokstav.BOKSTAV_A
import no.nav.helse.person.Bokstav.BOKSTAV_B
import no.nav.helse.person.Ledd
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Paragraf.PARAGRAF_2
import no.nav.helse.person.Paragraf.PARAGRAF_8_16
import no.nav.helse.person.Punktum
import no.nav.helse.person.Punktum.PUNKTUM_1
import no.nav.helse.person.Punktum.PUNKTUM_2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class JuridiskVurderingTest {

    @Test
    fun testEquals() {
        val paragraf1 = testParagraf(
            true,
            LocalDate.MIN,
            paragraf = PARAGRAF_2,
            ledd = 1.ledd,
            bokstav = listOf(BOKSTAV_A),
            punktum = listOf(PUNKTUM_1),
            input = mapOf("a" to "a"),
            output = mapOf("b" to "b")
        )
        val paragraf2 = testParagraf(
            true,
            LocalDate.MIN,
            paragraf = PARAGRAF_2,
            ledd = 1.ledd,
            bokstav = listOf(BOKSTAV_A),
            punktum = listOf(PUNKTUM_1),
            input = mapOf("a" to "a"),
            output = mapOf("b" to "b")
        )
        assertEquals(paragraf1, paragraf2)
    }

    @Test
    fun testEqualsByRef() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        assertEquals(paragraf1, paragraf1)
    }

    @Test
    fun `ulik når input varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, input = mapOf("a" to "a"))
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, input = mapOf("b" to "b"))
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når output varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, output = mapOf("a" to "a"))
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, output = mapOf("b" to "b"))
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når oppfylt varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(false, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når versjon varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(true, LocalDate.MAX, paragraf = PARAGRAF_2, ledd = 1.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når paragraf varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_8_16, ledd = 1.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når ledd varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 2.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når bokstav varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, bokstav = listOf(BOKSTAV_A))
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, bokstav = listOf(BOKSTAV_B))
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når punktum varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, punktum = listOf(PUNKTUM_1))
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, punktum = listOf(PUNKTUM_2))
        assertNotEquals(paragraf1, paragraf2)
    }

    private fun testParagraf(
        oppfylt: Boolean,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        bokstav: List<Bokstav> = listOf(),
        punktum: List<Punktum> = listOf(),
        input: Map<String, Any> = mapOf(),
        output: Map<String, Any> = mapOf()
    ) = object : JuridiskVurdering() {
        override val oppfylt = oppfylt
        override val versjon = versjon
        override val paragraf = paragraf
        override val ledd = ledd
        override val bokstaver = bokstav
        override val punktum = punktum
        override val input = input
        override val output = output

        override fun sammenstill(vurderinger: List<JuridiskVurdering>): List<JuridiskVurdering> {
            TODO("Not yet implemented")
        }

        override fun acceptSpesifikk(visitor: JuridiskVurderingVisitor) {}
    }
}
