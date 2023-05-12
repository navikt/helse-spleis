package no.nav.helse.spleis

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spleis.dao.HendelseDao
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
internal class HendelseDaoTest {

    private val UNG_PERSON_FNR = "12029240045"
    private val meldingsReferanse = UUID.randomUUID()
    private lateinit var dataSource: DataSource

    @BeforeAll
    fun setupDB() {
        dataSource = DB.migrate()
    }
    @BeforeEach
    internal fun setup() {
        DB.clean()
        dataSource = DB.migrate()
        dataSource.lagreHendelse(meldingsReferanse)
    }

    private fun DataSource.lagreHendelse(
        meldingsReferanse: UUID,
        meldingstype: HendelseDao.Meldingstype = HendelseDao.Meldingstype.INNTEKTSMELDING,
        fødselsnummer: String = UNG_PERSON_FNR,
        data: String = """{ "@opprettet": "${LocalDateTime.now()}" }"""
    ) {
        sessionOf(this).use {
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
        val ingenEvents = dao.hentHendelser(UNG_PERSON_FNR.toLong())
        Assertions.assertEquals(1, ingenEvents.size)
    }

    @Test
    fun `hentHendelse sql er valid`() {
        val dao = HendelseDao(dataSource)
        val event = dao.hentHendelse(meldingsReferanse)
        Assertions.assertNotNull(event)
    }
}
