package no.nav.helse.spleis.graphql

import com.apurebase.kgraphql.GraphQL
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import java.nio.file.Paths
import no.nav.helse.spleis.graphql.dto.GraphQLPerson
import no.nav.helse.spleis.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

internal class SchemaGenerator {

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    fun `Genererer GraphQL-schema og oppdaterer om det er noe nytt`() = testApplication {
        install(GraphQL) {
            schema {
                personSchema { fnr ->
                    GraphQLPerson(
                        aktorId = fnr,
                        fodselsnummer = fnr,
                        arbeidsgivere = emptyList(),
                        dodsdato = null,
                        versjon = 1,
                        vilkarsgrunnlag = emptyList()
                    )
                }
            }
        }

        val schemaPath = Paths.get("${Paths.get("").absolutePathString()}/src/main/resources/graphql-schema.json")
        val gammeltSchema = schemaPath.readText()
        val response = client.post("/graphql") { setBody(IntrospectionQuery) }.bodyAsText()
        val nyttSchema = objectMapper.readTree(response) as ObjectNode

        assertTrue(nyttSchema.path("data").path("__schema").isObject) {
            "Noe er galt med det nytt schema"
        }

        if (erLik(gammeltSchema, nyttSchema.toString())) {
            println("Ingen endringer i GraphQL schema 🤡")
        } else {
            schemaPath.writeText(nyttSchema.toPrettyString())
        }
    }

    internal companion object {
        @Language("JSON")
        internal const val IntrospectionQuery = """
        {"query":"\n    query IntrospectionQuery {\n      __schema {\n        queryType { name }\n        mutationType { name }\n        subscriptionType { name }\n        types {\n          ...FullType\n        }\n        directives {\n          name\n          description\n          locations\n          args {\n            ...InputValue\n          }\n        }\n      }\n    }\n    fragment FullType on __Type {\n      kind\n      name\n      description\n      fields(includeDeprecated: true) {\n        name\n        description\n        args {\n          ...InputValue\n        }\n        type {\n          ...TypeRef\n        }\n        isDeprecated\n        deprecationReason\n      }\n      inputFields {\n        ...InputValue\n      }\n      interfaces {\n        ...TypeRef\n      }\n      enumValues(includeDeprecated: true) {\n        name\n        description\n        isDeprecated\n        deprecationReason\n      }\n      possibleTypes {\n        ...TypeRef\n      }\n    }\n    fragment InputValue on __InputValue {\n      name\n      description\n      type { ...TypeRef }\n      defaultValue\n    }\n    fragment TypeRef on __Type {\n      kind\n      name\n      ofType {\n        kind\n        name\n        ofType {\n          kind\n          name\n          ofType {\n            kind\n            name\n            ofType {\n              kind\n              name\n              ofType {\n                kind\n                name\n                ofType {\n                  kind\n                  name\n                  ofType {\n                    kind\n                    name\n                  }\n                }\n              }\n            }\n          }\n        }\n      }\n    }\n    ","operationName":"IntrospectionQuery"}
        """

        private fun erLik(før: String, etter: String) = !JSONCompare.compareJSON(før, etter, STRICT).failed()
    }
}
