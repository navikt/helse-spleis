package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrupperbarVurderingTest {

    @Test
    fun `Grupperer bare vurderinger som ligger inntil hverandre som kommer Out-Of-Order`() {
        var vurderinger = emptyList<JuridiskVurdering>()

        vurderinger = grupperbarVurdering(4.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(1.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(2.januar).sammenstill(vurderinger)
        assertEquals(2, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 2.januar)
        observatør.assertVurdering(vurderinger[1], 4.januar, 4.januar)
    }

    @Test
    fun `Grupperer alle vurderingersom ligger inntil hverandre selv om de kommer Out-Of-Order`() {
        var vurderinger = emptyList<JuridiskVurdering>()

        vurderinger = grupperbarVurdering(2.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(4.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(1.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(3.januar).sammenstill(vurderinger)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 4.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre med helg`() {
        var vurderinger = emptyList<JuridiskVurdering>()

        vurderinger = grupperbarVurdering(2.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(4.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(1.januar).sammenstill(vurderinger)
        vurderinger = grupperbarVurdering(3.januar).sammenstill(vurderinger)

        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 4.januar)
    }

    private fun grupperbarVurdering(dato: LocalDate) = GrupperbarVurdering(dato, mapOf(), mapOf(), true, LocalDate.MAX, Paragraf.PARAGRAF_8_2, 1.ledd)

    private val observatør get() = GrupperbarVurderingObservatør()

    class GrupperbarVurderingObservatør : GrupperbarVurderingVisitor {
        private lateinit var fom: LocalDate
        private lateinit var tom: LocalDate
        override fun visitVurdering(fom: LocalDate, tom: LocalDate) {
            this.fom = fom
            this.tom = tom
        }

        fun assertVurdering(juridiskVurdering: JuridiskVurdering, fom: LocalDate, tom: LocalDate) {
            require(juridiskVurdering is GrupperbarVurdering)
            juridiskVurdering.accept(this)
            assertEquals(fom, this.fom)
            assertEquals(tom, this.tom)
        }
    }
}
