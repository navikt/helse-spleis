package no.nav.helse.sykdomstidslinje

import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.testhelpers.mandag
import no.nav.helse.tournament.dagturnering
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BesteDagTest {

    companion object {
        private val ukjentDag get() = Dag.UkjentDag(2.mandag, TestEvent.søknad)
        private val arbeidsdagFraSøknad get() = Dag.Arbeidsdag(2.mandag, TestEvent.søknad)
        private val ferieFraInntektsmelding get() = Dag.Feriedag(2.mandag, TestEvent.inntektsmelding)
        private val arbeidsgiverdagFraInntektsmelding get() = Dag.Arbeidsgiverdag(2.mandag, Økonomi.sykdomsgrad(100.prosent), TestEvent.inntektsmelding)
        private val ferieFraSøknad get() = Dag.Feriedag(2.mandag, TestEvent.søknad)
        private val sykedagFraSøknad get() = Dag.Sykedag(2.mandag, Økonomi.sykdomsgrad(100.prosent), TestEvent.søknad)
        private val utenlandsFraSøknad get() = Dag.Utenlandsdag(2.mandag, TestEvent.søknad)
    }

    @Test
    fun `inntektsmelding sier ferie, søknad sier syk blir feriedag`() {
        assertWinnerBidirectional(sykedagFraSøknad, ferieFraInntektsmelding, ferieFraInntektsmelding)
    }

    @Test
    fun `nulldag taper mot en gitt dag`() {
        assertWinnerBidirectional(ukjentDag, sykedagFraSøknad, sykedagFraSøknad)
    }

    @Test
    fun `ferie vinner over sykdom`() {
        assertWinner(sykedagFraSøknad, ferieFraSøknad, ferieFraSøknad)
        assertWinner(ferieFraSøknad, sykedagFraSøknad, sykedagFraSøknad)
    }

    @Test
    fun `søknad med egenmelding vinner over en gitt dag`() {
        assertWinnerBidirectional(ferieFraSøknad, arbeidsgiverdagFraInntektsmelding, arbeidsgiverdagFraInntektsmelding)
    }

    @Test
    fun `sammenligning med utenlandsdag git altid ubestemtdag`() {
        assertProblemDagBidirectional(ukjentDag, utenlandsFraSøknad)
        assertProblemDagBidirectional(arbeidsdagFraSøknad, utenlandsFraSøknad)
        assertProblemDagBidirectional(sykedagFraSøknad, utenlandsFraSøknad)
        assertProblemDagBidirectional(arbeidsgiverdagFraInntektsmelding, utenlandsFraSøknad)
        assertProblemDagBidirectional(ferieFraSøknad, utenlandsFraSøknad)
        assertProblemDagBidirectional(ferieFraInntektsmelding, utenlandsFraSøknad)
    }

    @Test
    fun `arbeidsdag vinner over sykedag`() {
        assertWinner(sykedagFraSøknad, arbeidsdagFraSøknad, arbeidsdagFraSøknad)
    }

    @Test
    fun `sykedag fra søknad vinner over egenmeldingsdag i inntektsmelding`() {
        assertWinner(sykedagFraSøknad, arbeidsgiverdagFraInntektsmelding, sykedagFraSøknad)
        assertWinner(arbeidsgiverdagFraInntektsmelding, sykedagFraSøknad, sykedagFraSøknad)
    }

    private fun assertWinner(
        dag1: Dag,
        dag2: Dag,
        expectedWinner: Dag
    ) {
        val winner = dagturnering.beste(dag1, dag2)
        assertEquals(expectedWinner, winner)
    }

    private fun assertWinnerBidirectional(
        dag1: Dag,
        dag2: Dag,
        expectedWinner: Dag
    ) {
        assertWinner(dag1, dag2, expectedWinner)
        assertWinner(dag2, dag1, expectedWinner)
    }

    private fun assertProblemDagBidirectional(
        dag1: Dag,
        dag2: Dag
    ) {
        assertWinner(dag1, dag2, dag2.problem(dag1))
        assertWinner(dag2, dag1, dag2.problem(dag1))
    }
}
