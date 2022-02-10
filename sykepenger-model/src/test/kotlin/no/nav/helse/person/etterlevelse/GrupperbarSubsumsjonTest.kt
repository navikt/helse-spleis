package no.nav.helse.person.etterlevelse

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.VILKAR_OPPFYLT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GrupperbarSubsumsjonTest {

    private val observatør get() = SubsumsjonObservatør()

    private lateinit var vurderinger: List<Subsumsjon>

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
        observatør.assertVurdering(vurderinger.first(), 1.januar til 10.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre`() {
        nyVurdering(1.januar)
        nyVurdering(2.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar til 2.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre - motsatt rekkefølge`() {
        nyVurdering(2.januar)
        nyVurdering(1.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar til 2.januar)
    }

    @Test
    fun `vurderinger som ikke ligger inntil hverandre blir gruppert som separate perioder`() {
        nyVurdering(1.januar)
        nyVurdering(3.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar til 1.januar, 3.januar til 3.januar)
    }

    @Test
    fun `vurderinger som ikke ligger inntil hverandre blir gruppert som separate perioder - motsatt rekkefølge`() {
        nyVurdering(3.januar)
        nyVurdering(1.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar til 1.januar, 3.januar til 3.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre inklusiv helg`() {
        nyVurdering(5.januar)
        nyVurdering(8.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 5.januar til 8.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre inni helg`() {
        nyVurdering(5.januar)
        nyVurdering(7.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 5.januar til 7.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre inni helg - eksisterende slutter i helg`() {
        nyVurdering(6.januar)
        nyVurdering(8.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 6.januar til 8.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre inklusiv helg - motsatt rekkefølge`() {
        nyVurdering(8.januar)
        nyVurdering(5.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 5.januar til 8.januar)
    }

    @Test
    fun `Grupperer vurderinger som overlapper`() {
        nyVurdering(1.januar)
        nyVurdering(2.januar)
        nyVurdering(3.januar)
        nyVurdering(2.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar til 3.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger i helg`() {
        nyVurdering(6.januar)
        nyVurdering(7.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 6.januar til 7.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger i helg - motsatt rekkefølge`() {
        nyVurdering(7.januar)
        nyVurdering(6.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 6.januar til 7.januar)
    }

    @Test
    fun `Grupperer bare vurderinger som ligger inntil hverandre som kommer Out-Of-Order`() {
        nyVurdering(4.januar)
        nyVurdering(1.januar)
        nyVurdering(2.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar til 2.januar, 4.januar til 4.januar)
    }

    @Test
    fun `Grupperer alle vurderingersom ligger inntil hverandre selv om de kommer Out-Of-Order`() {
        nyVurdering(2.januar)
        nyVurdering(4.januar)
        nyVurdering(1.januar)
        nyVurdering(3.januar)
        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar til 4.januar)
    }

    @Test
    fun `Grupperer vurderinger som ligger inntil hverandre med helg`() {
        nyVurdering(2.januar)
        nyVurdering(4.januar)
        nyVurdering(1.januar)
        nyVurdering(3.januar)

        assertEquals(1, vurderinger.size)
        observatør.assertVurdering(vurderinger.first(), 1.januar til 4.januar)
    }

    @Test
    fun `Grupperer ikke ting som har ulik input`() {
        nyVurdering(1.januar, input = mapOf("key" to "value"))
        nyVurdering(2.januar, input = mapOf("key" to "other value"))
        assertEquals(2, vurderinger.size)
        observatør.assertVurdering(vurderinger[0], 1.januar til 1.januar)
        observatør.assertVurdering(vurderinger[1], 2.januar til 2.januar)
    }

    @Test
    fun `Grupperer ikke ting som har ulik input - motsatt rekkefølge`() {
        nyVurdering(2.januar, input = mapOf("key" to "value"))
        nyVurdering(1.januar, input = mapOf("key" to "other value"))
        assertEquals(2, vurderinger.size)
        observatør.assertVurdering(vurderinger[0], 2.januar til 2.januar)
        observatør.assertVurdering(vurderinger[1], 1.januar til 1.januar)
    }

    @Test
    fun `Grupperer ikke ting som har ulik  output`() {
        nyVurdering(1.januar, output = mapOf("key" to "value"))
        nyVurdering(2.januar, output = mapOf("key" to "other value"))
        assertEquals(2, vurderinger.size)
        observatør.assertVurdering(vurderinger[0], 1.januar til 1.januar)
        observatør.assertVurdering(vurderinger[1], 2.januar til 2.januar)
    }

    @Test
    fun `Grupperer ikke ting som har ulik  output - motsatt rekkefølge`() {
        nyVurdering(2.januar, output = mapOf("key" to "value"))
        nyVurdering(1.januar, output = mapOf("key" to "other value"))
        assertEquals(2, vurderinger.size)
        observatør.assertVurdering(vurderinger[0], 2.januar til 2.januar)
        observatør.assertVurdering(vurderinger[1], 1.januar til 1.januar)
    }

    private fun nyVurdering(dato: LocalDate, input: Map<String, Any> = emptyMap(), output: Map<String, Any> = emptyMap()) {
        vurderinger = GrupperbarSubsumsjon(
            dato = dato,
            input = input,
            output = output,
            utfall = VILKAR_OPPFYLT,
            versjon = LocalDate.MAX,
            paragraf = Paragraf.PARAGRAF_8_2,
            ledd = 1.ledd,
            kontekster = emptyMap()
        ).sammenstill(vurderinger)
    }

    private class SubsumsjonObservatør : SubsumsjonVisitor {
        private val perioder: MutableList<Periode> = mutableListOf()
        private lateinit var output: Map<String, Any>
        override fun visitGrupperbarSubsumsjon(perioder: List<Periode>) {
            this.perioder.addAll(perioder)
        }

        fun assertVurdering(subsumsjon: Subsumsjon, vararg forventedePerioder: Periode) {
            require(subsumsjon is GrupperbarSubsumsjon)
            subsumsjon.accept(this)
            assertEquals(forventedePerioder.toList(), this.perioder)
            assertEquals(forventedePerioder.map {
                mapOf(
                    "fom" to it.start,
                    "tom" to it.endInclusive
                )
            }, output["perioder"])
        }

        override fun preVisitSubsumsjon(
            utfall: Subsumsjon.Utfall,
            versjon: LocalDate,
            paragraf: Paragraf,
            ledd: Ledd,
            punktum: Punktum?,
            bokstav: Bokstav?,
            input: Map<String, Any>,
            output: Map<String, Any>,
            kontekster: Map<String, String>
        ) {
            this.output = output
        }
    }
}
