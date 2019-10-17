package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.testhelpers.mandag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class BesteDagTest {

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val inntektsmelding = Inntektsmelding(objectMapper.readTree("/inntektsmelding.json".readResource()))
        private val sendtSøknad =
            SendtSykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))

        private val implisittDag get() = ImplisittDag(2.mandag, inntektsmelding)
        private val arbeidsdag get() = Arbeidsdag(2.mandag, sendtSøknad)
        private val ferieFraInntektsmelding get() = Sykdomstidslinje.ferie(2.mandag, inntektsmelding)
        private val egenmeldingFraInntektsmelding get() = Sykdomstidslinje.egenmeldingsdag(2.mandag, inntektsmelding)
        private val ferieFraSøknad get() = Sykdomstidslinje.ferie(2.mandag, sendtSøknad)
        private val sykdomFraSendtSøknad get() = Sykdomstidslinje.sykedag(2.mandag, sendtSøknad)
        private val utenlandsFraSendtSøknad get() = Sykdomstidslinje.utenlandsdag(2.mandag, sendtSøknad)
    }

    @Test
    fun `inntektsmelding sier ferie, søknad sier syk blir feriedag`() {
        assertWinner(sykdomFraSendtSøknad, ferieFraInntektsmelding, Feriedag::class, 1)
        assertWinner(ferieFraInntektsmelding, sykdomFraSendtSøknad, Feriedag::class, 1)
    }

    @Test
    fun `nulldag taper mot en gitt dag`() {
        assertWinner(implisittDag, sykdomFraSendtSøknad, Sykedag::class, 0)
        assertWinner(sykdomFraSendtSøknad, implisittDag, Sykedag::class, 0)
    }

    @Test
    fun `ferie vinner over sykdom`() {
        assertWinner(ferieFraSøknad, sykdomFraSendtSøknad, Feriedag::class, 1)
    }

    @Test
    fun `søknad med egenmelding vinner over en gitt dag`() {
        assertWinner(ferieFraSøknad, egenmeldingFraInntektsmelding, Egenmeldingsdag::class, 1)
        assertWinner(egenmeldingFraInntektsmelding, ferieFraSøknad, Egenmeldingsdag::class, 1)
    }

    @Test
    fun `sammenligning med utenlandsdag git altid ubestemtdag`() {
        assertWinnerBidirectional(implisittDag, utenlandsFraSendtSøknad, Ubestemtdag::class, 1)
        assertWinnerBidirectional(arbeidsdag, utenlandsFraSendtSøknad, Ubestemtdag::class, 2)
        assertWinnerBidirectional(sykdomFraSendtSøknad, utenlandsFraSendtSøknad, Ubestemtdag::class, 2)
        assertWinnerBidirectional(egenmeldingFraInntektsmelding, utenlandsFraSendtSøknad, Ubestemtdag::class, 2)
        assertWinnerBidirectional(ferieFraSøknad, utenlandsFraSendtSøknad, Ubestemtdag::class, 2)
        assertWinnerBidirectional(ferieFraInntektsmelding, utenlandsFraSendtSøknad, Ubestemtdag::class, 2)
    }

    @Test
    fun `arbeidsdag vinner over sykedag`() {
        assertWinner(arbeidsdag, sykdomFraSendtSøknad, Arbeidsdag::class, 1)
        assertWinner(sykdomFraSendtSøknad, arbeidsdag, Arbeidsdag::class, 1)
    }

    private fun <T : Dag> assertWinner(
        dag1: Dag,
        dag2: Dag,
        expectedWinnerClass: KClass<T>,
        antallDagerErstattet: Int
    ) {
        val winner = dag1.beste(dag2)
        assertEquals(expectedWinnerClass, winner::class)
        assertEquals(antallDagerErstattet, winner.dagerErstattet().size)
    }

    private fun <T : Dag> assertWinnerBidirectional(
        dag1: Dag,
        dag2: Dag,
        expectedWinnerClass: KClass<T>,
        antallDagerErstattet: Int
    ) {
        assertWinner(dag1, dag2, expectedWinnerClass, antallDagerErstattet)
        assertWinner(dag2, dag1, expectedWinnerClass, antallDagerErstattet)
    }
}

fun String.readResource() =
    object {}.javaClass.getResource(this)?.readText(Charsets.UTF_8)
        ?: throw RuntimeException("did not find resource <$this>")
