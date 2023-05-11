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

internal class PersonDao(private val dataSource: DataSource, private val STØTTER_IDENTBYTTE: Boolean) {

    fun hentEllerOpprettPerson(
        personidentifikator: Personidentifikator,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
        aktørId: String,
        message: HendelseMessage,
        lagNyPerson: () -> SerialisertPerson?,
        håndterPerson: (SerialisertPerson, List<SerialisertPerson>) -> SerialisertPerson
    ) {
        /* finner eksisterende person og låser den for oppdatering, slik at andre
            selects mot samme person (som inkluderer 'FOR UPDATE') blokkeres frem til transaksjonen slutter.

            det opprettes en ny person dersom det ikke finnes noe fra før.
            dersom to personer opprettes samtidig så vil den ene INSERT-spørringen kaste exception
            siden vi forventer at vi skal påvirke én, og bare én, rad

            1) person finnes ikke på hendelse.fnr eller noen historiskeFolkeregisteridenter
               opprett person og insert (fnr + historiskeFolkeregisteridenter) i person_alias
            2) person finnes ikke på hendelse.fnr, men finnes på historiskeFolkeregisteridenter (og de peker på samme person)
               sett inn kobling i person_alias og hent person
            3) person finnes på hendelse.fnr
               sett inn kobling(er) i person_alias fra historiskeFolkeregisteridenter; alle må peke på samme person_id som hendelse.fnr
               hent person
         */
        sessionOf(dataSource, returnGeneratedKey = true).use {
            it.transaction { txSession ->
                val (personId, serialisertPerson, tidligerePersoner) = hentPersonOgLåsPersonForBehandling(txSession, personidentifikator, historiskeFolkeregisteridenter)
                    ?: hentPersonFraHistoriskeIdenter(txSession, personidentifikator, historiskeFolkeregisteridenter)
                    ?: opprettNyPerson(txSession, personidentifikator, aktørId, lagNyPerson)
                    ?: return
                knyttPersonTilHistoriskeIdenter(txSession, personId, personidentifikator, historiskeFolkeregisteridenter)
                val oppdatertPerson = håndterPerson(serialisertPerson, tidligerePersoner)
                oppdaterAvstemmingtidspunkt(txSession, message, personidentifikator)
                oppdaterPersonversjon(txSession, personId, oppdatertPerson.skjemaVersjon, oppdatertPerson.json)
            }
        }
    }

    private fun hentTidligereBehandledeIdenter(session: Session, personId: Long, historiskeFolkeregisteridenter: Set<Personidentifikator>): List<SerialisertPerson> {
        if (historiskeFolkeregisteridenter.isEmpty()) return emptyList()
        @Language("PostgreSQL")
        val statement = """
            SELECT p.id, p.fnr, p.data FROM person p
            INNER JOIN person_alias pa ON pa.person_id = p.id
            WHERE p.fnr IN (${historiskeFolkeregisteridenter.joinToString { "?" }}) 
            FOR UPDATE
        """
        // forventer én rad tilbake ellers kastes en exception siden vi da kan knytte
        // flere ulike person-rader til samme person, og personen må merges manuelt
        return session.run(queryOf(statement, *historiskeFolkeregisteridenter.map { it.toLong() }.toTypedArray())
            .map { it.long("id") to SerialisertPerson(it.string("data")) }.asList)
            .filterNot { (id, _) -> id == personId }
            .map { (_, person) -> person }
    }

    private fun hentPersonOgLåsPersonForBehandling(session: Session, personidentifikator: Personidentifikator, historiskeFolkeregisteridenter: Set<Personidentifikator>): Triple<Long, SerialisertPerson, List<SerialisertPerson>>? {
        @Language("PostgreSQL")
        val statement = "SELECT id, data FROM person WHERE fnr = ? FOR UPDATE"
        val (personId, person) = session.run(queryOf(statement, personidentifikator.toLong()).map {
            it.long("id") to SerialisertPerson(it.string("data"))
        }.asList).singleOrNullOrThrow() ?: return null

        return Triple(personId, person, hentTidligereBehandledeIdenter(session, personId, historiskeFolkeregisteridenter))
    }

