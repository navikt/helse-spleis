package no.nav.helse.spleis.graphql

import com.apurebase.kgraphql.GraphQL
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import javax.sql.DataSource
import no.nav.helse.Toggle
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.graphql.ApiV2.installGraphQLApiV2

fun Application.installGraphQLApi(dataSource: DataSource) {
    if (Toggle.GraphQLV2.enabled) installGraphQLApiV2(dataSource, "/graphql")
    else installKGraphQL(dataSource)
}

private fun Application.installKGraphQL(dataSource: DataSource) {
    val personDao = PersonDao(dataSource)
    val hendelseDao = HendelseDao(dataSource)

    install(GraphQL) {
        endpoint = "/graphql"

        wrap {
            authenticate(build = it)
        }

        schema {
            personSchema(personResolver(personDao, hendelseDao))
        }
    }
}

