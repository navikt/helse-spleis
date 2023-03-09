package no.nav.helse.spleis.graphql

import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.AbstractObservableTest
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
    fun `person med person-resolver`() {
        val query = """
            {
                person(fnr: \"$UNG_PERSON_FNR\") {
                    aktorId,
                    fodselsnummer,
                    arbeidsgivere {
                        organisasjonsnummer,
                        id,
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
                                        personFagsystemId
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
                                    },
                                    aktivitetslogg {
                                        vedtaksperiodeId,
                                        alvorlighetsgrad,
                                        melding,
                                        tidsstempel
                                    }
                                }
                            }
                        }
                    },
                    dodsdato,
                    versjon
                }
            }
        """.trimIndent()

        requestBådeV1ogV2(
            body = """{"query": "$query"}""",
            testBlock = {
                objectMapper.readTree(this).get("data").get("person").let { person ->
                    assertEquals(5, person.size())
                }
            },
            v2TestBlock = {
                // For V2 vil man alltid få hele personen
                objectMapper.readTree(this).get("data").get("person").let { person ->
                    assertEquals(6, person.size())
                }
            }
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

        requestBådeV1ogV2(
            body = """{"query": "$query"}""",
            testBlock = {
                objectMapper.readTree(this).get("data").get("person").get("arbeidsgivere").let { arbeidsgivere ->
                    assertEquals(1, arbeidsgivere.size())
                    assertEquals(3, arbeidsgivere.get(0).size())
                }
            },
            v2TestBlock = {
                objectMapper.readTree(this).get("data").get("person").get("arbeidsgivere").let { arbeidsgivere ->
                    assertEquals(1, arbeidsgivere.size())
                    // For V2 vil man alltid få hele personen
                    assertEquals(4, arbeidsgivere.get(0).size())
                }
            }
        )
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
                                    },
                                    aktivitetslogg {
                                        vedtaksperiodeId,
                                        alvorlighetsgrad,
                                        melding,
                                        tidsstempel
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        requestBådeV1ogV2(
            body = """{"query": "$query"}"""
        ) {
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

        requestBådeV1ogV2(
            body = """{"query": "$query"}"""
        ) {
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
    fun `tester nøstede resolvers`() {
        val query = """
            {
                person(fnr: \"$UNG_PERSON_FNR\") {
                    arbeidsgiver(organisasjonsnummer: \"$ORGNUMMER\") {
                        organisasjonsnummer,
                        generasjon(index: 0) {
                            id,
                            perioderSlice(first: 1) {
                                ... on GraphQLBeregnetPeriode {
                                    fom,
                                    tom,
                                    beregningId,
                                    inntektsmeldinger {
                                        id,
                                        mottattDato,
                                        beregnetInntekt
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        requestBådeV1ogV2(
            body = """{"query": "$query"}""",
            testBlock = {
                val organisasjonsnummer = objectMapper.readTree(this).get("data").get("person").get("arbeidsgiver").get("organisasjonsnummer").asText()
                assertEquals(ORGNUMMER, organisasjonsnummer)
            },
            v2TestBlock = {
                // For V2 vil man alltid få hele personen
                val organisasjonsnummer = objectMapper.readTree(this).get("data").get("person").get("arbeidsgivere").single().get("organisasjonsnummer").asText()
                assertEquals(ORGNUMMER, organisasjonsnummer)
            }
        )
    }

    @Test
    fun `henter data for detaljvisning av første periode`() {
        val query = """
            {
                person(fnr: \"$UNG_PERSON_FNR\") {
                    arbeidsgiver(organisasjonsnummer: \"$ORGNUMMER\") {
                        organisasjonsnummer,
                        generasjon(index: 0) {
                            id,
                            perioderSlice(first: 1) {
                                ... on GraphQLBeregnetPeriode {
                                    fom,
                                    tom,
                                    beregningId
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        requestBådeV1ogV2(
            body = """{"query": "$query"}""",
            testBlock = {
                val organisasjonsnummer = objectMapper.readTree(this).get("data").get("person").get("arbeidsgiver").get("organisasjonsnummer").asText()
                assertEquals(ORGNUMMER, organisasjonsnummer)
            },
            v2TestBlock = {
                // For V2 vil man alltid få hele personen
                val organisasjonsnummer = objectMapper.readTree(this).get("data").get("person").get("arbeidsgivere").single().get("organisasjonsnummer").asText()
                assertEquals(ORGNUMMER, organisasjonsnummer)
            }
        )
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

        requestBådeV1ogV2(
            body = """{"query": "$query"}"""
        ) {
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

        requestBådeV1ogV2(
            body = """{"query": "$query"}"""
        ) {
            @Language("JSON")
            val forventet = """
                {
                  "data": {
                    "person": null
                  }
                }
            """
            JSONAssert.assertEquals(forventet, this, STRICT)
        }
    }

    @Test
    fun `response på introspection fra API og API V2 skal være like`() {
        val (v1Response, v2Response) = requestBådeV1ogV2(
            v1Path = "/graphql",
            v2Path = "/v2/graphql/introspection",
            body = IntrospectionQuery
        )

        JSONAssert.assertEquals(v1Response, v2Response, STRICT)
    }

    private fun requestBådeV1ogV2(
        v1Path: String = "/graphql",
        v2Path: String = "/v2/graphql",
        body: String,
        testBlock: String.() -> Unit = {},
        v2TestBlock: String.() -> Unit = testBlock,
    ): Pair<String, String> {
        lateinit var v1Response: String
        lateinit var v2Response: String

        testServer.httpPost(
            path = v1Path,
            body = body
        ) { v1Response = this }

        testServer.httpPost(
            path = v2Path,
            body = body
        ) { v2Response = this }

        testBlock(v1Response)
        v2TestBlock(v2Response)

        return v1Response to v2Response
    }
}
