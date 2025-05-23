package no.nav.helse.person.refusjon

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.perioderMedBeløp
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class RefusjonsservitørTest {

    @Test
    fun `Ubrukte refusjonsopplysninger håndterer om vi har nyere opplysninger tilbake i tid`() {
        val tidspunkt1 = LocalDateTime.now()
        val ubrukteRefusjonsopplysninger = Refusjonsservitør()

        val im1 = Kilde(MeldingsreferanseId(UUID.randomUUID()), ARBEIDSGIVER, tidspunkt1)
        val im2 = Kilde(MeldingsreferanseId(UUID.randomUUID()), ARBEIDSGIVER, tidspunkt1.plusSeconds(1))

        val beløpstidslinje1 = Beløpstidslinje.fra(5.januar.somPeriode(), 1000.daglig, im1)
        val beløpstidslinje2 = Beløpstidslinje.fra(1.januar.somPeriode(), 2000.daglig, im2)

        Refusjonsservitør.fra(beløpstidslinje1).servér(ubrukteRefusjonsopplysninger, Aktivitetslogg())
        Refusjonsservitør.fra(beløpstidslinje2).servér(ubrukteRefusjonsopplysninger, Aktivitetslogg())

        val refusjonstidslinjeJanuar = ubrukteRefusjonsopplysninger.servér(1.januar, januar)
        val forventet = Beløpstidslinje.fra(januar, 2000.daglig, im2)
        assertEquals(forventet, refusjonstidslinjeJanuar)
    }

    @Test
    fun `rester etter servering`() {
        val refusjonstidslinje = Beløpstidslinje.fra(januar, 100.daglig, kilde)
        val servitør = Refusjonsservitør.fra(refusjonstidslinje)
        val ubrukteRefusjonsopplysninger = Refusjonsservitør()
        servitør.servér(1.januar, 10.januar til 15.januar)
        servitør.servér(ubrukteRefusjonsopplysninger, Aktivitetslogg())
        assertNotNull(ubrukteRefusjonsopplysninger[1.januar])
        assertNull(ubrukteRefusjonsopplysninger[2.januar])
        assertEquals(listOf(16.januar til 31.januar), ubrukteRefusjonsopplysninger[1.januar]!!.perioderMedBeløp)
    }

    @Test
    fun `rester etter servering med fler startdatoer`() {
        val refusjonstidslinje = Beløpstidslinje.fra(januar, 100.daglig, kilde)
        val servitør = Refusjonsservitør.fra(refusjonstidslinje, setOf(5.januar, 16.januar))
        val ubrukteRefusjonsopplysninger = Refusjonsservitør()
        servitør.servér(2.januar, 10.januar til 12.januar)
        servitør.servér(16.januar, 23.januar til 25.januar)
        servitør.servér(ubrukteRefusjonsopplysninger, Aktivitetslogg())

        assertNull(ubrukteRefusjonsopplysninger[4.januar])
        assertNull(ubrukteRefusjonsopplysninger[5.januar])
        assertNotNull(ubrukteRefusjonsopplysninger[16.januar])
        assertNull(ubrukteRefusjonsopplysninger[17.januar])

        assertEquals(listOf(26.januar til 31.januar), ubrukteRefusjonsopplysninger[16.januar]!!.perioderMedBeløp)
    }

    private companion object {
        private val kilde = Kilde(MeldingsreferanseId(UUID.randomUUID()), ARBEIDSGIVER, LocalDateTime.now())
    }
}