    private fun hentPersonFraHistoriskeIdenter(session: Session, personidentifikator: Personidentifikator, historiskeFolkeregisteridenter: Set<Personidentifikator>): Triple<Long, SerialisertPerson, List<SerialisertPerson>>? {
        if (!STØTTER_IDENTBYTTE) return null
        val identer = historiskeFolkeregisteridenter.plusElement(personidentifikator)
        @Language("PostgreSQL")
        val statement = """
            SELECT p.id, p.data FROM person p
            INNER JOIN person_alias pa ON pa.person_id = p.id
            WHERE p.fnr IN (${identer.joinToString { "?" }})
            FOR UPDATE
        """
        // forventer én rad tilbake ellers kastes en exception siden vi da kan knytte
        // flere ulike person-rader til samme person, og personen må merges manuelt
        return session.run(queryOf(statement, *identer.map { it.toLong() }.toTypedArray()).map {
            it.long("id") to SerialisertPerson(it.string("data"))
        }.asList)
            .distinctBy { (personId, _) -> personId }
            .singleOrNullOrThrow()
            ?.let { (personId, person) -> Triple(personId, person, emptyList()) }
    }

    private fun knyttPersonTilHistoriskeIdenter(session: Session, personId: Long, personidentifikator: Personidentifikator, historiskeFolkeregisteridenter: Set<Personidentifikator>) {
        val identer = if (STØTTER_IDENTBYTTE) historiskeFolkeregisteridenter.plusElement(personidentifikator) else setOf(personidentifikator)
        @Language("PostgreSQL")
        val statement = """INSERT INTO person_alias(fnr, person_id) VALUES ${identer.joinToString { "(?, $personId)" }} ON CONFLICT DO NOTHING;"""
        session.run(queryOf(statement, *identer.map { it.toLong() }.toTypedArray()).asExecute)
    }

    private fun <R> Collection<R>.singleOrNullOrThrow() =
        if (size < 2) this.firstOrNull()
        else throw IllegalStateException("Listen inneholder mer enn ett element!")

    private fun opprettNyPerson(session: Session, personidentifikator: Personidentifikator, aktørId: String, lagNyPerson: () -> SerialisertPerson?): Triple<Long, SerialisertPerson, List<SerialisertPerson>>? {
        return lagNyPerson()?.let {
            opprettNyPersonRad(session, personidentifikator, aktørId)
            Triple(opprettNyPersonversjon(session, personidentifikator, it.skjemaVersjon, it.json), it, emptyList())
        }
    }

    private fun opprettNyPersonversjon(session: Session, personidentifikator: Personidentifikator, skjemaVersjon: Int, personJson: String): Long {
        @Language("PostgreSQL")
        val statement = """ INSERT INTO person (fnr, skjema_versjon, data) VALUES (:fnr, :skjemaversjon, :data) """
        return checkNotNull(session.run(queryOf(statement, mapOf(
            "fnr" to personidentifikator.toLong(),
            "skjemaversjon" to skjemaVersjon,
            "data" to personJson
        )).asUpdateAndReturnGeneratedKey)) { "klarte ikke inserte person" }
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

    internal fun manglerAvstemming(): Int {
        @Language("PostgresSQL")
        val statement = "SELECT count(1) FROM unike_person WHERE sist_avstemt < now() - interval '32 DAYS'"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(statement).map { row -> row.int(1) }.asSingle) ?: 0
        }
    }

    private fun oppdaterPersonversjon(session: Session, personId: Long, skjemaVersjon: Int, personJson: String) {
        @Language("PostgreSQL")
        val statement = """ UPDATE person SET skjema_versjon=:skjemaversjon, data=:data WHERE id=:personId; """
        check(1 == session.run(queryOf(statement, mapOf(
            "skjemaversjon" to skjemaVersjon,
            "data" to personJson,
            "personId" to personId
        )).asUpdate)) {
            "Forventet å påvirke én og bare én rad!"
        }
    }
}
