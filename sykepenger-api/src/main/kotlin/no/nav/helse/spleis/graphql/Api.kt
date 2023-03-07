package no.nav.helse.spleis.graphql

import com.apurebase.kgraphql.GraphQL
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import javax.sql.DataSource
import no.nav.helse.Toggle
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao

fun Application.installGraphQLApi(dataSource: DataSource, authProviderName: String) {
    val personDao = PersonDao(dataSource)
    val hendelseDao = HendelseDao(dataSource)

    install(GraphQL) {
        endpoint = "/graphql"

        if (Toggle.GraphQLPlayground.enabled) {
            playground = true
        } else {
            wrap {
                authenticate(authProviderName, build = it)
            }
        }

        schema {
            personSchema(personResolver(personDao, hendelseDao))
        }
    }
}

