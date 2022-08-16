package no.nav.helse.spleis.db

import java.util.UUID
import javax.sql.DataSource
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Avstemming
import no.nav.helse.person.PersonHendelse
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.PostgresProbe
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.intellij.lang.annotations.Language

internal class LagrePersonDao(private val dataSource: DataSource) {
    fun lagrePerson(message: HendelseMessage, person: SerialisertPerson, hendelse: PersonHendelse, vedtak: Boolean) {
        lagrePerson(
            aktørId = hendelse.aktørId(),
            personidentifikator = hendelse.fødselsnummer().somPersonidentifikator(),
            skjemaVersjon = person.skjemaVersjon,
            meldingId = message.id,
            personJson = person.json,
            vedtak = vedtak
        )
    }

    fun personAvstemt(hendelse: Avstemming) {
        @Language("PostgreSQL")
        val statement = "UPDATE unike_person SET sist_avstemt = now() WHERE fnr = :fnr"
        sessionOf(dataSource).use { session ->
            session.run(queryOf(statement, mapOf(
                "fnr" to hendelse.fødselsnummer().toLong()
            )).asExecute)
        }
    }

    private fun lagrePerson(aktørId: String, personidentifikator: Personidentifikator, skjemaVersjon: Int, meldingId: UUID, personJson: String, vedtak: Boolean) {
        sessionOf(dataSource).use { session ->
            session.transaction {
                opprettNyPerson(it, personidentifikator, aktørId, skjemaVersjon, meldingId, personJson, vedtak)
            }
        }.also {
            PostgresProbe.personSkrevetTilDb()
        }
    }

    private fun opprettNyPerson(session: TransactionalSession, personidentifikator: Personidentifikator, aktørId: String, skjemaVersjon: Int, meldingId: UUID, personJson: String, vedtak: Boolean) {
        @Language("PostgreSQL")
        val statement = "INSERT INTO unike_person (fnr, aktor_id) VALUES (:fnr, :aktor) ON CONFLICT DO NOTHING"
        session.run(queryOf(statement, mapOf(
            "fnr" to personidentifikator.toLong(),
            "aktor" to aktørId.toLong()
        )).asExecute)
        slettEldrePersonversjon(session, personidentifikator)
        opprettNyPersonversjon(session, personidentifikator, aktørId, skjemaVersjon, meldingId, personJson, vedtak)
    }

    private fun opprettNyPersonversjon(session: Session, personidentifikator: Personidentifikator, aktørId: String, skjemaVersjon: Int, meldingId: UUID, personJson: String, vedtak: Boolean) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO person (aktor_id, fnr, skjema_versjon, melding_id, data, vedtak)
            VALUES (?, ?, ?, ?, ?, ?)
        """
        session.run(queryOf(statement, aktørId.toLong(), personidentifikator.toLong(), skjemaVersjon, meldingId, personJson, vedtak).asExecute)
    }

    private fun slettEldrePersonversjon(session: Session, personidentifikator: Personidentifikator) {
        @Language("PostgreSQL")
        val statement = """
            DELETE FROM person
            WHERE vedtak = false AND fnr = ?
        """
        session.run(queryOf(statement, personidentifikator.toLong()).asExecute)
    }
}
