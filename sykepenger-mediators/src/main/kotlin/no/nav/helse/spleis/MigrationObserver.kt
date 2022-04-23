package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.person.PersonHendelse
import no.nav.helse.serde.migration.JsonMigrationObserver
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.db.SlettetVedtaksperiodeDao

internal class MigrationObserver(
    private val slettetVedtaksperiodeDao: SlettetVedtaksperiodeDao,
    private val hendelse: PersonHendelse
): JsonMigrationObserver {
    override fun vedtaksperiodeSlettet(vedtaksperiodeId: UUID, vedtaksperiodeNode: JsonNode) {
        slettetVedtaksperiodeDao.lagreSlettetVedtaksperiode(
            hendelse.fødselsnummer(),
            vedtaksperiodeId,
            vedtaksperiodeNode
        )
    }
}