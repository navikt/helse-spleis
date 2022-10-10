package no.nav.helse

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import no.nav.helse.person.Varselkode
import no.nav.helse.person.Varselkode.RV_AG_1
import no.nav.helse.person.Varselkode.RV_AY_10
import no.nav.helse.person.Varselkode.RV_IM_6
import no.nav.helse.person.Varselkode.RV_IM_7
import no.nav.helse.person.Varselkode.RV_IM_8
import no.nav.helse.person.Varselkode.RV_IT_10
import no.nav.helse.person.Varselkode.RV_IT_16
import no.nav.helse.person.Varselkode.RV_IT_17
import no.nav.helse.person.Varselkode.RV_IT_6
import no.nav.helse.person.Varselkode.RV_IT_7
import no.nav.helse.person.Varselkode.RV_IT_8
import no.nav.helse.person.Varselkode.RV_IT_9
import no.nav.helse.person.Varselkode.RV_SI_3
import no.nav.helse.person.Varselkode.RV_ST_1
import no.nav.helse.person.Varselkode.RV_SV_3
import no.nav.helse.person.Varselkode.RV_SØ_11
import no.nav.helse.person.Varselkode.RV_SØ_12
import no.nav.helse.person.Varselkode.RV_SØ_13
import no.nav.helse.person.Varselkode.RV_SØ_14
import no.nav.helse.person.Varselkode.RV_SØ_15
import no.nav.helse.person.Varselkode.RV_SØ_16
import no.nav.helse.person.Varselkode.RV_SØ_17
import no.nav.helse.person.Varselkode.RV_SØ_18
import no.nav.helse.person.Varselkode.RV_SØ_19
import no.nav.helse.person.Varselkode.RV_SØ_20
import no.nav.helse.person.Varselkode.RV_SØ_21
import no.nav.helse.person.Varselkode.RV_SØ_22
import no.nav.helse.person.Varselkode.RV_UT_10
import no.nav.helse.person.Varselkode.RV_UT_11
import no.nav.helse.person.Varselkode.RV_UT_12
import no.nav.helse.person.Varselkode.RV_UT_13
import no.nav.helse.person.Varselkode.RV_UT_14
import no.nav.helse.person.Varselkode.RV_UT_15
import no.nav.helse.person.Varselkode.RV_UT_16
import no.nav.helse.person.Varselkode.RV_UT_17
import no.nav.helse.person.Varselkode.RV_UT_18
import no.nav.helse.person.Varselkode.RV_UT_19
import no.nav.helse.person.Varselkode.RV_UT_3
import no.nav.helse.person.Varselkode.RV_UT_4
import no.nav.helse.person.Varselkode.RV_UT_5
import no.nav.helse.person.Varselkode.RV_UT_6
import no.nav.helse.person.Varselkode.RV_UT_7
import no.nav.helse.person.Varselkode.RV_UT_8
import no.nav.helse.person.Varselkode.RV_UT_9
import no.nav.helse.person.Varselkode.RV_VT_1
import no.nav.helse.person.Varselkode.RV_VT_2
import no.nav.helse.person.Varselkode.RV_VT_3
import no.nav.helse.person.Varselkode.RV_VT_4
import no.nav.helse.person.Varselkode.RV_VT_5
import no.nav.helse.person.Varselkode.RV_VT_6
import no.nav.helse.person.Varselkode.RV_VT_7
import no.nav.helse.person.Varselkode.RV_VV_10
import no.nav.helse.person.Varselkode.RV_VV_11
import no.nav.helse.person.Varselkode.RV_VV_12
import no.nav.helse.person.Varselkode.RV_VV_5
import no.nav.helse.person.Varselkode.RV_VV_9
import no.nav.helse.person.varselkodeformat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.name
import kotlin.io.path.pathString

internal class VarselkodeTest {

    @Test
    fun `alle varselkoder testes eksplisitt`() {
        val aktiveVarselkoder = Varselkode.aktiveVarselkoder.toSet()
        val ikkeTestedeVarselkoder = aktiveVarselkoder.toMutableSet().apply {
            removeAll(finnAlleVarselkoderITest().toSet())
        }.toSet()

        val varselkoderSomKjentManglerTest = listOf(
            RV_VV_5, RV_VV_9, RV_VV_12, RV_IM_6, RV_IM_7, RV_IM_8, RV_UT_3, RV_UT_4,
            RV_ST_1, RV_IT_6, RV_IT_7, RV_IT_8, RV_IT_9, RV_IT_10,
            RV_IT_16, RV_IT_17, RV_SØ_11, RV_SØ_12, RV_SØ_13, RV_SØ_14,
            RV_SØ_15, RV_SØ_16, RV_UT_5, RV_SØ_17, RV_SØ_18, RV_SØ_19, RV_SØ_21, RV_SØ_22,
            RV_VT_1, RV_VT_2, RV_VT_3, RV_VT_4, RV_VT_5, RV_VT_6, RV_VT_7,
            RV_SØ_20, RV_AY_10, RV_VV_10, RV_VV_11, RV_UT_6, RV_UT_7,
            RV_UT_8, RV_UT_9, RV_UT_10, RV_UT_11, RV_UT_12, RV_UT_13,
            RV_UT_14, RV_UT_15, RV_UT_16, RV_UT_17, RV_UT_18, RV_UT_19,
            RV_AG_1, RV_SV_3, RV_SI_3
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

        private fun finn(regex: Regex) =
            Files.walk(Paths.get("../")).use { paths ->
                paths
                    .filter(Files::isRegularFile)
                    .filter { it.pathString.contains("/src/test/") }
                    .filter { it.fileName.name == "VarselE2ETest.kt" }
                    .map { Files.readAllLines(it) }
                    .toList()
                    .asSequence()
                    .flatten()
                    .map { linje -> regex.findAll(linje).toList().map { it.groupValues[1] } }
                    .flatten()
                    .toSet()
            }

        private fun finnAlleVarselkoderITest() = finn("($varselkodeformat)".toRegex()).map { enumValueOf<Varselkode>(it) }.distinct()
    }
}