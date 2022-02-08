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
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.VILKAR_OPPFYLT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SubsumsjonTest {

    @Test
    fun testEquals() {
        val paragraf1 = testParagraf(
            VILKAR_OPPFYLT,
            LocalDate.MIN,
            paragraf = PARAGRAF_2,
            ledd = 1.ledd,
            bokstav = BOKSTAV_A,
            punktum = PUNKTUM_1,
            input = mapOf("a" to "a"),
            output = mapOf("b" to "b")
        )
        val paragraf2 = testParagraf(
            VILKAR_OPPFYLT,
            LocalDate.MIN,
            paragraf = PARAGRAF_2,
            ledd = 1.ledd,
            bokstav = BOKSTAV_A,
            punktum = PUNKTUM_1,
            input = mapOf("a" to "a"),
            output = mapOf("b" to "b")
        )
        assertEquals(paragraf1, paragraf2)
    }

    @Test
    fun testEqualsByRef() {
        val paragraf1 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        assertEquals(paragraf1, paragraf1)
    }

    @Test
    fun `ulik når input varierer`() {
        val paragraf1 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, input = mapOf("a" to "a"))
        val paragraf2 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, input = mapOf("b" to "b"))
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når output varierer`() {
        val paragraf1 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, output = mapOf("a" to "a"))
        val paragraf2 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, output = mapOf("b" to "b"))
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når oppfylt varierer`() {
        val paragraf1 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(VILKAR_IKKE_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når versjon varierer`() {
        val paragraf1 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(VILKAR_OPPFYLT, LocalDate.MAX, paragraf = PARAGRAF_2, ledd = 1.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når paragraf varierer`() {
        val paragraf1 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_8_16, ledd = 1.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når ledd varierer`() {
        val paragraf1 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd)
        val paragraf2 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 2.ledd)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når bokstav varierer`() {
        val paragraf1 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, bokstav = BOKSTAV_A)
        val paragraf2 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, bokstav = BOKSTAV_B)
        assertNotEquals(paragraf1, paragraf2)
    }

    @Test
    fun `ulik når punktum varierer`() {
        val paragraf1 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, punktum = PUNKTUM_1)
        val paragraf2 = testParagraf(VILKAR_OPPFYLT, LocalDate.MIN, paragraf = PARAGRAF_2, ledd = 1.ledd, punktum = PUNKTUM_2)
        assertNotEquals(paragraf1, paragraf2)
    }

    private fun testParagraf(
        utfall: Utfall,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        bokstav: Bokstav? = null,
        punktum: Punktum? = null,
        input: Map<String, Any> = mapOf(),
        output: Map<String, Any> = mapOf(),
        kontekster: Map<String, KontekstType> = mapOf()
    ) = object : Subsumsjon() {
        override val utfall = utfall
        override val versjon = versjon
        override val paragraf = paragraf
        override val ledd = ledd
        override val bokstav: Bokstav? = bokstav
        override val punktum: Punktum? = punktum
        override val input = input
        override val output = output
        override val kontekster = kontekster

        override fun sammenstill(subsumsjoner: List<Subsumsjon>): List<Subsumsjon> = emptyList()

        override fun acceptSpesifikk(visitor: SubsumsjonVisitor) {}
    }
}
