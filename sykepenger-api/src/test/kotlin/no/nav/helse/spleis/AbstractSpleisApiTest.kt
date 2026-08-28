package no.nav.helse.spleis

import com.github.navikt.tbd_libs.naisful.test.TestContext
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import com.github.navikt.tbd_libs.result_object.ok
import com.github.navikt.tbd_libs.speed.IdentResponse
import com.github.navikt.tbd_libs.speed.SpeedClient
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.long
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import com.github.navikt.tbd_libs.sql_dsl.transaction
import com.github.navikt.tbd_libs.test_support.TestDataSource
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.til
import no.nav.helse.person.EventBus
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.testhelpers.YrkesaktivitetHendelsefabrikk
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.intellij.lang.annotations.Language

/**
 * Felles testoppsett for api-ene i sykepenger-api: starter opp applikasjonsmodulen mot en
 * testdatabase, med en falsk autentisering som alltid slipper igjennom.
 */
internal abstract class AbstractSpleisApiTest : AbstractObservableTest() {

    protected fun speedClient() = mockk<SpeedClient> {
        every { hentFødselsnummerOgAktørId(any(), any()) } returns IdentResponse(
            fødselsnummer = UNG_PERSON_FNR,
            aktørId = "ikke_kult",
            npid = null,
            kilde = IdentResponse.KildeResponse.PDL
        ).ok()
    }

    protected fun spleisApiTestApplication(
        speedClient: SpeedClient = speedClient(),
        spekematClient: SpekematClient = mockk<SpekematClient>(),
        testdata: (TestDataSource) -> Unit = { },
        testblokk: suspend TestContext.() -> Unit
    ) {
        val testDataSource = databaseContainer.nyTilkobling()
        testdata(testDataSource)
        lagTestapplikasjon(speedClient = speedClient, spekematClient = spekematClient, testDataSource = testDataSource, testblokk = testblokk)
        databaseContainer.droppTilkobling(testDataSource)
    }

    private fun lagTestapplikasjon(speedClient: SpeedClient, spekematClient: SpekematClient, testDataSource: TestDataSource, testblokk: suspend TestContext.() -> Unit) {
        val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        naisfulTestApp(
            testApplicationModule = {
                authentication {
                    // setter opp en falsk autentisering som alltid svarer med en principal
                    // uavhengig om requesten inneholder bearer eller ei
                    provider {
                        authenticate { context ->
                            context.principal(JWTPrincipal(LokalePayload(mapOf("azp_name" to "spesialist"))))
                        }
                    }
                }
                val dataSource = testDataSource.ds
                lagApplikasjonsmodul(speedClient, spekematClient, { dataSource }, meterRegistry)
            },
            objectMapper = objectMapper,
            meterRegistry = meterRegistry,
            testblokk = testblokk
        )
    }

    protected class Simuleringsutfisker : EventSubscription {
        lateinit var vedtaksperiodeId: UUID private set
        lateinit var behandlingId: UUID private set
        lateinit var utbetalingId: UUID private set
        lateinit var fagsystemId: String private set
        lateinit var fagområde: String private set

        override fun simuler(event: EventSubscription.SimuleringEvent) {
            vedtaksperiodeId = event.vedtaksperiodeId
            behandlingId = event.behandlingId
            utbetalingId = event.utbetalingId
            fagsystemId = event.oppdragsdetaljer.fagsystemId
            fagområde = event.oppdragsdetaljer.fagområde
        }
    }

