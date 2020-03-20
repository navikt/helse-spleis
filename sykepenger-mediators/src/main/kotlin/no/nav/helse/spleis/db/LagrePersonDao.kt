package no.nav.helse.spleis.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Person
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.PostgresProbe
import javax.sql.DataSource

class LagrePersonDao(private val dataSource: DataSource) {
    fun lagrePerson(person: Person, hendelse: ArbeidstakerHendelse) {
        val serialisering = person.serialize()
        lagrePerson(
            aktørId = hendelse.aktørId(),
            fødselsnummer = hendelse.fødselsnummer(),
            skjemaVersjon = serialisering.skjemaVersjon,
            personJson = serialisering.json
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
