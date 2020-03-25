package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.testhelpers.mandag
import no.nav.helse.tournament.historiskDagturnering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class BesteDagTest {

    companion object {
        private val implisittDag get() = ImplisittDag(2.mandag)
        private val arbeidsdag get() = Arbeidsdag.Søknad(2.mandag)
        private val ferieFraInntektsmelding get() = Inntektsmelding.InntektsmeldingDagFactory.feriedag(2.mandag)
        private val egenmeldingFraInntektsmelding get() = Inntektsmelding.InntektsmeldingDagFactory.egenmeldingsdag(2.mandag)
        private val ferieFraSøknad get() = Søknad.SøknadDagFactory.feriedag(2.mandag)
        private val sykedagFraSøknad get() = Søknad.SøknadDagFactory.sykedag(2.mandag, 100.0)
        private val utenlandsFraSøknad get() = Søknad.SøknadDagFactory.utenlandsdag(2.mandag)
    }

    @Test
    fun `inntektsmelding sier ferie, søknad sier syk blir feriedag`() {
        assertWinner(sykedagFraSøknad, ferieFraInntektsmelding, Feriedag.Inntektsmelding::class)
        assertWinner(ferieFraInntektsmelding, sykedagFraSøknad, Feriedag.Inntektsmelding::class)
    }

    @Test
    fun `nulldag taper mot en gitt dag`() {
        assertWinner(implisittDag, sykedagFraSøknad, Sykedag.Søknad::class)
        assertWinner(sykedagFraSøknad, implisittDag, Sykedag.Søknad::class)
    }

    @Test
    fun `ferie vinner over sykdom`() {
        assertWinner(sykedagFraSøknad, ferieFraSøknad, Feriedag.Søknad::class)
    }

    @Test
    fun `søknad med egenmelding vinner over en gitt dag`() {
        assertWinnerBidirectional(ferieFraSøknad, egenmeldingFraInntektsmelding, Egenmeldingsdag.Inntektsmelding::class)
    }

    @Test
    fun `sammenligning med utenlandsdag git altid ubestemtdag`() {
        assertWinnerBidirectional(implisittDag, utenlandsFraSøknad, Ubestemtdag::class)
        assertWinnerBidirectional(arbeidsdag, utenlandsFraSøknad, Ubestemtdag::class)
        assertWinnerBidirectional(sykedagFraSøknad, utenlandsFraSøknad, Ubestemtdag::class)
        assertWinnerBidirectional(egenmeldingFraInntektsmelding, utenlandsFraSøknad, Ubestemtdag::class)
        assertWinnerBidirectional(ferieFraSøknad, utenlandsFraSøknad, Ubestemtdag::class)
        assertWinnerBidirectional(ferieFraInntektsmelding, utenlandsFraSøknad, Ubestemtdag::class)
    }

    @Test
    fun `arbeidsdag vinner over sykedag`() {
        assertWinner(sykedagFraSøknad, arbeidsdag, Arbeidsdag.Søknad::class)
    }

    @Test
    fun `sykedag fra søknad vinner over egenmeldingsdag i inntektsmelding`() {
        assertWinner(sykedagFraSøknad, egenmeldingFraInntektsmelding, Sykedag.Søknad::class)
        assertWinner(egenmeldingFraInntektsmelding, sykedagFraSøknad, Sykedag.Søknad::class)
    }

    private fun <T : Dag> assertWinner(
        dag1: Dag,
        dag2: Dag,
        expectedWinnerClass: KClass<T>
    ) {
        val winner = historiskDagturnering.beste(dag1, dag2)
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
