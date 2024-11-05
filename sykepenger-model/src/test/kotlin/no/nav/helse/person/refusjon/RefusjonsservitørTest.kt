package no.nav.helse.person.refusjon

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class RefusjonsservitørTest {

    @Test
    fun `rester etter servering`() {
        val refusjonstidslinje = Beløpstidslinje.fra(januar, 100.daglig, kilde)
        val servitør = Refusjonsservitør.fra(refusjonstidslinje)
        val suppekjøkken = Refusjonsservitør()
        servitør.servér(1.januar, 10.januar til 15.januar)
        servitør.servér(suppekjøkken, Aktivitetslogg())
        assertNotNull(suppekjøkken[1.januar])
        assertNull(suppekjøkken[2.januar])
        assertEquals(listOf(1.januar til 9.januar, 16.januar til 31.januar), suppekjøkken[1.januar]!!.perioderMedBeløp)
    }

    @Test
    fun `rester etter servering med fler startdatoer`() {
        val refusjonstidslinje = Beløpstidslinje.fra(januar, 100.daglig, kilde)
        val servitør = Refusjonsservitør.fra(refusjonstidslinje, setOf(5.januar, 16.januar))
        val suppekjøkken = Refusjonsservitør()
        servitør.servér(2.januar, 10.januar til 12.januar)
        servitør.servér(16.januar, 23.januar til 25.januar)
        servitør.servér(suppekjøkken, Aktivitetslogg())

        assertNull(suppekjøkken[4.januar])
        assertNotNull(suppekjøkken[5.januar])
        assertNotNull(suppekjøkken[16.januar])
        assertNull(suppekjøkken[17.januar])

        assertEquals(listOf(5.januar til 9.januar, 13.januar til 15.januar), suppekjøkken[5.januar]!!.perioderMedBeløp)
        assertEquals(listOf(16.januar til 22.januar, 26.januar til 31.januar), suppekjøkken[16.januar]!!.perioderMedBeløp)
    }

    private companion object {
        private val kilde = Kilde(UUID.randomUUID(), Avsender.ARBEIDSGIVER, LocalDateTime.now())
    }
}