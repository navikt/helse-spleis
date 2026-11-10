package no.nav.helse.spleis.maksdato

import com.github.navikt.tbd_libs.naisful.test.TestContext
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.til
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.AbstractSpleisApiTest
import no.nav.helse.spleis.Applikasjonsservere
import no.nav.helse.spleis.objectMapper
import no.nav.helse.spleis.testhelpers.TestObservatør
import no.nav.helse.spleis.testhelpers.YrkesaktivitetHendelsefabrikk
import no.nav.helse.testdatabase.TestDataSource
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.sql.DataSource

internal class MaksdatoApiTest : AbstractSpleisApiTest() {
    private companion object {
        // 248 dager minus de 10 ukedagene 17.–30. januar 2018 etter arbeidsgiverperioden
        const val GJENSTÅENDE_DAGER_ETTER_VEDTAKET = 238
    }

    private lateinit var eventBus: EventBus

    @BeforeEach
    fun setup() {
        observatør = TestObservatør()
        eventBus = EventBus().apply { register(observatør) }
        person = Person(Personidentifikator(UNG_PERSON_FNR), UNG_PERSON_FØDSELSDATO.alder, EmptyLog)
    }

    @Test
    fun `uten nye Infotrygd-dager er svaret likt tallet i siste vedtak`() =
        spleisApiTestApplication(testdata = opprettPerson()) {
            val svar = hentMaksdato(infotrygdperioder("""{"fom":"2018-02-05","tom":"2018-02-09","type":"FERIE"}"""))

            assertEquals(gjenståendeDagerIVedtaket(), svar.gjenståendeDager)
            assertEquals(GJENSTÅENDE_DAGER_ETTER_VEDTAKET, svar.gjenståendeDager)
            assertEquals(TOM.toString(), svar.vurdertTilOgMed)
            assertEquals(vedtaket().maksdato.toString(), svar.maksdato)
        }

    @Test
    fun `ny Infotrygd-periode etter vedtaket trekker fra ukedagene i perioden`() =
        spleisApiTestApplication(testdata = opprettPerson()) {
            val svar =
                hentMaksdato(
                    infotrygdperioder("""{"fom":"2018-02-05","tom":"2018-02-18","type":"UTBETALING","organisasjonsnummer":"123456789"}"""),
                )

            assertEquals(GJENSTÅENDE_DAGER_ETTER_VEDTAKET - 10, svar.gjenståendeDager)
            assertEquals("2018-02-18", svar.vurdertTilOgMed)
        }

    @Test
    fun `refusjon til arbeidsgiver i Infotrygd teller som brukte dager`() =
        spleisApiTestApplication(testdata = opprettPerson()) {
            val svar =
                hentMaksdato(
                    infotrygdperioder(
                        """{"fom":"2018-02-05","tom":"2018-02-09","type":"ARB_REF"}""",
                        """{"fom":"2018-02-12","tom":"2018-02-16","type":"REDUKSJON_MEDLEM"}""",
                    ),
                )

            assertEquals(GJENSTÅENDE_DAGER_ETTER_VEDTAKET - 10, svar.gjenståendeDager)
            assertEquals("2018-02-16", svar.vurdertTilOgMed)
        }

    @Test
    fun `vurdertTilOgMed er siste dag i vedtaket når det slutter etter Infotrygd-periodene`() =
        spleisApiTestApplication(testdata = opprettPerson()) {
            val svar = hentMaksdato(infotrygdperioder("""{"fom":"2017-12-04","tom":"2017-12-08","type":"UTBETALING"}"""))

            assertEquals(GJENSTÅENDE_DAGER_ETTER_VEDTAKET - 5, svar.gjenståendeDager)
            assertEquals(TOM.toString(), svar.vurdertTilOgMed)
        }

    @Test
    fun `ferie og tilbakeførte perioder gir ingen brukte dager`() =
        spleisApiTestApplication(testdata = opprettPerson()) {
            val svar =
                hentMaksdato(
                    infotrygdperioder(
                        """{"fom":"2018-02-05","tom":"2018-02-09","type":"FERIE"}""",
                        """{"fom":"2018-02-12","tom":"2018-02-16","type":"TILBAKEFØRT"}""",
                        """{"fom":"2018-02-19","tom":"2018-02-23","type":"UKJENT"}""",
                    ),
                )

            assertEquals(GJENSTÅENDE_DAGER_ETTER_VEDTAKET, svar.gjenståendeDager)
            assertEquals(TOM.toString(), svar.vurdertTilOgMed)
        }

