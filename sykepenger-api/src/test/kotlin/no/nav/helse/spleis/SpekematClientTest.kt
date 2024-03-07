package no.nav.helse.spleis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.azure.AzureToken
import com.github.navikt.tbd_libs.azure.AzureTokenProvider
import com.github.navikt.tbd_libs.mock.MockHttpResponse
import io.mockk.every
import io.mockk.mockk
import java.net.http.HttpClient
import java.time.LocalDateTime
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpekematClientTest {
    private val azureTokenProvider = object : AzureTokenProvider {
        override fun bearerToken(scope: String) = AzureToken("liksom-token", LocalDateTime.MAX)
        override fun onBehalfOfToken(scope: String, token: String): AzureToken {
            throw NotImplementedError("ikke implementert i mock")
        }
    }
    private var httpClientMock = mockk<HttpClient>()
    private val client = SpekematClient(httpClientMock, azureTokenProvider, "scope-til-spekemat", jacksonObjectMapper())

    @Test
    fun `tolker response fra spekemat`() {
        every { httpClientMock.send<String>(any(), any()) } returns MockHttpResponse(responsFraSpekemat, 200, mapOf("callId" to "liksom call id"))
        val result = client.hentSpekemat("fnr", "callId")
        assertEquals(1, result.pakker.size)
        assertEquals(1, result.pakker.single().rader.size)
        assertEquals(2, result.pakker.single().rader.single().pølser.size)
    }
}

@Language("JSON")
private const val responsFraSpekemat = """{
    "yrkesaktiviteter": [
        {
            "yrkesaktivitetidentifikator": "990739323",
            "rader": [
                {
                    "pølser": [
                        {
                            "vedtaksperiodeId": "617fc6ad-345b-4afa-8e75-b5a6aca95b17",
                            "behandlingId": "e874ce3b-ccf7-45ec-a0f5-ddd201e68a49",
                            "status": "ÅPEN",
                            "kilde": "3c3399e4-ce95-4425-9cf2-aba45afb0d16"
                        },
                        {
                            "vedtaksperiodeId": "2c144579-1377-41db-96cf-b611f5dfb697",
                            "behandlingId": "f4f3738b-37da-4393-bb4a-3feeb7453e40",
                            "status": "LUKKET",
                            "kilde": "d45d4dd0-c404-45be-a310-628e69e5295e"
                        }
                    ],
                    "kildeTilRad": "d45d4dd0-c404-45be-a310-628e69e5295e"
                }
            ]
        }
    ]
}"""