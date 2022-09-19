package no.nav.helse

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import no.nav.helse.person.AktivitetsloggTest
import no.nav.helse.person.Varselkode
import no.nav.helse.person.Varselkode.*
import no.nav.helse.person.varselkodeformat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.extension
import kotlin.io.path.pathString

internal class VarselkodeTest {
    private val alleVarselkoder = values()

    @Test
    fun `alle varselkoder testes eksplisitt`() {
        val ikkeTestedeVarselkoder = alleVarselkoder.toMutableSet()

        finnAlleVarselKoderITest().forEach { kode ->
            ikkeTestedeVarselkoder.removeIf { (it == kode) }
        }

        val varselkoderSomManglerEksplisittTest = alleVarselkoder.toSet().filterNot { it in setOf(RV_SØ_2) }.toSet() // Overgangsperiode til vi har overført alle varsler til Varselkode

        val nyeVarselkoderSomManglerEksplisittTest = ikkeTestedeVarselkoder.minus(varselkoderSomManglerEksplisittTest)
        val varselkoderSomNåTestesEkplisitt = varselkoderSomManglerEksplisittTest.minus(ikkeTestedeVarselkoder)

        assertForventetFeil(
            forklaring = "Ikke alle varselkoder testes eksplisitt",
            ønsket = { assertEquals(emptySet<Varselkode>(), ikkeTestedeVarselkoder) },
            nå = { assertEquals(emptySet<Varselkode>(), nyeVarselkoderSomManglerEksplisittTest) {
                "Legg til eksplisitt test for nye varselkoder! _ikke_ legg den i listen av varselkoder som mangler eksplisitt test."
            }}
        )

        assertEquals(emptySet<Varselkode>(), varselkoderSomNåTestesEkplisitt) {
            "Finnes nå eksplisitt tester for disse varselkoder. Fjern dem fra listen over varselkoder som mangler eksplisitt test."
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

        private fun finnAlleVarselKoderITest() = finn("test", "($varselkodeformat)".toRegex(), ignorePath = { path ->
            path.slutterPåEnAv("${VarselkodeTest::class.simpleName}.kt", "${this::class.simpleName}.kt", "${AktivitetsloggTest::class.simpleName}.kt")
        }).map { enumValueOf<Varselkode>(it) }.distinct()
    }
}