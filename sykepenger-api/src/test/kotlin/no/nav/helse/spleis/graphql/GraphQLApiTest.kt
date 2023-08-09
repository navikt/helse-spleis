package no.nav.helse.spleis.graphql

import io.ktor.http.HttpStatusCode
import java.net.URL
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.AbstractObservableTest
import no.nav.helse.spleis.Issuer
import no.nav.helse.spleis.graphql.SchemaGenerator.Companion.IntrospectionQuery
import no.nav.helse.spleis.objectMapper
import no.nav.helse.spleis.testhelpers.ApiTestServer
import no.nav.helse.spleis.testhelpers.TestObservatør
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT_ORDER

@TestInstance(Lifecycle.PER_CLASS)
internal class GraphQLApiTest : AbstractObservableTest() {

    private lateinit var testServer: ApiTestServer

    @BeforeAll
    internal fun setupServer() {
        testServer = ApiTestServer()
        testServer.start()
    }

    @AfterAll
    internal fun tearDownServer() {
        testServer.tearDown()
    }

    @BeforeEach
    internal fun setup() {
        val sykmelding = sykmelding()
        person = Person(AKTØRID, UNG_PERSON_FNR.somPersonidentifikator(), UNG_PERSON_FØDSELSDATO.alder, MaskinellJurist())
        observatør = TestObservatør().also { person.addObserver(it) }
        person.håndter(sykmelding)
        person.håndter(utbetalinghistorikk())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(ytelser())
        person.håndter(vilkårsgrunnlag())
        val ytelser = ytelser()
        person.håndter(ytelser)
        val simuleringsbehov = ytelser.behov().last { it.type == Simulering }
        val utbetalingId = UUID.fromString(simuleringsbehov.kontekst().getValue("utbetalingId"))
        val fagsystemId = simuleringsbehov.detaljer().getValue("fagsystemId") as String
        val fagområde = simuleringsbehov.detaljer().getValue("fagområde") as String
        person.håndter(simulering(utbetalingId = utbetalingId, fagsystemId = fagsystemId, fagområde = fagområde))
        person.håndter(utbetalingsgodkjenning(utbetalingId = utbetalingId))
        person.håndter(utbetaling(utbetalingId = utbetalingId, fagsystemId = fagsystemId))

        testServer.clean()
        testServer.lagrePerson(AKTØRID, UNG_PERSON_FNR, person)
        testServer.lagreSykmelding(
            fødselsnummer = UNG_PERSON_FNR,
            meldingsReferanse = SYKMELDING_ID,
            fom = FOM,
            tom = TOM,
        )
        testServer.lagreSøknadNav(
            fødselsnummer = UNG_PERSON_FNR,
            meldingsReferanse = SØKNAD_ID,
            fom = FOM,
            tom = TOM,
            sendtNav = TOM.plusDays(1).atStartOfDay()
        )
        testServer.lagreInntektsmelding(
            fødselsnummer = UNG_PERSON_FNR,
            meldingsReferanse = INNTEKTSMELDING_ID,
            beregnetInntekt = INNTEKT,
            førsteFraværsdag = FOM
        )
    }