    @Test
    fun `en revurdering som ikke er vedtatt teller ikke med`() =
        spleisApiTestApplication(
            testdata =
                opprettPerson {
                    person.håndterOverstyrTidslinje(
                        eventBus,
                        YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(ORGNUMMER)).lagHåndterOverstyrTidslinje(
                            (29..30).map { ManuellOverskrivingDag(LocalDate.of(2018, 1, it), Dagtype.Feriedag) },
                        ),
                        Aktivitetslogg(),
                    )
                    person.håndterYtelser(eventBus, ytelser(), Aktivitetslogg())
                },
        ) {
            val svar = hentMaksdato(infotrygdperioder("""{"fom":"2018-02-05","tom":"2018-02-09","type":"FERIE"}"""))

            assertEquals(GJENSTÅENDE_DAGER_ETTER_VEDTAKET, svar.gjenståendeDager)
            assertEquals(TOM.toString(), svar.vurdertTilOgMed)
        }

    @Test
    fun `behandling som ikke er vedtatt teller ikke med`() =
        spleisApiTestApplication(testdata = opprettPerson(fattVedtak = false)) {
            val svar = hentMaksdato(infotrygdperioder("""{"fom":"2017-12-04","tom":"2017-12-08","type":"UTBETALING"}"""))

            assertEquals(243, svar.gjenståendeDager)
            assertEquals("2017-12-08", svar.vurdertTilOgMed)
        }

    @Test
    fun `periode med tom før fom blir oversett, som i Sykepengehistorikk-behovet`() =
        spleisApiTestApplication(testdata = opprettPerson()) {
            val svar = hentMaksdato(infotrygdperioder("""{"fom":"2018-02-09","tom":"2018-02-05","type":"UTBETALING"}"""))

            assertEquals(GJENSTÅENDE_DAGER_ETTER_VEDTAKET, svar.gjenståendeDager)
            assertEquals(TOM.toString(), svar.vurdertTilOgMed)
        }

    @Test
    fun `personen i databasen er uendret etter kallet`() =
        spleisApiTestApplication(testdata = opprettPerson()) {
            val før = lagretPerson()

            hentMaksdato(infotrygdperioder("""{"fom":"2018-02-05","tom":"2018-02-18","type":"UTBETALING"}"""))

            assertEquals(før, lagretPerson())
        }

    @Test
    fun `ugyldig request gir 400`() =
        spleisApiTestApplication(testdata = opprettPerson()) {
            val periode = """{"fom":"2018-02-05","tom":"2018-02-09","type":"UTBETALING"}"""
            post("""{"fødselsnummer":"$UNG_PERSON_FNR","infotrygdperioder":[]}""", HttpStatusCode.BadRequest)
            post("""{"fødselsnummer":"$UNG_PERSON_FNR"}""", HttpStatusCode.BadRequest)
            post("""{"infotrygdperioder":[$periode]}""", HttpStatusCode.BadRequest)
            post(infotrygdperioder("""{"tom":"2018-02-09","type":"UTBETALING"}"""), HttpStatusCode.BadRequest)
            post(infotrygdperioder("""{"fom":"2018-02-05","type":"UTBETALING"}"""), HttpStatusCode.BadRequest)
            post(infotrygdperioder("""{"fom":"2018-02-05","tom":"2018-02-09"}"""), HttpStatusCode.BadRequest)
            post(infotrygdperioder("""{"fom":"tullball","tom":"2018-02-05","type":"UTBETALING"}"""), HttpStatusCode.BadRequest)
        }

    @Test
    fun `ukjent person gir 404`() =
        spleisApiTestApplication {
            post(
                """{"fødselsnummer":"40440440440","infotrygdperioder":[{"fom":"2018-02-05","tom":"2018-02-09","type":"UTBETALING"}]}""",
                HttpStatusCode.NotFound,
            )
        }

    @Test
    fun `krever gyldig access token`() {
        val applikasjonsservere = Applikasjonsservere()
        try {
            applikasjonsservere.kjørTest({}) {
                val periode = """{"fom":"2018-02-05","tom":"2018-02-09","type":"UTBETALING"}"""
                val body = """{"fødselsnummer":"40440440440","infotrygdperioder":[$periode]}"""
                post("/api/maksdato", body, HttpStatusCode.Unauthorized, accessToken = null)
                post("/api/maksdato", body, HttpStatusCode.Unauthorized, accessToken = issuer.accessToken { withAudience("feil_audience") })
                post("/api/maksdato", body, HttpStatusCode.NotFound, accessToken = issuer.accessToken())
            }
        } finally {
            applikasjonsservere.ryddOpp()
        }
    }

    private fun lagretPerson(): String =
        dataSource.connection {
            prepareStatementWithNamedParameters("SELECT data FROM person WHERE fnr = :fnr") {
                withParameter("fnr", UNG_PERSON_FNR.toLong())
            }.use { statement ->
                statement.executeQuery().use { rs -> rs.single { it.getString("data") } }
            }
        }

    private fun infotrygdperioder(vararg perioder: String) =
        """{"fødselsnummer":"$UNG_PERSON_FNR","infotrygdperioder":[${perioder.joinToString()}]}"""

    private fun vedtaket() =
        eventBus.events
            .filterIsInstance<EventSubscription.UtbetalingUtbetaltEvent>()
            .single()

    private fun gjenståendeDagerIVedtaket() = vedtaket().gjenståendeSykedager

    private data class Svar(
        val gjenståendeDager: Int,
        val vurdertTilOgMed: String,
        val maksdato: String,
    )

    private suspend fun TestContext.hentMaksdato(body: String): Svar {
        val json = objectMapper.readTree(post(body, HttpStatusCode.OK))
        return Svar(json["gjenståendeDager"].asInt(), json["vurdertTilOgMed"].asText(), json["maksdato"].asText())
    }

    private suspend fun TestContext.post(
        body: String,
        forventetStatus: HttpStatusCode,
    ): String {
        val response =
            client.post("/api/maksdato") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        val responseBody = response.bodyAsText()
        assertEquals(forventetStatus, response.status, responseBody)
        return responseBody
    }

    private lateinit var dataSource: DataSource

    // opprettTestdata i AbstractSpleisApiTest lagrer meldinger med faste id-er, som kolliderer mellom testene i den delte databasen
    private fun opprettPerson(
        fattVedtak: Boolean = true,
        førLagring: () -> Unit = {},
    ): (TestDataSource) -> Unit =
        { testDataSource ->
            dataSource = testDataSource.ds
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
                ),
                Aktivitetslogg(),
            )
            person.håndterYtelser(eventBus, ytelser(), Aktivitetslogg())
            person.håndterRettighetskategori(eventBus, rettighetskategori(), Aktivitetslogg())
            person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag(), Aktivitetslogg())
            val simuleringsutfisker = Simuleringsutfisker()
            eventBus.register(simuleringsutfisker)
            person.håndterYtelser(eventBus, ytelser(), Aktivitetslogg())
            person.håndterSimulering(
                eventBus,
                simulering(
                    utbetalingId = simuleringsutfisker.utbetalingId,
                    fagsystemId = simuleringsutfisker.fagsystemId,
                    fagområde = simuleringsutfisker.fagområde,
                ),
                Aktivitetslogg(),
            )
            if (fattVedtak) {
                person.håndterUtbetalingsgodkjenning(
                    eventBus,
                    utbetalingsgodkjenning(
                        behandlingId = simuleringsutfisker.behandlingId,
                        utbetalingId = simuleringsutfisker.utbetalingId,
                    ),
                    Aktivitetslogg(),
                )
                person.håndterUtbetalingHendelse(
                    eventBus,
                    utbetaling(
                        vedtaksperiodeId = vedtaksperiodeId,
                        behandlingId = simuleringsutfisker.behandlingId,
                        utbetalingId = simuleringsutfisker.utbetalingId,
                        fagsystemId = simuleringsutfisker.fagsystemId,
                    ),
                    Aktivitetslogg(),
                )
            }
            førLagring()
            lagrePerson(testDataSource.ds, UNG_PERSON_FNR, person)
        }
}
