package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.Periode.Companion.flattenMutableSet
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.hendelser.Periode.Companion.intersect
import no.nav.helse.hendelser.Periode.Companion.lik
import no.nav.helse.hendelser.Periode.Companion.merge
import no.nav.helse.hendelser.Periode.Companion.mursteinsperioder
import no.nav.helse.hendelser.Periode.Companion.omsluttendePeriode
import no.nav.helse.hendelser.Periode.Companion.overlapper
import no.nav.helse.hendelser.Periode.Companion.periodeRettFør
import no.nav.helse.hendelser.Periode.Companion.slutterEtter
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.lørdag
import no.nav.helse.mai
import no.nav.helse.mandag
import no.nav.helse.mars
import no.nav.helse.søndag
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PeriodeTest {
    private val periode = Periode(1.juli, 10.juli)

    @Test
    fun `flatten til mutable list`() {
        val perioder = listOf(
            3.juli til 4.juli,
            1.juli til 5.juli,
            5.juli til 7.juli
        )
        val expected = mutableSetOf(
            3.juli, 4.juli,
            1.juli, 2.juli, 5.juli,
            6.juli, 7.juli
        )
        assertEquals(expected, perioder.flattenMutableSet())
    }

    @Test
    fun overlapperEllerStarterFør() {
        val p1 = 3.januar til 5.januar

        (1.januar til 2.januar).also { p2 ->
            assertFalse(p1.overlapperEllerStarterFør(p2))
            assertTrue(p2.overlapperEllerStarterFør(p1))
        }

        (3.januar til 4.januar).also { p2 ->
            assertTrue(p1.overlapperEllerStarterFør(p2))
            assertTrue(p2.overlapperEllerStarterFør(p1))
        }

        (5.januar til 6.januar).also { p2 ->
            assertTrue(p1.overlapperEllerStarterFør(p2))
            assertTrue(p2.overlapperEllerStarterFør(p1))
        }

        (6.januar til 7.januar).also { p2 ->
            assertTrue(p1.overlapperEllerStarterFør(p2))
            assertFalse(p2.overlapperEllerStarterFør(p1))
        }
    }

    @Test
    fun mursteinsperioder() {
        val perioder = listOf(
            1.januar til 10.januar,
            5.januar til 19.januar,
            13.januar til 14.januar,
            17.januar til 21.januar,

            23.januar til 25.januar,
            26.januar til 28.januar
        ).shuffled()

        assertEquals(
            listOf(
                1.januar til 10.januar,
                5.januar til 19.januar,
                13.januar til 14.januar,
                17.januar til 21.januar,
            ),
            perioder.mursteinsperioder(1.januar til 10.januar)
        )

        assertEquals(
            listOf(
                1.januar til 10.januar,
                5.januar til 19.januar,
                13.januar til 14.januar,
                17.januar til 21.januar,
            ),
            perioder.mursteinsperioder(13.januar til 14.januar)
        )
    }

    @Test
    fun `uten dager før`() {
        val periode = 15.januar til 20.januar
        assertEquals(15.januar til 16.januar, periode.utenDagerFør(17.januar til 19.januar))
        assertEquals(15.januar til 16.januar, periode.utenDagerFør(17.januar til 31.januar))
        assertEquals(periode, periode.utenDagerFør(21.januar.somPeriode()))
        assertNull(periode.utenDagerFør(15.januar til 31.januar))
        assertNull(periode.utenDagerFør(1.januar til 31.januar))
        assertNull(periode.utenDagerFør(1.januar til 5.januar))
    }

    @Test
    fun `uten helgehale`() {
        val fredag = fredag(5.januar)
        val lørdag = lørdag(6.januar)
        val søndag = søndag(7.januar)
        val mandag = mandag(8.januar)
        assertEquals(fredag til fredag, (fredag til lørdag).utenHelgehale())
        assertNull((lørdag til lørdag).utenHelgehale())
        assertNull((lørdag til søndag).utenHelgehale())
        assertNull((søndag til søndag).utenHelgehale())

        assertEquals(fredag til fredag, (fredag til søndag).utenHelgehale())

        (fredag til mandag).let { assertEquals(it, it.utenHelgehale()) }
        val langPeriode = lørdag(7.januar(2017)) til søndag
        assertEquals(lørdag(7.januar(2017)) til fredag, langPeriode.utenHelgehale())
    }

    @Test
    fun `grupperer perioder`() {
        val perioder = listOf(
            1.januar til 31.januar,
            5.januar til 6.januar,
            1.desember(2017) til 10.desember(2017),
            4.mai til 20.mai,
            28.desember(2017) til 31.desember(2017),
            1.mai til 3.mai
        )

        val forventet = listOf(
            1.desember(2017) til 10.desember(2017),
            28.desember(2017) til 31.januar,
            1.mai til 20.mai
        )

        assertEquals(forventet, perioder.grupperSammenhengendePerioder())
    }

    @Test
    fun `siste periode linker alle sammen`() {
        val perioder = listOf(
            1.januar til 2.januar,
            4.januar til 4.januar,
            3.januar til 3.januar
        )
        val forventet = listOf(1.januar til 4.januar)
        assertEquals(forventet, perioder.grupperSammenhengendePerioderMedHensynTilHelg())
    }

    @Test
    fun `periode mellom`() {
        assertNull(periode.periodeMellom(periode.start.minusDays(1)))
        assertNull(periode.periodeMellom(periode.endInclusive))
        assertEquals(11.juli til 15.juli, periode.periodeMellom(16.juli))
    }

    @Test
    fun `periode mellom ikke mellom`() {
        assertNull(Periode.mellom(januar, februar))
        assertNull(Periode.mellom(februar, januar))
        assertEquals(februar, Periode.mellom(januar, mars))
        assertEquals(februar, Periode.mellom(mars, januar))
    }

    @Test
    fun `overlapper med periode`() {
        assertTrue(periode.overlapperMed(Periode(20.juni, 10.juli)))
        assertTrue(periode.overlapperMed(Periode(20.juni, 1.juli)))
        assertTrue(periode.overlapperMed(Periode(2.juni, 7.juli)))
        assertTrue(periode.overlapperMed(Periode(1.juli, 12.juli)))
        assertTrue(periode.overlapperMed(Periode(10.juli, 12.juli)))
        assertTrue(periode.overlapperMed(Periode(30.juni, 11.juli)))
        assertTrue(periode.overlapperMed(periode))
    }

    @Test
    fun `overlappende periode`() {
        assertNull((1.januar til 3.januar).overlappendePeriode(4.januar til 5.januar))
        assertNull((4.januar til 5.januar).overlappendePeriode(1.januar til 3.januar))
        assertEquals(3.januar til 5.januar, ((1.januar til 10.januar).overlappendePeriode(3.januar til 5.januar)))
        assertEquals(3.januar til 5.januar, ((1.januar til 5.januar).overlappendePeriode(3.januar til 10.januar)))
        assertEquals(3.januar til 5.januar, ((3.januar til 10.januar).overlappendePeriode(1.januar til 5.januar)))
    }

    @Test
    fun `inneholder med periode`() {
        assertTrue(periode.inneholder(periode))
        assertTrue(periode.inneholder(2.juli til 9.juli))
        assertFalse(periode.inneholder(periode.oppdaterFom(30.juni)))
        assertFalse(periode.inneholder(periode.oppdaterTom(11.juli)))
        assertFalse(periode.inneholder(29.juni til 30.juni))
        assertFalse(periode.inneholder(11.juli til 12.juli))
    }

    @Test
    fun `overlapper ikke med periode`() {
        assertFalse(periode.overlapperMed(Periode(1.april, 10.april)))
        assertFalse(periode.overlapperMed(Periode(1.april, 30.juni)))
        assertFalse(periode.overlapperMed(Periode(11.juli, 11.juli)))
        assertFalse(periode.overlapperMed(Periode(1.august, 10.august)))
    }

    @Test
    fun `fom kan være lik tom, men ikke før tom`() {
        assertDoesNotThrow { Periode(1.juli, 1.juli) }
        assertThrows<IllegalArgumentException> { Periode(2.juli, 1.juli) }
    }

    @Test
    fun `inneholder ikke ved delvis overlapp`() {
        assertFalse(periode.contains(Periode(10.juli, 11.juli)))
        assertFalse(periode.contains(Periode(30.juni, 1.juli)))
        assertFalse(Periode(10.juli, 11.juli) in periode)
        assertFalse(Periode(30.juni, 1.juli) in periode)
    }

    @Test
    fun `inneholder ikke perioder utenfor`() {
        assertFalse(periode.contains(Periode(11.juli, 12.juli)))
        assertFalse(periode.contains(Periode(29.juni, 30.juni)))
        assertFalse(Periode(11.juli, 12.juli) in periode)
        assertFalse(Periode(29.juni, 30.juni) in periode)
    }

    @Test
    fun `inneholder hele perioder`() {
        assertTrue(periode.contains(periode))
        assertTrue(periode.contains(Periode(10.juli, 10.juli)))
        assertTrue(periode.contains(Periode(1.juli, 1.juli)))
        assertTrue(periode in periode)
        assertTrue(Periode(10.juli, 10.juli) in periode)
        assertTrue(Periode(1.juli, 1.juli) in periode)
    }

    @Test
    fun `inneholder datoer`() {
        assertTrue(periode.contains(listOf(1.juli)))
        assertTrue(periode.contains(listOf(10.juli)))
        assertTrue(periode.contains(listOf(30.juni, 5.juli)))
        assertFalse(periode.contains(listOf(30.juni, 11.juli)))
        assertFalse(periode.contains(emptyList()))
    }

    @Test
    fun `inneholder enkeltdager`() {
        assertTrue(1.juli in periode)
        assertTrue(10.juli in periode)
        assertFalse(30.juni in periode)
        assertFalse(11.juli in periode)
    }

    @Test
    fun `perioder inneholder enkeltdager`() {
        val periode1 = Periode(1.juli, 2.juli)
        val periode2 = Periode(4.juli, 5.juli)
        val perioder = listOf(periode1, periode2)
        assertTrue(perioder.contains(1.juli))
        assertTrue(perioder.contains(5.juli))
        assertFalse(perioder.contains(3.juli))
        assertFalse(perioder.contains(6.juli))
        assertFalse(perioder.contains(30.juni))
    }

    @Test
    fun `slutter etter`() {
        assertTrue(periode.slutterEtter(periode.endInclusive))
        assertTrue(periode.slutterEtter(periode.endInclusive.minusDays(1)))
        assertFalse(periode.slutterEtter(periode.endInclusive.plusDays(1)))
    }

    @Test
    fun `periode før helg er rett før periode etter helg`() {
        val førHelg = Periode(4.januar, 5.januar)
        val helg = Periode(6.januar, 7.januar)
        val etterHelg = Periode(8.januar, 9.januar)
        val ukenEtter = Periode(9.januar, 12.januar)
        assertTrue(førHelg.erRettFør(helg))
        assertTrue(førHelg.erRettFør(etterHelg))
        assertFalse(førHelg.erRettFør(ukenEtter))
    }

    @Test
    fun `er utenfor en annen periode`() {
        val other = Periode(3.juli, 4.juli)
        assertFalse(Periode(3.juli, 4.juli).utenfor(other))
        assertTrue(Periode(2.juli, 4.juli).utenfor(other))
        assertTrue(Periode(1.juli, 2.juli).utenfor(other))
        assertTrue(Periode(3.juli, 5.juli).utenfor(other))
        assertTrue(Periode(5.juli, 6.juli).utenfor(other))
    }

    @Test
    fun `periode på eller etter dato`() {
        assertFalse(listOf(Periode(1.juli, 2.juli)).slutterEtter(3.juli))
        assertTrue(listOf(Periode(1.juli, 2.juli)).slutterEtter(2.juli))
        assertTrue(listOf(Periode(1.juli, 2.juli)).slutterEtter(1.juli))
        assertTrue(listOf(Periode(1.juli, 2.juli)).slutterEtter(30.juni))
    }

    @Test
    fun `oppdatere fom`() {
        val periode = Periode(2.januar, 3.januar)
        assertEquals(1.januar, periode.oppdaterFom(Periode(1.januar, 5.januar)).start)
        assertEquals(2.januar, periode.oppdaterFom(Periode(3.januar, 5.januar)).start)
    }

    @Test
    fun `oppdatere tom`() {
        val periode = Periode(2.januar, 3.januar)
        assertEquals(3.januar, periode.oppdaterTom(Periode(1.januar, 2.januar)).endInclusive)
        assertEquals(5.januar, periode.oppdaterTom(Periode(3.januar, 5.januar)).endInclusive)
    }

    @Test
    fun likhet() {
        val periode = Periode(2.januar, 3.januar)
        @Suppress("KotlinConstantConditions") // det er jo akkurat dét vi sjekker, dumme inspection.
        assertTrue(periode == periode)
        assertTrue(periode == Periode(2.januar, 3.januar))
        assertFalse(periode == Periode(periode.start, periode.endInclusive.plusDays(1)))
        assertFalse(periode == Periode(periode.start.plusDays(1), periode.endInclusive))
    }

    @Test
    fun `kan gjennomløpe tidslinjen`() {
        val actual = (1.januar til 5.januar).plus(15.januar til 19.januar)
        assertSize(19, actual)
    }

    @Test
    fun `slår sammen dager`() {
        assertTrue(emptyList<LocalDate>().grupperSammenhengendePerioder().isEmpty())
        assertEquals(listOf(1.januar til 1.januar), listOf(1.januar, 1.januar).grupperSammenhengendePerioder())
        assertEquals(listOf(1.januar til 1.januar), listOf(1.januar).grupperSammenhengendePerioder())
        assertEquals(listOf(1.januar til 2.januar), listOf(2.januar, 1.januar).grupperSammenhengendePerioder())
        assertEquals(listOf(1.januar til 1.januar, 3.januar til 3.januar), listOf(1.januar, 3.januar).grupperSammenhengendePerioder())
        assertEquals(
            listOf(1.januar til 5.januar, 8.januar til 8.januar),
            listOf(1.januar, 2.januar, 3.januar, 4.januar, 5.januar, 8.januar).grupperSammenhengendePerioder()
        )
    }

    @Test
    fun `strekk en periode for å dekke en annen periode`() {
        assertEquals(
            1.januar til 31.januar,
            (15.januar til 31.januar).plus(1.januar til 20.januar)
        )
    }

    @Test
    fun `tekstrepresentasjon av periode`() {
        assertEquals("01-01-2018 til 02-01-2018", Periode(1.januar, 2.januar).toString())
    }

    @Test
    fun subset() {
        val periode1 = 10.januar til 15.januar
        assertEquals(periode1, periode1.subset(januar))
        assertEquals(11.januar til 12.januar, periode1.subset(11.januar til 12.januar))
        assertEquals(2.januar til 2.januar, (1.januar til 2.januar).subset(2.januar til 12.januar))
        assertEquals(2.januar til 2.januar, (2.januar til 3.januar).subset(1.januar til 2.januar))
        assertThrows<IllegalArgumentException> { (1.januar til 2.januar).subset(11.januar til 12.januar) }
    }

    @Test
    fun beholdDagerEtter() {
        val periode = februar
        assertEquals(null, periode.beholdDagerEtter(28.februar))
        assertEquals(2.februar til 28.februar, periode.beholdDagerEtter(1.februar))
        assertEquals(28.februar til 28.februar, periode.beholdDagerEtter(27.februar))
        assertEquals(16.februar til 28.februar, periode.beholdDagerEtter(15.februar))
        assertEquals(periode, periode.beholdDagerEtter(31.januar))
    }

    @Test
    fun `perioder som overlapper`() {
        assertFalse(listOf<Periode>().overlapper())
        assertFalse(listOf(1.januar til 31.januar).overlapper())
        assertFalse(listOf(1.desember(2017) til 31.desember(2017), 1.januar til 31.januar, 1.februar til 28.februar).overlapper())
        assertTrue(listOf(1.desember(2017) til 1.januar, 1.januar til 31.januar).overlapper())
        assertTrue(listOf(1.januar til 31.januar, 31.januar til 28.februar).overlapper())
        assertTrue(listOf(1.januar til LocalDate.MAX, 1.februar til LocalDate.MAX).overlapper())
        assertFalse(listOf(1.januar til 31.januar, 1.februar til LocalDate.MAX).overlapper())
        assertTrue(listOf(1.januar til 31.januar, 1.januar til 31.januar).overlapper())
        assertTrue(listOf(1.januar til 31.januar, 1.januar til 1.januar).overlapper())
    }

    @Test
    fun `antall dager`() {
        val periode = januar
        assertEquals(31, periode.count())
    }

    @Test
    fun `omsluttende periode`() {
        assertNull(emptyList<LocalDate>().omsluttendePeriode)
        assertEquals(1.januar til 31.januar, listOf(1.januar, 5.januar, 31.januar).omsluttendePeriode)
        assertEquals(1.januar til 31.januar, listOf(31.januar, 1.januar).omsluttendePeriode)
        assertEquals(1.januar til 31.januar, (1.januar til 31.januar).omsluttendePeriode)
    }

    @Test
    fun `periode rett før`() {
        val dager = listOf(1.januar, 3.januar, 2.januar, 10.januar, 11.januar, 12.januar)
        assertNull(dager.periodeRettFør(1.januar))
        assertEquals(1.januar til 3.januar, dager.periodeRettFør(4.januar))
        assertNull(dager.periodeRettFør(5.januar))
        assertEquals(10.januar til 11.januar, dager.periodeRettFør(12.januar))
        assertEquals(10.januar til 12.januar, dager.periodeRettFør(15.januar))
        assertEquals(1.januar til 31.januar, (1.januar til 31.januar).periodeRettFør(1.februar))
    }

    @Test
    fun `liste av perioder trimmer annen`() {
        val periode = 5.januar til 31.januar
        val result = periode.uten(listOf(
            1.januar til 5.januar,
            10.januar til 14.januar,
            28.januar til 29.januar
        ))

        assertEquals(
            listOf(6.januar til 9.januar, 15.januar til 27.januar, 30.januar til 31.januar),
            result
        )
    }

    @Test
    fun `trimmer periode`() {
        val periode = 5.januar til 20.januar

        // trimmer ingenting
        assertEquals(listOf(periode), periode.uten(1.januar til 4.januar))
        assertEquals(listOf(periode), periode.uten(21.januar til 31.januar))

        // trimmer hele perioden
        assertEquals(emptyList<Periode>(), periode.uten(4.januar til 20.januar))
        assertEquals(emptyList<Periode>(), periode.uten(5.januar til 20.januar))
        assertEquals(emptyList<Periode>(), periode.uten(5.januar til 21.januar))
        assertEquals(emptyList<Periode>(), periode.uten(4.januar til 21.januar))

        // trimmer perioden i to deler
        assertEquals(listOf(5.januar.somPeriode(), 20.januar.somPeriode()), periode.uten(6.januar til 19.januar))
        assertEquals(listOf(5.januar til 9.januar, 16.januar til 20.januar), periode.uten(10.januar til 15.januar))
        assertEquals(listOf(5.januar til 13.januar, 15.januar til 20.januar), periode.uten(14.januar.somPeriode()))

        // trimmer bort snute
        assertEquals(listOf(20.januar.somPeriode()), periode.uten(4.januar til 19.januar))
        assertEquals(listOf(20.januar.somPeriode()), periode.uten(5.januar til 19.januar))
        assertEquals(listOf(15.januar til 20.januar), periode.uten(5.januar til 14.januar))

        // trimmer bort hale
        assertEquals(listOf(5.januar.somPeriode()), periode.uten(6.januar til 20.januar))
        assertEquals(listOf(5.januar.somPeriode()), periode.uten(6.januar til 21.januar))
        assertEquals(listOf(5.januar til 9.januar), periode.uten(10.januar til 21.januar))
    }

    @Test
    fun `legger til en ny periode i en liste`() {
        val perioden = 5.januar til 10.januar
        assertEquals(listOf(perioden), emptyList<Periode>().merge(perioden))
        assertEquals(listOf(4.januar.somPeriode(), 5.januar til 10.januar, 11.januar.somPeriode()), listOf(4.januar til 11.januar).merge(perioden))
        assertEquals(listOf(1.januar til 4.januar, 5.januar til 10.januar, 11.januar til 12.januar), listOf(11.januar til 12.januar, 1.januar til 5.januar).merge(perioden))
    }

    @Test
    fun `intersect mellom to lister av perioder`() {
        val perioder1 = listOf(5.januar til 15.januar, 25.januar til 31.januar)
        val perioder2 = listOf(1.januar til 6.januar, 16.januar til 2.februar)
        val iBegge = listOf(5.januar til 6.januar, 25.januar til 31.januar)
        assertEquals(iBegge, perioder1.intersect(perioder2))
        assertEquals(iBegge, perioder2.intersect(perioder1))
        assertEquals(emptyList<Periode>(), emptyList<Periode>().intersect(emptyList()))
        assertEquals(emptyList<Periode>(), perioder1.intersect(emptyList()))
        assertEquals(emptyList<Periode>(), emptyList<Periode>().intersect(perioder1))
    }

    @Test
    fun `likhet på lister av perioder`() {
        assertTrue(listOf(januar, februar).lik(listOf(februar, januar)))
        assertTrue(listOf(januar, februar).lik(listOf(januar, februar)))
        assertFalse(listOf(januar, februar).lik(listOf(januar, 1.februar til 27.februar)))
        assertTrue((januar.map { it til it }).lik(januar.shuffled().map { it til it }))
        assertFalse(((1.januar til 30.januar).map { it til it }).lik(januar.shuffled().map { it til it }))
        assertTrue(emptyList<Periode>().lik(emptyList()))
    }

    private fun assertSize(expected: Int, periode: Periode) {
        var count = 0
        periode.forEach { _ -> count++ }
        assertEquals(expected, count)

        count = 0
        for (date: LocalDate in periode) {
            count++
        }
        assertEquals(expected, count)
    }
}
