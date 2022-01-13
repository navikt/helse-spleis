package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrupperbarVurderingTest {

    private val observatør get() = JuridiskVurderingObservatør()

    private lateinit var vurderinger: List<JuridiskVurdering>

    @BeforeEach
    fun beforeEach() {
        vurderinger = emptyList()
    }

    @Test
    fun `Vurderinger på dagnivå blir slått sammen`() {
        repeat(10) {
            nyVurdering(1.januar.plusDays(it.toLong()))
        }
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 10.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre`() {
        nyVurdering(1.januar)
        nyVurdering(2.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 2.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre - motsatt rekkefølge`() {
        nyVurdering(2.januar)
        nyVurdering(1.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 2.januar)
    }

    @Test
    fun `Grupperer ikke vurderinger som ikke ligger inntil hverandre`() {
        nyVurdering(1.januar)
        nyVurdering(3.januar)
        assertEquals(2, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 1.januar)
        observatør.assertVurdering(vurderinger[1], 3.januar, 3.januar)
    }

    @Test
    fun `Grupperer ikke vurderinger som ikke ligger inntil hverandre - motsatt rekkefølge`() {
        nyVurdering(3.januar)
        nyVurdering(1.januar)
        assertEquals(2, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 1.januar)
        observatør.assertVurdering(vurderinger[1], 3.januar, 3.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre inklusiv helg`() {
        nyVurdering(5.januar)
        nyVurdering(8.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 5.januar, 8.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre inni helg`() {
        nyVurdering(5.januar)
        nyVurdering(7.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 5.januar, 7.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre inni helg - eksisterende slutter i helg`() {
        nyVurdering(6.januar)
        nyVurdering(8.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 6.januar, 8.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre inklusiv helg - motsatt rekkefølge`() {
        nyVurdering(8.januar)
        nyVurdering(5.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 5.januar, 8.januar)
    }

    @Test
    fun `Grupperer vurderinger som overlapper`() {
        nyVurdering(1.januar)
        nyVurdering(2.januar)
        nyVurdering(3.januar)
        nyVurdering(2.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 3.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger i helg`() {
        nyVurdering(6.januar)
        nyVurdering(7.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 6.januar, 7.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger i helg - motsatt rekkefølge`() {
        nyVurdering(7.januar)
        nyVurdering(6.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 6.januar, 7.januar)
    }

    @Test
    fun `Grupperer bare vurderinger som ligger inntil hverandre som kommer Out-Of-Order`() {
        nyVurdering(4.januar)
        nyVurdering(1.januar)
        nyVurdering(2.januar)
        assertEquals(2, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 2.januar)
        observatør.assertVurdering(vurderinger[1], 4.januar, 4.januar)
    }

    @Test
    fun `Grupperer alle vurderingersom ligger inntil hverandre selv om de kommer Out-Of-Order`() {
        nyVurdering(2.januar)
        nyVurdering(4.januar)
        nyVurdering(1.januar)
        nyVurdering(3.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 4.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre med helg`() {
        nyVurdering(2.januar)
        nyVurdering(4.januar)
        nyVurdering(1.januar)
        nyVurdering(3.januar)

        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar, 4.januar)
    }

    private fun nyVurdering(dato: LocalDate) {
        vurderinger = GrupperbarVurdering(
            dato = dato,
            input = mapOf(),
            output = mapOf(),
            oppfylt = true,
            versjon = LocalDate.MAX,
            paragraf = Paragraf.PARAGRAF_8_2,
            ledd = 1.ledd,
            kontekster = emptyMap()
        ).sammenstill(vurderinger)
    }

    private class JuridiskVurderingObservatør : JuridiskVurderingVisitor {
        private lateinit var fom: LocalDate
        private lateinit var tom: LocalDate
        override fun visitGrupperbarVurdering(fom: LocalDate, tom: LocalDate) {
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
