package no.nav.helse.serde

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.jacksonMapperBuilder

/**
 * Utgangspunkt for alle ObjectMappere i spleis. Jackson 3 dropper ellers, uten feilmelding,
 * properties som starter med æ, ø eller å (f.eks. årstall).
 */
fun spleisJsonMapperBuilder(): JsonMapper.Builder =
    jacksonMapperBuilder()
        .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))

internal val serdeObjectMapper: ObjectMapper =
    spleisJsonMapperBuilder()
        .addModule(SimpleModule().addSerializer(SetSerializer(Set::class.java)))
        .addModule(SimpleModule().addDeserializer(Set::class.java, SetDeserializer(Set::class.java)))
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        // Jackson 2-standarden: datoer lagres som tall-arrays i persistert json
        .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()
