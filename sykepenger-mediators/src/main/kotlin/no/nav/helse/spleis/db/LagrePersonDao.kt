package no.nav.helse.spleis.db

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.hendelser.Avstemming
import no.nav.helse.person.Person
import no.nav.helse.person.PersonHendelse
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class LagrePersonDao(private val dataSource: DataSource) {
    fun lagrePerson(message: HendelseMessage, person: Person, hendelse: PersonHendelse, vedtak: Boolean) {
        val serialisering = person.serialize()
        lagrePerson(
            aktørId = hendelse.aktørId(),
            fødselsnummer = hendelse.fødselsnummer(),
            skjemaVersjon = serialisering.skjemaVersjon,
            meldingId = message.id,
            personJson = serialisering.json,
            vedtak = vedtak
        )
    }

    fun personAvstemt(hendelse: Avstemming) {
        @Language("PostreSQL")
        val statement = "UPDATE unike_person SET sist_avstemt = now() WHERE fnr = :fnr"
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf(statement, mapOf(
                "fnr" to hendelse.fødselsnummer().toLong()
            )).asExecute)
        }
    }

    private fun lagrePerson(aktørId: String, fødselsnummer: String, skjemaVersjon: Int, meldingId: UUID, personJson: String, vedtak: Boolean) {
        using(sessionOf(dataSource)) { session ->
            opprettNyPerson(session, fødselsnummer, aktørId, skjemaVersjon, meldingId, personJson, vedtak)
        }.also {
            PostgresProbe.personSkrevetTilDb()
        }
    }

    private fun opprettNyPerson(session: Session, fødselsnummer: String, aktørId: String, skjemaVersjon: Int, meldingId: UUID, personJson: String, vedtak: Boolean) {
        @Language("PostreSQL")
        val statement = "INSERT INTO unike_person (fnr, aktor_id) VALUES (:fnr, :aktor) ON CONFLICT DO NOTHING"
        session.run(queryOf(statement, mapOf(
            "fnr" to fødselsnummer.toLong(),
            "aktor" to aktørId.toLong()
        )).asExecute)
        opprettNyPersonversjon(session, fødselsnummer, aktørId, skjemaVersjon, meldingId, personJson, vedtak)
    }

    private fun opprettNyPersonversjon(session: Session, fødselsnummer: String, aktørId: String, skjemaVersjon: Int, meldingId: UUID, personJson: String, vedtak: Boolean) {
        @Language("PostreSQL")
        val statement = """
            INSERT INTO person (aktor_id, fnr, skjema_versjon, melding_id, data, vedtak)
            VALUES (?, ?, ?, ?, (to_json(?::json)), ?)
        """
        session.run(queryOf(statement, aktørId, fødselsnummer, skjemaVersjon, meldingId, personJson, vedtak).asExecute)
    }
}