    protected fun opprettTestdata(eventBus: EventBus): (TestDataSource) -> Unit {
        return fun(testDataSource: TestDataSource) {
            person.håndterSykmelding(eventBus, sykmelding(), Aktivitetslogg())
            person.håndterUtbetalingshistorikkEtterInfotrygdendring(eventBus, utbetalinghistorikk(), Aktivitetslogg())
            person.håndterSøknad(eventBus, søknad(), Aktivitetslogg())
            val vedtaksperiodeId = eventBus.events.filterIsInstance<EventSubscription.VedtaksperiodeOpprettet>().single().vedtaksperiodeId
            person.håndterArbeidsgiveropplysninger(
                eventBus,
                YrkesaktivitetHendelsefabrikk(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(ORGNUMMER)).lagArbeidsgiveropplysninger(
                    arbeidsgiverperioder = listOf(FOM til FOM.plusDays(15)),
                    vedtaksperiodeId = vedtaksperiodeId,
                    beregnetInntekt = 31000.månedlig,
                    id = INNTEKTSMELDING_ID
                ),
                Aktivitetslogg()
            )
            person.håndterYtelser(eventBus, ytelser(), Aktivitetslogg())
            person.håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag(), Aktivitetslogg())
            val ytelser = ytelser()
            val aktivitetslogg = Aktivitetslogg()
            val simuleringsutfisker = Simuleringsutfisker()
            eventBus.register(simuleringsutfisker)
            person.håndterYtelser(eventBus, ytelser, aktivitetslogg)
            val behandlingId = simuleringsutfisker.behandlingId
            val utbetalingId = simuleringsutfisker.utbetalingId
            val fagsystemId = simuleringsutfisker.fagsystemId
            val fagområde = simuleringsutfisker.fagområde
            person.håndterSimulering(eventBus, simulering(utbetalingId = utbetalingId, fagsystemId = fagsystemId, fagområde = fagområde), Aktivitetslogg())
            person.håndterUtbetalingsgodkjenning(eventBus, utbetalingsgodkjenning(behandlingId = behandlingId, utbetalingId = utbetalingId), Aktivitetslogg())
            person.håndterUtbetalingHendelse(eventBus, utbetaling(vedtaksperiodeId = vedtaksperiodeId, behandlingId = behandlingId, utbetalingId = utbetalingId, fagsystemId = fagsystemId), Aktivitetslogg())

            lagrePerson(testDataSource.ds, UNG_PERSON_FNR, person)
            lagreSykmelding(
                dataSource = testDataSource.ds,
                fødselsnummer = UNG_PERSON_FNR,
                meldingsReferanse = SYKMELDING_ID,
                fom = FOM,
                tom = TOM
            )
            lagreSøknadNav(
                dataSource = testDataSource.ds,
                fødselsnummer = UNG_PERSON_FNR,
                meldingsReferanse = SØKNAD_ID,
                fom = FOM,
                tom = TOM,
                sendtNav = TOM.plusDays(1).atStartOfDay()
            )
            lagreInntektsmelding(
                dataSource = testDataSource.ds,
                fødselsnummer = UNG_PERSON_FNR,
                meldingsReferanse = INNTEKTSMELDING_ID,
                beregnetInntekt = INNTEKT,
                førsteFraværsdag = FOM
            )
        }
    }

    protected fun lagrePerson(dataSource: DataSource, fødselsnummer: String, person: Person) {
        val serialisertPerson = person.dto().tilPersonData().tilSerialisertPerson()
        dataSource.connection {
            transaction {
                @Language("PostgreSQL")
                val opprettPerson = "INSERT INTO person(skjema_versjon, fnr, data) VALUES(:skjemaVersjon, :fnr, :data) RETURNING id"
                val personId = prepareStatementWithNamedParameters(opprettPerson) {
                    withParameter("fnr", fødselsnummer.toLong())
                    withParameter("skjemaVersjon", serialisertPerson.skjemaVersjon)
                    withParameter("data", serialisertPerson.json)
                }.use {
                    it.executeQuery().use { rs ->
                        rs.single { it.long(1) }
                    }
                }

                @Language("PostgreSQL")
                val opprettPersonAlias = "INSERT INTO person_alias (fnr, person_id) VALUES (:fnr, :personId)"
                prepareStatementWithNamedParameters(opprettPersonAlias) {
                    withParameter("fnr", fødselsnummer.toLong())
                    withParameter("personId", personId)
                }.use {
                    it.execute()
                }
            }
        }
    }

    protected fun lagreHendelse(
        dataSource: DataSource,
        fødselsnummer: String,
        meldingsReferanse: UUID,
        meldingstype: HendelseDao.Meldingstype = HendelseDao.Meldingstype.NAV_NO_INNTEKTSMELDING,
        data: String = "{}"
    ) {
        dataSource.connection {
            @Language("PostgreSQL")
            val opprettMelding = "INSERT INTO melding(fnr, melding_id, melding_type, data, behandlet_tidspunkt) VALUES(:fnr, :meldingId, :meldingType, cast(:data as json), now())"
            prepareStatementWithNamedParameters(opprettMelding) {
                withParameter("fnr", fødselsnummer.toLong())
                withParameter("meldingId", meldingsReferanse)
                withParameter("meldingType", meldingstype.name)
                withParameter("data", data)
            }.use { it.execute() }
        }
    }

    protected fun lagreInntektsmelding(dataSource: DataSource, fødselsnummer: String, meldingsReferanse: UUID, beregnetInntekt: Inntekt, førsteFraværsdag: LocalDate) {
        lagreHendelse(
            dataSource = dataSource,
            fødselsnummer = fødselsnummer,
            meldingsReferanse = meldingsReferanse,
            meldingstype = HendelseDao.Meldingstype.NAV_NO_INNTEKTSMELDING,
            data = """
                {
                    "beregnetInntekt": "$beregnetInntekt",
                    "mottattDato": "${LocalDateTime.now()}",
                    "@opprettet": "${LocalDateTime.now()}",
                    "foersteFravaersdag": "$førsteFraværsdag",
                    "@id": "$meldingsReferanse"
                }
            """.trimIndent()
        )
    }

    protected fun lagreSykmelding(dataSource: DataSource, fødselsnummer: String, meldingsReferanse: UUID, fom: LocalDate, tom: LocalDate) {
        lagreHendelse(
            dataSource = dataSource,
            fødselsnummer = fødselsnummer,
            meldingsReferanse = meldingsReferanse,
            meldingstype = HendelseDao.Meldingstype.NY_SØKNAD,
            data = """
                {
                    "@opprettet": "${LocalDateTime.now()}",
                    "@id": "$meldingsReferanse",
                    "fom": "$fom",
                    "tom": "$tom"
                }
            """.trimIndent()
        )
    }

    protected fun lagreSøknadNav(dataSource: DataSource, fødselsnummer: String, meldingsReferanse: UUID, fom: LocalDate, tom: LocalDate, sendtNav: LocalDateTime) {
        lagreHendelse(
            dataSource = dataSource,
            fødselsnummer = fødselsnummer,
            meldingsReferanse = meldingsReferanse,
            meldingstype = HendelseDao.Meldingstype.SENDT_SØKNAD_NAV,
            data = """
                {
                    "@opprettet": "${LocalDateTime.now()}",
                    "@id": "$meldingsReferanse",
                    "fom": "$fom",
                    "tom": "$tom",
                    "sendtNav": "$sendtNav"
                }
            """.trimIndent()
        )
    }
}
