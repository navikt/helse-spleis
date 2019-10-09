package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.NySykepengesøknad
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.hendelse.Sykepengesøknad
import no.nav.helse.sykdomstidslinje.dag.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
 import java.time.LocalDate

class DagsturneringTest {
    companion object {
        private val mandag = LocalDate.of(2019, 7, 1)
        private val lørdag = LocalDate.of(2019, 7, 6)

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val inntektsmelding = Inntektsmelding(objectMapper.readTree("/inntektsmelding.json".readResource()))
        private val sendtSøknad = SendtSykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))
        private val nySøknad = NySykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_ny.json".readResource()))
    }

    @Disabled("Ikke ferdig implementert gjennom")
    @Test
    fun `Sjekker utfallsmatrisen fra CSV fil`() {
        val reader = DagsturneringTest::class.java.getResourceAsStream("/pattern_matching_dager_epic_1.csv")
            .bufferedReader(Charsets.UTF_8)
        val initialLine = reader.readLine().split(",")
        val cellTypes = initialLine.subList(1, initialLine.size)
        val outcomes = reader
            .readLines()
            .mapIndexed { row, rowText ->
                rowText.split(",").subList(1, row+2)
                    .mapIndexed { column, cell -> Combination(cellTypes[row], cellTypes[column], cell) }
            }
            .flatten()
        outcomes.forEach { combo ->
            print("\n${combo.eventTypeAName} + ${combo.eventTypeBName} = ${combo.eventTypeOutcomeName}")
            assertCorrect(combo)
            print(" passed")
        }

        println(outcomes.size)
    }

    private fun assertCorrect(combo: Combination) {
        val vinner = (combo.left()!! + combo.right()!!).flatten().first()

        assertEquals(
            combo.resultFor(),
            vinner::class,
            "${combo.left()} + ${combo.right()} ble $vinner og ikke ${combo.resultFor().simpleName} som forventet"
        )
    }

    data class Combination(val eventTypeAName: String, val eventTypeBName: String, val eventTypeOutcomeName: String) {

        fun basedag(typeA: String, typeB: String): LocalDate? = when {
            erHelg(typeA) && erHelg(typeB) -> lørdag
            erHelg(typeA) || erHelg(typeB) -> null
            else -> mandag
        }

        fun erHelg(type: String): Boolean = when (type) {
            "W" -> true
            "SW" -> true
            else -> false
        }

        fun nameToEventType(name: String, dato: LocalDate): Dag? = when (name) {
            "WD-I" -> Fylldag(dato, sendtSøknad)
            "WD-A" -> Sykdomstidslinje.ikkeSykedag(dato, sendtSøknad)
            "WD-IM" -> Sykdomstidslinje.ikkeSykedag(dato, inntektsmelding)
            "S" -> Sykdomstidslinje.sykedager(dato, nySøknad)
            "GS" -> Sykdomstidslinje.sykedager(dato, nySøknad)
            "GS-A" -> Sykdomstidslinje.sykedager(dato, sendtSøknad)
            "V-A" -> Sykdomstidslinje.ferie(dato, sendtSøknad)
            "V-IM" -> Sykdomstidslinje.ferie(dato, inntektsmelding)
            "W" -> null // TODO: Weekend
            "Le-Areg" -> null // TODO: Implementer når vi har permisjon
            "Le-A" -> null // TODO: Implementer når vi har permisjon
            "SW" -> null // TODO: Sick workday
            "SRD-IM" -> Sykdomstidslinje.egenmeldingsdager(dato, inntektsmelding)
            "SRD-A" -> Sykdomstidslinje.egenmeldingsdager(dato, sendtSøknad)
            "EDU" -> null // TODO: Implementer når vi har utdannelsesdager
            "OI-Int" -> null // TODO: Implementer når vi har andre inntektskilder
            "OI-A" -> null // TODO: Implementer når vi har andre inntektskilder
            "DA" -> Sykdomstidslinje.utenlandsdag(dato, sendtSøknad)
            "Null" -> Nulldag(dato, sendtSøknad)
            "Undecided" -> Ubestemtdag(
                Sykdomstidslinje.sykedager(dato, sendtSøknad), Sykdomstidslinje.ikkeSykedag(
                    dato, inntektsmelding
                )
            )
            else -> throw RuntimeException("Unmapped event type $name")
        }

        fun left() = nameToEventType(eventTypeAName, basedag(eventTypeAName, eventTypeBName)!!)
        fun right() = nameToEventType(eventTypeBName, basedag(eventTypeAName, eventTypeBName)!!)

        private fun latest() = if (left()!!.hendelse > right()!!.hendelse) {
            left()
        } else {
            right()
        }!!

        fun resultFor() = when (eventTypeOutcomeName) {
            "WD-I" -> Fylldag::class
            "WD" -> Arbeidsdag::class
            "S" -> Sykedag::class
            "L" -> latest()::class
            "V" -> Feriedag::class
            "X" -> throw RuntimeException("This should never happen")
            "U" -> Ubestemtdag::class
            "SW" -> SykHelgedag::class
            "DOI" -> TODO("Dager med andre inntektskilder, implementer meg!")
            "EDU" -> TODO("Utdannelsesdager, implementer meg!")
            "DA" -> Utenlandsdag::class
            "GS" -> Sykedag::class
            "SRD" -> Egenmeldingsdag::class
            else -> throw RuntimeException("Unmapped event type $eventTypeOutcomeName")
        }
    }
}
