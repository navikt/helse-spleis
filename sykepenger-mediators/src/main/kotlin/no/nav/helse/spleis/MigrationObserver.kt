package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.person.PersonHendelse
import no.nav.helse.serde.migration.JsonMigrationObserver
import no.nav.helse.spleis.db.EndretVedtaksperiodeDao
import no.nav.helse.spleis.db.SlettetVedtaksperiodeDao

internal class MigrationObserver(
    private val slettetVedtaksperiodeDao: SlettetVedtaksperiodeDao,
    private val endretVedtaksperiodeDao: EndretVedtaksperiodeDao,
    private val hendelse: PersonHendelse
): JsonMigrationObserver {
    override fun vedtaksperiodeSlettet(vedtaksperiodeId: UUID, vedtaksperiodeNode: JsonNode) {
        slettetVedtaksperiodeDao.lagreSlettetVedtaksperiode(
            hendelse.fødselsnummer(),
            vedtaksperiodeId,
            vedtaksperiodeNode
        )
    }

    override fun vedtaksperiodeEndret(vedtaksperiodeId: UUID, gammelTilstand: String, nyTilstand: String) {
        endretVedtaksperiodeDao.lagreEndretVedtaksperiode(
            hendelse.fødselsnummer(),
            vedtaksperiodeId,
            gammelTilstand,
            nyTilstand
        )
    }
}