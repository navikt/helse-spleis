package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ParagrafIKodeTest {

    fun testParagraf(
        oppfylt: Boolean,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        bokstav: List<Bokstav> = listOf(),
        punktum: List<Punktum> = listOf(),
        input: Map<String, Any> = mapOf(),
        output: Map<String, Any> = mapOf()
    ) = object : ParagrafIKode() {
        override val oppfylt = oppfylt
        override val versjon = versjon
        override val paragraf = paragraf
        override val ledd = ledd
        override val bokstav = bokstav
        override val punktum = punktum
        override val input = input
        override val output = output

        override fun aggreger(vurderinger: Set<ParagrafIKode>): ParagrafIKode {
            TODO("Not yet implemented")
        }

    }

    @Test
    fun testEquals() {
        val paragraf1 = testParagraf(
            true,
            LocalDate.MIN,
            paragraf = Paragraf.PARAGRAF_2,
            ledd = 1.ledd,
            bokstav = listOf(Bokstav.BOKSTAV_A),
            punktum = listOf(Punktum.PUNKTUM_1)
        )
        val paragraf2 = testParagraf(
            true,
            LocalDate.MIN,
            paragraf = Paragraf.PARAGRAF_2,
            ledd = 1.ledd,
            bokstav = listOf(Bokstav.BOKSTAV_A),
            punktum = listOf(Punktum.PUNKTUM_1)
        )
        assertEquals(paragraf1, paragraf2)
    }

    @Test
    fun testEqualsByRef() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd)
        assertEquals(paragraf1, paragraf1)
    }

    @Test
    fun `like selv om input og eller output varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd, input = mapOf("a" to "a"))
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd, input = mapOf("b" to "b"))
        assertEquals(paragraf1, paragraf2)
        val paragraf3 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd, output = mapOf("a" to "a"))
        val paragraf4 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd, output = mapOf("b" to "b"))
        assertEquals(paragraf3, paragraf4)
    }


    @Test
    fun `ulik når oppfylt varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(false, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når versjon varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(true, LocalDate.MAX, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når paragraf varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_8_16, ledd = 1.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når ledd varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 2.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når bokstav varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd, bokstav = listOf(Bokstav.BOKSTAV_A))
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd, bokstav = listOf(Bokstav.BOKSTAV_B))
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når punktum varierer`() {
        val paragraf1 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd, punktum = listOf(Punktum.PUNKTUM_1))
        val paragraf2 = testParagraf(true, LocalDate.MIN, paragraf = Paragraf.PARAGRAF_2, ledd = 1.ledd, punktum = listOf(Punktum.PUNKTUM_2))
        assertNotEquals(paragraf1, paragraf2)
    }

}
