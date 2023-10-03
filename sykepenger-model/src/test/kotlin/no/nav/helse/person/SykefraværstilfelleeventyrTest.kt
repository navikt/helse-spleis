package no.nav.helse.person

import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Sykefraværstilfelleeventyr.Companion.bliMed
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SykefraværstilfelleeventyrTest {

    @Test
    fun `vedtaksperioder blir et eget sykefraværstilfelle hvis det ikke finnes noen`() {
        val input = listOf(
            Sykefraværstilfelleeventyr(4.januar)
        )

        val vedtaksperiode1 = UUID.randomUUID()
        val result = input.bliMed(vedtaksperiode1, "a1", 2.januar til 3.januar, 2.januar).inspektør

        assertEquals(2, result.event.size)
        result.event[0].also { sykefraværstilfelle ->
            assertEquals(2.januar, sykefraværstilfelle.dato)
            assertEquals(vedtaksperiode1, sykefraværstilfelle.perioder.single().id)
        }
        result.event[1].also { sykefraværstilfelle ->
            assertEquals(4.januar, sykefraværstilfelle.dato)
            assertEquals(0, sykefraværstilfelle.perioder.size)
        }
    }

    @Test
    fun `vedtaksperioder blir riktig skjæringstidspunkt`() {
        val input = listOf(
            Sykefraværstilfelleeventyr(4.januar),
            Sykefraværstilfelleeventyr(1.januar)
        )

        val vedtaksperiode1 = UUID.randomUUID()
        val result = input.bliMed(vedtaksperiode1, "a1", 2.januar til 3.januar, 1.januar).inspektør

        assertEquals(2, result.event.size)
        result.event[0].also { sykefraværstilfelle ->
            assertEquals(1.januar, sykefraværstilfelle.dato)
            assertEquals(vedtaksperiode1, sykefraværstilfelle.perioder.single().id)
        }
        result.event[1].also { sykefraværstilfelle ->
            assertEquals(4.januar, sykefraværstilfelle.dato)
            assertEquals(0, sykefraværstilfelle.perioder.size)
        }
    }

    @Test
    fun `vedtaksperioder uten sykdomsperiode blir nytt skjæringstidspunkt`() {
        val input = listOf(
            Sykefraværstilfelleeventyr(1.januar)
        )

        val vedtaksperiode1 = UUID.randomUUID()
        val vedtaksperiode2 = UUID.randomUUID()
        val result1 = input.bliMed(vedtaksperiode1, "a1", 2.januar til 3.januar, 1.januar)
        val result = result1.bliMed(vedtaksperiode2, "a1", 4.januar til 5.januar, 4.januar).inspektør

        assertEquals(2, result.event.size)
        result.event[0].also { sykefraværstilfelle ->
            assertEquals(1.januar, sykefraværstilfelle.dato)
            assertEquals(vedtaksperiode1, sykefraværstilfelle.perioder.single().id)
        }
        result.event[1].also { sykefraværstilfelle ->
            assertEquals(4.januar, sykefraværstilfelle.dato)
            assertEquals(vedtaksperiode2, sykefraværstilfelle.perioder.single().id)
        }
    }

    @Test
    fun `vedtaksperioder blir med i sykefraværstilfelle`() {
        val input = listOf(
            Sykefraværstilfelleeventyr(1.februar),
            Sykefraværstilfelleeventyr(1.januar)
        )

        val vedtaksperiode1 = UUID.randomUUID()
        val vedtaksperiode2 = UUID.randomUUID()
        val vedtaksperiode3 = UUID.randomUUID()
        val result1 = input.bliMed(vedtaksperiode1, "a1", 2.januar til 5.januar, 1.januar)
        val result2 = result1.bliMed(vedtaksperiode2, "a2", 2.januar til 5.januar, 1.januar)
        val result3 = result2.bliMed(vedtaksperiode3, "a3", 1.februar til 15.februar, 1.februar)

        val inspektør = result3.inspektør

        assertEquals(2, inspektør.event.size)
        inspektør.event[0].also { sykefraværstilfelle ->
            assertEquals(1.januar, sykefraværstilfelle.dato)
            assertEquals(2, sykefraværstilfelle.perioder.size)
            sykefraværstilfelle.perioder[0].also { tilfelle ->
                assertEquals(vedtaksperiode1, tilfelle.id)
                assertEquals("a1", tilfelle.organisasjonsnummer)
                assertEquals(2.januar, tilfelle.fom)
                assertEquals(5.januar, tilfelle.tom)
            }
            sykefraværstilfelle.perioder[1].also { tilfelle ->
                assertEquals(vedtaksperiode2, tilfelle.id)
                assertEquals("a2", tilfelle.organisasjonsnummer)
                assertEquals(2.januar, tilfelle.fom)
                assertEquals(5.januar, tilfelle.tom)
            }
        }
        inspektør.event[1].also { sykefraværstilfelle ->
            assertEquals(1.februar, sykefraværstilfelle.dato)
            assertEquals(1, sykefraværstilfelle.perioder.size)
            sykefraværstilfelle.perioder[0].also { tilfelle ->
                assertEquals(vedtaksperiode3, tilfelle.id)
                assertEquals("a3", tilfelle.organisasjonsnummer)
                assertEquals(1.februar, tilfelle.fom)
                assertEquals(15.februar, tilfelle.tom)
            }
        }
    }
}