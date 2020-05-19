package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import java.util.*
import javax.sql.DataSource

internal class LagrePersonDao(private val dataSource: DataSource) {
    fun lagrePerson(message: HendelseMessage, person: Person, hendelse: PersonHendelse) {
        val serialisering = person.serialize()
        lagrePerson(
            aktørId = hendelse.aktørId(),
            fødselsnummer = hendelse.fødselsnummer(),
            skjemaVersjon = serialisering.skjemaVersjon,
            meldingId = message.id,
            personJson = serialisering.json
        )
    }

    private fun lagrePerson(aktørId: String, fødselsnummer: String, skjemaVersjon: Int, meldingId: UUID, personJson: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO person (aktor_id, fnr, skjema_versjon, melding_id, data) VALUES (?, ?, ?, ?, (to_json(?::json)))",
                    aktørId, fødselsnummer, skjemaVersjon, meldingId, personJson
                ).asExecute
            )
        }.also {
            PostgresProbe.personSkrevetTilDb()
        }
    }

}
