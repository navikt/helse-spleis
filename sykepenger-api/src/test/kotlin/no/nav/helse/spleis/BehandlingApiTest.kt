package no.nav.helse.spleis

import com.github.navikt.tbd_libs.naisful.test.TestContext
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.til
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.testhelpers.TestObservatør
import no.nav.helse.spleis.testhelpers.YrkesaktivitetHendelsefabrikk
import no.nav.helse.testdatabase.TestDataSource
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class BehandlingApiTest : AbstractSpleisApiTest() {
    @Test
    fun `utbetalingstidslinjen er lik godkjenningsbehovet, beriket med forespurt refusjon og kilde`() {
        observatør = TestObservatør()
        val eventBus = EventBus().apply { register(observatør) }
        val simuleringsutfisker = Simuleringsutfisker()
        eventBus.register(simuleringsutfisker)
        person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)

        spleisApiTestApplication(testdata = opprettPersonMedUtbetaling(eventBus)) {
            val behandlingId = simuleringsutfisker.behandlingId
            val body = hentBehandling("""{"fødselsnummer":"$UNG_PERSON_FNR","behandlingId":"$behandlingId"}""", HttpStatusCode.OK)
            val json = objectMapper.readTree(body)

            assertEquals(behandlingId.toString(), json["behandlingId"].asText())
            assertEquals(simuleringsutfisker.vedtaksperiodeId.toString(), json["vedtaksperiodeId"].asText())
            assertEquals(ORGNUMMER, json["organisasjonsnummer"].asText())
            assertEquals("ARBEIDSTAKER", json["yrkesaktivitetstype"].asText())
            assertFalse(json.has("refusjonstidslinje"))

            val godkjenningsbehov = eventBus.events.filterIsInstance<EventSubscription.GodkjenningEvent>().last()
            assertEquals(behandlingId, godkjenningsbehov.behandlingId)
            val utbetalingstidslinje = json["utbetalingstidslinje"].toList()
            assertEquals(godkjenningsbehov.utbetalingsdager.size, utbetalingstidslinje.size)
            godkjenningsbehov.utbetalingsdager.zip(utbetalingstidslinje).forEach { (forventet, dag) ->
                assertEquals(forventet.dato.toString(), dag["dato"].asText())
                assertEquals(forventet.type.name, dag["type"].asText())
                assertEquals(forventet.beløpTilArbeidsgiver, dag["beløpTilArbeidsgiver"].asInt())
                assertEquals(forventet.beløpTilBruker, dag["beløpTilBruker"].asInt())
                assertEquals(forventet.sykdomsgrad, dag["sykdomsgrad"].asInt())
                assertEquals(forventet.dekningsgrad, dag["dekningsgrad"].asInt())
                assertEquals(forventet.begrunnelser.orEmpty().map { it.name }, dag["begrunnelser"].map { it.asText() })
            }

            val navDag = utbetalingstidslinje.first { it["type"].asText() == "NavDag" }
            assertEquals(1431, navDag["beløpTilArbeidsgiver"].asInt())
            assertEquals(1431, navDag["forespurtRefusjon"]["beløp"].asInt())
            assertEquals("ARBEIDSGIVER", navDag["forespurtRefusjon"]["avsender"].asText())
            assertEquals(arbeidsgiveropplysningerId.toString(), navDag["forespurtRefusjon"]["sasInternInntektsmeldingReferanse"].asText())
            assertTrue(
                utbetalingstidslinje.all {
                    it["forespurtRefusjon"]["sasInternInntektsmeldingReferanse"].asText() ==
                        arbeidsgiveropplysningerId.toString()
                },
            )
        }
    }

    private val arbeidsgiveropplysningerId = UUID.randomUUID()

    @Test
    fun `ukjent person gir 404`() =
        spleisApiTestApplication {
            hentBehandling("""{"fødselsnummer":"40440440440","behandlingId":"${UUID.randomUUID()}"}""", HttpStatusCode.NotFound)
        }

    @Test
    fun `ukjent behandling gir 404`() {
        val tomPerson = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)

        spleisApiTestApplication(testdata = { lagrePerson(it.ds, UNG_PERSON_FNR, tomPerson) }) {
            val body =
                hentBehandling("""{"fødselsnummer":"$UNG_PERSON_FNR","behandlingId":"${UUID.randomUUID()}"}""", HttpStatusCode.NotFound)
            assertFalse(body.contains(UNG_PERSON_FNR))
        }
    }

    @Test
    fun `ugyldig fødselsnummer gir 400`() =
        spleisApiTestApplication {
            hentBehandling("""{"fødselsnummer":"tullball","behandlingId":"${UUID.randomUUID()}"}""", HttpStatusCode.BadRequest)
            hentBehandling("""{"fødselsnummer":"123","behandlingId":"${UUID.randomUUID()}"}""", HttpStatusCode.BadRequest)
            hentBehandling("""{"behandlingId":"${UUID.randomUUID()}"}""", HttpStatusCode.BadRequest)
        }

    @Test
    fun `ugyldig behandlingId gir 400`() =
        spleisApiTestApplication {
            hentBehandling("""{"fødselsnummer":"40440440440","behandlingId":"tullball"}""", HttpStatusCode.BadRequest)
            hentBehandling("""{"fødselsnummer":"40440440440"}""", HttpStatusCode.BadRequest)
        }

    // Samme flyt som opprettTestdata, men uten å lagre meldinger: melding-id-ene er delt på tvers av testklasser
    private fun opprettPersonMedUtbetaling(eventBus: EventBus): (TestDataSource) -> Unit =
        { testDataSource ->
            person.håndterSykmelding(eventBus, sykmelding(), Aktivitetslogg())
            person.håndterUtbetalingshistorikkEtterInfotrygdendring(eventBus, utbetalinghistorikk(), Aktivitetslogg())
            person.håndterSøknad(eventBus, søknad(), Aktivitetslogg())
            val vedtaksperiodeId =
                eventBus.events
                    .filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>()
                    .single()
                    .vedtaksperiodeId
            person.håndterArbeidsgiveropplysninger(
                eventBus,
                YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(ORGNUMMER)).lagArbeidsgiveropplysninger(
                    arbeidsgiverperioder = listOf(FOM til FOM.plusDays(15)),
                    vedtaksperiodeId = vedtaksperiodeId,
                    beregnetInntekt = 31000.månedlig,
                    id = arbeidsgiveropplysningerId,
                ),
                Aktivitetslogg(),
            )
            person.håndterYtelser(eventBus, ytelser(), Aktivitetslogg())
            person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag(), Aktivitetslogg())
            val simuleringsutfisker = Simuleringsutfisker()
            eventBus.register(simuleringsutfisker)
            person.håndterYtelser(eventBus, ytelser(), Aktivitetslogg())
            val behandlingId = simuleringsutfisker.behandlingId
            val utbetalingId = simuleringsutfisker.utbetalingId
            val fagsystemId = simuleringsutfisker.fagsystemId
            person.håndterSimulering(
                eventBus,
                simulering(utbetalingId = utbetalingId, fagsystemId = fagsystemId, fagområde = simuleringsutfisker.fagområde),
                Aktivitetslogg(),
            )
            person.håndterUtbetalingsgodkjenning(
                eventBus,
                utbetalingsgodkjenning(behandlingId = behandlingId, utbetalingId = utbetalingId),
                Aktivitetslogg(),
            )
            person.håndterUtbetalingHendelse(
                eventBus,
                utbetaling(
                    vedtaksperiodeId = vedtaksperiodeId,
                    behandlingId = behandlingId,
                    utbetalingId = utbetalingId,
                    fagsystemId = fagsystemId,
                ),
                Aktivitetslogg(),
            )
            lagrePerson(testDataSource.ds, UNG_PERSON_FNR, person)
        }

    private suspend fun TestContext.hentBehandling(
        body: String,
        forventetStatus: HttpStatusCode,
    ): String {
        val response =
            client.post("/api/behandling") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        val responseBody = response.bodyAsText()
        assertEquals(forventetStatus, response.status, responseBody)
        return responseBody
    }
}
