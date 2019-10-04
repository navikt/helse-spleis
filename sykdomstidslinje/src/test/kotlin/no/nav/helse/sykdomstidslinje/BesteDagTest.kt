package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.Sykepengesøknad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.reflect.KClass

internal class BesteDagTest {

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val inntektsmelding = Inntektsmelding(objectMapper.readTree("/inntektsmelding.json".readResource()))
        val sendtSøknad = Sykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))

        val nulldag get() = Nulldag(2.mandag, Testhendelse())
        val arbeidsdag get() = Arbeidsdag(2.mandag, Testhendelse())
        val ferieFraInntektsmelding get() = Sykdomstidslinje.ferie(2.mandag, inntektsmelding)
        val sykdomFraInntektsmelding get() = Sykdomstidslinje.sykedager(2.mandag, inntektsmelding)
        val ferieFraSøknad get() = Sykdomstidslinje.ferie(2.mandag, sendtSøknad)
        val sykdomFraSendtSøknad get() = Sykdomstidslinje.sykedager(2.mandag, sendtSøknad)
        val utenlandsFraSendtSøknad get() = Sykdomstidslinje.utenlandsdag(2.mandag, sendtSøknad)
    }

    @Test
    fun `inntektsmelding sier ferie, søknad sier syk blir udefinert`() {
        assertWinner(sykdomFraSendtSøknad, ferieFraInntektsmelding, Ubestemtdag::class, 2)
        assertWinner(ferieFraInntektsmelding, sykdomFraSendtSøknad, Ubestemtdag::class, 2)
    }

    @Test
    fun `nulldag taper mot en gitt dag`() {
        assertWinner(nulldag, sykdomFraSendtSøknad, Sykedag::class, 0)
        assertWinner(sykdomFraSendtSøknad, nulldag, Sykedag::class, 0)
    }

    @Test
    fun `ferie vinner over sykdom`() {
        assertWinner(ferieFraSøknad, sykdomFraSendtSøknad, Feriedag::class, 1)
    }

    @Test
    fun `søknad med ferie vinner over en gitt dag`() {
        assertWinner(ferieFraSøknad, sykdomFraInntektsmelding, Feriedag::class, 1)
        assertWinner(sykdomFraInntektsmelding, ferieFraSøknad, Feriedag::class, 1)
    }

    @Test
    fun `ferie vinner over utenlandsdag`() {
        assertWinner(ferieFraSøknad, utenlandsFraSendtSøknad, Feriedag::class, 1)
        assertWinner(utenlandsFraSendtSøknad, ferieFraSøknad, Feriedag::class, 1)
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
}

private val Int.mandag get() = LocalDate.of(2019, 7, (this - 1) * 7 + 1)
private val Int.tirsdag get() = this.mandag.plusDays(1)
private val Int.onsdag get() = this.mandag.plusDays(2)
private val Int.torsdag get() = this.mandag.plusDays(3)
private val Int.fredag get() = this.mandag.plusDays(4)
private val Int.lørdag get() = this.mandag.plusDays(5)
private val Int.søndag get() = this.mandag.plusDays(6)

fun String.readResource() =
    object {}.javaClass.getResource(this)?.readText(Charsets.UTF_8)
        ?: throw RuntimeException("did not find resource <$this>")
