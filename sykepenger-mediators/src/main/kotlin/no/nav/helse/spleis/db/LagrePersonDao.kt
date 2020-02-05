package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.PersonObserver
import no.nav.helse.serde.serializePerson
import no.nav.helse.spleis.PostgresProbe
import javax.sql.DataSource

class LagrePersonDao(
    private val dataSource: DataSource,
    private val probe: PostgresProbe = PostgresProbe
) : PersonObserver {
    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
        val (skjemaVersjon, personJson) = serializePerson(personEndretEvent.person)
        lagrePerson(
            aktørId = personEndretEvent.aktørId,
            fødselsnummer = personEndretEvent.fødselsnummer,
            skjemaVersjon = skjemaVersjon,
            personJson = personJson
        )
    }

    private fun lagrePerson(aktørId: String, fødselsnummer: String, skjemaVersjon: Int, personJson: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO person (aktor_id, fnr, skjema_versjon, data) VALUES (?, ?, ?, (to_json(?::json)))",
                    aktørId, fødselsnummer, skjemaVersjon, personJson
                ).asExecute
            )
        }.also {
            PostgresProbe.personSkrevetTilDb()
        }
    }

}
