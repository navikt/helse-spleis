package no.nav.helse.sykdomstidslinje

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mandag
import no.nav.helse.mars
import no.nav.helse.onsdag
import no.nav.helse.sykdomstidslinje.Dag.Companion.default
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.AIG
import no.nav.helse.testhelpers.F
import no.nav.helse.testhelpers.K
import no.nav.helse.testhelpers.P
import no.nav.helse.testhelpers.R
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.testhelpers.U
import no.nav.helse.testhelpers.UK
import no.nav.helse.testhelpers.YF
import no.nav.helse.testhelpers.YP
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.tirsdag
import no.nav.helse.torsdag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SykdomstidslinjeTest {

    @BeforeEach
    fun setup() {
        resetSeed()
    }

    @Test
    fun `Like sykdomstidslinjer med samme kilde er like`() {
        val meldingsreferanse = MeldingsreferanseId(UUID.randomUUID())
        val nå = LocalDateTime.now()
        val kilde = Hendelseskilde.INGEN.copy(meldingsreferanseId = meldingsreferanse, tidsstempel = nå)
        val kilde2 = kilde.copy(meldingsreferanseId = meldingsreferanse)

        val sykdomstidslinje1 = 5.S(kilde)
        resetSeed()
        val sykdomstidslinje2 = 5.S(kilde2)
        resetSeed()
        val sykdomstidslinje3 = 5.S(kilde.copy(meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())))
        assertEquals(sykdomstidslinje1, sykdomstidslinje2)
        assertNotEquals(sykdomstidslinje1, sykdomstidslinje3)
    }

    @Test
    fun `unik dagtype short string `() {
        assertEquals("Tom tidslinje", Sykdomstidslinje().toUnikDagtypeShortString())
        val tidslinje = 5.S + 5.F + 5.UK + 5.P
        assertEquals("SSSSSFF FFF???? ?PPPPP", tidslinje.toShortString())
        assertEquals("?FPS", tidslinje.toUnikDagtypeShortString())
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
    fun `er rett før for andre ytelser`() {
        assertTrue(1.S.erRettFør(1.YF))
        assertTrue(1.YF.erRettFør(1.YF))
        assertTrue(1.YF.erRettFør(1.YF + 1.S))
        assertTrue(1.YF.erRettFør(1.S))
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
    fun `kutter frem til og med`() {
        val tidslinje = Sykdomstidslinje.sykedager(1.februar, 1.mars, 100.prosent, TestEvent.testkilde)
        assertEquals(14.februar, tidslinje.fremTilOgMed(14.februar).sisteDag())
        assertEquals(1.mars, tidslinje.fremTilOgMed(1.mars).sisteDag())
        assertEquals(1.mars, tidslinje.fremTilOgMed(1.april).sisteDag())
        assertEquals(1.februar, tidslinje.fremTilOgMed(1.februar).sisteDag())
        assertEquals(
            1.februar, Sykdomstidslinje.sykedager(1.februar, 1.februar, 100.prosent, TestEvent.testkilde)
            .fremTilOgMed(1.februar)
            .sisteDag()
        )
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
    fun `legger bare til dager som var ukjente`() {
        val tidslinje1 = 1.S + 1.UK + 1.F + 1.AIG + 1.P + 1.YF + 1.U + 1.K + 1.R
        resetSeed()
        val tidslinje2 = 9.K

        val merged = tidslinje1.merge(tidslinje2, Dag.bareNyeDager)
        assertEquals("S?FJPYG KR", tidslinje1.toShortString())
        assertEquals("KKKKKHH KK", tidslinje2.toShortString())
        assertEquals("SKFJPYG KR", merged.toShortString())
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

    @Test
    fun `trekke en sykdomstidslinje fra en annen`() {
        assertEquals(Sykdomstidslinje(), Sykdomstidslinje() - Sykdomstidslinje())
        val tidslinje1 = 31.S
        resetSeed(2.januar)
        val tidslinje2 = 29.S // mangler 1.januar og 31.januar
        val resultat = tidslinje1 - tidslinje2
        assertEquals(setOf(1.januar, 31.januar), resultat.inspektør.dager.filterValues { it !is Dag.UkjentDag }.keys)
        assertEquals(tidslinje1, tidslinje2 + resultat)
        assertEquals(Sykdomstidslinje(), tidslinje2 - tidslinje1)
    }

    @Test
    fun `feriedager uten sykmelding`() {
        val tidslinje = 7.AIG
        assertEquals("JJJJJJJ", tidslinje.toShortString())
    }

    private val konfliktsky = { venstre: Dag, høyre: Dag ->
        when {
            venstre is Dag.UkjentDag -> høyre
            høyre is Dag.UkjentDag -> venstre
            else -> venstre.problem(høyre)
        }
    }
}
