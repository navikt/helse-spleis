package no.nav.helse.sykdomstidslinje

import no.nav.helse.*
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag.Companion.default
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SykdomstidslinjeTest {

    @BeforeEach
    fun setup() {
        resetSeed()
    }

    @Test
    fun `tom tidslinje er gyldig`() {
        assertEquals(0, Sykdomstidslinje().count())
    }

    @Test
    fun subset() {
        assertNull(Sykdomstidslinje().subset(1.januar til 5.januar).periode())
        resetSeed()
        assertEquals(1.januar til 5.januar, 10.S.subset(1.januar til 5.januar).periode())
        resetSeed()
        assertEquals(1.januar til 1.januar, 10.S.subset(31.desember(2017) til 1.januar).periode())
        resetSeed()
        assertEquals(10.januar til 10.januar, 10.S.subset(10.januar til 11.januar).periode())
        resetSeed()
        assertNull(10.S.subset(11.januar til 11.januar).periode())
        resetSeed()
        assertNull(10.S.subset(31.desember(2017) til 31.desember(2017)).periode())
    }

    @Test
    fun `sykdomstidslinje er rett før når det ikke er arbeidsdag mellom`() {
        assertTrue(1.S.erRettFør(1.S))
        assertTrue(1.F.erRettFør(1.F))
        assertTrue((1.S + 1.UK).erRettFør(2.UK + 1.F))
        assertFalse(1.S.erRettFør(1.A + 5.S))
        assertFalse((1.S + 1.A).erRettFør(1.S))
    }

    @Test
    fun `dager mellom to perioder blir UkjentDag`() {
        val tidslinje1 = Sykdomstidslinje.sykedager(
            1.mandag, 1.onsdag, 100.prosent, TestEvent.søknad
        )
        val tidslinje2 = Sykdomstidslinje.sykedager(
            2.onsdag, 2.fredag, 100.prosent, TestEvent.søknad
        )
        val tidslinje = tidslinje1.merge(tidslinje2, konfliktsky)
        assertEquals("SSS???? ??SSS", tidslinje.toShortString())
    }

    @Test
    fun `default strategi støtter ikke å mørje dager med forskjellig kilde`() {
        val tidslinje1 = Sykdomstidslinje.sykedager(
            1.mandag, 1.onsdag, 100.prosent, TestEvent.søknad
        )
        val tidslinje2 = Sykdomstidslinje.sykedager(
            1.mandag, 1.onsdag, 100.prosent, TestEvent.inntektsmelding
        )
        val tidslinje = tidslinje1.merge(tidslinje2, default)
        assertEquals("XXX", tidslinje.toShortString())
    }

    @Test
    fun `sykeperiode med permisjon`() {
        val sykedager = Sykdomstidslinje.sykedager(
            1.mandag, 1.fredag, 100.prosent, TestEvent.søknad
        )
        val permisjonsdager = Sykdomstidslinje.permisjonsdager(
            2.mandag, 2.fredag, TestEvent.søknad
        )

        val tidslinje = sykedager + permisjonsdager

        assertEquals("SSSSS?? PPPPP", tidslinje.toShortString())
    }

    @Test
    fun `to sykeperioder med mellomrom får riktig slutt og start dato`() {
        val tidslinje1 = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, 100.prosent, TestEvent.søknad)
        val tidslinje2 = Sykdomstidslinje.sykedager(1.fredag, 2.mandag, 100.prosent, TestEvent.søknad)

        val tidslinje = tidslinje2 + tidslinje1

        assertEquals(1.mandag, tidslinje.periode()?.start)
        assertEquals(2.mandag, tidslinje.periode()?.endInclusive)
        assertEquals(8, tidslinje.count())
        assertEquals("SS??SHH S", tidslinje.toShortString())
    }

    @Test
    fun `tidslinje med problemdag er utenfor omfang`() {
        val tidslinje = Sykdomstidslinje.problemdager(1.mandag, 1.mandag, TestEvent.testkilde, "Dette er en problemdag")
        val aktivitetslogg = Aktivitetslogg()
        assertFalse(tidslinje.valider(aktivitetslogg))
        assertTrue(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `kutter frem til og med`() {
        val tidslinje = Sykdomstidslinje.sykedager(1.februar, 1.mars, 100.prosent, TestEvent.testkilde)
        assertEquals(14.februar, tidslinje.fremTilOgMed(14.februar).sisteDag())
        assertEquals(1.mars, tidslinje.fremTilOgMed(1.mars).sisteDag())
        assertEquals(1.mars, tidslinje.fremTilOgMed(1.april).sisteDag())
        assertEquals(1.februar, tidslinje.fremTilOgMed(1.februar).sisteDag())
        assertEquals(1.februar, Sykdomstidslinje.sykedager(1.februar, 1.februar, 100.prosent, TestEvent.testkilde)
            .fremTilOgMed(1.februar)
            .sisteDag())
        assertEquals(0, tidslinje.fremTilOgMed(tidslinje.førsteDag().minusDays(1)).count())
        assertEquals(0, Sykdomstidslinje().fremTilOgMed(1.januar).count())
    }

    @Test
    fun `overskriving av tidslinje`() {
        val tidslinje1 = (Sykdomstidslinje.problemdager(1.mandag, 1.onsdag, TestEvent.sykmelding, "Yes")
            + Sykdomstidslinje.sykedager(1.torsdag, 1.fredag, 100.prosent, TestEvent.sykmelding))
        val tidslinje2 = (Sykdomstidslinje.arbeidsdager(1.mandag, 1.onsdag, TestEvent.testkilde))

        val merged = tidslinje1.merge(tidslinje2, Dag.replace)
        assertEquals("AAASS", merged.toShortString())
    }

    @Test
    fun `dager som har sykmelding som kilde`(){
        val tidslinje = Sykdomstidslinje.sykedager(1.mandag, 1.onsdag, 100.prosent, TestEvent.sykmelding) + Sykdomstidslinje.sykedager(1.torsdag, 1.fredag, 100.prosent, TestEvent.søknad)
        assertTrue(tidslinje.harDagUtenSøknad(1.januar til 5.januar))
        assertFalse(tidslinje.harDagUtenSøknad(4.januar til 5.januar))
    }

    @Test
    fun `ferie + ukjente dager i tidslinje skaper rare skjæringstidspunkt`() {
        val tidslinje = 24.S + 2.F + 5.opphold + 16.F + 5.S
        val skjæringstidspunkter = tidslinje.skjæringstidspunkter()
        assertTrue(skjæringstidspunkter.contains(1.januar))
        assertTrue(skjæringstidspunkter.contains(17.februar))
        assertEquals(2, skjæringstidspunkter.size)
    }

    @Test
    fun `skjæringstidspunkt - sammenhengende syk`() {
        val tidslinje = 24.S
        val skjæringstidspunkter = tidslinje.skjæringstidspunkter()
        assertEquals(1, skjæringstidspunkter.size)
        assertEquals(1.januar, skjæringstidspunkter.first())
    }

    @Test
    fun `skjæringstidspunkt - syk opphold syk`() {
        val tidslinje = 24.S + 10.opphold + 10.S
        val skjæringstidspunkter = tidslinje.skjæringstidspunkter()
        assertEquals(2, skjæringstidspunkter.size)
        assertTrue(skjæringstidspunkter.contains(1.januar))
        assertTrue(skjæringstidspunkter.contains(4.februar))
    }

    @Test
    fun `skjæringstidspunkt - syk ferie syk`() {
        val tidslinje = 24.S + 10.F + 10.S
        val skjæringstidspunkter = tidslinje.skjæringstidspunkter()
        assertEquals(1, skjæringstidspunkter.size)
        assertEquals(1.januar, skjæringstidspunkter.first())
    }

    @Test
    fun `skjæringstidspunkt - syk opphold ferie`() {
        val tidslinje = 24.S + 10.opphold + 10.F
        val skjæringstidspunkter = tidslinje.skjæringstidspunkter()
        assertEquals(1, skjæringstidspunkter.size)
        assertEquals(1.januar, skjæringstidspunkter.first())
    }

    @Test
    fun `skjæringstidspunkt - syk ferie opphold syk`() {
        val tidslinje = 24.S + 10.F + 10.opphold + 10.S
        val skjæringstidspunkter = tidslinje.skjæringstidspunkter()
        assertEquals(2, skjæringstidspunkter.size)
        assertTrue(skjæringstidspunkter.contains(1.januar))
        assertTrue(skjæringstidspunkter.contains(14.februar))
    }

    @Test
    fun `erlåst - låst periode`() {
        val tidslinje = 24.S
        val periode = requireNotNull(tidslinje.periode())
        tidslinje.lås(periode)
        assertTrue(tidslinje.erLåst(periode))
    }

    @Test
    fun `erlåst - ikke låst periode`() {
        val tidslinje = 24.S
        val periode = requireNotNull(tidslinje.periode())
        assertFalse(tidslinje.erLåst(periode))
    }

    @Test
    fun `Tar med låser som ikke er forkastet`() {
        val tidslinje = 31.S
        tidslinje.lås(1.januar til 9.januar)
        tidslinje.lås(10.januar til 31.januar)
        val nyTidslinje = tidslinje.trim(listOf(10.januar til 31.januar))
        assertEquals(1.januar, nyTidslinje.førsteDag())
        assertEquals(9.januar, nyTidslinje.sisteDag())
        val forsøktOverskrevetTidslinje = resetSeed {
            nyTidslinje.merge(9.F)
        }
        assertEquals(nyTidslinje, forsøktOverskrevetTidslinje)
    }

    private val konfliktsky = { venstre: Dag, høyre: Dag ->
        when {
            venstre is Dag.UkjentDag -> høyre
            høyre is Dag.UkjentDag -> venstre
            else -> venstre.problem(høyre)
        }
    }
}
