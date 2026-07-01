package no.nav.helse.spleis

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.test_support.TestDataSource
import java.util.UUID
import kotlin.test.assertEquals
import no.nav.helse.spleis.dao.SendtDao
import no.nav.helse.spleis.dao.SendtDao.Companion.responseJson
import no.nav.helse.spleis.dao.SendtMelding
import no.nav.helse.spleis.dao.SendteMeldinger
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

internal class SendtDaoTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = databaseContainer.nyTilkobling()
    }

    @AfterEach
    fun teardown() {
        databaseContainer.droppTilkobling(dataSource)
    }

    @Test
    fun `Henter opp sendte meldinger`() {
        val forårsaketAv = UUID.randomUUID()
        val dao = SendtDao(dataSource::ds)

        stappInnSendtMelding(forårsaketAv)

        assertEquals(SendteMeldinger(antallMeldinger = 0, meldinger = emptyList()), dao.sendteMeldinger(UUID.randomUUID()))

        val forventet = SendteMeldinger(
            antallMeldinger = 2,
            meldinger = listOf(
                SendtMelding(key = null, json = objectMapper.readTree("""{"testJson": true}""") as ObjectNode, mottaker = "RAPID"),
                SendtMelding(key = "foo-bar", json = objectMapper.readTree("""{"testSubsumsjon": "oui"}""") as ObjectNode, mottaker = "SUBSUMSJON"),
            )
        )
        assertEquals(forventet, dao.sendteMeldinger(forårsaketAv))

        @Language("JSON")
        val forventetJson = """
        {
          "antallMeldinger": 2,
          "meldinger": [
            {
              "key": null,
              "json": {
                "testJson": true
              },
              "mottaker": "RAPID"
            },
            {
              "key": "foo-bar",
              "json": {
                "testSubsumsjon": "oui"
              },
              "mottaker": "SUBSUMSJON"
            }
          ]
        }
        """

        JSONAssert.assertEquals(forventetJson, forventet.responseJson(), true)
    }

    private fun stappInnSendtMelding(forårsaketAv: UUID) {
        @Language("PostgreSQL")
        val sql = """
            INSERT INTO sendt (id, lopenummer, forarsaket_av, key, json, mottaker, opprettet, sendt)
            VALUES 
                (gen_random_uuid(), 1, :forarsaket_av, null, '{"testJson": true}'::jsonb, 'RAPID', now(), now()),
                (gen_random_uuid(), 2, :forarsaket_av, 'foo-bar', '{"testSubsumsjon": "oui"}'::jsonb, 'SUBSUMSJON', now(), now());
        """
        dataSource.ds.connection {
            prepareStatementWithNamedParameters(sql) {
                withParameter("forarsaket_av", forårsaketAv)
            }.use { it.execute() }
        }
    }

    private companion object {
        private val objectMapper = jacksonObjectMapper()
    }
}
