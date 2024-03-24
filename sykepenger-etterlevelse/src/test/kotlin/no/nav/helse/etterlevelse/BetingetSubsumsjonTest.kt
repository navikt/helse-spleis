package no.nav.helse.etterlevelse

import java.time.LocalDate
import no.nav.helse.etterlevelse.Ledd.Companion.ledd
import no.nav.helse.etterlevelse.Subsumsjon.Utfall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BetingetSubsumsjonTest {

    private val observatør get() = SubsumsjonObservatør()
    private val vurderinger: MutableList<Subsumsjon> = mutableListOf()

    @BeforeEach
    fun beforeEach() {
        vurderinger.clear()
    }

    @Test
    fun `Legger til betingede vurderinger`() {
        nyVurdering()
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), true)
    }

    @Test
    fun `En betinget vurdering erstatter en annen dersom de er like`() {
        nyVurdering()
        nyVurdering()
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), true)
    }

    @Test
    fun `En betinget vurdering blir ikke lagt til dersom betingelsen ikke er oppfylt`() {
        nyVurdering(false)
        assertEquals(0, vurderinger.size)
    }

    private fun nyVurdering(
        funnetRelevant: Boolean = true,
        lovverk: String = "folketrygdloven",
        utfall: Utfall = Utfall.VILKAR_OPPFYLT,
        versjon: LocalDate = LocalDate.MAX,
        paragraf: Paragraf = Paragraf.PARAGRAF_8_2,
        ledd: Ledd = 1.ledd,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        input: Map<String, Any> = emptyMap(),
        output: Map<String, Any> = emptyMap(),
        kontekster: Map<String, KontekstType> = emptyMap()
    ) {
        BetingetSubsumsjon(funnetRelevant, lovverk, utfall, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster).also {
            if (!it.sammenstill(vurderinger)) vurderinger.add(it)
        }
    }

    private class SubsumsjonObservatør : SubsumsjonVisitor {
        private var funnetRelevant: Boolean? = null

        override fun visitBetingetSubsumsjon(funnetRelevant: Boolean) {
            this.funnetRelevant = funnetRelevant
        }

        fun assertVurdering(subsumsjon: Subsumsjon, funnetRelevant: Boolean) {
            require(subsumsjon is BetingetSubsumsjon)
            subsumsjon.accept(this)
            assertEquals(funnetRelevant, this.funnetRelevant)
        }
    }
}
