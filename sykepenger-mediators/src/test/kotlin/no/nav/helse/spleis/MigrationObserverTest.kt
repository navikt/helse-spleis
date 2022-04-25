package no.nav.helse.spleis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.spleis.db.EndretVedtaksperiodeDao
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
    private lateinit var endretVedtaksperiodeDao: EndretVedtaksperiodeDao

    @BeforeAll
    internal fun setupAll() {
        dataSource = SpleisDataSource.migratedDb
        slettetVedtaksperiodeDao = SlettetVedtaksperiodeDao(dataSource)
        endretVedtaksperiodeDao = EndretVedtaksperiodeDao(dataSource)
    }

    @BeforeEach
    internal fun setupEach() {
        resetDatabase()
    }

    @Test
    fun `Lagrer slettede vedtaksperioder som har blitt observert til db`() {
        val migrationObserver = MigrationObserver(slettetVedtaksperiodeDao, endretVedtaksperiodeDao, Testhendelse())
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

    @Test
    fun `Lagrer endrede vedtaksperioder som har blitt observert til db`() {
        val migrationObserver = MigrationObserver(slettetVedtaksperiodeDao, endretVedtaksperiodeDao, Testhendelse())
        val vedtaksperiodeId = UUID.randomUUID()
        val gammelTilstand = "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP"
        val nyTilstand = "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK"

        migrationObserver.vedtaksperiodeEndret(vedtaksperiodeId, gammelTilstand, nyTilstand)
        assertEquals(
            listOf(mapOf(
                "id" to vedtaksperiodeId.toString(),
                "gammelTilstand" to gammelTilstand,
                "nyTilstand" to nyTilstand

            )),
            hentEndredeVedtaksperioder(FØDSELSNUMMER)
        )
    }

    private fun hentSlettedeVedtaksperioder(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        session.run(queryOf("SELECT * FROM slettet_vedtaksperiode WHERE fodselsnummer = ?", fødselsnummer).map {
            it.string("vedtaksperiode_id") to objectMapper.readTree(it.string("data"))
        }.asList)
    }

    private fun hentEndredeVedtaksperioder(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        session.run(queryOf("SELECT * FROM endret_vedtaksperiode WHERE fodselsnummer = ?", fødselsnummer).map {
            mapOf(
                "id" to it.string("vedtaksperiode_id"),
                "gammelTilstand" to it.string("gammel_tilstand"),
                "nyTilstand" to it.string("ny_tilstand")
            )
        }.asList)
    }

}