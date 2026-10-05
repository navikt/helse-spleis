package no.nav.helse.serde

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.jacksonMapperBuilder

internal val serdeObjectMapper: ObjectMapper = jacksonMapperBuilder()
    .addModule(SimpleModule().addSerializer(SetSerializer(Set::class.java)))
    .addModule(SimpleModule().addDeserializer(Set::class.java, SetDeserializer(Set::class.java)))
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    .build()
