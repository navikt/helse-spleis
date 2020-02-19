package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.testhelpers.Uke
import no.nav.helse.tournament.historiskDagturnering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class BesteDagTest {

    companion object {
        private val implisittDag get() = ImplisittDag(Uke(2).mandag)
        private val arbeidsdag get() = Arbeidsdag.Søknad(Uke(2).mandag)
        private val ferieFraInntektsmelding get() = ConcreteSykdomstidslinje.ferie(Uke(2).mandag, Inntektsmelding.InntektsmeldingDagFactory)
        private val egenmeldingFraInntektsmelding get() = ConcreteSykdomstidslinje.egenmeldingsdag(Uke(2).mandag, Inntektsmelding.InntektsmeldingDagFactory)
        private val ferieFraSøknad get() = ConcreteSykdomstidslinje.ferie(Uke(2).mandag, Søknad.SøknadDagFactory)
        private val sykdomFraSendtSøknad get() = ConcreteSykdomstidslinje.sykedag(Uke(2).mandag, Søknad.SøknadDagFactory)
        private val utenlandsFraSendtSøknad get() = ConcreteSykdomstidslinje.utenlandsdag(Uke(2).mandag, Søknad.SøknadDagFactory)
    }

    @Test
    fun `inntektsmelding sier ferie, søknad sier syk blir feriedag`() {
        assertWinner(sykdomFraSendtSøknad, ferieFraInntektsmelding, Feriedag.Inntektsmelding::class)
        assertWinner(ferieFraInntektsmelding, sykdomFraSendtSøknad, Feriedag.Inntektsmelding::class)
    }

    @Test
    fun `nulldag taper mot en gitt dag`() {
        assertWinner(implisittDag, sykdomFraSendtSøknad, Sykedag.Søknad::class)
        assertWinner(sykdomFraSendtSøknad, implisittDag, Sykedag.Søknad::class)
    }

    @Test
    fun `ferie vinner over sykdom`() {
        assertWinner(sykdomFraSendtSøknad, ferieFraSøknad, Feriedag.Søknad::class)
    }

    @Test
    fun `søknad med egenmelding vinner over en gitt dag`() {
        assertWinnerBidirectional(ferieFraSøknad, egenmeldingFraInntektsmelding, Egenmeldingsdag.Inntektsmelding::class)
    }

    @Test
    fun `sammenligning med utenlandsdag git altid ubestemtdag`() {
        assertWinnerBidirectional(implisittDag, utenlandsFraSendtSøknad, Ubestemtdag::class)
        assertWinnerBidirectional(arbeidsdag, utenlandsFraSendtSøknad, Ubestemtdag::class)
        assertWinnerBidirectional(sykdomFraSendtSøknad, utenlandsFraSendtSøknad, Ubestemtdag::class)
        assertWinnerBidirectional(egenmeldingFraInntektsmelding, utenlandsFraSendtSøknad, Ubestemtdag::class)
        assertWinnerBidirectional(ferieFraSøknad, utenlandsFraSendtSøknad, Ubestemtdag::class)
        assertWinnerBidirectional(ferieFraInntektsmelding, utenlandsFraSendtSøknad, Ubestemtdag::class)
    }

    @Test
    fun `arbeidsdag vinner over sykedag`() {
        assertWinner(sykdomFraSendtSøknad, arbeidsdag, Arbeidsdag.Søknad::class)
    }

    @Test
    fun `sykedag fra søknad vinner over egenmeldingsdag i inntektsmelding`() {
        assertWinner(sykdomFraSendtSøknad, egenmeldingFraInntektsmelding, Sykedag.Søknad::class)
        assertWinner(egenmeldingFraInntektsmelding, sykdomFraSendtSøknad, Sykedag.Søknad::class)
    }

    private fun <T : Dag> assertWinner(
        dag1: Dag,
        dag2: Dag,
        expectedWinnerClass: KClass<T>
    ) {
        val winner = dag1.beste(dag2, historiskDagturnering)
        assertEquals(expectedWinnerClass, winner::class)
    }

    private fun <T : Dag> assertWinnerBidirectional(
        dag1: Dag,
        dag2: Dag,
        expectedWinnerClass: KClass<T>
    ) {
        assertWinner(dag1, dag2, expectedWinnerClass)
        assertWinner(dag2, dag1, expectedWinnerClass)
    }
}
