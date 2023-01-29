package no.nav.helse.person.etterlevelse

import java.time.LocalDate
import no.nav.helse.person.etterlevelse.Ledd.Companion.ledd
import no.nav.helse.person.etterlevelse.MaskinellJurist.KontekstType
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.VILKAR_OPPFYLT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EnkelSubsumsjonTest {

    private lateinit var vurderinger: List<Subsumsjon>

    @BeforeEach
    fun beforeEach() {
        vurderinger = emptyList()
    }

    @Test
    fun `enkel vurdering`() {
        nyVurdering()
        assertEquals(1, vurderinger.size)
    }

    @Test
    fun `enkel vurdering som gjøres flere ganger innenfor samme hendelse forekommer kun en gang`() {
        nyVurdering()
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
        val versjon = LocalDate.MAX
        val paragraf = Paragraf.PARAGRAF_8_2
        val ledd  = 1.ledd
        val punktum: Punktum = Punktum.PUNKTUM_1
        val bokstav = Bokstav.BOKSTAV_A
        val input = mapOf("foo" to "bar")
        val output = mapOf("bar" to "baz")
        val kontekster = emptyMap<String, KontekstType>()

        val enkel = EnkelSubsumsjon(utfall, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster)
        val enkelKopi = EnkelSubsumsjon(utfall, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster)
        val betinget = BetingetSubsumsjon(true, utfall, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster)

        assertEquals(enkel, enkel)
        assertEquals(enkel, enkelKopi)
        assertNotEquals(enkel, betinget)
    }

    private fun nyVurdering(
        utfall: Utfall = VILKAR_OPPFYLT,
        versjon: LocalDate = LocalDate.MAX,
        paragraf: Paragraf = Paragraf.PARAGRAF_8_2,
        ledd: Ledd = 1.ledd,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any> = emptyMap(),
        output: Map<String, Any> = emptyMap(),
        kontekster: Map<String, KontekstType> = emptyMap()
    ) {
        vurderinger = EnkelSubsumsjon(utfall, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster).sammenstill(vurderinger)
    }
}
