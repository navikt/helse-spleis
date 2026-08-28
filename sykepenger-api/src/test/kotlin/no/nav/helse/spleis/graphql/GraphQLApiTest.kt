package no.nav.helse.spleis.graphql

import com.github.navikt.tbd_libs.naisful.test.TestContext
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import java.net.URI
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.person.EventBus
import no.nav.helse.person.Person
import no.nav.helse.spleis.AbstractSpleisApiTest
import no.nav.helse.spleis.SpekematClient
import no.nav.helse.spleis.testhelpers.TestObservatør
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT_ORDER

internal class GraphQLApiTest : AbstractSpleisApiTest() {

    @Test
    fun `hente person som ikke finnes`() = spleisApiTestApplication {
        val query = """
            {
                person(fnr: \"40440440440\") {
                    arbeidsgivere {
                        organisasjonsnummer,
                        id,
                        generasjoner {
                            id,
                        }
                    }
                }
            }
        """.trimIndent()

        request("""{"query": "$query"}""") {
            @Language("JSON")
            val forventet = """
                {
                  "data": {
                    "person": null
                  }
                }
            """
            assertHeltLike(forventet, this)
        }
    }

    @Test
    fun `response på introspection`() = spleisApiTestApplication {
        request(IntrospectionQuery) {
            assertHeltLike("/graphql-schema.json".readResource(), this)
        }
    }

    @Test
    fun `Det Spesialist faktisk henter`() {
        val spekemat = Spekemat()
        observatør = TestObservatør()
        val eventBus = EventBus().apply {
            register(spekemat)
            register(observatør)
        }
        person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)

