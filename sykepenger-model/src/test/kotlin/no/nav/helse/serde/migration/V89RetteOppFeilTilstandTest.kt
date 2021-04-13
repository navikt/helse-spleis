package no.nav.helse.serde.migration

import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class V89RetteOppFeilTilstandTest {

    @Test
    fun `bytter tilstand på relevant periode`() {
        assertEquals(
            listOf(V89RetteOppFeilTilstand()).migrate(deser(person)), deser(expectedPerson)
        )
    }

    @Test
    fun `ber om hjelp hvis perioden har gått videre i tilstandsmaskinen`() {
        assertThrows(IllegalStateException::class.java) { listOf(V89RetteOppFeilTilstand()).migrate(deser(personSomIkkeKanMigreres))}
    }

    private fun deser(json: String) = serdeObjectMapper.readTree(json)

    @Language("JSON")
    private val person = """
{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "c400ce20-3568-4813-a488-83e6865f36d0",
                    "tilstand": "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE"
                }
            ]
        },
        {
            "vedtaksperioder": [
                {
                    "id": "fea7efa2-b287-4002-8201-71d43711b8f5",
                    "tilstand": "MOTTATT_SYKMELDING_FERDIG_GAP"
               }
            ]
        }
    ],
    "skjemaVersjon": 88
}
"""

    @Language("JSON")
    val expectedPerson = """
{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "c400ce20-3568-4813-a488-83e6865f36d0",
                    "tilstand": "MOTTATT_SYKMELDING_UFERDIG_GAP"
                }
            ]
        },
        {
            "vedtaksperioder": [
                {
                    "id": "fea7efa2-b287-4002-8201-71d43711b8f5",
                    "tilstand": "MOTTATT_SYKMELDING_FERDIG_GAP"
               }
            ]
        }
    ],
    "skjemaVersjon": 89
}
    """

    @Language("JSON")
    private val personSomIkkeKanMigreres = """
{
    "arbeidsgivere": [
        {
            "vedtaksperioder": [
                {
                    "id": "c400ce20-3568-4813-a488-83e6865f36d0",
                    "tilstand": "AVVENTER_SØKNAD_UFERDIG_FORLENGELSE"
                }
            ]
        },
        {
            "vedtaksperioder": [
                {
                    "id": "fea7efa2-b287-4002-8201-71d43711b8f5",
                    "tilstand": "MOTTATT_SYKMELDING_FERDIG_GAP"
               }
            ]
        }
    ],
    "skjemaVersjon": 88
}
"""

}
