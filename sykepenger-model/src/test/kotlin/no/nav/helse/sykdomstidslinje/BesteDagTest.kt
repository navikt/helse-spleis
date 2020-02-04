package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class BesteDagTest {

    companion object {

        private val inntektsmeldingHendelse = Testhendelse(hendelsetype = Dag.NøkkelHendelseType.Inntektsmelding)
        private val sendtSøknadHendelse = Testhendelse(hendelsetype = Dag.NøkkelHendelseType.Søknad)

        private val implisittDag get() = ImplisittDag(Uke(2).mandag, inntektsmeldingHendelse)
        private val arbeidsdag get() = Arbeidsdag(Uke(2).mandag, sendtSøknadHendelse)
        private val ferieFraInntektsmelding get() = ConcreteSykdomstidslinje.ferie(Uke(2).mandag, inntektsmeldingHendelse)
        private val egenmeldingFraInntektsmelding get() = ConcreteSykdomstidslinje.egenmeldingsdag(Uke(2).mandag, inntektsmeldingHendelse)
        private val ferieFraSøknad get() = ConcreteSykdomstidslinje.ferie(Uke(2).mandag, sendtSøknadHendelse)
        private val sykdomFraSendtSøknad get() = ConcreteSykdomstidslinje.sykedag(Uke(2).mandag, sendtSøknadHendelse)
        private val utenlandsFraSendtSøknad get() = ConcreteSykdomstidslinje.utenlandsdag(Uke(2).mandag, sendtSøknadHendelse)
    }

    @Test
    fun `inntektsmelding sier ferie, søknad sier syk blir feriedag`() {
        assertWinner(sykdomFraSendtSøknad, ferieFraInntektsmelding, Feriedag::class)
        assertWinner(ferieFraInntektsmelding, sykdomFraSendtSøknad, Feriedag::class)
    }

    @Test
    fun `nulldag taper mot en gitt dag`() {
        assertWinner(implisittDag, sykdomFraSendtSøknad, Sykedag::class)
        assertWinner(sykdomFraSendtSøknad, implisittDag, Sykedag::class)
    }

    @Test
    fun `ferie vinner over sykdom`() {
        assertWinner(ferieFraSøknad, sykdomFraSendtSøknad, Feriedag::class)
    }

    @Test
    fun `søknad med egenmelding vinner over en gitt dag`() {
        assertWinner(ferieFraSøknad, egenmeldingFraInntektsmelding, Egenmeldingsdag::class)
        assertWinner(egenmeldingFraInntektsmelding, ferieFraSøknad, Egenmeldingsdag::class)
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
        assertWinner(arbeidsdag, sykdomFraSendtSøknad, Arbeidsdag::class)
        assertWinner(sykdomFraSendtSøknad, arbeidsdag, Arbeidsdag::class)
    }

    @Test
    fun `sykedag fra søknad vinner over egenmeldingsdag i inntektsmelding`() {
        assertWinner(sykdomFraSendtSøknad, egenmeldingFraInntektsmelding, Sykedag::class)
        assertWinner(egenmeldingFraInntektsmelding, sykdomFraSendtSøknad, Sykedag::class)
    }

    private fun <T : Dag> assertWinner(
        dag1: Dag,
        dag2: Dag,
        expectedWinnerClass: KClass<T>
    ) {
        val winner = dag1.beste(dag2)
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

fun String.readResource() =
    object {}.javaClass.getResource(this)?.readText(Charsets.UTF_8)
        ?: throw RuntimeException("did not find resource <$this>")
