package no.nav.helse.spleis

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.test_support.TestDataSource
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.spleis.dao.HendelseDao
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class HendelseDaoTest {

    private val UNG_PERSON_FNR = "12029240045"
    private val meldingsReferanse = UUID.randomUUID()
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    internal fun setup() {
        dataSource = databaseContainer.nyTilkobling()
        dataSource.ds.lagreHendelse(meldingsReferanse)
    }

    @AfterEach
    fun teardown() {
        databaseContainer.droppTilkobling(dataSource)
    }

    private fun DataSource.lagreHendelse(
        meldingsReferanse: UUID,
        meldingstype: HendelseDao.Meldingstype = HendelseDao.Meldingstype.INNTEKTSMELDING,
        fødselsnummer: String = UNG_PERSON_FNR,
        data: String = """{ "@opprettet": "${LocalDateTime.now()}" }"""
    ) {
        @Language("PostgreSQL")
        val sql = "INSERT INTO melding (fnr, melding_id, melding_type, data) VALUES (:fnr, :meldingId, :meldingType, cast(:data as json))"
        this.connection {
            prepareStatementWithNamedParameters(sql) {
                withParameter("fnr", fødselsnummer.toLong())
                withParameter("meldingId", meldingsReferanse)
                withParameter("meldingType", meldingstype.name)
                withParameter("data", data)
            }.use { it.execute() }
        }
    }

    @Test
    fun `hentHendelser sql er valid`() {
        val dao = HendelseDao(dataSource::ds, mockk(relaxed = true))
        val ingenEvents = dao.hentHendelser(UNG_PERSON_FNR.toLong())
        Assertions.assertEquals(1, ingenEvents.size)
    }

    @Test
    fun `hentHendelse sql er valid`() {
        val dao = HendelseDao(dataSource::ds, mockk(relaxed = true))
        val event = dao.hentHendelse(meldingsReferanse)
        Assertions.assertNotNull(event)
    }
}
