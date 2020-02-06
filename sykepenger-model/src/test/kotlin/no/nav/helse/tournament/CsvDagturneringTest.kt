package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.Uke
import no.nav.helse.testhelpers.get
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class CsvDagturneringTest {

    companion object {
        val turnering = historiskDagturnering
        private val mandag get() = TestHendelseBuilder(LocalDate.of(2018, 1, 1))
        private val søndag get() = TestHendelseBuilder(LocalDate.of(2018, 1, 7))
    }

    @Test
    internal fun `Arbeidsdag fra søknad vinner over sykedag fra sykmelding`() {
        val sykedag = mandag.sykedag.fraSykmelding.rapportertTidlig
        val arbeidsdag = mandag.arbeidsdag.fraSøknad.rapportertSent

        val vinner = turnering.beste(sykedag, arbeidsdag)

        assertEquals(vinner, arbeidsdag)
    }

    @Test
    internal fun `kombinering av tidslinjer fører til at dagsturnering slår sammen dagene`() {
        val sendtSøknadSykedager = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, Dag.NøkkelHendelseType.Søknad)
        val sendtSøknadArbeidsdager = ConcreteSykdomstidslinje.ikkeSykedager(Uke(1).torsdag, Uke(1).fredag, Dag.NøkkelHendelseType.Søknad)

        val tidslinje = (sendtSøknadSykedager + sendtSøknadArbeidsdager)
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

        val vinner = turnering.beste(egenmeldingsdagFraArbeidsgiver, sykHelgedag)
        assertEquals(sykHelgedag, vinner)
    }

    @Test
    internal fun `arbeidsdag fra inntektsmelding vinner over egenmelding fra søknad`() {
        val egenmeldingsdag = mandag.egenmeldingsdag.fraSøknad.rapportertTidlig
        val arbeidsdag = mandag.arbeidsdag.fraInntektsmelding.rapportertTidlig

        val vinner = turnering.beste(egenmeldingsdag, arbeidsdag)
        assertEquals(arbeidsdag, vinner)
    }

    @Test
    internal fun `arbeidsdag fra inntektsmelding vinner over sykedag fra sykmelding`() {
        val sykedag = mandag.sykedag.fraSykmelding.rapportertTidlig
        val arbeidsdag = mandag.arbeidsdag.fraInntektsmelding.rapportertTidlig

        val vinner = turnering.beste(sykedag, arbeidsdag)
        assertEquals(arbeidsdag, vinner)
    }

    private class TestHendelseBuilder(private val dato: LocalDate) {
        private var dagbuilder: ((LocalDate, Dag.NøkkelHendelseType) -> Dag)? = null
        private var hendelsetype: Dag.NøkkelHendelseType? = null

        val sykedag: TestHendelseBuilder
            get() {
                dagbuilder = { dato, hendelseType -> ConcreteSykdomstidslinje.sykedag(dato, hendelseType) }
                return this
            }

        val arbeidsdag: TestHendelseBuilder
            get() {
                dagbuilder = { dato, hendelseType -> ConcreteSykdomstidslinje.ikkeSykedag(dato, hendelseType) }
                return this
            }

        val egenmeldingsdag: TestHendelseBuilder
            get() {
                dagbuilder = { dato, hendelseType -> ConcreteSykdomstidslinje.egenmeldingsdag(dato, hendelseType) }
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
                dato, hendelsetype ?: Dag.NøkkelHendelseType.Søknad
            )
        val rapportertSent
            get() = dagbuilder!!(
                dato, hendelsetype ?: Dag.NøkkelHendelseType.Søknad
            )

    }

    private operator fun ConcreteSykdomstidslinje.plus(other: ConcreteSykdomstidslinje) =
        this.plus(other, ConcreteSykdomstidslinje.Companion::implisittDag, historiskDagturnering)
}

