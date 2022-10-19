package no.nav.helse.spleis.db

import javax.sql.DataSource
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Personidentifikator
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.spleis.meldinger.model.AvstemmingMessage
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.intellij.lang.annotations.Language

internal class PersonDao(private val dataSource: DataSource) {
    fun hentEllerOpprettPerson(
        personidentifikator: Personidentifikator,
        aktørId: String,
        message: HendelseMessage,
        lagNyPerson: () -> SerialisertPerson,
        håndterPerson: (SerialisertPerson) -> SerialisertPerson
    ) {
        /* finner eksisterende person og låser den for oppdatering, slik at andre
            selects mot samme person (som inkluderer 'FOR UPDATE') blokkeres frem til transaksjonen slutter.

            det opprettes en ny person dersom det ikke finnes noe fra før.
            dersom to personer opprettes samtidig så vil den ene INSERT-spørringen kaste exception
            siden vi forventer at vi skal påvirke én, og bare én, rad
         */
        sessionOf(dataSource).use {
            it.transaction { txSession ->
                låsPersonForBehandling(txSession, personidentifikator)
                val serialisertPerson = finnSistePersonJson(txSession, personidentifikator)?.second
                    ?: opprettNyPerson(txSession, personidentifikator, aktørId, message, lagNyPerson)
                val oppdatertPerson = håndterPerson(serialisertPerson)
                oppdaterAvstemmingtidspunkt(txSession, message, personidentifikator)
                oppdaterPerson(
                    txSession,
                    personidentifikator,
                    oppdatertPerson.skjemaVersjon,
                    oppdatertPerson.json
                )
            }
        }
    }

    private fun låsPersonForBehandling(session: TransactionalSession, personidentifikator: Personidentifikator) {
        @Language("PostgreSQL")
        val statement = "SELECT * FROM unike_person WHERE fnr = ? FOR UPDATE" // 'FOR UPDATE' låser personen
        session.run(queryOf(statement, personidentifikator.toLong()).asExecute)
    }

    private fun finnSistePersonJson(session: Session, personidentifikator: Personidentifikator) =
        session.run(queryOf("SELECT id, data FROM person WHERE fnr = ? ORDER BY id DESC LIMIT 1", personidentifikator.toLong()).map {
            it.long("id") to SerialisertPerson(it.string("data"))
        }.asSingle)

    // TODO: gjøre fnr som primary key i person-tabellen, og fjerne behov for unike_person
    private fun opprettNyPerson(session: Session, personidentifikator: Personidentifikator, aktørId: String, message: HendelseMessage, lagNyPerson: () -> SerialisertPerson): SerialisertPerson {
        return lagNyPerson().also {
            opprettNyPersonRad(session, personidentifikator, aktørId)
            opprettNyPersonversjon(session, personidentifikator, aktørId, it.skjemaVersjon, it.json)
        }
    }

    private fun opprettNyPersonversjon(session: Session, personidentifikator: Personidentifikator, aktørId: String, skjemaVersjon: Int, personJson: String) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO person (aktor_id, fnr, skjema_versjon, data)
            VALUES (:aktor, :fnr, :skjemaversjon, :data)
        """
        session.run(queryOf(statement, mapOf(
            "aktor" to aktørId.toLong(),
            "fnr" to personidentifikator.toLong(),
            "skjemaversjon" to skjemaVersjon,
            "data" to personJson
        )).asExecute)
    }

    private fun opprettNyPersonRad(session: Session, personidentifikator: Personidentifikator, aktørId: String) {
        // om to prosesser inserter en person samtidig, så vil den ene klare å det mens den andre vil få 0 rader affected (pga. DO NOTHING)
        @Language("PostgreSQL")
        val statement = "INSERT INTO unike_person (fnr, aktor_id) VALUES (:fnr, :aktor) ON CONFLICT DO NOTHING"
        check(1 == session.run(queryOf(statement, mapOf(
            "fnr" to personidentifikator.toLong(),
            "aktor" to aktørId.toLong()
        )).asUpdate)) { "Forventet å påvirke én rad. Det kan indikere at vi har forsøkt å opprette en person samtidig (race condition)." }
    }

    private fun oppdaterAvstemmingtidspunkt(session: Session, message: HendelseMessage, personidentifikator: Personidentifikator) {
        if (message !is AvstemmingMessage) return
        @Language("PostgreSQL")
        val statement = "UPDATE unike_person SET sist_avstemt = now() WHERE fnr = :fnr"
        session.run(queryOf(statement, mapOf("fnr" to personidentifikator.toLong())).asExecute)
    }

    private fun oppdaterPerson(session: TransactionalSession, personidentifikator: Personidentifikator, skjemaVersjon: Int, personJson: String) {
        val sisteId = checkNotNull(finnSistePersonJson(session, personidentifikator)).first
        // TODO: fjerne sletting når det bare finnes én rad per person
        slettEldrePersonversjon(session, personidentifikator, sisteId)
        oppdaterPersonversjon(session, sisteId, skjemaVersjon, personJson)
    }

    private fun oppdaterPersonversjon(session: Session, id: Long, skjemaVersjon: Int, personJson: String) {
        @Language("PostgreSQL")
        val statement = """ UPDATE person SET skjema_versjon=:skjemaversjon, data=:data WHERE id=:id; """
        check(1 == session.run(queryOf(statement, mapOf(
            "skjemaversjon" to skjemaVersjon,
            "data" to personJson,
            "id" to id
        )).asUpdate)) {
            "Forventet å påvirke én og bare én rad!"
        }
    }

    private fun slettEldrePersonversjon(session: Session, personidentifikator: Personidentifikator, sisteId: Long) {
        @Language("PostgreSQL")
        val statement = """ DELETE FROM person WHERE fnr = :fnr AND id != :sisteId; """
        session.run(queryOf(statement, mapOf(
            "fnr" to personidentifikator.toLong(),
            "sisteId" to sisteId
        )).asExecute)
    }
}
