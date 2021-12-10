package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class V133VilkårsgrunnlagIdPåVilkårsgrunnlagTest: MigrationTest(V133VilkårsgrunnlagIdPåVilkårsgrunnlag()) {

    @Test
    fun `vilkårsgrunnlag får id`() {
        val migrertJson = migrer(original)
        migrertJson["vilkårsgrunnlagHistorikk"]
            .assertSize(2)
            .forEach { innslag ->
                innslag["vilkårsgrunnlag"]
                    .assertSize(3)
                    .forEach {
                        assertDoesNotThrow {
                            it["vilkårsgrunnlagId"].asUuid()
                        }
                        assertEquals("2018-01-01", it["skjæringstidspunkt"].asText())
                    }
            }
        assertEquals("133", migrertJson["skjemaVersjon"].asText())
    }

    @Test
    fun `Endrer ikke feltet dersom det allerede er satt`() {
        val vilkårsgrunnlagId = UUID.randomUUID()

        assertMigrationRaw(
            expectedJson = jsonMedIdSatt(vilkårsgrunnlagId, skjemaversjon = 133),
            originalJson = jsonMedIdSatt(vilkårsgrunnlagId, skjemaversjon = 132)
        )
    }

    @Test
    fun `Like objekter har samme id`() {
        val migrert = migrer(originalMedLikeObjekter)
        val vilkårsgrunnlagId1 = migrert["vilkårsgrunnlagHistorikk"][0]["vilkårsgrunnlag"][0]["vilkårsgrunnlagId"].asUuid()
        val vilkårsgrunnlagId2 = migrert["vilkårsgrunnlagHistorikk"][0]["vilkårsgrunnlag"][1]["vilkårsgrunnlagId"].asUuid()
        assertEquals(toNode(expectedMedLikeObjekter(vilkårsgrunnlagId1, vilkårsgrunnlagId2)), migrert)
    }

    private fun <T> Iterable<T>.assertSize(size: Int): Iterable<T> {
        assertEquals(size, this.count())
        return this
    }

    private fun JsonNode.asUuid() = UUID.fromString(this.asText())

    @Language("JSON")
    private val original = """{
    "vilkårsgrunnlagHistorikk": [
        {
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2018-01-01"
                },
                {
                    "skjæringstidspunkt": "2018-01-01"
                },
                {
                    "skjæringstidspunkt": "2018-01-01"
                }
            ]
        },
        {
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2018-01-01"
                },
                {
                    "skjæringstidspunkt": "2018-01-01"
                },
                {
                    "skjæringstidspunkt": "2018-01-01"
                }
            ]
        }
    ],
    "skjemaVersjon": 132
}
    """

    @Language("JSON")
    private val originalMedLikeObjekter = """{
    "vilkårsgrunnlagHistorikk": [
        {
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2018-02-01"
                },
                {
                    "skjæringstidspunkt": "2018-01-01"
                }
            ]
        },
        {
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2018-01-01"
                }
            ]
        }
    ],
    "skjemaVersjon": 132
}
    """

    @Language("JSON")
    private fun expectedMedLikeObjekter(vilkårsgrunnlagId1: UUID, vilkårsgrunnlagId2: UUID) = """{
    "vilkårsgrunnlagHistorikk": [
        {
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2018-02-01",
                    "vilkårsgrunnlagId": "$vilkårsgrunnlagId1"
                },
                {
                    "skjæringstidspunkt": "2018-01-01",
                    "vilkårsgrunnlagId": "$vilkårsgrunnlagId2"
                }
            ]
        },
        {
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2018-01-01",
                    "vilkårsgrunnlagId": "$vilkårsgrunnlagId2"
                }
            ]
        }
    ],
    "skjemaVersjon": 133
}
    """

    @Language("JSON")
    private fun jsonMedIdSatt(id: UUID, skjemaversjon: Int) = """{
    "vilkårsgrunnlagHistorikk": [
        {
            "vilkårsgrunnlag": [
                {
                    "skjæringstidspunkt": "2018-01-01",
                    "vilkårsgrunnlagId": "$id"
                }
            ]
        }
    ],
    "skjemaVersjon": $skjemaversjon
}
    """
}
