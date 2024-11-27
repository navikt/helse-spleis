package no.nav.helse.spleis.graphql

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.github.navikt.tbd_libs.result_object.getOrThrow
import com.github.navikt.tbd_libs.speed.SpeedClient
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.MeterRegistry
import java.util.*
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.spleis.SpekematClient
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.graphql.dto.*
import no.nav.helse.spleis.nyObjectmapper
import no.nav.helse.spleis.objectMapper
import org.slf4j.LoggerFactory

internal object Api {
    private val logger = LoggerFactory.getLogger(Api::class.java)
    private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    private val schema = Api::class.java.getResource("/graphql-schema.json")!!.readText()
    private val fraQueryRegex = "person\\(fnr:\"(\\d+)\"\\)".toRegex()
    private val sifferRegex = "\\d+".toRegex()
    private val String.fnr
        get() =
            objectMapper.readTree(this.replace(" ", "").replace("\n", "")).let { body ->
                val fraVariables =
                    body
                        .path("variables")
                        .fields()
                        .asSequence()
                        .singleOrNull { (_, value) -> value.asText().matches(sifferRegex) }
                        ?.value
                        ?.asText()
                fraVariables
                    ?: fraQueryRegex.find(body.path("query").asText())?.groupValues?.lastOrNull()
            }

    private data class Response(val data: Data)

    private data class Data(val person: GraphQLPerson?)

    private val graphQLV2ObjectMapper =
        nyObjectmapper.also {
            it.registerSubtypes(NamedType(GraphQLBeregnetPeriode::class.java))
            it.registerSubtypes(NamedType(GraphQLUberegnetPeriode::class.java))
            it.registerSubtypes(NamedType(GraphQLInfotrygdVilkarsgrunnlag::class.java))
            it.registerSubtypes(NamedType(GraphQLSpleisVilkarsgrunnlag::class.java))
            it.setMixIns(
                mapOf(
                    GraphQLArbeidsgiver::class.java to GraphQLArbeidsgiverMixin::class.java,
                    GraphQLUtbetaling::class.java to GraphQLUtbetalingMixin::class.java,
                    GraphQLGhostPeriode::class.java to GraphQLGhostPeriodeMixin::class.java,
                )
            )
        }

    internal fun Application.installGraphQLApi(
        speedClient: SpeedClient,
        spekematClient: SpekematClient,
        hendelseDao: HendelseDao,
        personDao: PersonDao,
        meterRegistry: MeterRegistry,
    ) {
        routing {
            authenticate(optional = true) {
                post("/graphql") {
                    val ident = call.receiveText().fnr ?: return@post call.respondText(schema, Json)
                    call.principal<JWTPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    try {
                        val callId = call.callId ?: UUID.randomUUID().toString()
                        val (_, aktørId) =
                            speedClient.hentFødselsnummerOgAktørId(ident, callId).getOrThrow()

                        val person =
                            personResolver(
                                spekematClient,
                                personDao,
                                hendelseDao,
                                ident,
                                aktørId,
                                callId,
                                meterRegistry,
                            )
                        call.respondText(
                            graphQLV2ObjectMapper.writeValueAsString(Response(Data(person))),
                            Json,
                        )
                    } catch (err: Exception) {
                        logger.error(
                            "callId=${call.callId} Kunne ikke lage JSON for Spesialist, sjekk tjenestekall-indeksen!"
                        )
                        sikkerlogger.error(
                            "callId=${call.callId} {} Kunne ikke lage JSON for Spesialist: ${err.javaClass.simpleName} - ${err.message}",
                            keyValue("fødselsnummer", ident),
                            err,
                        )
                        call.respondText(
                            graphQLV2ObjectMapper.writeValueAsString(
                                mapOf(
                                    "errors" to
                                        listOf(
                                            mapOf(
                                                "message" to
                                                    "Det har skjedd en feil 😵‍💫 Det er logget, og vi er kanskje på saken! 🫡",
                                                "locations" to emptyList<Any>(),
                                                "path" to emptyList<Any>(),
                                            )
                                        )
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    @JsonIgnoreProperties("id") private class GraphQLArbeidsgiverMixin

    @JsonIgnoreProperties("status", "type") private class GraphQLUtbetalingMixin

    @JsonIgnoreProperties("organisasjonsnummer") private class GraphQLGhostPeriodeMixin
}
