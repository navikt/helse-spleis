package no.nav.helse.tournament

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.DagFactory
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
        val søknadSykedager = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, 100.0, Søknad.SøknadDagFactory)
        val søknadArbeidsdager = ConcreteSykdomstidslinje.ikkeSykedager(Uke(1).torsdag, Uke(1).fredag, Søknad.SøknadDagFactory)

        val tidslinje = søknadSykedager.merge(søknadArbeidsdager, historiskDagturnering)
        assertTrue(
            tidslinje[Uke(1).onsdag] is Sykedag,
            "Onsdag er fortsatt en sykedag etter kombinering av sykmelding og søknad"
        )
        assertTrue(
            tidslinje[Uke(1).torsdag] is Arbeidsdag,
            "Torsdag er en arbeidsdag etter kombinering av sykmelding og søknad"
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
        private var dagbuilder: ((LocalDate, DagFactory) -> Dag)? = null
        private var dagFactory: DagFactory? = null

        val sykedag: TestHendelseBuilder
            get() {
                dagbuilder = { dag:LocalDate, factory:DagFactory ->
                    ConcreteSykdomstidslinje.sykedag(dag, 100.0, factory)
                }
                return this
            }

        val arbeidsdag: TestHendelseBuilder
            get() {
                dagbuilder = ConcreteSykdomstidslinje.Companion::ikkeSykedag
                return this
            }

        val egenmeldingsdag: TestHendelseBuilder
            get() {
                dagbuilder = ConcreteSykdomstidslinje.Companion::egenmeldingsdag
                return this
            }

        val fraSykmelding
            get(): TestHendelseBuilder {
                dagFactory = Sykmelding.SykmeldingDagFactory
                return this
            }

        val fraSøknad
            get(): TestHendelseBuilder {
                dagFactory = Søknad.SøknadDagFactory
                return this
            }

        val fraInntektsmelding
            get(): TestHendelseBuilder {
                dagFactory = Inntektsmelding.InntektsmeldingDagFactory
                return this
            }
        val rapportertTidlig
            get() = dagbuilder!!(
                dato, dagFactory ?: Søknad.SøknadDagFactory
            )
        val rapportertSent
            get() = dagbuilder!!(
                dato, dagFactory ?: Søknad.SøknadDagFactory
            )

    }
}

