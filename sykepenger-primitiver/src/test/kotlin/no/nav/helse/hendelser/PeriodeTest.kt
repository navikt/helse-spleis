package no.nav.helse.hendelser

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
import no.nav.helse.hendelser.Periode.Companion.utenPerioder
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.lørdag
import no.nav.helse.mai
import no.nav.helse.mandag
import no.nav.helse.mars
import no.nav.helse.søndag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

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
        Assertions.assertEquals(expected, perioder.flattenMutableSet())
    }

    @Test
    fun overlapperEllerStarterFør() {
        val p1 = 3.januar til 5.januar

        (1.januar til 2.januar).also { p2 ->
            Assertions.assertFalse(p1.overlapperEllerStarterFør(p2))
            Assertions.assertTrue(p2.overlapperEllerStarterFør(p1))
        }

        (3.januar til 4.januar).also { p2 ->
            Assertions.assertTrue(p1.overlapperEllerStarterFør(p2))
            Assertions.assertTrue(p2.overlapperEllerStarterFør(p1))
        }

        (5.januar til 6.januar).also { p2 ->
            Assertions.assertTrue(p1.overlapperEllerStarterFør(p2))
            Assertions.assertTrue(p2.overlapperEllerStarterFør(p1))
        }

        (6.januar til 7.januar).also { p2 ->
            Assertions.assertTrue(p1.overlapperEllerStarterFør(p2))
            Assertions.assertFalse(p2.overlapperEllerStarterFør(p1))
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

        Assertions.assertEquals(
            listOf(
                1.januar til 10.januar,
                5.januar til 19.januar,
                13.januar til 14.januar,
                17.januar til 21.januar,
            ),
            perioder.mursteinsperioder(1.januar til 10.januar)
        )

        Assertions.assertEquals(
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
        Assertions.assertEquals(15.januar til 16.januar, periode.utenDagerFør(17.januar til 19.januar))
        Assertions.assertEquals(15.januar til 16.januar, periode.utenDagerFør(17.januar til 31.januar))
        Assertions.assertEquals(periode, periode.utenDagerFør(21.januar.somPeriode()))
        Assertions.assertNull(periode.utenDagerFør(15.januar til 31.januar))
        Assertions.assertNull(periode.utenDagerFør(1.januar til 31.januar))
        Assertions.assertNull(periode.utenDagerFør(1.januar til 5.januar))
    }

    @Test
    fun `uten helgehale`() {
        val fredag = fredag(5.januar)
        val lørdag = lørdag(6.januar)
        val søndag = søndag(7.januar)
        val mandag = mandag(8.januar)
        Assertions.assertEquals(fredag til fredag, (fredag til lørdag).utenHelgehale())
        Assertions.assertNull((lørdag til lørdag).utenHelgehale())
        Assertions.assertNull((lørdag til søndag).utenHelgehale())
        Assertions.assertNull((søndag til søndag).utenHelgehale())

        Assertions.assertEquals(fredag til fredag, (fredag til søndag).utenHelgehale())

        (fredag til mandag).let { Assertions.assertEquals(it, it.utenHelgehale()) }
        val langPeriode = lørdag(7.januar(2017)) til søndag
        Assertions.assertEquals(lørdag(7.januar(2017)) til fredag, langPeriode.utenHelgehale())
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

        Assertions.assertEquals(forventet, perioder.grupperSammenhengendePerioder())
    }

    @Test
    fun `siste periode linker alle sammen`() {
        val perioder = listOf(
            1.januar til 2.januar,
            4.januar til 4.januar,
            3.januar til 3.januar
        )
        val forventet = listOf(1.januar til 4.januar)
        Assertions.assertEquals(forventet, perioder.grupperSammenhengendePerioderMedHensynTilHelg())
    }

    @Test
    fun `periode mellom`() {
        Assertions.assertNull(periode.periodeMellom(periode.start.minusDays(1)))
        Assertions.assertNull(periode.periodeMellom(periode.endInclusive))
        Assertions.assertEquals(11.juli til 15.juli, periode.periodeMellom(16.juli))
    }

    @Test
    fun `periode mellom ikke mellom`() {
        Assertions.assertNull(Periode.mellom(januar, februar))
        Assertions.assertNull(Periode.mellom(februar, januar))
        Assertions.assertEquals(februar, Periode.mellom(januar, mars))
        Assertions.assertEquals(februar, Periode.mellom(mars, januar))
    }

    @Test
    fun `overlapper med periode`() {
        Assertions.assertTrue(periode.overlapperMed(Periode(20.juni, 10.juli)))
        Assertions.assertTrue(periode.overlapperMed(Periode(20.juni, 1.juli)))
        Assertions.assertTrue(periode.overlapperMed(Periode(2.juni, 7.juli)))
        Assertions.assertTrue(periode.overlapperMed(Periode(1.juli, 12.juli)))
        Assertions.assertTrue(periode.overlapperMed(Periode(10.juli, 12.juli)))
        Assertions.assertTrue(periode.overlapperMed(Periode(30.juni, 11.juli)))
        Assertions.assertTrue(periode.overlapperMed(periode))
    }

    @Test
    fun `overlappende periode`() {
        Assertions.assertNull((1.januar til 3.januar).overlappendePeriode(4.januar til 5.januar))
        Assertions.assertNull((4.januar til 5.januar).overlappendePeriode(1.januar til 3.januar))
        Assertions.assertEquals(
            3.januar til 5.januar,
            ((1.januar til 10.januar).overlappendePeriode(3.januar til 5.januar))
        )
        Assertions.assertEquals(
            3.januar til 5.januar,
            ((1.januar til 5.januar).overlappendePeriode(3.januar til 10.januar))
        )
        Assertions.assertEquals(
            3.januar til 5.januar,
            ((3.januar til 10.januar).overlappendePeriode(1.januar til 5.januar))
        )
    }

    @Test
    fun `inneholder med periode`() {
        Assertions.assertTrue(periode.inneholder(periode))
        Assertions.assertTrue(periode.inneholder(2.juli til 9.juli))
        Assertions.assertFalse(periode.inneholder(periode.oppdaterFom(30.juni)))
        Assertions.assertFalse(periode.inneholder(periode.oppdaterTom(11.juli)))
        Assertions.assertFalse(periode.inneholder(29.juni til 30.juni))
        Assertions.assertFalse(periode.inneholder(11.juli til 12.juli))
    }

    @Test
    fun `overlapper ikke med periode`() {
        Assertions.assertFalse(periode.overlapperMed(Periode(1.april, 10.april)))
        Assertions.assertFalse(periode.overlapperMed(Periode(1.april, 30.juni)))
        Assertions.assertFalse(periode.overlapperMed(Periode(11.juli, 11.juli)))
        Assertions.assertFalse(periode.overlapperMed(Periode(1.august, 10.august)))
    }

    @Test
    fun `fom kan være lik tom, men ikke før tom`() {
        Assertions.assertDoesNotThrow { Periode(1.juli, 1.juli) }
        assertThrows<IllegalArgumentException> { Periode(2.juli, 1.juli) }
    }

    @Test
    fun `inneholder ikke ved delvis overlapp`() {
        Assertions.assertFalse(periode.contains(Periode(10.juli, 11.juli)))
        Assertions.assertFalse(periode.contains(Periode(30.juni, 1.juli)))
        Assertions.assertFalse(Periode(10.juli, 11.juli) in periode)
        Assertions.assertFalse(Periode(30.juni, 1.juli) in periode)
    }

    @Test
    fun `inneholder ikke perioder utenfor`() {
        Assertions.assertFalse(periode.contains(Periode(11.juli, 12.juli)))
        Assertions.assertFalse(periode.contains(Periode(29.juni, 30.juni)))
        Assertions.assertFalse(Periode(11.juli, 12.juli) in periode)
        Assertions.assertFalse(Periode(29.juni, 30.juni) in periode)
    }

    @Test
    fun `inneholder hele perioder`() {
        Assertions.assertTrue(periode.contains(periode))
        Assertions.assertTrue(periode.contains(Periode(10.juli, 10.juli)))
        Assertions.assertTrue(periode.contains(Periode(1.juli, 1.juli)))
        Assertions.assertTrue(periode in periode)
        Assertions.assertTrue(Periode(10.juli, 10.juli) in periode)
        Assertions.assertTrue(Periode(1.juli, 1.juli) in periode)
    }

    @Test
    fun `inneholder datoer`() {
        Assertions.assertTrue(periode.contains(listOf(1.juli)))
        Assertions.assertTrue(periode.contains(listOf(10.juli)))
        Assertions.assertTrue(periode.contains(listOf(30.juni, 5.juli)))
        Assertions.assertFalse(periode.contains(listOf(30.juni, 11.juli)))
        Assertions.assertFalse(periode.contains(emptyList()))
    }

    @Test
    fun `inneholder enkeltdager`() {
        Assertions.assertTrue(1.juli in periode)
        Assertions.assertTrue(10.juli in periode)
        Assertions.assertFalse(30.juni in periode)
        Assertions.assertFalse(11.juli in periode)
    }

    @Test
    fun `perioder inneholder enkeltdager`() {
        val periode1 = Periode(1.juli, 2.juli)
        val periode2 = Periode(4.juli, 5.juli)
        val perioder = listOf(periode1, periode2)
        Assertions.assertTrue(perioder.contains(1.juli))
        Assertions.assertTrue(perioder.contains(5.juli))
        Assertions.assertFalse(perioder.contains(3.juli))
        Assertions.assertFalse(perioder.contains(6.juli))
        Assertions.assertFalse(perioder.contains(30.juni))
    }

    @Test
    fun `slutter etter`() {
        Assertions.assertTrue(periode.slutterEtter(periode.endInclusive))
        Assertions.assertTrue(periode.slutterEtter(periode.endInclusive.minusDays(1)))
        Assertions.assertFalse(periode.slutterEtter(periode.endInclusive.plusDays(1)))
    }

    @Test
    fun `periode før helg er rett før periode etter helg`() {
        val førHelg = Periode(4.januar, 5.januar)
        val helg = Periode(6.januar, 7.januar)
        val etterHelg = Periode(8.januar, 9.januar)
        val ukenEtter = Periode(9.januar, 12.januar)
        Assertions.assertTrue(førHelg.erRettFør(helg))
        Assertions.assertTrue(førHelg.erRettFør(etterHelg))
        Assertions.assertFalse(førHelg.erRettFør(ukenEtter))
    }

    @Test
    fun `er utenfor en annen periode`() {
        val other = Periode(3.juli, 4.juli)
        Assertions.assertFalse(Periode(3.juli, 4.juli).utenfor(other))
        Assertions.assertTrue(Periode(2.juli, 4.juli).utenfor(other))
        Assertions.assertTrue(Periode(1.juli, 2.juli).utenfor(other))
        Assertions.assertTrue(Periode(3.juli, 5.juli).utenfor(other))
        Assertions.assertTrue(Periode(5.juli, 6.juli).utenfor(other))
    }

    @Test
    fun `periode på eller etter dato`() {
        Assertions.assertFalse(listOf(Periode(1.juli, 2.juli)).slutterEtter(3.juli))
        Assertions.assertTrue(listOf(Periode(1.juli, 2.juli)).slutterEtter(2.juli))
        Assertions.assertTrue(listOf(Periode(1.juli, 2.juli)).slutterEtter(1.juli))
        Assertions.assertTrue(listOf(Periode(1.juli, 2.juli)).slutterEtter(30.juni))
    }

    @Test
    fun `oppdatere fom`() {
        val periode = Periode(2.januar, 3.januar)
        Assertions.assertEquals(1.januar, periode.oppdaterFom(Periode(1.januar, 5.januar)).start)
        Assertions.assertEquals(2.januar, periode.oppdaterFom(Periode(3.januar, 5.januar)).start)
    }

    @Test
    fun `oppdatere tom`() {
        val periode = Periode(2.januar, 3.januar)
        Assertions.assertEquals(3.januar, periode.oppdaterTom(Periode(1.januar, 2.januar)).endInclusive)
        Assertions.assertEquals(5.januar, periode.oppdaterTom(Periode(3.januar, 5.januar)).endInclusive)
    }

    @Test
    fun likhet() {
        val periode = Periode(2.januar, 3.januar)
        @Suppress("KotlinConstantConditions") // det er jo akkurat dét vi sjekker, dumme inspection.
        (Assertions.assertTrue(periode == periode))
        Assertions.assertTrue(periode == Periode(2.januar, 3.januar))
        Assertions.assertFalse(periode == Periode(periode.start, periode.endInclusive.plusDays(1)))
        Assertions.assertFalse(periode == Periode(periode.start.plusDays(1), periode.endInclusive))
    }

    @Test
    fun `kan gjennomløpe tidslinjen`() {
        val actual = (1.januar til 5.januar).plus(15.januar til 19.januar)
        assertSize(19, actual)
    }

    @Test
    fun `slår sammen dager`() {
        Assertions.assertTrue(emptyList<LocalDate>().grupperSammenhengendePerioder().isEmpty())
        Assertions.assertEquals(
            listOf(1.januar til 1.januar),
            listOf(1.januar, 1.januar).grupperSammenhengendePerioder()
        )
        Assertions.assertEquals(listOf(1.januar til 1.januar), listOf(1.januar).grupperSammenhengendePerioder())
        Assertions.assertEquals(
            listOf(1.januar til 2.januar),
            listOf(2.januar, 1.januar).grupperSammenhengendePerioder()
        )
        Assertions.assertEquals(
            listOf(1.januar til 1.januar, 3.januar til 3.januar),
            listOf(1.januar, 3.januar).grupperSammenhengendePerioder()
        )
        Assertions.assertEquals(
            listOf(1.januar til 5.januar, 8.januar til 8.januar),
            listOf(1.januar, 2.januar, 3.januar, 4.januar, 5.januar, 8.januar).grupperSammenhengendePerioder()
        )
    }

    @Test
    fun `strekk en periode for å dekke en annen periode`() {
        Assertions.assertEquals(
            1.januar til 31.januar,
            (15.januar til 31.januar).plus(1.januar til 20.januar)
        )
    }

    @Test
    fun `tekstrepresentasjon av periode`() {
        Assertions.assertEquals("01-01-2018 til 02-01-2018", Periode(1.januar, 2.januar).toString())
    }

    @Test
    fun subset() {
        val periode1 = 10.januar til 15.januar
        Assertions.assertEquals(periode1, periode1.subset(januar))
        Assertions.assertEquals(11.januar til 12.januar, periode1.subset(11.januar til 12.januar))
        Assertions.assertEquals(2.januar til 2.januar, (1.januar til 2.januar).subset(2.januar til 12.januar))
        Assertions.assertEquals(2.januar til 2.januar, (2.januar til 3.januar).subset(1.januar til 2.januar))
        assertThrows<IllegalArgumentException> { (1.januar til 2.januar).subset(11.januar til 12.januar) }
    }

    @Test
    fun beholdDagerFremTil() {
        val periode = februar
        Assertions.assertEquals(null, periode.beholdDagerTil(31.januar))
        Assertions.assertEquals(1.februar.somPeriode(), periode.beholdDagerTil(1.februar))
        Assertions.assertEquals(1.februar til 15.februar, periode.beholdDagerTil(15.februar))
        Assertions.assertEquals(1.februar til 27.februar, periode.beholdDagerTil(27.februar))
        Assertions.assertEquals(periode, periode.beholdDagerTil(28.februar))
    }

    @Test
    fun beholdDagerEtter() {
        val periode = februar
        Assertions.assertEquals(null, periode.beholdDagerEtter(28.februar))
        Assertions.assertEquals(2.februar til 28.februar, periode.beholdDagerEtter(1.februar))
        Assertions.assertEquals(28.februar til 28.februar, periode.beholdDagerEtter(27.februar))
        Assertions.assertEquals(16.februar til 28.februar, periode.beholdDagerEtter(15.februar))
        Assertions.assertEquals(periode, periode.beholdDagerEtter(31.januar))
    }

    @Test
    fun `perioder som overlapper`() {
        Assertions.assertFalse(listOf<Periode>().overlapper())
        Assertions.assertFalse(listOf(1.januar til 31.januar).overlapper())
        Assertions.assertFalse(
            listOf(
                1.desember(2017) til 31.desember(2017),
                1.januar til 31.januar,
                1.februar til 28.februar
            ).overlapper()
        )
        Assertions.assertTrue(listOf(1.desember(2017) til 1.januar, 1.januar til 31.januar).overlapper())
        Assertions.assertTrue(listOf(1.januar til 31.januar, 31.januar til 28.februar).overlapper())
        Assertions.assertTrue(listOf(1.januar til LocalDate.MAX, 1.februar til LocalDate.MAX).overlapper())
        Assertions.assertFalse(listOf(1.januar til 31.januar, 1.februar til LocalDate.MAX).overlapper())
        Assertions.assertTrue(listOf(1.januar til 31.januar, 1.januar til 31.januar).overlapper())
        Assertions.assertTrue(listOf(1.januar til 31.januar, 1.januar til 1.januar).overlapper())
    }

    @Test
    fun `antall dager`() {
        val periode = januar
        Assertions.assertEquals(31, periode.count())
    }

    @Test
    fun `omsluttende periode`() {
        Assertions.assertNull(emptyList<LocalDate>().omsluttendePeriode)
        Assertions.assertEquals(1.januar til 31.januar, listOf(1.januar, 5.januar, 31.januar).omsluttendePeriode)
        Assertions.assertEquals(1.januar til 31.januar, listOf(31.januar, 1.januar).omsluttendePeriode)
        Assertions.assertEquals(1.januar til 31.januar, (1.januar til 31.januar).omsluttendePeriode)
    }

    @Test
    fun `periode rett før`() {
        val dager = listOf(1.januar, 3.januar, 2.januar, 10.januar, 11.januar, 12.januar)
        Assertions.assertNull(dager.periodeRettFør(1.januar))
        Assertions.assertEquals(1.januar til 3.januar, dager.periodeRettFør(4.januar))
        Assertions.assertNull(dager.periodeRettFør(5.januar))
        Assertions.assertEquals(10.januar til 11.januar, dager.periodeRettFør(12.januar))
        Assertions.assertEquals(10.januar til 12.januar, dager.periodeRettFør(15.januar))
        Assertions.assertEquals(1.januar til 31.januar, (1.januar til 31.januar).periodeRettFør(1.februar))
    }

    @Test
    fun `uten perioder`() {
        val perioder = setOf(1.januar til 20.januar, 25.januar til 31.januar)
        val result = perioder.utenPerioder(listOf(5.januar til 15.januar, 23.januar til 26.januar, 30.januar til 1.februar))
        Assertions.assertEquals(
            listOf(1.januar til 4.januar, 16.januar til 20.januar, 27.januar til 29.januar),
            result
        )
        Assertions.assertEquals(emptyList<Periode>(), perioder.utenPerioder(perioder))
    }

    @Test
    fun `liste av perioder trimmer annen`() {
        val periode = 5.januar til 31.januar
        val result = periode.uten(listOf(
            1.januar til 5.januar,
            10.januar til 14.januar,
            28.januar til 29.januar
        ))

        Assertions.assertEquals(
            listOf(6.januar til 9.januar, 15.januar til 27.januar, 30.januar til 31.januar),
            result
        )
    }

    @Test
    fun `trimmer periode`() {
        val periode = 5.januar til 20.januar

        // trimmer ingenting
        Assertions.assertEquals(listOf(periode), periode.uten(1.januar til 4.januar))
        Assertions.assertEquals(listOf(periode), periode.uten(21.januar til 31.januar))

        // trimmer hele perioden
        Assertions.assertEquals(emptyList<Periode>(), periode.uten(4.januar til 20.januar))
        Assertions.assertEquals(emptyList<Periode>(), periode.uten(5.januar til 20.januar))
        Assertions.assertEquals(emptyList<Periode>(), periode.uten(5.januar til 21.januar))
        Assertions.assertEquals(emptyList<Periode>(), periode.uten(4.januar til 21.januar))

        // trimmer perioden i to deler
        Assertions.assertEquals(
            listOf(5.januar.somPeriode(), 20.januar.somPeriode()),
            periode.uten(6.januar til 19.januar)
        )
        Assertions.assertEquals(
            listOf(5.januar til 9.januar, 16.januar til 20.januar),
            periode.uten(10.januar til 15.januar)
        )
        Assertions.assertEquals(
            listOf(5.januar til 13.januar, 15.januar til 20.januar),
            periode.uten(14.januar.somPeriode())
        )

        // trimmer bort snute
        Assertions.assertEquals(listOf(20.januar.somPeriode()), periode.uten(4.januar til 19.januar))
        Assertions.assertEquals(listOf(20.januar.somPeriode()), periode.uten(5.januar til 19.januar))
        Assertions.assertEquals(listOf(15.januar til 20.januar), periode.uten(5.januar til 14.januar))

        // trimmer bort hale
        Assertions.assertEquals(listOf(5.januar.somPeriode()), periode.uten(6.januar til 20.januar))
        Assertions.assertEquals(listOf(5.januar.somPeriode()), periode.uten(6.januar til 21.januar))
        Assertions.assertEquals(listOf(5.januar til 9.januar), periode.uten(10.januar til 21.januar))
    }

    @Test
    fun `legger til en ny periode i en liste`() {
        val perioden = 5.januar til 10.januar
        Assertions.assertEquals(listOf(perioden), emptyList<Periode>().merge(perioden))
        Assertions.assertEquals(
            listOf(4.januar.somPeriode(), 5.januar til 10.januar, 11.januar.somPeriode()),
            listOf(4.januar til 11.januar).merge(perioden)
        )
        Assertions.assertEquals(
            listOf(1.januar til 4.januar, 5.januar til 10.januar, 11.januar til 12.januar),
            listOf(11.januar til 12.januar, 1.januar til 5.januar).merge(perioden)
        )
    }

    @Test
    fun `intersect mellom to lister av perioder`() {
        val perioder1 = listOf(5.januar til 15.januar, 25.januar til 31.januar)
        val perioder2 = listOf(1.januar til 6.januar, 16.januar til 2.februar)
        val iBegge = listOf(5.januar til 6.januar, 25.januar til 31.januar)
        Assertions.assertEquals(iBegge, perioder1.intersect(perioder2))
        Assertions.assertEquals(iBegge, perioder2.intersect(perioder1))
        Assertions.assertEquals(emptyList<Periode>(), emptyList<Periode>().intersect(emptyList()))
        Assertions.assertEquals(emptyList<Periode>(), perioder1.intersect(emptyList()))
        Assertions.assertEquals(emptyList<Periode>(), emptyList<Periode>().intersect(perioder1))
    }

    @Test
    fun `likhet på lister av perioder`() {
        Assertions.assertTrue(listOf(januar, februar).lik(listOf(februar, januar)))
        Assertions.assertTrue(listOf(januar, februar).lik(listOf(januar, februar)))
        Assertions.assertFalse(listOf(januar, februar).lik(listOf(januar, 1.februar til 27.februar)))
        Assertions.assertTrue((januar.map { it til it }).lik(januar.shuffled().map { it til it }))
        Assertions.assertFalse(((1.januar til 30.januar).map { it til it }).lik(januar.shuffled().map { it til it }))
        Assertions.assertTrue(emptyList<Periode>().lik(emptyList()))
    }

    private fun assertSize(expected: Int, periode: Periode) {
        var count = 0
        periode.forEach { _ -> count++ }
        Assertions.assertEquals(expected, count)

        count = 0
        for (date: LocalDate in periode) {
            count++
        }
        Assertions.assertEquals(expected, count)
    }
}
