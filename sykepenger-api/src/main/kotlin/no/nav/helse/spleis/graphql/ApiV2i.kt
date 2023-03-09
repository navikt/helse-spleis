package no.nav.helse.spleis.graphql

import io.ktor.http.ContentType.Application.Json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import javax.sql.DataSource
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.graphql.dto.GraphQLPerson
import no.nav.helse.spleis.objectMapper

internal object ApiV2i {
    private val schema = ApiV2i::class.java.getResource("/graphql-schema.json")!!.readText()
    private val regex = "person\\(fnr:\"(\\d+)\"\\)".toRegex()
    private val String.fnr get() = objectMapper.readTree(this.replace(" ", "").replace("\n", "")).path("query").asText().let { query ->
        regex.find(query)?.groupValues?.lastOrNull()
    }

    private data class Response(val data: Data)
    private data class Data(val person: GraphQLPerson?)

    internal fun Application.installGraphQLApiV2(dataSource: DataSource, authProviderName: String) {
        val personDao = PersonDao(dataSource)
        val hendelseDao = HendelseDao(dataSource)

        routing {
            post("/v2/graphql/introspection") {
                call.respondText(schema, Json)
            }
            authenticate(authProviderName) {
                post("/v2/graphql") {
                    val fødselsnummer = call.receiveText().fnr ?: return@post call.respondText(schema, Json)
                    val person = personResolver(personDao, hendelseDao)(fødselsnummer)
                    call.respond(Response(Data(person)))
                }
            }
        }
    }
}
