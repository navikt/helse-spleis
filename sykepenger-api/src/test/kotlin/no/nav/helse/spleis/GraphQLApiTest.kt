package no.nav.helse.spleis

import no.nav.helse.Toggles
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.person.Person
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.testhelpers.ApiTestServer
import no.nav.helse.spleis.testhelpers.TestObservatør
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.time.LocalDateTime
import java.util.*

@TestInstance(Lifecycle.PER_CLASS)
internal class GraphQLApiTest : AbstractObservableTest() {

    private lateinit var testServer: ApiTestServer

    @BeforeAll
    internal fun setupServer() {
        Toggles.SpeilApiV2.enable()
        testServer = ApiTestServer()
        testServer.start()
    }

    @AfterAll
    internal fun tearDownServer() {
        testServer.tearDown()
        Toggles.SpeilApiV2.pop()
    }

    @BeforeEach
    internal fun setup() {
        person = Person(AKTØRID, UNG_PERSON_FNR.somFødselsnummer())
        observatør = TestObservatør().also { person.addObserver(it) }

        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(ytelser())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
        person.håndter(simulering())
        person.håndter(utbetalingsgodkjenning())
        person.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR,
                orgnummer = ORGNUMMER,
                fagsystemId = "tilfeldig-string",
                utbetalingId = UUID.randomUUID().toString(),
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        person.håndter(utbetaling())

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
                person(fnr: ${UNG_PERSON_FNR.toLong()}) {
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
                                behandlingstype,
                                periodetype,
                                inntektskilde,
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
                                    vilkarsgrunnlaghistorikkId,
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
                    inntektsgrunnlag {
                        sykepengegrunnlag
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
                assertEquals(6, person.size())
            }
        }
    }

    @Test
    fun `arbeidsgivere med person-resolver`() {
        val query = """
            {
                person(fnr: ${UNG_PERSON_FNR.toLong()}) {
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
    fun `inntektsgrunnlag med person-resolver`() {
        val query = """
            {
                person(fnr: ${UNG_PERSON_FNR.toLong()}) {
                    inntektsgrunnlag {
                        skjaeringstidspunkt,
                        sykepengegrunnlag,
                        omregnetArsinntekt,
                        sammenligningsgrunnlag,
                        avviksprosent,
                        maksUtbetalingPerDag,
                        inntekter {
                            arbeidsgiver,
                            omregnetArsinntekt {
                                kilde,
                                belop,
                                manedsbelop,
                                inntekterFraAOrdningen {
                                    maned,
                                    sum
                                }
                            },
                            sammenligningsgrunnlag {
                                belop,
                                inntekterFraAOrdningen {
                                    maned,
                                    sum
                                }
                            }
                        },
                        oppfyllerKravOmMinstelonn,
                        grunnbelop
                    }
                }
            }
        """.trimIndent()

        testServer.httpPost(
            path = "/graphql",
            body = """{"query": "$query"}"""
        ) {
            objectMapper.readTree(this).get("data").get("person").get("inntektsgrunnlag").let { inntektsgrunnlag ->
                assertEquals(1, inntektsgrunnlag.size())
                assertEquals(9, inntektsgrunnlag.get(0).size())
            }
        }
    }

    @Test
    fun `generasjoner med person-resolver`() {
        val query = """
            {
                person(fnr: ${UNG_PERSON_FNR.toLong()}) {
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
                                behandlingstype,
                                periodetype,
                                inntektskilde,
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
                                    vilkarsgrunnlaghistorikkId,
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
    fun `vilkårsgrunnlag med person-resolver`() {
        val query = """
            {
                person(fnr: ${UNG_PERSON_FNR.toLong()}) {
                    arbeidsgivere {
                        generasjoner {
                            perioder {
                                ... on GraphQLBeregnetPeriode {
                                    vilkarsgrunnlaghistorikkId
                                }
                            }
                        }
                    },
                    vilkarsgrunnlaghistorikk {
                        id,
                        grunnlag {
                            skjaeringstidspunkt,
                            omregnetArsinntekt,
                            sammenligningsgrunnlag,
                            sykepengegrunnlag,
                            inntekter {
                                arbeidsgiver,
                                omregnetArsinntekt {
                                    kilde,
                                    belop,
                                    manedsbelop,
                                    inntekterFraAOrdningen {
                                        maned,
                                        sum
                                    }
                                },
                                sammenligningsgrunnlag
                            },
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
                val vilkårsId: String =
                    person.get("arbeidsgivere").get(0).get("generasjoner").get(0).get("perioder").get(0).get("vilkarsgrunnlaghistorikkId").asText()
                val vilkårsgrunnlaghistorikk = person.get("vilkarsgrunnlaghistorikk")
                assertEquals(1, vilkårsgrunnlaghistorikk.size())
                assertEquals(vilkårsId, vilkårsgrunnlaghistorikk.get(0).get("id").asText())
                assertEquals(6, vilkårsgrunnlaghistorikk.get(0).get("grunnlag").get(0).size())
            }
        }
    }

    @Test
    fun `tester generasjon-resolver`() {
        val query = """
            {
                generasjon(fnr: ${UNG_PERSON_FNR.toLong()}, orgnr: \"$ORGNUMMER\", indeks: 0) {
                    fom,
                    tom
                }
            }
        """.trimIndent()

        testServer.httpPost(
            path = "/graphql",
            body = """{"query": "$query"}"""
        )
    }
}
