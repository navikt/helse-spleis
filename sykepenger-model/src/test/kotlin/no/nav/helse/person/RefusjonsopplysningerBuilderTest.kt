package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.hendelser.til
import no.nav.helse.i
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RefusjonsopplysningerBuilderTest {

    @Test
    fun `håndtering av gråsone ved korrigerende refusjonsopplysninger`() {
        val eksisterendeId = UUID.randomUUID()
        val eksisterendeRefusjonsopplysninger = Refusjonsopplysning(eksisterendeId, 1.januar, null, 1000.daglig).refusjonsopplysninger // AGP 1-16.januar, FF 1.januar

        val nyId = UUID.randomUUID()
        val nyRefusjonsopplysning = Refusjonsopplysning(nyId, 20.januar, null, 1500.daglig) // AGP 1-16.januar, FF 20.januar
        val gråsonen = 1.januar til 19.januar
        val nyeRefusjonsopplysninger = RefusjonsopplysningerBuilder(eksisterendeRefusjonsopplysninger)
            .leggTil(nyRefusjonsopplysning, LocalDateTime.now())
            .build(gråsonen)

        val oppdaterteRefusjonsopplysninger = eksisterendeRefusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        assertEquals(listOf(
            Refusjonsopplysning(eksisterendeId, 1.januar, 19.januar, 1000.daglig),
            Refusjonsopplysning(nyId, 20.januar, null, 1500.daglig)
        ), oppdaterteRefusjonsopplysninger.inspektør.refusjonsopplysninger)
    }

    @Test
    fun `håndtering av gråsone når vi ikke har noen refusjonsopplysninger fra før`() {
        val eksisterendeRefusjonsopplysninger = Refusjonsopplysninger()

        val nyId = UUID.randomUUID()
        val nyRefusjonsopplysning = Refusjonsopplysning(nyId, 20.januar, null, 1500.daglig) // AGP 1-16.januar, FF 20.januar
        val gråsonen = 1.januar til 19.januar
        val nyeRefusjonsopplysninger = RefusjonsopplysningerBuilder(eksisterendeRefusjonsopplysninger)
            .leggTil(nyRefusjonsopplysning, LocalDateTime.now())
            .build(gråsonen)

        val oppdaterteRefusjonsopplysninger = eksisterendeRefusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        assertEquals(listOf(
            Refusjonsopplysning(nyId, 1.januar, null, 1500.daglig)
        ), oppdaterteRefusjonsopplysninger.inspektør.refusjonsopplysninger)
    }

    @Test
    fun `håndtering av gråsone når vi kun har tidligere refusjonsopplysninger`() {
        val eksisterendeId = UUID.randomUUID()
        val eksisterendeRefusjonsopplysninger = Refusjonsopplysning(eksisterendeId, 1.januar i 2017, 31.desember i 2017, 1000.daglig).refusjonsopplysninger

        val nyId = UUID.randomUUID()
        val nyRefusjonsopplysning = Refusjonsopplysning(nyId, 20.januar, null, 1500.daglig) // AGP 1-16.januar, FF 20.januar
        val gråsonen = 1.januar til 19.januar
        val nyeRefusjonsopplysninger = RefusjonsopplysningerBuilder(eksisterendeRefusjonsopplysninger)
            .leggTil(nyRefusjonsopplysning, LocalDateTime.now())
            .build(gråsonen)

        val oppdaterteRefusjonsopplysninger = eksisterendeRefusjonsopplysninger.merge(nyeRefusjonsopplysninger)
        assertEquals(listOf(
            Refusjonsopplysning(eksisterendeId, 1.januar i 2017, 31.desember i 2017, 1000.daglig),
            Refusjonsopplysning(nyId, 1.januar, null, 1500.daglig)
        ), oppdaterteRefusjonsopplysninger.inspektør.refusjonsopplysninger)
    }
}