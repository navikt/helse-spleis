package no.nav.helse.tournament


import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.Uke
import no.nav.helse.testhelpers.get
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DagTurneringTest {

    companion object {
        val turnering = DagTurnering()
        val microturnering = DagTurnering("/microturnering.csv")
        val testhendelse = { dato: LocalDate ->
            Testhendelse(dato.atStartOfDay())
        }

        private val mandag get() = TestHendelseBuilder(LocalDate.of(2018, 1, 1))
        private val søndag get() = TestHendelseBuilder(LocalDate.of(2018, 1, 7))
    }

    @Test
    internal fun `Turneringen skal inneholde riktige strategier basert på en csv-fil`() {

        assertEquals(
            LatestOrRow::class,
            microturnering.strategies.getValue(Dag.Nøkkel.I).getValue(Dag.Nøkkel.I)::class
        )
        assertEquals(
            LatestOrColumn::class,
            microturnering.strategies.getValue(Dag.Nøkkel.S_A).getValue(Dag.Nøkkel.I)::class
        )
        assertEquals(Row::class, microturnering.strategies.getValue(Dag.Nøkkel.WD_A).getValue(Dag.Nøkkel.I)::class)
        assertEquals(
            Undecided::class,
            microturnering.strategies.getValue(Dag.Nøkkel.WD_A).getValue(Dag.Nøkkel.WD_A)::class
        )
        assertEquals(
            Column::class,
            microturnering.strategies.getValue(Dag.Nøkkel.S_A).getValue(Dag.Nøkkel.WD_A)::class
        )
        assertEquals(
            Impossible::class,
            microturnering.strategies.getValue(Dag.Nøkkel.S_A).getValue(Dag.Nøkkel.S_A)::class
        )
    }


    @Test
    internal fun `Arbeidsdag fra søknad vinner over sykedag fra sykmelding`() {
        val sykedag = mandag.sykedag.fraSykmelding.rapportertTidlig
        val arbeidsdag = mandag.arbeidsdag.fraSøknad.rapportertSent

        val vinner = turnering.slåss(arbeidsdag, sykedag)

        assertEquals(vinner, arbeidsdag)
    }

    @Test
    internal fun `kombinering av tidslinjer fører til at dagsturnering slår sammen dagene`() {
        val nySøknad = ConcreteSykdomstidslinje.sykedager(
            Uke(1).mandag, Uke(1).fredag, Testhendelse(
                rapportertdato = Uke(1).mandag.atTime(9, 0)
            )
        )
        val sendtSøknad = Testhendelse(
            rapportertdato = Uke(1).mandag.atTime(12, 0)
        )
        val sendtSøknadSykedager = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, sendtSøknad)
        val sendtSøknadArbeidsdager = ConcreteSykdomstidslinje.ikkeSykedager(Uke(1).torsdag, Uke(1).fredag, sendtSøknad)

        val tidslinje = nySøknad + (sendtSøknadSykedager + sendtSøknadArbeidsdager)
        assertTrue(
            tidslinje[Uke(1).onsdag] is Sykedag,
            "Onsdag er fortsatt en sykedag etter kombinering av ny og sendt søknad"
        )
        assertTrue(
            tidslinje[Uke(1).torsdag] is Arbeidsdag,
            "Torsdag er en arbeidsdag etter kombinering av ny og sendt søknad"
        )
    }

    @Test
    internal fun `sykedag fra arbeidsgiver taper mot syk helgedag fra sykmeldingen`() {
        val egenmeldingsdagFraArbeidsgiver = søndag.egenmeldingsdag.fraInntektsmelding.rapportertSent
        val sykHelgedag = søndag.sykedag.fraSykmelding.rapportertTidlig

        val vinner = turnering.slåss(egenmeldingsdagFraArbeidsgiver, sykHelgedag)
        assertEquals(sykHelgedag, vinner)
    }

    @Test
    internal fun `arbeidsdag fra inntektsmelding vinner over egenmelding fra søknad`() {
        val egenmeldingsdag = mandag.egenmeldingsdag.fraSøknad.rapportertTidlig
        val arbeidsdag = mandag.arbeidsdag.fraInntektsmelding.rapportertTidlig

        val vinner = turnering.slåss(egenmeldingsdag, arbeidsdag)
        assertEquals(arbeidsdag, vinner)
    }

    @Test
    internal fun `arbeidsdag fra inntektsmelding vinner over sykedag fra sykmelding`() {
        val sykedag = mandag.sykedag.fraSykmelding.rapportertTidlig
        val arbeidsdag = mandag.arbeidsdag.fraInntektsmelding.rapportertTidlig

        val vinner = turnering.slåss(sykedag, arbeidsdag)
        assertEquals(arbeidsdag, vinner)
    }

    private class TestHendelseBuilder(private val dato: LocalDate) {
        private var dagbuilder: ((LocalDate, SykdomstidslinjeHendelse) -> Dag)? = null
        private var hendelsetype: Dag.NøkkelHendelseType? = null

        val sykedag: TestHendelseBuilder
            get() {
                dagbuilder = { dato, hendelse -> ConcreteSykdomstidslinje.sykedag(dato, hendelse) }
                return this
            }

        val arbeidsdag: TestHendelseBuilder
            get() {
                dagbuilder = { dato, hendelse -> ConcreteSykdomstidslinje.ikkeSykedag(dato, hendelse) }
                return this
            }

        val egenmeldingsdag: TestHendelseBuilder
            get() {
                dagbuilder = { dato, hendelse -> ConcreteSykdomstidslinje.egenmeldingsdag(dato, hendelse) }
                return this
            }

        val fraSykmelding
            get(): TestHendelseBuilder {
                hendelsetype = Dag.NøkkelHendelseType.Sykmelding
                return this
            }

        val fraSøknad
            get(): TestHendelseBuilder {
                hendelsetype = Dag.NøkkelHendelseType.Søknad
                return this
            }

        val fraInntektsmelding
            get(): TestHendelseBuilder {
                hendelsetype = Dag.NøkkelHendelseType.Inntektsmelding
                return this
            }
        val rapportertTidlig
            get() = dagbuilder!!(
                dato, Testhendelse(
                    dato.atStartOfDay(),
                    hendelsetype ?: Dag.NøkkelHendelseType.Søknad
                )
            )
        val rapportertSent
            get() = dagbuilder!!(
                dato, Testhendelse(
                    dato.atTime(18, 0),
                    hendelsetype ?: Dag.NøkkelHendelseType.Søknad
                )
            )

    }
}

