package no.nav.helse.etterlevelse

import java.time.LocalDate
import no.nav.helse.etterlevelse.Ledd.Companion.ledd
import no.nav.helse.etterlevelse.Subsumsjon.Utfall
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_OPPFYLT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EnkelSubsumsjonTest {

    private val vurderinger: MutableList<Subsumsjon> = mutableListOf()

    @BeforeEach
    fun beforeEach() {
        vurderinger.clear()
    }

    @Test
    fun `enkel vurdering`() {
        nyVurdering()
        assertEquals(1, vurderinger.size)
    }

    @Test
    fun `vurderinger med ulike data utgjør hvert sitt innslag`() {
        nyVurdering(VILKAR_OPPFYLT)
        nyVurdering(VILKAR_IKKE_OPPFYLT)

        assertEquals(2, vurderinger.size)
    }

    @Test
    fun equality() {
        val utfall = VILKAR_OPPFYLT
        val lovverk = "folketrygdloven"
        val versjon = LocalDate.MAX
        val paragraf = Paragraf.PARAGRAF_8_2
        val ledd  = 1.ledd
        val punktum: Punktum = Punktum.PUNKTUM_1
        val bokstav = Bokstav.BOKSTAV_A
        val input = mapOf("foo" to "bar")
        val output = mapOf("bar" to "baz")
        val kontekster = emptyMap<String, KontekstType>()

        val enkel = EnkelSubsumsjon(utfall, lovverk, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster)
        val enkelKopi = EnkelSubsumsjon(utfall, lovverk, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster)

        assertEquals(enkel, enkel)
        assertEquals(enkel, enkelKopi)
    }

    private fun nyVurdering(
        utfall: Utfall = VILKAR_OPPFYLT,
        lovverk: String = "folketrygdloven",
        versjon: LocalDate = LocalDate.MAX,
        paragraf: Paragraf = Paragraf.PARAGRAF_8_2,
        ledd: Ledd = 1.ledd,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any> = emptyMap(),
        output: Map<String, Any> = emptyMap(),
        kontekster: Map<String, KontekstType> = emptyMap()
    ) {
        EnkelSubsumsjon(utfall, lovverk, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster).also {
            vurderinger.add(it)
        }
    }
}
