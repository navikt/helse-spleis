package no.nav.helse.sykdomstidslinje

import no.nav.helse.januar
import no.nav.helse.sykdomstidslinje.Dag.Companion.sammenhengendeSykdom
import no.nav.helse.testhelpers.TestEvent
import no.nav.helse.tournament.Dagturnering
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BesteDagTest {

    companion object {
        private val ukjentDag = Dag.UkjentDag(1.januar, TestEvent.søknad)
        private val arbeidsdagFraSøknad = Dag.Arbeidsdag(1.januar, TestEvent.søknad)
        private val arbeidsdagFraInntektsmelding = Dag.Arbeidsdag(1.januar, TestEvent.inntektsmelding)
        private val ferieFraInntektsmelding = Dag.Feriedag(1.januar, TestEvent.inntektsmelding)
        private val friskHelgFraInntektsmelding = Dag.FriskHelgedag(1.januar, TestEvent.inntektsmelding)
        private val arbeidIkkeGjenopptattDag = Dag.ArbeidIkkeGjenopptattDag(1.januar, TestEvent.saksbehandler)
        private val arbeidsgiverdagFraInntektsmelding = Dag.Arbeidsgiverdag(1.januar, 100.prosent, TestEvent.inntektsmelding)
        private val ferieFraSøknad = Dag.Feriedag(1.januar, TestEvent.søknad)
        private val ferieFraSaksbehandler = Dag.Feriedag(1.januar, TestEvent.saksbehandler)
        private val permisjonFraSøknad = Dag.Permisjonsdag(1.januar, TestEvent.søknad)
        private val sykedagFraSøknad = Dag.Sykedag(1.januar, 100.prosent, TestEvent.søknad)
        private val sykHelgedagFraSøknad = Dag.SykHelgedag(6.januar, 100.prosent, TestEvent.søknad)
        private val permisjonHelgedagFraSøknad = Dag.Permisjonsdag(6.januar, TestEvent.søknad)
        private val sykedagFraSaksbehandler = Dag.Sykedag(1.januar, 100.prosent, TestEvent.saksbehandler)
        private val egenmeldingsdagFraSaksbehandler = Dag.Arbeidsgiverdag(1.januar, 100.prosent, TestEvent.saksbehandler)
        private val arbeidsdagFraSaksbehandler = Dag.Arbeidsdag(1.januar, TestEvent.saksbehandler)
        private val andreYtelser = Dag.AndreYtelser(1.januar, TestEvent.testkilde, Dag.AndreYtelser.AnnenYtelse.Foreldrepenger)
    }

    @Test
    fun `sammenhengende sykdom`() {
        sammenhengendeSykdom betyr_at sykedagFraSøknad slår arbeidsdagFraSøknad
        sammenhengendeSykdom betyr_at sykedagFraSøknad slår ferieFraSøknad
        sammenhengendeSykdom betyr_at sykedagFraSøknad slår permisjonFraSøknad
        sammenhengendeSykdom betyr_at sykedagFraSøknad slår arbeidsdagFraSøknad
        sammenhengendeSykdom betyr_at ferieFraSøknad slår arbeidsdagFraSøknad
        sammenhengendeSykdom betyr_at permisjonFraSøknad slår arbeidsdagFraSøknad
    }

    @Test
    fun `inntektsmelding sier ferie, søknad sier syk blir feriedag`() {
        ferieFraInntektsmelding slår sykedagFraSøknad
    }

    @Test
    fun `egensmeldingdag fra saksbehandler`() {
        egenmeldingsdagFraSaksbehandler slår arbeidsdagFraSøknad
        egenmeldingsdagFraSaksbehandler slår ukjentDag
        sykedagFraSøknad slår egenmeldingsdagFraSaksbehandler
        ferieFraSøknad slår egenmeldingsdagFraSaksbehandler
        permisjonFraSøknad slår egenmeldingsdagFraSaksbehandler
        arbeidsgiverdagFraInntektsmelding slår egenmeldingsdagFraSaksbehandler
    }

    @Test
    fun `arbeid ikke gjenopptatt`() {
        arbeidsdagFraSøknad slår arbeidIkkeGjenopptattDag

        arbeidIkkeGjenopptattDag mot arbeidsdagFraInntektsmelding gir arbeidsdagFraInntektsmelding
        arbeidIkkeGjenopptattDag mot friskHelgFraInntektsmelding gir friskHelgFraInntektsmelding
        arbeidsdagFraInntektsmelding mot arbeidIkkeGjenopptattDag gir arbeidIkkeGjenopptattDag
        friskHelgFraInntektsmelding mot arbeidIkkeGjenopptattDag gir arbeidIkkeGjenopptattDag

        sykedagFraSøknad slår arbeidIkkeGjenopptattDag
        ferieFraSøknad slår arbeidIkkeGjenopptattDag
        arbeidsgiverdagFraInntektsmelding slår arbeidIkkeGjenopptattDag

        ferieFraSaksbehandler mot arbeidIkkeGjenopptattDag gir arbeidIkkeGjenopptattDag
        arbeidIkkeGjenopptattDag mot ferieFraSaksbehandler gir ferieFraSaksbehandler

        sykedagFraSaksbehandler mot arbeidIkkeGjenopptattDag gir arbeidIkkeGjenopptattDag
        arbeidIkkeGjenopptattDag mot sykedagFraSaksbehandler gir sykedagFraSaksbehandler
    }

    @Test
    fun `arbeidsdag fra saksbehandler`() {
        arbeidsdagFraSaksbehandler slår arbeidsdagFraSøknad
        arbeidsdagFraSaksbehandler slår sykedagFraSøknad
        arbeidsdagFraSaksbehandler slår ferieFraSøknad
        arbeidsdagFraSaksbehandler slår permisjonFraSøknad
        arbeidsdagFraSaksbehandler slår arbeidsgiverdagFraInntektsmelding
        arbeidsdagFraSaksbehandler slår ukjentDag
    }

    @Test
    fun `nulldag taper mot en gitt dag`() {
        sykedagFraSøknad slår ukjentDag
    }

    @Test
    fun `ferie vinner over sykdom`() {
        sykedagFraSøknad mot ferieFraSøknad gir ferieFraSøknad
        ferieFraSøknad mot sykedagFraSøknad gir sykedagFraSøknad
    }

    @Test
    fun `permisjon vinner over sykdom`() {
        sykedagFraSøknad mot permisjonFraSøknad gir permisjonFraSøknad
        permisjonFraSøknad mot sykedagFraSøknad gir sykedagFraSøknad

        permisjonHelgedagFraSøknad mot sykHelgedagFraSøknad gir sykHelgedagFraSøknad
        sykHelgedagFraSøknad mot permisjonHelgedagFraSøknad gir permisjonHelgedagFraSøknad
    }

    @Test
    fun `søknad med egenmelding vinner over en gitt dag`() {
        arbeidsgiverdagFraInntektsmelding slår ferieFraSøknad
    }

    @Test
    fun `arbeidsdag vinner over sykedag`() {
        sykedagFraSøknad mot arbeidsdagFraSøknad gir arbeidsdagFraSøknad
    }

    @Test
    fun `sykedag fra søknad vinner over egenmeldingsdag i inntektsmelding`() {
        sykedagFraSøknad slår arbeidsgiverdagFraInntektsmelding
    }

    @Test
    fun `andre ytelser vinner over sykedag fra søknad`() {
        andreYtelser slår sykedagFraSøknad
    }

    @Test
    fun `saksbehandler vinner over andre ytelser`() {
        arbeidsdagFraSaksbehandler slår andreYtelser
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

    private infix fun Dag.slår(taper: Dag) = Pair(Dagturnering.TURNERING::beste, this) slår taper
    private infix fun Pair<BesteStrategy, Dag>.slår(taper: Dag) = assertWinnerBidirectional(this.second, taper, this.second, this.first)
    private infix fun Dag.mot(høyre: Dag) = Pair(this, høyre)
    private infix fun Pair<Dag, Dag>.gir(vinner: Dag) = assertWinner(this.first, this.second, vinner)
    private infix fun BesteStrategy.betyr_at(dag: Dag) = Pair(this, dag)
}
