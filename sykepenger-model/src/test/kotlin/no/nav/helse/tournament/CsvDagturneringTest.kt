package no.nav.helse.tournament

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.DagFactory
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.onsdag
import no.nav.helse.testhelpers.torsdag
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
        val søknadSykedager = NySykdomstidslinje.sykedager(1.mandag, 1.fredag, 100.0, Søknad.SøknadDagFactory)
        val søknadArbeidsdager = NySykdomstidslinje.ikkeSykedager(1.torsdag, 1.fredag, Søknad.SøknadDagFactory)

        val tidslinje = søknadSykedager.merge(søknadArbeidsdager, historiskDagturnering)
        assertTrue(
            tidslinje[1.onsdag] is Sykedag,
            "Onsdag er fortsatt en sykedag etter kombinering av sykmelding og søknad"
        )
        assertTrue(
            tidslinje[1.torsdag] is Arbeidsdag,
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
                    NySykdomstidslinje.sykedag(dag, 100.0, factory)
                }
                return this
            }

        val arbeidsdag: TestHendelseBuilder
            get() {
                dagbuilder = NySykdomstidslinje.Companion::ikkeSykedag
                return this
            }

        val egenmeldingsdag: TestHendelseBuilder
            get() {
                dagbuilder = NySykdomstidslinje.Companion::egenmeldingsdag
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

