package no.nav.helse.serde.migration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class V8FjernAktivitetsloggerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `Fjerner aktivitetslogger`() {
        val json = objectMapper.readTree(personJson)
        listOf(V8FjernAktivitetslogger()).migrate(json)
        val migratedJson = json.toString()

        assertNotContains(migratedJson, "\"aktivitetslogger\"")
    }

    private fun assertNotContains(json: String, value: String) {
        Assertions.assertFalse(json.contains(value)) { "Did not expect to find $value in $json" }
    }
}

private const val personJson = """
{
    "aktørId" : "12020052345",
    "fødselsnummer" : "42",
    "aktivitetslogger" : {
        "aktiviteter" : [ {
            "alvorlighetsgrad" : "INFO",
            "melding" : "Behandler sykmelding (Sykmelding)",
            "tidsstempel" : "2020-02-07 15:43:10.343"
        }],
        "originalMessage" : null
    },
    "arbeidsgivere" : [ {
        "organisasjonsnummer" : "987654321",
        "id" : "1426a4b8-0b5a-455d-8389-6909ab94d56e",
        "aktivitetslogger" : {
            "aktiviteter" : [ {
                "alvorlighetsgrad" : "INFO",
                "melding" : "Behandler sykmelding (Sykmelding)",
                "tidsstempel" : "2020-02-07 15:43:10.343"
            }],
            "originalMessage" : null
        },
        "vedtaksperioder" : [ {
            "id" : "586d4d3a-e7da-4799-a47d-cd8d610f9bb5",
            "maksdato" : "2019-01-01",
            "godkjentAv" : "Ola Nordmann",
            "utbetalingsreferanse" : "LBWU2OXH3JDZTJD5ZWGWCD43WU",
            "førsteFraværsdag" : "2018-01-01",
            "inntektFraInntektsmelding" : 31000.0,
            "aktivitetslogger" : {
                "aktiviteter" : [ {
                    "alvorlighetsgrad" : "INFO",
                    "melding" : "Behandler sykmelding (Sykmelding)",
                    "tidsstempel" : "2020-02-07 15:43:10.343"
                } ],
                "originalMessage" : null
            },
            "tilstand" : "TIL_UTBETALING"
        } ]
    } ]
}

"""
