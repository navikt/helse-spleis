package no.nav.helse.spleis.db

import javax.sql.DataSource
import kotliquery.Session
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
                val serialisertPerson = hentPersonOgLåsPersonForBehandling(txSession, personidentifikator)
                    ?: opprettNyPerson(txSession, personidentifikator, aktørId, lagNyPerson)
                val oppdatertPerson = håndterPerson(serialisertPerson)
                oppdaterAvstemmingtidspunkt(txSession, message, personidentifikator)
                oppdaterPersonversjon(txSession, personidentifikator, oppdatertPerson.skjemaVersjon, oppdatertPerson.json)
            }
        }
    }

    private fun hentPersonOgLåsPersonForBehandling(session: Session, personidentifikator: Personidentifikator): SerialisertPerson? {
        @Language("PostgreSQL")
        val statement = "SELECT data FROM person WHERE fnr = ? FOR UPDATE"
        return session.run(queryOf(statement, personidentifikator.toLong()).map {
            SerialisertPerson(it.string("data"))
        }.asList).singleOrNullOrThrow()
    }

    private fun <R> Collection<R>.singleOrNullOrThrow() =
        if (size < 2) this.firstOrNull()
        else throw IllegalStateException("Listen inneholder mer enn et element!")

    private fun opprettNyPerson(session: Session, personidentifikator: Personidentifikator, aktørId: String, lagNyPerson: () -> SerialisertPerson): SerialisertPerson {
        return lagNyPerson().also {
            opprettNyPersonRad(session, personidentifikator, aktørId)
            opprettNyPersonversjon(session, personidentifikator, it.skjemaVersjon, it.json)
        }
    }

    private fun opprettNyPersonversjon(session: Session, personidentifikator: Personidentifikator, skjemaVersjon: Int, personJson: String) {
        @Language("PostgreSQL")
        val statement = """ INSERT INTO person (fnr, skjema_versjon, data) VALUES (:fnr, :skjemaversjon, :data) """
        session.run(queryOf(statement, mapOf(
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

    private fun oppdaterPersonversjon(session: Session, personidentifikator: Personidentifikator, skjemaVersjon: Int, personJson: String) {
        @Language("PostgreSQL")
        val statement = """ UPDATE person SET skjema_versjon=:skjemaversjon, data=:data WHERE fnr=:ident; """
        check(1 == session.run(queryOf(statement, mapOf(
            "skjemaversjon" to skjemaVersjon,
            "data" to personJson,
            "ident" to personidentifikator.toLong()
        )).asUpdate)) {
            "Forventet å påvirke én og bare én rad!"
        }
    }
}
