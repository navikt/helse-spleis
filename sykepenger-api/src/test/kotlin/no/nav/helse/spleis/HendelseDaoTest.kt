package no.nav.helse.spleis

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.dao.HendelseDao
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.*
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*
import javax.sql.DataSource

@TestInstance(Lifecycle.PER_CLASS)
class HendelseDaoTest {

    private val UNG_PERSON_FNR = "12029240045"
    private val postgres = PostgreSQLContainer<Nothing>("postgres:13")
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway
    private val meldingsReferanse = UUID.randomUUID()

    @BeforeAll
    internal fun `start embedded environment`() {
        postgres.start()

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 3
            minimumIdle = 1
            initializationFailTimeout = 5000
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        })
        flyway = Flyway
            .configure()
            .dataSource(dataSource)
            .load()

    }

    @BeforeEach
    internal fun setup() {
        flyway.clean()
        flyway.migrate()
        dataSource.lagreHendelse(meldingsReferanse)
    }


    @AfterAll
    internal fun `stop embedded environment`() {
        postgres.stop()
    }

    private fun DataSource.lagreHendelse(
        meldingsReferanse: UUID,
        meldingstype: HendelseDao.Meldingstype = HendelseDao.Meldingstype.INNTEKTSMELDING,
        fødselsnummer: String = UNG_PERSON_FNR,
        data: String = "{}"
    ) {
        using(sessionOf(this)) {
            it.run(
                queryOf(
                    "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (?, ?, ?, (to_json(?::json)))",
                    fødselsnummer.toLong(),
                    meldingsReferanse.toString(),
                    meldingstype.toString(),
                    data
                ).asExecute
            )
        }
    }

    @Test
    fun `hentAlleHendelser sql er valid`() {
        val dao = HendelseDao(dataSource)
        val events = dao.hentAlleHendelser(UNG_PERSON_FNR.toLong())
        Assertions.assertEquals(1, events.size)
    }

    @Test
    fun `hentHendelser sql er valid`() {
        val dao = HendelseDao(dataSource)
        val ingenEvents = dao.hentHendelser(setOf(meldingsReferanse))
        Assertions.assertEquals(1, ingenEvents.size)
    }

    @Test
    fun `hentHendelse sql er valid`() {
        val dao = HendelseDao(dataSource)
        val event = dao.hentHendelse(meldingsReferanse)
        Assertions.assertNotNull(event)
    }
}