        val spekematClient = mockk<SpekematClient>()
        spleisApiTestApplication(spekematClient = spekematClient, testdata = opprettTestdata(eventBus)) {
            every { spekematClient.hentSpekemat(UNG_PERSON_FNR, any()) } returns spekemat.resultat()
            val query =
                URI("https://raw.githubusercontent.com/navikt/helse-spesialist/main/clients/spesialist-client-spleis/src/main/resources/graphql/hentSnapshot.graphql").toURL()
                    .readText()

            @Language("JSON")
            val requestBody = """
                {
                    "query": "$query",
                    "variables": {
                      "fnr": "$UNG_PERSON_FNR"
                    },
                    "operationName": "HentSnapshot"
                }
            """

            request(requestBody) {
                assertHeltLike(detSpesialistFaktiskForventer, this.utenVariableVerdier)
            }
        }
    }

    private companion object {
        private val UUIDRegex = "\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b".toRegex()
        private val NullUUID = "00000000-0000-0000-0000-000000000000"
        private val LocalDateTimeRegex = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}".toRegex()
        private val LocalDateTimePrecisionRegex = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+".toRegex()
        private val LocalDateTimeMandagsfrø = "2018-01-01T00:00:00"
        private val TidsstempelRegex = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}".toRegex()
        private val TidsstempelMandagsfrø = "2018-01-01 00:00:00.000"
        private val FagsystemIdRegex = "[A-Z,2-7]{26}".toRegex()
        private val FagsystemId = "ZZZZZZZZZZZZZZZZZZZZZZZZZZ"
        private val String.utenVariableVerdier
            get() = replace(UUIDRegex, NullUUID)
                .replace(LocalDateTimeRegex, LocalDateTimeMandagsfrø)
                .replace(LocalDateTimePrecisionRegex, LocalDateTimeMandagsfrø)
                .replace(TidsstempelRegex, TidsstempelMandagsfrø)
                .replace(FagsystemIdRegex, FagsystemId)

        @Language("JSON")
        private const val IntrospectionQuery = """
        {"query":"\n    query IntrospectionQuery {\n      __schema {\n        queryType { name }\n        mutationType { name }\n        subscriptionType { name }\n        types {\n          ...FullType\n        }\n        directives {\n          name\n          description\n          locations\n          args {\n            ...InputValue\n          }\n        }\n      }\n    }\n    fragment FullType on __Type {\n      kind\n      name\n      description\n      fields(includeDeprecated: true) {\n        name\n        description\n        args {\n          ...InputValue\n        }\n        type {\n          ...TypeRef\n        }\n        isDeprecated\n        deprecationReason\n      }\n      inputFields {\n        ...InputValue\n      }\n      interfaces {\n        ...TypeRef\n      }\n      enumValues(includeDeprecated: true) {\n        name\n        description\n        isDeprecated\n        deprecationReason\n      }\n      possibleTypes {\n        ...TypeRef\n      }\n    }\n    fragment InputValue on __InputValue {\n      name\n      description\n      type { ...TypeRef }\n      defaultValue\n    }\n    fragment TypeRef on __Type {\n      kind\n      name\n      ofType {\n        kind\n        name\n        ofType {\n          kind\n          name\n          ofType {\n            kind\n            name\n            ofType {\n              kind\n              name\n              ofType {\n                kind\n                name\n                ofType {\n                  kind\n                  name\n                  ofType {\n                    kind\n                    name\n                  }\n                }\n              }\n            }\n          }\n        }\n      }\n    }\n    ","operationName":"IntrospectionQuery"}
        """

        @Language("JSON")
        private val detSpesialistFaktiskForventer = """
{
  "data": {
    "person": {
      "aktorId": "ikke_kult",
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "ghostPerioder": [],
          "generasjoner": [
            {
              "id":  "00000000-0000-0000-0000-000000000000",
              "kildeTilGenerasjon": "00000000-0000-0000-0000-000000000000",
              "perioder": [
                {
                  "__typename": "GraphQLBeregnetPeriode",
                  "erForkastet": false,
                  "fom": "2018-01-01",
                  "tom": "2018-01-30",
                  "inntektstype": "EnArbeidsgiver",
                  "opprettet": "2018-01-01T00:00:00",
                  "periodetype": "Forstegangsbehandling",
                  "periodetilstand": "Utbetalt",
                  "skjaeringstidspunkt": "2018-01-01",
                  "pensjonsgivendeInntekter": [],
                  "annulleringskandidater": [
                      {
                        "vedtaksperiodeId": "00000000-0000-0000-0000-000000000000",
                        "organisasjonsnummer": "987654321",
                        "fom": "2018-01-01",
                        "tom": "2018-01-30"
                      }
                  ],
                  "tidslinje": [
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-01",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-02",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-03",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-04",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-05",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-06",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "SykHelgedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-07",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "SykHelgedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-08",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-09",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-10",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-11",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-12",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-13",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "SykHelgedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-14",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "SykHelgedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-15",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-16",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "ArbeidsgiverperiodeDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-17",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "NavDag",
                      "utbetalingsinfo": {
                        "arbeidsgiverbelop": 1431,
                        "inntekt": null,
                        "personbelop": 0,
                        "refusjonsbelop": null,
                        "totalGrad": 100.0,
                        "utbetaling": 1431
                      }
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-18",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "NavDag",
                      "utbetalingsinfo": {
                        "arbeidsgiverbelop": 1431,
                        "inntekt": null,
                        "personbelop": 0,
                        "refusjonsbelop": null,
                        "totalGrad": 100.0,
                        "utbetaling": 1431
                      }
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-19",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "NavDag",
                      "utbetalingsinfo": {
                        "arbeidsgiverbelop": 1431,
                        "inntekt": null,
                        "personbelop": 0,
                        "refusjonsbelop": null,
                        "totalGrad": 100.0,
                        "utbetaling": 1431
                      }
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-20",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "SykHelgedag",
                      "utbetalingsdagtype": "NavHelgDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-21",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "SykHelgedag",
                      "utbetalingsdagtype": "NavHelgDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-22",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "NavDag",
                      "utbetalingsinfo": {
                        "arbeidsgiverbelop": 1431,
                        "inntekt": null,
                        "personbelop": 0,
                        "refusjonsbelop": null,
                        "totalGrad": 100.0,
                        "utbetaling": 1431
                      }
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-23",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "NavDag",
                      "utbetalingsinfo": {
                        "arbeidsgiverbelop": 1431,
                        "inntekt": null,
                        "personbelop": 0,
                        "refusjonsbelop": null,
                        "totalGrad": 100.0,
                        "utbetaling": 1431
                      }
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-24",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "NavDag",
                      "utbetalingsinfo": {
                        "arbeidsgiverbelop": 1431,
                        "inntekt": null,
                        "personbelop": 0,
                        "refusjonsbelop": null,
                        "totalGrad": 100.0,
                        "utbetaling": 1431
                      }
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-25",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "NavDag",
                      "utbetalingsinfo": {
                        "arbeidsgiverbelop": 1431,
                        "inntekt": null,
                        "personbelop": 0,
                        "refusjonsbelop": null,
                        "totalGrad": 100.0,
                        "utbetaling": 1431
                      }
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-26",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "NavDag",
                      "utbetalingsinfo": {
                        "arbeidsgiverbelop": 1431,
                        "inntekt": null,
                        "personbelop": 0,
                        "refusjonsbelop": null,
                        "totalGrad": 100.0,
                        "utbetaling": 1431
                      }
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-27",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "SykHelgedag",
                      "utbetalingsdagtype": "NavHelgDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-28",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "SykHelgedag",
                      "utbetalingsdagtype": "NavHelgDag",
                      "utbetalingsinfo": null
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-29",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "NavDag",
                      "utbetalingsinfo": {
                        "arbeidsgiverbelop": 1431,
                        "inntekt": null,
                        "personbelop": 0,
                        "refusjonsbelop": null,
                        "totalGrad": 100.0,
                        "utbetaling": 1431
                      }
                    },
                    {
                      "begrunnelser": null,
                      "dato": "2018-01-30",
                      "grad": 100.0,
                      "kilde": {
                        "id": "00000000-0000-0000-0000-000000000000",
                        "type": "Soknad"
                      },
                      "sykdomsdagtype": "Sykedag",
                      "utbetalingsdagtype": "NavDag",
                      "utbetalingsinfo": {
                        "arbeidsgiverbelop": 1431,
                        "inntekt": null,
                        "personbelop": 0,
                        "refusjonsbelop": null,
                        "totalGrad": 100.0,
                        "utbetaling": 1431
                      }
                    }
                  ],
                  "vedtaksperiodeId": "00000000-0000-0000-0000-000000000000",
                  "beregningId": "00000000-0000-0000-0000-000000000000",
                  "behandlingId": "00000000-0000-0000-0000-000000000000",
                  "forbrukteSykedager": 10,
                  "gjenstaendeSykedager": 238,
                  "hendelser": [
                    {
                      "__typename": "GraphQLSoknadNav",
                      "fom": "2018-01-01",
                      "id": "00000000-0000-0000-0000-000000000000",
                      "rapportertDato": "2018-01-01T00:00:00",
                      "sendtNav": "2018-01-01T00:00:00",
                      "tom": "2018-01-30",
                      "type": "SendtSoknadNav",
                      "eksternDokumentId": ""
                    },
                    {
                      "__typename": "GraphQLInntektsmelding",
                      "beregnetInntekt": 0.0,
                      "id": "00000000-0000-0000-0000-000000000000",
                      "mottattDato": "2018-01-01T00:00:00",
                      "type": "Inntektsmelding",
                      "eksternDokumentId": ""
                    }
                  ],
                  "maksdato": "2018-12-28",
                  "periodevilkar": {
                    "alder": {
                      "alderSisteSykedag": 25,
                      "oppfylt": true
                    },
                    "sykepengedager": {
                      "forbrukteSykedager": 10,
                      "gjenstaendeSykedager": 238,
                      "maksdato": "2018-12-28",
                      "oppfylt": true,
                      "skjaeringstidspunkt": "2018-01-01"
                    }
                  },
                  "utbetaling": {
                    "id": "00000000-0000-0000-0000-000000000000",
                    "arbeidsgiverFagsystemId": "ZZZZZZZZZZZZZZZZZZZZZZZZZZ",
                    "arbeidsgiverNettoBelop": 14310,
                    "personFagsystemId": "ZZZZZZZZZZZZZZZZZZZZZZZZZZ",
                    "personNettoBelop": 0,
                    "statusEnum": "Utbetalt",
                    "typeEnum": "UTBETALING",
                    "vurdering": {
                      "automatisk": false,
                      "godkjent": true,
                      "ident": "Ola Nordmann",
                      "tidsstempel": "2018-01-01T00:00:00"
                    },
                    "personoppdrag": {
                      "fagsystemId": "ZZZZZZZZZZZZZZZZZZZZZZZZZZ",
                      "tidsstempel": "2018-01-01T00:00:00",
                      "utbetalingslinjer": [],
                      "simulering": null
                    },
                    "arbeidsgiveroppdrag": {
                      "fagsystemId": "ZZZZZZZZZZZZZZZZZZZZZZZZZZ",
                      "tidsstempel": "2018-01-01T00:00:00",
                      "utbetalingslinjer": [],
                      "simulering": {
                        "totalbelop": 2000,
                        "perioder": [
                          {
                            "fom": "2018-01-17",
                            "tom": "2018-01-30",
                            "utbetalinger": [
                              {
                                "detaljer": [
                                  {
                                    "belop": 2000,
                                    "antallSats": 2,
                                    "faktiskFom": "2018-01-17",
                                    "faktiskTom": "2018-01-30",
                                    "klassekode": "SPREFAG-IOP",
                                    "klassekodeBeskrivelse": "Sykepenger, Refusjon arbeidsgiver",
                                    "konto": "81549300",
                                    "refunderesOrgNr": "987654321",
                                    "sats": 1000.0,
                                    "tilbakeforing": false,
                                    "typeSats": "DAG",
                                    "uforegrad": 100,
                                    "utbetalingstype": "YTEL"
                                  }
                                ],
                                "feilkonto": false,
                                "forfall": "2018-01-31",
                                "utbetalesTilId": "987654321",
                                "utbetalesTilNavn": "Org Orgesen AS"
                              }
                            ]
                          }
                        ]
                      }
                    }
                  },
                  "vilkarsgrunnlagId": "00000000-0000-0000-0000-000000000000",
                  "kilde": "00000000-0000-0000-0000-000000000000"
                }
              ]
            }
          ]
        }
      ],
      "dodsdato": null,
      "fodselsnummer": "12029240045",
      "vilkarsgrunnlag": [
        {
          "id": "00000000-0000-0000-0000-000000000000",
          "inntekter": [
            {
              "arbeidsgiver": "987654321",
              "omregnetArsinntekt": {
                "belop": 372000.0,
                "inntekterFraAOrdningen": null,
                "kilde": "Inntektsmelding",
                "manedsbelop": 31000.0
              },
              "fom": "2018-01-01",
              "tom":  null,
              "deaktivert": false, 
              "skjonnsmessigFastsatt": null,
              "skjonnsmessigFastsattAarlig": null
            }
          ],
          "arbeidsgiverrefusjoner": [
            {
              "arbeidsgiver": "987654321",
              "refusjonsopplysninger": [
                {
                  "fom": "2018-01-01",
                  "tom": null,
                  "belop": 31000.0,
                  "meldingsreferanseId": "00000000-0000-0000-0000-000000000000"
                }
              ]
            }
          ],
          "omregnetArsinntekt": 372000.0,
          "skjaeringstidspunkt": "2018-01-01",
          "sykepengegrunnlag": 372000.0,
          "beregningsgrunnlag": 372000.0,
          "__typename": "GraphQLSpleisVilkarsgrunnlag",
          "antallOpptjeningsdagerErMinst": 365,
          "grunnbelop": 93634,
          "sykepengegrunnlagsgrense": {
            "grunnbelop": 93634,
            "grense": 561804,
            "virkningstidspunkt": "2017-05-01"
          },
          "oppfyllerKravOmMedlemskap": true,
          "oppfyllerKravOmMinstelonn": true,
          "oppfyllerKravOmOpptjening": true,
          "opptjeningFra": "2017-01-01",
          "skjonnsmessigFastsattAarlig": null,
          "forsikringsvurderingId": null,
          "opptjeningsvurderingId": "00000000-0000-0000-0000-000000000000"
        }
      ], 
      "versjon": 54
    }
  }
}
        """

        private fun assertHeltLike(forventet: String, faktisk: String) =
            JSONAssert.assertEquals(forventet, faktisk, STRICT)

        private fun assertIngenFærreFelt(forventet: String, faktisk: String) =
            JSONAssert.assertEquals(forventet, faktisk, STRICT_ORDER)

        private fun String.readResource() =
            object {}.javaClass.getResource(this)?.readText(Charsets.UTF_8) ?: throw RuntimeException("Fant ikke filen på $this")

    }

    suspend fun TestContext.request(
        body: String,
        forventetHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        assertBlock: String.() -> Unit = {}
    ): String {
        return client
            .post("/graphql") { setBody(body) }
            .also { response ->
                assertEquals(forventetHttpStatusCode, response.status)
            }
            .bodyAsText()
            .also(assertBlock)
    }







}

