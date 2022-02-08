package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BetingetSubsumsjonTest {

    private val observatør get() = SubsumsjonObservatør()
    private lateinit var vurderinger: List<Subsumsjon>

    @BeforeEach
    fun beforeEach() {
        vurderinger = emptyList()
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
        vurderinger = BetingetSubsumsjon(funnetRelevant, utfall, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster).sammenstill(vurderinger)
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
