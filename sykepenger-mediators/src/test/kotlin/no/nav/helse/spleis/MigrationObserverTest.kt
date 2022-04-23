package no.nav.helse.spleis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.spleis.db.SlettetVedtaksperiodeDao
import no.nav.helse.spleis.e2e.SpleisDataSource
import no.nav.helse.spleis.e2e.resetDatabase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrationObserverTest {
    private companion object {
        private const val FØDSELSNUMMER = "01011012345"
        private const val AKTØR = "aktørId"
        private const val ORGNR = "orgnr"
        private val MELDINGSREFERANSE = UUID.randomUUID()

        private val objectMapper = jacksonObjectMapper()
    }

    private class Testhendelse : ArbeidstakerHendelse(MELDINGSREFERANSE, FØDSELSNUMMER, AKTØR, ORGNR)

    private lateinit var dataSource: DataSource
    private lateinit var slettetVedtaksperiodeDao: SlettetVedtaksperiodeDao

    @BeforeAll
    internal fun setupAll() {
        dataSource = SpleisDataSource.migratedDb
        slettetVedtaksperiodeDao = SlettetVedtaksperiodeDao(dataSource)
    }

    @BeforeEach
    internal fun setupEach() {
        resetDatabase()
    }

    @Test
    fun `Lagrer slettede vedtaksperioder som har blitt observert til db`() {
        val migrationObserver = MigrationObserver(slettetVedtaksperiodeDao, Testhendelse())
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeNode = objectMapper.readTree(
            """{
                "id": "$vedtaksperiodeId",
                "tilstand": "AVVENTER_SØKNAD_FERDIG_FORLENGELSE"
            }"""
        )
        migrationObserver.vedtaksperiodeSlettet(vedtaksperiodeId, vedtaksperiodeNode)
        assertEquals(
            listOf(vedtaksperiodeId.toString() to vedtaksperiodeNode),
            hentSlettedeVedtaksperioder(FØDSELSNUMMER)
        )
    }

    private fun hentSlettedeVedtaksperioder(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        session.run(queryOf("SELECT * FROM slettet_vedtaksperiode WHERE fodselsnummer = ?", fødselsnummer).map {
            it.string("vedtaksperiode_id") to objectMapper.readTree(it.string("data"))
        }.asList)
    }

}