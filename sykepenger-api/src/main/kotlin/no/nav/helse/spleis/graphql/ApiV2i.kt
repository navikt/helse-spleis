package no.nav.helse.spleis.graphql

import com.fasterxml.jackson.databind.jsontype.NamedType
import io.ktor.http.ContentType.Application.Json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import javax.sql.DataSource
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.graphql.dto.GraphQLBeregnetPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLInfotrygdVilkarsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLInntektsmelding
import no.nav.helse.spleis.graphql.dto.GraphQLPerson
import no.nav.helse.spleis.graphql.dto.GraphQLSoknadArbeidsgiver
import no.nav.helse.spleis.graphql.dto.GraphQLSoknadNav
import no.nav.helse.spleis.graphql.dto.GraphQLSpleisVilkarsgrunnlag
import no.nav.helse.spleis.graphql.dto.GraphQLSykmelding
import no.nav.helse.spleis.graphql.dto.GraphQLUberegnetPeriode
import no.nav.helse.spleis.nyObjectmapper
import no.nav.helse.spleis.objectMapper

internal object ApiV2i {
    private val schema = ApiV2i::class.java.getResource("/graphql-schema.json")!!.readText()
    private val fraQueryRegex = "person\\(fnr:\"(\\d+)\"\\)".toRegex()
    private val sifferRegex = "\\d+".toRegex()
    private val String.fnr get() = objectMapper.readTree(this.replace(" ", "").replace("\n", "")).let { body ->
        val fraVariables = body.path("variables").fields().asSequence().singleOrNull { (_, value) -> value.asText().matches(sifferRegex) }?.value?.asText()
        fraVariables ?: fraQueryRegex.find(body.path("query").asText())?.groupValues?.lastOrNull()
    }
    private data class Response(val data: Data)
    private data class Data(val person: GraphQLPerson?)

    private val graphQLV2ObjectMapper = nyObjectmapper.also {
        it.registerSubtypes(NamedType(GraphQLBeregnetPeriode::class.java))
        it.registerSubtypes(NamedType(GraphQLUberegnetPeriode::class.java))
        it.registerSubtypes(NamedType(GraphQLInntektsmelding::class.java))
        it.registerSubtypes(NamedType(GraphQLSykmelding::class.java))
        it.registerSubtypes(NamedType(GraphQLSoknadNav::class.java))
        it.registerSubtypes(NamedType(GraphQLSoknadArbeidsgiver::class.java))
        it.registerSubtypes(NamedType(GraphQLInfotrygdVilkarsgrunnlag::class.java))
        it.registerSubtypes(NamedType(GraphQLSpleisVilkarsgrunnlag::class.java))
    }

    internal fun Application.installGraphQLApiV2(dataSource: DataSource) {
        val personDao = PersonDao(dataSource)
        val hendelseDao = HendelseDao(dataSource)

        routing {
            post("/v2/graphql/introspection") {
                call.respondText(schema, Json)
            }
            authenticate {
                post("/v2/graphql") {
                    val fødselsnummer = call.receiveText().fnr ?: return@post call.respondText(schema, Json)
                    val person = personResolver(personDao, hendelseDao)(fødselsnummer)
                    call.respondText(graphQLV2ObjectMapper.writeValueAsString(Response(Data(person))), Json)
                }
            }
        }
    }
}
