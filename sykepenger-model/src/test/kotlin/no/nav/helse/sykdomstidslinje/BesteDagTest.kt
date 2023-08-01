package no.nav.helse.sykdomstidslinje

import no.nav.helse.januar
import no.nav.helse.mandag
import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.tournament.Dagturnering
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BesteDagTest {

    companion object {
        private val ukjentDag = Dag.UkjentDag(1.januar, TestEvent.søknad)
        private val arbeidsdagFraSøknad = Dag.Arbeidsdag(1.januar, TestEvent.søknad)
        private val arbeidsdagFraInntektsmelding = Dag.Arbeidsdag(1.januar, TestEvent.inntektsmelding)
        private val ferieFraInntektsmelding = Dag.Feriedag(1.januar, TestEvent.inntektsmelding)
        private val ferieUtenSykmeldingdag = Dag.FerieUtenSykmeldingDag(1.januar, TestEvent.saksbehandler)
        private val arbeidsgiverdagFraInntektsmelding = Dag.Arbeidsgiverdag(1.januar, Økonomi.sykdomsgrad(100.prosent), TestEvent.inntektsmelding)
        private val ferieFraSøknad = Dag.Feriedag(1.januar, TestEvent.søknad)
        private val ferieFraSaksbehandler = Dag.Feriedag(1.januar, TestEvent.saksbehandler)
        private val permisjonFraSøknad = Dag.Permisjonsdag(1.januar, TestEvent.søknad)
        private val sykedagFraSøknad = Dag.Sykedag(1.januar, Økonomi.sykdomsgrad(100.prosent), TestEvent.søknad)
        private val sykedagFraSaksbehandler = Dag.Sykedag(1.januar, Økonomi.sykdomsgrad(100.prosent), TestEvent.saksbehandler)
        private val egenmeldingsdagFraSaksbehandler = Dag.Arbeidsgiverdag(1.januar, Økonomi.sykdomsgrad(100.prosent), TestEvent.saksbehandler)
        private val arbeidsdagFraSaksbehandler = Dag.Arbeidsdag(1.januar, TestEvent.saksbehandler)
        private val sykedagNavFraSaksbehandler = Dag.SykedagNav(1.januar, Økonomi.sykdomsgrad(100.prosent), TestEvent.saksbehandler)
    }

    @Test
    fun `sammenhengende sykdom`() {
        assertWinnerBidirectional(sykedagFraSøknad, arbeidsdagFraSøknad, sykedagFraSøknad, Dag.sammenhengendeSykdom)
        assertWinnerBidirectional(ferieFraSøknad, arbeidsdagFraSøknad, ferieFraSøknad, Dag.sammenhengendeSykdom)
        assertWinnerBidirectional(sykedagFraSøknad, ferieFraSøknad, sykedagFraSøknad, Dag.sammenhengendeSykdom)
        assertWinnerBidirectional(sykedagFraSøknad, permisjonFraSøknad, sykedagFraSøknad, Dag.sammenhengendeSykdom)
        assertWinnerBidirectional(permisjonFraSøknad, arbeidsdagFraSøknad, permisjonFraSøknad, Dag.sammenhengendeSykdom)
        assertWinnerBidirectional(sykedagFraSøknad, arbeidsdagFraSøknad, sykedagFraSøknad, Dag.sammenhengendeSykdom)
        assertWinnerBidirectional(sykedagNavFraSaksbehandler, arbeidsdagFraSøknad, sykedagNavFraSaksbehandler, Dag.sammenhengendeSykdom)
        assertWinnerBidirectional(sykedagNavFraSaksbehandler, permisjonFraSøknad, sykedagNavFraSaksbehandler, Dag.sammenhengendeSykdom)
    }

    @Test
    fun `inntektsmelding sier ferie, søknad sier syk blir feriedag`() {
        assertWinnerBidirectional(sykedagFraSøknad, ferieFraInntektsmelding, ferieFraInntektsmelding)
    }

    @Test
    fun `egensmeldingdag fra saksbehandler`() {
        assertWinnerBidirectional(egenmeldingsdagFraSaksbehandler, arbeidsdagFraSøknad, egenmeldingsdagFraSaksbehandler)
        assertWinnerBidirectional(egenmeldingsdagFraSaksbehandler, sykedagFraSøknad, sykedagFraSøknad)
        assertWinnerBidirectional(egenmeldingsdagFraSaksbehandler, ferieFraSøknad, ferieFraSøknad)
        assertWinnerBidirectional(egenmeldingsdagFraSaksbehandler, permisjonFraSøknad, permisjonFraSøknad)
        assertWinnerBidirectional(egenmeldingsdagFraSaksbehandler, arbeidsgiverdagFraInntektsmelding, arbeidsgiverdagFraInntektsmelding)
        assertWinnerBidirectional(egenmeldingsdagFraSaksbehandler, ukjentDag, egenmeldingsdagFraSaksbehandler)
    }

    @Test
    fun `ferie uten sykmelding`() {
        assertWinnerBidirectional(arbeidsdagFraSøknad, ferieUtenSykmeldingdag, arbeidsdagFraSøknad)
        assertWinnerBidirectional(arbeidsdagFraInntektsmelding, ferieUtenSykmeldingdag, arbeidsdagFraInntektsmelding)
        assertWinnerBidirectional(sykedagFraSøknad, ferieUtenSykmeldingdag, sykedagFraSøknad)
        assertWinnerBidirectional(ferieFraSøknad, ferieUtenSykmeldingdag, ferieFraSøknad)
        assertWinnerBidirectional(arbeidsgiverdagFraInntektsmelding, ferieUtenSykmeldingdag, arbeidsgiverdagFraInntektsmelding)

        assertWinner(ferieFraSaksbehandler, ferieUtenSykmeldingdag, ferieUtenSykmeldingdag)
        assertWinner(ferieUtenSykmeldingdag, ferieFraSaksbehandler, ferieFraSaksbehandler)

        assertWinner(sykedagNavFraSaksbehandler, ferieUtenSykmeldingdag, ferieUtenSykmeldingdag)
        assertWinner(ferieUtenSykmeldingdag, sykedagNavFraSaksbehandler, sykedagNavFraSaksbehandler)

        assertWinner(sykedagFraSaksbehandler, ferieUtenSykmeldingdag, ferieUtenSykmeldingdag)
        assertWinner(ferieUtenSykmeldingdag, sykedagFraSaksbehandler, sykedagFraSaksbehandler)
    }

    @Test
    fun `arbeidsdag fra saksbehandler`() {
        assertWinnerBidirectional(arbeidsdagFraSaksbehandler, arbeidsdagFraSøknad, arbeidsdagFraSaksbehandler)
        assertWinnerBidirectional(arbeidsdagFraSaksbehandler, sykedagFraSøknad, arbeidsdagFraSaksbehandler)
        assertWinnerBidirectional(arbeidsdagFraSaksbehandler, ferieFraSøknad, arbeidsdagFraSaksbehandler)
        assertWinnerBidirectional(arbeidsdagFraSaksbehandler, permisjonFraSøknad, arbeidsdagFraSaksbehandler)
        assertWinnerBidirectional(arbeidsdagFraSaksbehandler, arbeidsgiverdagFraInntektsmelding, arbeidsdagFraSaksbehandler)
        assertWinnerBidirectional(arbeidsdagFraSaksbehandler, ukjentDag, arbeidsdagFraSaksbehandler)
    }

    @Test
    fun `sykedag nav fra saksbehandler`() {
        assertWinnerBidirectional(sykedagNavFraSaksbehandler, arbeidsdagFraSøknad, sykedagNavFraSaksbehandler)
        assertWinner(sykedagNavFraSaksbehandler, arbeidsdagFraSaksbehandler, arbeidsdagFraSaksbehandler)
        assertWinner(arbeidsdagFraSaksbehandler, sykedagNavFraSaksbehandler, sykedagNavFraSaksbehandler)
        assertWinnerBidirectional(sykedagNavFraSaksbehandler, sykedagFraSøknad, sykedagNavFraSaksbehandler)
        assertWinnerBidirectional(sykedagNavFraSaksbehandler, ferieFraSøknad, sykedagNavFraSaksbehandler)
        assertWinnerBidirectional(sykedagNavFraSaksbehandler, permisjonFraSøknad, sykedagNavFraSaksbehandler)
        assertWinnerBidirectional(sykedagNavFraSaksbehandler, arbeidsgiverdagFraInntektsmelding, sykedagNavFraSaksbehandler)
        assertWinnerBidirectional(sykedagNavFraSaksbehandler, ukjentDag, sykedagNavFraSaksbehandler)
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
    fun `permisjon vinner over sykdom`() {
        assertWinner(sykedagFraSøknad, permisjonFraSøknad, permisjonFraSøknad)
        assertWinner(permisjonFraSøknad, sykedagFraSøknad, sykedagFraSøknad)
    }

    @Test
    fun `søknad med egenmelding vinner over en gitt dag`() {
        assertWinnerBidirectional(ferieFraSøknad, arbeidsgiverdagFraInntektsmelding, arbeidsgiverdagFraInntektsmelding)
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
        expectedWinner: Dag,
        turnering: (Dag, Dag) -> Dag = Dagturnering.TURNERING::beste
    ) {
        val winner = turnering(dag1, dag2)
        assertEquals(expectedWinner, winner)
    }

    private fun assertWinnerBidirectional(
        dag1: Dag,
        dag2: Dag,
        expectedWinner: Dag,
        turnering: (Dag, Dag) -> Dag = Dagturnering.TURNERING::beste
    ) {
        assertWinner(dag1, dag2, expectedWinner, turnering)
        assertWinner(dag2, dag1, expectedWinner, turnering)
    }
}
