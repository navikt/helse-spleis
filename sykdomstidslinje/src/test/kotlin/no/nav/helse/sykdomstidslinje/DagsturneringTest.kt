package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.NySykepengesøknad
import no.nav.helse.hendelse.SendtSykepengesøknad
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
        private val sendtSøknad =
            SendtSykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_sendt_nav.json".readResource()))
        private val nySøknad = NySykepengesøknad(objectMapper.readTree("/søknad_arbeidstaker_ny.json".readResource()))
    }

    @Disabled("Ikke ferdig implementert")
    @Test
    fun `Sjekker utfallsmatrisen fra CSV fil`() {
        val outcomes = readTestcases()
        val resultat = outcomes
            .map(TestCase::toResult)
            .onEach { println(it.message) }

        assertEquals(0, resultat.filterNot { it.passed }.size)
        assertEquals(outcomes.size, resultat.size)
    }

    private fun readTestcases(): List<TestCase> {

        val reader = DagsturneringTest::class.java.getResourceAsStream("/pattern_matching_dager_epic_1.csv")
            .bufferedReader(Charsets.UTF_8)
        val initialLine = reader.readLine().split(",")
        val cellTypes = initialLine.subList(1, initialLine.size)

        return reader
            .readLines()
            .mapIndexed { row, rowText ->
                rowText.split(",").subList(1, row + 2)
                    .mapIndexed { column, cell -> TestCase(cellTypes[row], cellTypes[column], cell) }
            }
            .flatten()
            .filter { it.resultEventName != "X" }
            .filter { it.columnEventName != "Le-Areg" && it.rowEventName != "Le-Areg"}
            .filter { it.columnEventName != "OI-Int" && it.rowEventName != "OI-Int"}
            .filter { it.columnEventName != "OI-A" && it.rowEventName != "OI-A"}
    }

    data class Resultat(val passed: Boolean, val message: String)

    data class TestCase(val columnEventName: String, val rowEventName: String, val resultEventName: String) {
        fun toResult(): Resultat {
            val vinner = (this.left() + this.right()).flatten().first()

            return if (vinner::class == resultFor()) {
                Resultat(true, "${this.left()} + ${this.right()} = $resultEventName")
            } else {
                Resultat(
                    false,
                    "${this.left()} + ${this.right()} ble $vinner og ikke ${this.resultFor().simpleName} som forventet"
                )
            }
        }

        fun basedag(typeA: String, typeB: String): LocalDate? = when {
            erHelg(typeA) || erHelg(typeB) -> lørdag
            else -> mandag
        }

        fun erHelg(type: String): Boolean = when (type) {
            "W" -> true
            "SW" -> true
            else -> false
        }

        fun nameToEventType(name: String, dato: LocalDate): Dag = when (name) {
            "WD-A" -> Sykdomstidslinje.ikkeSykedag(dato, sendtSøknad)
            "WD-IM" -> Sykdomstidslinje.ikkeSykedag(dato, inntektsmelding)
            "S" -> Sykdomstidslinje.sykedag(dato, nySøknad)
            "GS" -> Sykdomstidslinje.sykedag(dato, nySøknad)
            "GS-A" -> Sykdomstidslinje.sykedag(dato, sendtSøknad)
            "V-A" -> Sykdomstidslinje.ferie(dato, sendtSøknad)
            "V-IM" -> Sykdomstidslinje.ferie(dato, inntektsmelding)
//            "Le-Areg" -> Permisjonsdag(dato, ) // TODO: Implementer når vi har permisjon
            "Le-A" -> Permisjonsdag(dato, sendtSøknad) // TODO: Implementer når vi har permisjon
            "SW" -> SykHelgedag(lørdag, nySøknad)
            "SRD-IM" -> Sykdomstidslinje.egenmeldingsdag(dato, inntektsmelding)
            "SRD-A" -> Sykdomstidslinje.egenmeldingsdag(dato, sendtSøknad)
//            "OI-Int" -> null // TODO: Implementer når vi har andre inntektskilder
//            "OI-A" -> Ubestemtdag(dato, sendtSøknad)
            "DA" -> Sykdomstidslinje.utenlandsdag(dato, sendtSøknad)
            "Null" -> ImplisittDag(dato, sendtSøknad)
            "Le" -> Permisjonsdag(dato, sendtSøknad)
            "SW-SM" -> SykHelgedag(dato, nySøknad)
            "Undecided" -> Ubestemtdag(
                Sykdomstidslinje.sykedag(dato, sendtSøknad), Sykdomstidslinje.ikkeSykedag(
                    dato, inntektsmelding
                )
            )
            else -> throw RuntimeException("Unmapped event type $name")
        }

        fun left() = nameToEventType(columnEventName, basedag(columnEventName, rowEventName)!!)
        fun right() = nameToEventType(rowEventName, basedag(columnEventName, rowEventName)!!)

        private fun latest() = if (left().hendelse > right().hendelse) {
            left()
        } else {
            right()
        }

        fun resultFor() = when (resultEventName.toUpperCase()) {
            "WD" -> Arbeidsdag::class
            "S" -> Sykedag::class
            "L" -> latest()::class
            "LE" -> Permisjonsdag::class
            "V" -> Feriedag::class
            "U" -> Ubestemtdag::class
            "SW" -> SykHelgedag::class
            "DOI" -> TODO("Dager med andre inntektskilder, implementer meg!")
            "EDU" -> TODO("Utdannelsesdager, implementer meg!")
            "DA" -> Utenlandsdag::class
            "GS" -> Sykedag::class
            "SRD" -> Egenmeldingsdag::class
            "SW-SM" -> SykHelgedag::class
            else -> throw RuntimeException("Unmapped event type $resultEventName")
        }
    }


}

