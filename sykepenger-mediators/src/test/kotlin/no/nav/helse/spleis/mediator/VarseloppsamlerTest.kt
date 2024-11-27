package no.nav.helse.spleis.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.spleis.mediator.VarseloppsamlerTest.Companion.Varsel.Companion.finn
import no.nav.helse.spleis.mediator.VarseloppsamlerTest.Companion.Varsel.Companion.unike
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarseloppsamlerTest {

    @Test
    fun `Samler opp varsler fra aktivitetslogg_ny_aktivitet`() {
        val varsel1 = UUID.randomUUID()
        val varsel2 = UUID.randomUUID()
        val varsler = listOf(aktivietsloggNyAktivitet(varsel1, RV_IM_4), aktivietsloggNyAktivitet(varsel2, RV_IM_3)).varsler

        val forventetVarsel1 = Varsel(
            id = varsel1,
            varselkode = RV_IM_4,
            kontekster = mapOf(
                "meldingsreferanseId" to "30c41872-5157-4611-aee5-fbed49d1234d",
                "fødselsnummer" to "11111111111",
                "organisasjonsnummer" to "999999999",
                "vedtaksperiodeId" to "fa51fb29-c9ca-4def-95fd-d93b0d8e5e0d",
                "tilstand" to "START"
            )
        )
        val forventetVarsel2 = Varsel(
            id = varsel2,
            varselkode = RV_IM_3,
            kontekster = mapOf(
                "meldingsreferanseId" to "30c41872-5157-4611-aee5-fbed49d1234d",
                "fødselsnummer" to "11111111111",
                "organisasjonsnummer" to "999999999",
                "vedtaksperiodeId" to "fa51fb29-c9ca-4def-95fd-d93b0d8e5e0d",
                "tilstand" to "START"
            )
        )

        assertEquals(listOf(forventetVarsel1, forventetVarsel2), varsler)
        assertEquals(forventetVarsel1, varsler.finn(UUID.fromString("fa51fb29-c9ca-4def-95fd-d93b0d8e5e0d"), RV_IM_4))
        assertEquals(forventetVarsel2, varsler.finn(UUID.fromString("fa51fb29-c9ca-4def-95fd-d93b0d8e5e0d"), RV_IM_3))
    }

    internal companion object {

        internal data class Varsel(
            private val id: UUID,
            private val varselkode: Varselkode,
            private val kontekster: Map<String, String>
        ) {
            internal companion object {
                internal val List<Varsel>.unike get() = distinctBy { it.id }
                internal fun List<Varsel>.finn(vedtaksperiodeId: UUID, varselkode: Varselkode) =
                    firstOrNull { it.kontekster.any { (key, value) -> key == "vedtaksperiodeId" && value == "$vedtaksperiodeId" } && it.varselkode == varselkode }

                internal fun List<Varsel>.finn(vedtaksperiodeId: UUID) =
                    filter { it.kontekster.any { (key, value) -> key == "vedtaksperiodeId" && value == "$vedtaksperiodeId" } }
            }
        }

        internal val List<JsonNode>.varsler get() = flatMap { it.varsler }.unike

        private val JsonNode.varsler
            get() = path("aktiviteter")
                .filter { aktivitet -> aktivitet.path("nivå").asText() == "VARSEL" }
                .map { varsel ->
                    val id = UUID.fromString(varsel.path("id").asText())
                    val varselkode = Varselkode.valueOf(varsel.path("varselkode").asText())
                    val kontekster = varsel.path("kontekster")
                        .map { it.path("kontekstmap") as ObjectNode }
                        .flatMap { kontekstmap -> kontekstmap.properties().map { it.key to it.value.asText() } }
                        .toMap()
                    Varsel(id, varselkode, kontekster)
                }

        @Language("JSON")
        private val aktivitetsloggNyAktivitetTemplate = """
        {
          "aktiviteter": [
            {
              "id": "b0b2e9e8-3bc7-4130-a273-5e33a9dc77ff",
              "nivå": "INFO",
              "kontekster": [
                {
                  "kontekstmap": {
                    "meldingsreferanseId": "d3d70111-9ca2-4876-a04a-652aa77c0662",
                    "fødselsnummer": "11111111111",
                    "organisasjonsnummer": "999999999"
                  }
                },
                {
                  "kontekstmap": {
                    "fødselsnummer": "11111111111"
                  }
                }
              ]
            },
            {
              "id": "{{varsel-id}}",
              "nivå": "VARSEL",
              "varselkode": "{{varselkode}}",
              "kontekster": [
                {
                  "kontekstmap": {
                    "meldingsreferanseId": "30c41872-5157-4611-aee5-fbed49d1234d",
                    "fødselsnummer": "11111111111",
                    "organisasjonsnummer": "999999999"
                  }
                },
                {
                  "kontekstmap": {
                    "fødselsnummer": "11111111111"
                  }
                },
                {
                  "kontekstmap": {
                    "organisasjonsnummer": "999999999"
                  }
                },
                {
                  "kontekstmap": {
                    "vedtaksperiodeId": "fa51fb29-c9ca-4def-95fd-d93b0d8e5e0d"
                  }
                },
                {
                  "kontekstmap": {
                    "tilstand": "START"
                  }
                }
              ]
            }
          ]
        }
        """

        private fun aktivietsloggNyAktivitet(varselId: UUID, varselkode: Varselkode) =
            aktivitetsloggNyAktivitetTemplate
                .replace("{{varsel-id}}", "$varselId")
                .replace("{{varselkode}}", varselkode.name).let {
                    jacksonObjectMapper().readTree(it)
                }
    }
}
