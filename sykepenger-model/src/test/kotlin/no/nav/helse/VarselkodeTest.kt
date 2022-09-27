package no.nav.helse

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import no.nav.helse.person.AktivitetsloggObserverTest
import no.nav.helse.person.AktivitetsloggTest
import no.nav.helse.person.Varselkode
import no.nav.helse.person.Varselkode.RV_AY_1
import no.nav.helse.person.Varselkode.RV_AY_2
import no.nav.helse.person.Varselkode.RV_OS_1
import no.nav.helse.person.Varselkode.RV_OS_2
import no.nav.helse.person.Varselkode.RV_OS_3
import no.nav.helse.person.Varselkode.RV_RV_1
import no.nav.helse.person.Varselkode.RV_UT_1
import no.nav.helse.person.Varselkode.RV_UT_2
import no.nav.helse.person.Varselkode.RV_VV_3
import no.nav.helse.person.Varselkode.RV_VV_5
import no.nav.helse.person.varselkodeformat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.extension
import kotlin.io.path.pathString

internal class VarselkodeTest {

    @Test
    fun `alle varselkoder testes eksplisitt`() {
        val aktiveVarselkoder = Varselkode.aktiveVarselkoder.toSet()
        val ikkeTestedeVarselkoder = aktiveVarselkoder.toMutableSet().apply {
            removeAll(finnAlleVarselkoderITest().toSet())
        }.toSet()

        val varselkoderSomKjentManglerTest = listOf(
            RV_VV_3, RV_VV_5, RV_AY_1, RV_AY_2,
            RV_UT_1, RV_UT_2, RV_OS_1,
            RV_OS_2, RV_OS_3
        )

        val varselkoderSomNåManglerTest = ikkeTestedeVarselkoder.minus(varselkoderSomKjentManglerTest.toSet())
        val (varselkoderSomFortsattBrukes, varselkoderSomIkkeBrukesLenger) = varselkoderSomKjentManglerTest.partition { it in aktiveVarselkoder }
        val varselkoderSomNåTestesEksplisitt = varselkoderSomFortsattBrukes.minus(ikkeTestedeVarselkoder)

        assertForventetFeil(
            forklaring = "Ikke alle varselkoder testes eksplisitt",
            ønsket = { assertEquals(emptySet<Varselkode>(), aktiveVarselkoder) },
            nå = {
                assertEquals(emptySet<Varselkode>(), varselkoderSomNåManglerTest) {
                    "Du har tatt i bruk en ny varselkode! Legg til eksplisitt test for nye varselkoder. _Ikke_ legg den i listen av varselkoder som mangler eksplisitt test."
                }
            }
        )

            assertEquals(emptySet<Varselkode>(), varselkoderSomIkkeBrukesLenger.toSet()) {
            "Disse varselkodene er ikke lenger i bruk. Du kan fjerne de fra listen av varsler som kjent mangler test."
        }

        assertEquals(emptySet<Varselkode>(), varselkoderSomNåTestesEksplisitt.toSet()) {
            "Disse varselkodene finnes det nå test for. Du kan fjerne de fra listen av varsler som kjent mangler test."
        }
    }

    private companion object {

        private fun Path.slutterPåEnAv(vararg suffix: String) = let { path -> suffix.firstOrNull { path.endsWith(it) } != null }

        private fun finn(
            scope: String,
            regex: Regex,
            ignorePath: (path: Path) -> Boolean = { false },
            ignoreLinje: (linje: String) -> Boolean = { false }) =
            Files.walk(Paths.get("../")).use { paths ->
                paths
                    .filter(Files::isRegularFile)
                    .filter { it.pathString.contains("/src/$scope/") }
                    .filter { it.fileName.extension == "kt" }
                    .filter { !ignorePath(it) }
                    .map { Files.readAllLines(it) }
                    .toList()
                    .asSequence()
                    .flatten()
                    .filterNot(ignoreLinje)
                    .map { linje -> regex.findAll(linje).toList().map { it.groupValues[1] } }
                    .flatten()
                    .toSet()
            }

        private fun finnAlleVarselkoderITest() = finn("test", "($varselkodeformat)".toRegex(), ignorePath = { path ->
            path.slutterPåEnAv("${VarselkodeTest::class.simpleName}.kt", "${this::class.simpleName}.kt", "${AktivitetsloggTest::class.simpleName}.kt", "${AktivitetsloggObserverTest::class.simpleName}.kt")
        }).map { enumValueOf<Varselkode>(it) }.distinct()
    }
}