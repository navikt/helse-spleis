package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.Sykepengesøknad
import no.nav.helse.sykdomstidslinje.dag.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.lang.RuntimeException
import java.time.LocalDate
import java.time.LocalDateTime

class DagsturneringTest {
    companion object {
        val baseDag = LocalDate.of(2019, 4, 20)
        val innsendt = LocalDateTime.of(2019, 4, 21, 12, 0)

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val inntektsmelding = Inntektsmelding(objectMapper.readTree("/inntektsmelding.json".readResource()))
        val sendtSøknad = Sykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))
        val nySøknad = Sykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_ny.json".readResource()))
    }

    @Disabled("Fungerer ikke enda")
    @Test
    fun `Sjekker utfallsmatrisen fra CSV fil`() {
        val reader = DagsturneringTest::class.java.getResourceAsStream("/pattern_matching_dager.csv").bufferedReader(Charsets.UTF_8)
        val initialLine = reader.readLine().split(",")
        val cellTypes = initialLine.subList(1, initialLine.size)
        val outcomes = reader
            .readLines()
            .mapIndexed { row, rowText ->
                rowText.split(",").subList(1, row+2)
                    .mapIndexed { column, cell -> Combination(cellTypes[row], cellTypes[column], cell) }
            }
            .flatten()
        outcomes.forEach {
            it.nameToEventType(it.eventTypeAName)
            it.nameToEventType(it.eventTypeBName)
        }

        outcomes.forEach { combo ->
            assertCorrect(combo)
        }

        println(outcomes.size)
    }

    private fun assertCorrect(combo: Combination) {
        val vinner = (combo.left()!! + combo.right()!!).flatten().first()

        assertEquals(combo.resultFor(), vinner::class, "${combo.left()} + ${combo.right()} ble $vinner og ikke ${combo.resultFor()} som forventet")
    }

    data class Combination(val eventTypeAName: String, val eventTypeBName: String, val eventTypeOutcomeName: String) {
        fun nameToEventType(name: String): Dag? = when (name) {
            "WD-I" -> Sykdomstidslinje.ikkeSykedag(baseDag, sendtSøknad)
            "WD-A" -> Sykdomstidslinje.ikkeSykedag(baseDag, sendtSøknad)
            "WD-IM" -> Sykdomstidslinje.ikkeSykedag(baseDag, inntektsmelding)
            "S" -> Sykdomstidslinje.sykedager(baseDag, nySøknad)
            "GS" -> Sykdomstidslinje.sykedager(baseDag, nySøknad)
            "GS-A" -> Sykdomstidslinje.sykedager(baseDag, sendtSøknad)
            "V-A" -> Sykdomstidslinje.ferie(baseDag, sendtSøknad)
            "V-IM" -> Sykdomstidslinje.ferie(baseDag, inntektsmelding)
            "W" -> null // TODO: Weekend
            "Le-Areg" -> null // TODO: Implementer når vi har permisjon
            "Le-A" -> null // TODO: Implementer når vi har permisjon
            "SW" -> null // TODO: Sick workday
            "SRD-IM" -> Sykdomstidslinje.egenmeldingsdager(baseDag, inntektsmelding)
            "SRD-A" -> Sykdomstidslinje.egenmeldingsdager(baseDag, sendtSøknad)
            "EDU" -> null // TODO: Implementer når vi har utdannelsesdager
            "OI-Int" -> null // TODO: Implementer når vi har andre inntektskilder
            "OI-A" -> null // TODO: Implementer når vi har andre inntektskilder
            "DA" -> Sykdomstidslinje.utenlandsdag(baseDag, sendtSøknad)
            "Null" -> Nulldag(baseDag, sendtSøknad)
            "Undecided" -> Ubestemtdag(
                Sykdomstidslinje.sykedager(baseDag, sendtSøknad), Sykdomstidslinje.ikkeSykedag(
                    baseDag, inntektsmelding
                )
            )
            else -> throw RuntimeException("Unmapped event type $name")
        }

        fun left() = nameToEventType(eventTypeAName)
        fun right() = nameToEventType(eventTypeAName)

        private fun latest() = if (left()!!.hendelse > right()!!.hendelse) { left() } else { right() }!!

        fun resultFor() = when(eventTypeOutcomeName) {
            "WD" -> Arbeidsdag::class
            "L" -> latest()::class
            "V" -> Feriedag::class
            "X" -> throw RuntimeException("This should never happen")
            "U" -> Ubestemtdag::class
            "SW" -> SykHelgedag::class
            "DOI" -> TODO("Dager med andre inntektskilder, implementer meg!")
            "EDU" -> TODO("Utdannelsesdager, implementer meg!")
            "DA" -> Utenlandsdag::class
            "GS" -> TODO("Gradert sykmelding, implementer meg!")
            "SRD" -> Egenmeldingsdag::class
            else -> throw RuntimeException("Unmapped event type $eventTypeOutcomeName")
        }
    }
}