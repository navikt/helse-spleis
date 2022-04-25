package no.nav.helse.serde.migration

import java.util.UUID
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.NyTilstandsflyt::class)
internal class V150MigrerVedtaksperioderTilNyTilstandsflytTest :
    MigrationTest({ V150MigrerVedtaksperioderTilNyTilstandsflyt() }) {

    @Test
    fun `Endrer alle tilstander til vedtaksperioder til tilstander i ny flyt`() {
        assertMigration(
            "/migrations/150/expected.json",
            "/migrations/150/original.json"
        )
        val forventetSlettedeVedtaksperioder = listOf(
            UUID.fromString("241f0cdc-95c0-409d-876d-2d3ac6a31a9c") to toNode(
                """{
                "id": "241f0cdc-95c0-409d-876d-2d3ac6a31a9c",
                "tilstand": "AVVENTER_SØKNAD_UFERDIG_FORLENGELSE"
            }"""
            ),
            UUID.fromString("bfb92723-04ae-46bd-9425-2fed2df6cc3c") to toNode(
                """{
                "id": "bfb92723-04ae-46bd-9425-2fed2df6cc3c",
                "tilstand": "AVVENTER_SØKNAD_FERDIG_FORLENGELSE"
            }"""
            ),
            UUID.fromString("a206320d-69f3-4a5b-90d3-d16b36a5af23") to toNode(
                """{
                "id": "a206320d-69f3-4a5b-90d3-d16b36a5af23",
                "tilstand": "AVVENTER_SØKNAD_FERDIG_GAP"
            }"""
            ),
            UUID.fromString("569d96a6-c7ad-4fe1-920c-ca815b27ff35") to toNode(
                """{
                "id": "569d96a6-c7ad-4fe1-920c-ca815b27ff35",
                "tilstand": "AVVENTER_SØKNAD_UFERDIG_GAP"
            }"""
            ),
            UUID.fromString("730efe78-4020-407c-ba80-a044b05f53ac") to toNode(
                """{
                "id": "730efe78-4020-407c-ba80-a044b05f53ac",
                "tilstand": "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE"
            }"""
            ),
            UUID.fromString("693f09a3-19e2-4693-9d90-a921f0d77ec3") to toNode(
                """{
                "id": "693f09a3-19e2-4693-9d90-a921f0d77ec3",
                "tilstand": "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE"
            }"""
            ),
            UUID.fromString("b48f5fb6-ba22-43cb-b0d9-59a95e52e9b3") to toNode(
                """{
                "id": "b48f5fb6-ba22-43cb-b0d9-59a95e52e9b3",
                "tilstand": "MOTTATT_SYKMELDING_FERDIG_GAP"
            }"""
            ),
            UUID.fromString("0c91a3c8-0abb-42cb-80b1-2c552a63eec6") to toNode(
                """{
                "id": "0c91a3c8-0abb-42cb-80b1-2c552a63eec6",
                "tilstand": "MOTTATT_SYKMELDING_UFERDIG_GAP"
            }"""
            )
        )
        assertEquals(forventetSlettedeVedtaksperioder, observatør.slettedeVedtaksperioder)

        val tilstandsendinger = listOf(
            mapOf(
                "id" to "326323d4-572a-402c-baf4-5ddf04d24a43",
                "gammelTilstand" to "AVVENTER_ARBEIDSGIVERE",
                "nyTilstand" to "AVVENTER_BLOKKERENDE_PERIODE"
            ),
            mapOf(
                "id" to "5615ca7f-2519-4bae-8b70-57b475f958fb",
                "gammelTilstand" to "AVVENTER_UFERDIG",
                "nyTilstand" to "AVVENTER_BLOKKERENDE_PERIODE"
            ),
            mapOf(
                "id" to "559cfe35-153a-4297-94cc-e303374c1665",
                "gammelTilstand" to "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
                "nyTilstand" to "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK"
            ),
            mapOf(
                "id" to "d69bdf89-9fca-46c5-b80a-b66951bb69e3",
                "gammelTilstand" to "AVVENTER_INNTEKTSMELDING_UFERDIG_GAP",
                "nyTilstand" to "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK"
            ),
            mapOf(
                "id" to "61983033-3acc-47b2-b1ad-ed63cf5136e1",
                "gammelTilstand" to "AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE",
                "nyTilstand" to "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK"
            ),
            mapOf(
                "id" to "e32738c2-22fe-439b-8b55-1d6eb0b4eb3d",
                "gammelTilstand" to "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE",
                "nyTilstand" to "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK"
            )
        )
        assertEquals(tilstandsendinger, observatør.endredeVedtaksperioder)

    }
}