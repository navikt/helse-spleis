package no.nav.helse.spleis.graphql

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Simulering
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.spleis.AbstractObservableTest
import no.nav.helse.spleis.objectMapper
import no.nav.helse.spleis.testhelpers.ApiTestServer
import no.nav.helse.spleis.testhelpers.TestObservatør
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

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
        person = Person.fraHendelse(sykmelding, MaskinellJurist())
        observatør = TestObservatør().also { person.addObserver(it) }
        person.håndter(sykmelding)
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
        person.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR,
                orgnummer = ORGNUMMER,
                fagsystemId = fagsystemId,
                utbetalingId = "$utbetalingId",
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
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

        testServer.httpPost(
            path = "/graphql",
            body = """{"query": "$query"}"""
        ) {
            objectMapper.readTree(this).get("data").get("person").let { person ->
                assertEquals(5, person.size())
            }
        }
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

        testServer.httpPost(
            path = "/graphql",
            body = """{"query": "$query"}"""
        ) {
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

        testServer.httpPost(
            path = "/graphql",
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

        testServer.httpPost(
            path = "/graphql",
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

        testServer.httpPost(
            path = "/graphql",
            body = """{"query": "$query"}"""
        ) {
            val organisasjonsnummer = objectMapper.readTree(this).get("data").get("person").get("arbeidsgiver").get("organisasjonsnummer").asText()
            assertEquals(ORGNUMMER, organisasjonsnummer)
        }
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

        testServer.httpPost(
            path = "/graphql",
            body = """{"query": "$query"}"""
        ) {
            val organisasjonsnummer = objectMapper.readTree(this).get("data").get("person").get("arbeidsgiver").get("organisasjonsnummer").asText()
            assertEquals(ORGNUMMER, organisasjonsnummer)
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

        testServer.httpPost(
            path = "/graphql",
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

}
