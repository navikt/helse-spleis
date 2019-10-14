package no.nav.helse.component.person

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.ApplicationStarted
import io.mockk.mockk
import kafka.tools.ConsoleConsumer.addShutdownHook
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.Topics
import no.nav.helse.Topics.inntektsmeldingTopic
import no.nav.helse.Topics.sykmeldingTopic
import no.nav.helse.Topics.søknadTopic
import no.nav.helse.component.PersonRepositoryPostgresTest
import no.nav.helse.createHikariConfig
import no.nav.helse.inntektsmelding.InntektsmeldingConsumer
import no.nav.helse.person.PersonMediator
import no.nav.helse.person.PersonPostgresRepository
import no.nav.helse.person.PersonRepository
import no.nav.helse.sakskompleks.SakskompleksProbe
import no.nav.helse.sakskompleks.db.runMigration
import no.nav.helse.søknad.SøknadConsumer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection


internal class PersonComponentTest {

    companion object {

        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        private val embeddedEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = false,
            withSchemaRegistry = false,
            withSecurity = true,
            topicNames = listOf(sykmeldingTopic, inntektsmeldingTopic, søknadTopic)
        )

        private lateinit var embeddedPostgres: EmbeddedPostgres
        private lateinit var postgresConnection: Connection

        private lateinit var hikariConfig: HikariConfig

        @BeforeAll
        @JvmStatic
        internal fun `start embedded environment`() {
            embeddedPostgres = EmbeddedPostgres.builder().start()
            postgresConnection = embeddedPostgres.postgresDatabase.connection
            hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
            runMigration(HikariDataSource(hikariConfig))
            embeddedEnvironment.start()

        }

        @AfterAll
        @JvmStatic
        internal fun `stop embedded environment`() {
            embeddedEnvironment.tearDown()
            postgresConnection.close()
            embeddedPostgres.close()
        }


    }

    @Test
    internal fun `testtopology`() {
        val repo = PersonPostgresRepository(HikariDataSource(hikariConfig))

        val personMediator = PersonMediator(
            personRepository = repo
        )

    }
}