    @Test
    fun `arbeidsgivere med person-resolver`() {
        val query = """
            {
                person(fnr: \"$UNG_PERSON_FNR\") {
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
            objectMapper.readTree(this).get("data").get("person").get("arbeidsgivere").let { arbeidsgivere ->
                assertEquals(1, arbeidsgivere.size())
                assertEquals(3, arbeidsgivere.get(0).size())
            }
        }
    }

    @Test
    fun `generasjoner med person-resolver`() {
        val query = """
            {
                person(fnr: \"$UNG_PERSON_FNR\") {
                    arbeidsgivere {
                        generasjoner {
                            id,
                            perioder {
                                id,
                                fom,
                                tom,
                                tidslinje {
                                    dato,
                                    sykdomsdagtype,
                                    utbetalingsdagtype,
                                    kilde {
                                        type,
                                        id
                                    },
                                    grad,
                                    utbetalingsinfo {
                                        inntekt,
                                        utbetaling,
                                        totalGrad
                                    },
                                    begrunnelser
                                }
                                periodetype,
                                inntektstype,
                                erForkastet,
                                opprettet,
                                ... on GraphQLBeregnetPeriode {
                                    beregningId,
                                    gjenstaendeSykedager,
                                    forbrukteSykedager,
                                    skjaeringstidspunkt,
                                    maksdato,
                                    utbetaling {
                                        type,
                                        status,
                                        arbeidsgiverNettoBelop,
                                        personNettoBelop,
                                        arbeidsgiverFagsystemId,
                                        personFagsystemId,
                                        personoppdrag {
                                            fagsystemId,
                                            tidsstempel,
                                            simulering {
                                                totalbelop,
                                                perioder {
                                                    fom,
                                                    tom,
                                                    utbetalinger {
                                                        utbetalesTilId,
                                                        utbetalesTilNavn,
                                                        forfall,
                                                        feilkonto,
                                                        detaljer {
                                                            faktiskFom,
                                                            faktiskTom,
                                                            konto,
                                                            belop,
                                                            tilbakeforing,
                                                            sats,
                                                            typeSats,
                                                            antallSats,
                                                            uforegrad,
                                                            klassekode,
                                                            klassekodeBeskrivelse,
                                                            utbetalingstype,
                                                            refunderesOrgNr
                                                        }
                                                    }
                                                }
                                            },
                                            utbetalingslinjer {
                                                fom,
                                                tom,
                                                dagsats,
                                                grad
                                            }
                                        },
                                        arbeidsgiveroppdrag {
                                            fagsystemId,
                                            tidsstempel,
                                            simulering {
                                                totalbelop,
                                                perioder {
                                                    fom,
                                                    tom,
                                                    utbetalinger {
                                                        utbetalesTilId,
                                                        utbetalesTilNavn,
                                                        forfall,
                                                        feilkonto,
                                                        detaljer {
                                                            faktiskFom,
                                                            faktiskTom,
                                                            konto,
                                                            belop,
                                                            tilbakeforing,
                                                            sats,
                                                            typeSats,
                                                            antallSats,
                                                            uforegrad,
                                                            klassekode,
                                                            klassekodeBeskrivelse,
                                                            utbetalingstype,
                                                            refunderesOrgNr
                                                        }
                                                    }
                                                }
                                            },
                                            utbetalingslinjer {
                                                fom,
                                                tom,
                                                dagsats,
                                                grad
                                            }
                                        }
                                    },
                                    hendelser {
                                        id,
                                        type,
                                        ... on GraphQLInntektsmelding {
                                            mottattDato,
                                            beregnetInntekt
                                        }
                                        ... on GraphQLSoknadNav {
                                            fom,
                                            tom,
                                            rapportertDato,
                                            sendtNav
                                        }
                                        ... on GraphQLSykmelding {
                                            fom,
                                            tom,
                                            rapportertDato
                                        }
                                    },
                                    vilkarsgrunnlagId,
                                    periodevilkar {
                                        sykepengedager {
                                            skjaeringstidspunkt,
                                            maksdato,
                                            forbrukteSykedager,
                                            gjenstaendeSykedager,
                                            oppfylt
                                        },
                                        alder {
                                            alderSisteSykedag,
                                            oppfylt
                                        },
                                        soknadsfrist {
                                            sendtNav,
                                            soknadFom,
                                            soknadTom,
                                            oppfylt
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        request("""{"query": "$query"}""") {
            objectMapper.readTree(this).get("data").get("person").get("arbeidsgivere").get(0).get("generasjoner").get(0).let { generasjon ->
                assertEquals(2, generasjon.size())
                assertEquals(1, generasjon.get("perioder").size())
            }
        }
    }

    @Test
    fun `refusjonsopplysninger på vilkårsgrunnlag med person-resolver`() {
        val query = """
            {
                person(fnr: \"$UNG_PERSON_FNR\") {
                    arbeidsgivere {
                        generasjoner {
                            perioder {
                                ... on GraphQLBeregnetPeriode {
                                    vilkarsgrunnlagId
                                }
                            }
                        }
                    },
                    vilkarsgrunnlag {
                            ... on GraphQLSpleisVilkarsgrunnlag {
                                id,
                                skjaeringstidspunkt,
                                arbeidsgiverrefusjoner {
                                    arbeidsgiver
                                    refusjonsopplysninger {
                                        fom
                                        tom
                                        belop
                                        meldingsreferanseId
                                    }
                                }
                                vilkarsgrunnlagtype
                            }
                        }
                }
            }
        """.trimIndent()

        request("""{"query": "$query"}""") {
            objectMapper.readTree(this).get("data").get("person").let { person ->
                val vilkårsgrunnlagId: String =
                    person.get("arbeidsgivere").get(0).get("generasjoner").get(0).get("perioder").get(0).get("vilkarsgrunnlagId").asText()
                val vilkårsgrunnlårsgrunnlag = person.get("vilkarsgrunnlag")
                assertEquals(1, vilkårsgrunnlårsgrunnlag.size())
                assertEquals(vilkårsgrunnlagId, vilkårsgrunnlårsgrunnlag.get(0).get("id").asText())
                assertEquals(1, vilkårsgrunnlårsgrunnlag.get(0).get("arbeidsgiverrefusjoner").size())
                assertEquals(1, vilkårsgrunnlårsgrunnlag.get(0).get("arbeidsgiverrefusjoner")[0].get("refusjonsopplysninger").size())
                val refusjonselement = vilkårsgrunnlårsgrunnlag.get(0).get("arbeidsgiverrefusjoner")[0].get("refusjonsopplysninger")[0]
                assertEquals("2018-01-01", refusjonselement.get("fom").asText())
            }
        }
    }

    @Test
    fun `vilkårsgrunnlag med grunnbeløpgrense`() {
        val query = """
            {
                person(fnr: \"$UNG_PERSON_FNR\") {
                    arbeidsgivere {
                        generasjoner {
                            perioder {
                                ... on GraphQLBeregnetPeriode {
                                    vilkarsgrunnlagId
                                }
                            }
                        }
                    },
                    vilkarsgrunnlag {
                        id, 
                        ... on GraphQLSpleisVilkarsgrunnlag {
                            sykepengegrunnlagsgrense {
                                grunnbelop,
                                grense,
                                virkningstidspunkt
                            }                            
                        }
                    }
                }
            }
        """.trimIndent()

        request("""{"query": "$query"}""") {
            objectMapper.readTree(this).get("data").get("person").let { person ->
                val vilkårsgrunnlagId: String =
                    person.get("arbeidsgivere").get(0).get("generasjoner").get(0).get("perioder").get(0).get("vilkarsgrunnlagId").asText()
                val vilkårsgrunnlag = person.get("vilkarsgrunnlag")
                assertEquals(1, vilkårsgrunnlag.size())
                assertEquals(vilkårsgrunnlagId, vilkårsgrunnlag.get(0).get("id").asText())
                assertEquals(93634, vilkårsgrunnlag.get(0).get("sykepengegrunnlagsgrense").get("grunnbelop").asInt())
                assertEquals( 561804, vilkårsgrunnlag.get(0).get("sykepengegrunnlagsgrense").get("grense").asInt())
                assertEquals("2017-05-01", vilkårsgrunnlag.get(0).get("sykepengegrunnlagsgrense").get("virkningstidspunkt").asText())
            }
        }
    }

    @Test
    fun `hente person som ikke finnes`() {
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
    fun `request med manglende eller feil access token`() {
        val query = """
            {
                person(fnr: \"$UNG_PERSON_FNR\") {
                    arbeidsgivere {
                        organisasjonsnummer,
                        id,
                        generasjoner {
                            id,
                        }
                    }
                }
            }
        """

        val body = """{"query": "$query"}"""

        val annenIssuer = Issuer("annen")

        request(body = body, accesToken = null, forventetHttpStatusCode = HttpStatusCode.Unauthorized)
        request(body = body, accesToken = testServer.createToken(), forventetHttpStatusCode = HttpStatusCode.OK)
        request(body = body, accesToken = testServer.createToken("feil_audience"), forventetHttpStatusCode = HttpStatusCode.Unauthorized)
        request(body = body, accesToken = annenIssuer.createToken(), forventetHttpStatusCode = HttpStatusCode.Unauthorized)
    }

    @Test
    fun `response på introspection`() {
        request(IntrospectionQuery) {
            assertHeltLike("/graphql-schema.json".readResource(), this)
        }
    }

    @Test
    fun `Det Spesialist faktisk henter`() {
        val query = URL("https://raw.githubusercontent.com/navikt/helse-spesialist/master/spesialist-api/src/main/resources/graphql/hentSnapshot.graphql").readText()
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
            assertIngenFærreFelt(detSpesialistFaktiskForventer, this.utenVariableVerdier)
        }
    }

    private fun request(
        body: String,
        accesToken: String? = testServer.createToken(),
        forventetHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        assertBlock: String.() -> Unit = {}
    ){
        lateinit var v1Response: String

        testServer.httpPost(
            path = "/graphql",
            body = body,
            accessToken = accesToken,
            expectedStatus = forventetHttpStatusCode
        ) { v1Response = this }


        assertBlock(v1Response)
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
        private val detSpesialistFaktiskForventer = """
{
  "data": {
    "person": {
      "aktorId": "42",
      "arbeidsgivere": [
        {
          "organisasjonsnummer": "987654321",
          "ghostPerioder": [],
          "generasjoner": [
            {
              "id": "00000000-0000-0000-0000-000000000000",
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
                  "id": "00000000-0000-0000-0000-000000000000",
                  "beregningId": "00000000-0000-0000-0000-000000000000",
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
                      "type": "SendtSoknadNav"
                    },
                    {
                      "__typename": "GraphQLInntektsmelding",
                      "beregnetInntekt": 0.0,
                      "id": "00000000-0000-0000-0000-000000000000",
                      "mottattDato": "2018-01-01T00:00:00",
                      "type": "Inntektsmelding"
                    }
                  ],
                  "maksdato": "2018-12-28",
                  "periodevilkar": {
                    "alder": {
                      "alderSisteSykedag": 25,
                      "oppfylt": true
                    },
                    "soknadsfrist": {
                      "oppfylt": true,
                      "sendtNav": "2018-01-01T00:00:00",
                      "soknadFom": "2018-01-01",
                      "soknadTom": "2018-01-30"
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
                  "vilkarsgrunnlagId": "00000000-0000-0000-0000-000000000000"
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
          "vilkarsgrunnlagtype": "Spleis",
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
              "sammenligningsgrunnlag": {
                "belop": 372000.0,
                "inntekterFraAOrdningen": []
              },
              "deaktivert": false
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
          "sammenligningsgrunnlag": 372000.0,
          "skjaeringstidspunkt": "2018-01-01",
          "sykepengegrunnlag": 372000.0,
          "__typename": "GraphQLSpleisVilkarsgrunnlag",
          "antallOpptjeningsdagerErMinst": 365,
          "avviksprosent": 0.0,
          "grunnbelop": 93634,
          "sykepengegrunnlagsgrense": {
            "grunnbelop": 93634,
            "grense": 561804,
            "virkningstidspunkt": "2017-05-01"
          },
          "oppfyllerKravOmMedlemskap": true,
          "oppfyllerKravOmMinstelonn": true,
          "oppfyllerKravOmOpptjening": true,
          "opptjeningFra": "2017-01-01"
        }
      ]
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
}
