package no.nav.helse.spleis.db

import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.int
import com.github.navikt.tbd_libs.sql_dsl.long
import com.github.navikt.tbd_libs.sql_dsl.mapNotNull
import com.github.navikt.tbd_libs.sql_dsl.prepareStatementWithNamedParameters
import com.github.navikt.tbd_libs.sql_dsl.single
import com.github.navikt.tbd_libs.sql_dsl.string
import com.github.navikt.tbd_libs.sql_dsl.transaction
import java.sql.Connection
import javax.sql.DataSource
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.serde.tilPersonData
import no.nav.helse.serde.tilSerialisertPerson
import no.nav.helse.spleis.meldinger.model.AvstemmingMessage
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

internal class PersonDao(private val dataSource: DataSource, private val STØTTER_IDENTBYTTE: Boolean) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    fun hentEllerOpprettPerson(
        regelverkslogg: Regelverkslogg,
        personidentifikator: Personidentifikator,
        historiskeFolkeregisteridenter: Set<Personidentifikator>,
        message: HendelseMessage,
        hendelseRepository: HendelseRepository,
        lagNyPerson: () -> Person?,
        håndterPerson: (Person) -> Unit
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
        dataSource.connection {
            transaction {
                val (personId, person) = hentPersonEllerOpprettNy(this, regelverkslogg, hendelseRepository, personidentifikator, lagNyPerson, historiskeFolkeregisteridenter)
                    ?: return@transaction fantIkkePerson(personidentifikator)

                knyttPersonTilHistoriskeIdenter(this, personId, personidentifikator, historiskeFolkeregisteridenter)

                val personUt = person.also(håndterPerson).dto()

                val personData = personUt.tilPersonData()
                val oppdatertJson = personData.tilSerialisertPerson().json

                oppdaterAvstemmingtidspunkt(this, message, personidentifikator)
                oppdaterPersonversjon(this, personId, personData.skjemaVersjon, oppdatertJson)
            }
        }
    }

    private fun hentPersonEllerOpprettNy(connection: Connection, regelverkslogg: Regelverkslogg, hendelseRepository: HendelseRepository, personidentifikator: Personidentifikator, lagNyPerson: () -> Person?, historiskeFolkeregisteridenter: Set<Personidentifikator>): Pair<Long, Person>? {
        return gjenopprettFraTidligereBehandling(connection, regelverkslogg, hendelseRepository, personidentifikator, historiskeFolkeregisteridenter) ?: opprettNyPerson(connection, lagNyPerson)
    }

    private fun gjenopprettFraTidligereBehandling(connection: Connection, regelverkslogg: Regelverkslogg, hendelseRepository: HendelseRepository, personidentifikator: Personidentifikator, historiskeFolkeregisteridenter: Set<Personidentifikator>): Pair<Long, Person>? {
        val (personId, serialisertPerson, tidligerePersoner) = hentPersonOgLåsPersonForBehandling(connection, personidentifikator, historiskeFolkeregisteridenter)
            ?: hentPersonFraHistoriskeIdenter(connection, personidentifikator, historiskeFolkeregisteridenter)
            ?: return null
        val tidligereBehandlinger = tidligerePersoner.map { tidligerePerson -> Person.gjenopprett(regelverkslogg, tidligerePerson.tilPersonDto()) }
        val personInn = serialisertPerson.tilPersonDto { hendelseRepository.hentAlleHendelser(personidentifikator) }
        return personId to Person.gjenopprett(regelverkslogg, personInn, tidligereBehandlinger)
    }

    private fun opprettNyPerson(connection: Connection, lagNyPerson: () -> Person?): Pair<Long, Person>? {
        val person = lagNyPerson() ?: return null
        val personId = opprettNyPersonversjon(connection, person.personidentifikator, 0, "{}")
        return personId to person
    }

    private fun fantIkkePerson(personidentifikator: Personidentifikator) {
        sikkerlogg.info("fant ikke person $personidentifikator, oppretter heller ingen ny person")
    }

    private fun hentTidligereBehandledeIdenter(connection: Connection, personId: Long, historiskeFolkeregisteridenter: Set<Personidentifikator>): List<SerialisertPerson> {
        if (historiskeFolkeregisteridenter.isEmpty()) return emptyList()
        // forventer én rad tilbake ellers kastes en exception siden vi da kan knytte
        // flere ulike person-rader til samme person, og personen må merges manuelt
        return hentIdenter(connection, historiskeFolkeregisteridenter)
            .filterNot { (id, _) -> id == personId }
            .map { (_, person) -> person }
    }

    private fun hentPersonOgLåsPersonForBehandling(connection: Connection, personidentifikator: Personidentifikator, historiskeFolkeregisteridenter: Set<Personidentifikator>): Triple<Long, SerialisertPerson, List<SerialisertPerson>>? {
        @Language("PostgreSQL")
        val statement = "SELECT id, data FROM person WHERE fnr = ? FOR UPDATE"
        val (personId, person) = connection.prepareStatement(statement).use {
            it.setLong(1, personidentifikator.toLong())
            it.executeQuery().use { rs ->
                rs.mapNotNull { row ->
                    row.long("id") to SerialisertPerson(row.string("data"))
                }
            }
        }.singleOrNullOrThrow() ?: return null

        return Triple(personId, person, hentTidligereBehandledeIdenter(connection, personId, historiskeFolkeregisteridenter))
    }

    private fun hentPersonFraHistoriskeIdenter(connection: Connection, personidentifikator: Personidentifikator, historiskeFolkeregisteridenter: Set<Personidentifikator>): Triple<Long, SerialisertPerson, List<SerialisertPerson>>? {
        if (!STØTTER_IDENTBYTTE) return null
        val identer = historiskeFolkeregisteridenter.plusElement(personidentifikator)

        return hentIdenter(connection, identer)
            .singleOrNullOrThrow()
            ?.let { (personId, person) -> Triple(personId, person, emptyList()) }
    }

    private fun hentIdenter(connection: Connection, identer: Collection<Personidentifikator>): List<Pair<Long, SerialisertPerson>> {
        @Language("PostgreSQL")
        val statement = """
            SELECT p.id, p.data FROM person p
            INNER JOIN person_alias pa ON pa.person_id = p.id
            WHERE pa.fnr = ANY(:identer)
            FOR UPDATE
        """
        // forventer én rad tilbake ellers kastes en exception siden vi da kan knytte
        // flere ulike person-rader til samme person, og personen må merges manuelt
        return connection.prepareStatementWithNamedParameters(statement) {
            withParameter("identer", identer.map { it.toLong() })
        }.use { stmt ->
            stmt.executeQuery().use { rs ->
                rs.mapNotNull { row ->
                    row.long("id") to SerialisertPerson(row.string("data"))
                }
            }
        }
            .distinctBy { (personId, _) -> personId }
    }

    private fun knyttPersonTilHistoriskeIdenter(connection: Connection, personId: Long, personidentifikator: Personidentifikator, historiskeFolkeregisteridenter: Set<Personidentifikator>) {
        val identer = if (STØTTER_IDENTBYTTE) historiskeFolkeregisteridenter.plusElement(personidentifikator) else setOf(personidentifikator)

        @Language("PostgreSQL")
        val statement = """INSERT INTO person_alias(fnr, person_id) VALUES ${identer.joinToString { "(?, $personId)" }} ON CONFLICT DO NOTHING;"""
        connection.prepareStatement(statement).use { stmt ->
            identer.forEachIndexed { index, ident ->
                stmt.setLong(index + 1, ident.toLong())
            }
            stmt.execute()
        }
    }

    private fun <R> Collection<R>.singleOrNullOrThrow() =
        if (size < 2) this.firstOrNull()
        else throw IllegalStateException("Listen inneholder mer enn ett element!")

    private fun opprettNyPersonversjon(connection: Connection, personidentifikator: Personidentifikator, skjemaVersjon: Int, personJson: String): Long {
        @Language("PostgreSQL")
        val statement = """ INSERT INTO person (fnr, skjema_versjon, data) VALUES (:fnr, :skjemaversjon, :data) RETURNING id """
        return connection.prepareStatementWithNamedParameters(statement) {
            withParameter("fnr", personidentifikator.toLong())
            withParameter("skjemaversjon", skjemaVersjon)
            withParameter("data", personJson)
        }.use { stmt ->
            stmt.executeQuery().use { rs ->
                rs.single { it.long("id") }
            }
        }
    }

    private fun oppdaterAvstemmingtidspunkt(connection: Connection, message: HendelseMessage, personidentifikator: Personidentifikator) {
        if (message !is AvstemmingMessage) return
        @Language("PostgreSQL")
        val statement = "UPDATE person SET sist_avstemt = now() WHERE fnr = :fnr"
        connection.prepareStatementWithNamedParameters(statement) {
            withParameter("fnr", personidentifikator.toLong())
        }.use { stmt ->
            stmt.execute()
        }
    }

    internal fun manglerAvstemming(): Int {
        @Language("PostgresSQL")
        val statement = "SELECT count(1) FROM person WHERE sist_avstemt::date < now()::date - interval '31 DAYS'"
        return dataSource.connection {
            createStatement().use {
                it.executeQuery(statement).use { rs ->
                    rs.single { row -> row.int(1) }
                }
            }
        }
    }

    private fun oppdaterPersonversjon(connection: Connection, personId: Long, skjemaVersjon: Int, personJson: String) {
        @Language("PostgreSQL")
        val statement = """ UPDATE person SET skjema_versjon=:skjemaversjon, data=:data WHERE id=:personId; """
        check(
            1 == connection.prepareStatementWithNamedParameters(statement) {
                withParameter("skjemaversjon", skjemaVersjon)
                withParameter("data", personJson)
                withParameter("personId", personId)
            }.use { stmt ->
                stmt.executeUpdate()
            }
        ) {
            "Forventet å påvirke én og bare én rad!"
        }
    }
}
