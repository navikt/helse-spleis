package no.nav.helse.serde

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.dto.DokumenttypeDto
import no.nav.helse.dto.GenerasjonTilstandDto
import no.nav.helse.dto.InntektsopplysningDto
import no.nav.helse.dto.InntekttypeDto
import no.nav.helse.dto.MedlemskapsvurderingDto
import no.nav.helse.dto.SykdomstidslinjeDagDto
import no.nav.helse.dto.UtbetaltDagDto
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.dto.VilkårsgrunnlagDto

internal val serdeObjectMapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .registerModule(SimpleModule().addSerializer(SetSerializer(Set::class.java)))
    .registerModule(SimpleModule().addDeserializer(Set::class.java, SetDeserializer(Set::class.java)))
    .addMixIn(SykdomstidslinjeDagDto::class.java, SealedClassMixIn::class.java)
    .addMixIn(VedtaksperiodetilstandDto::class.java, SealedClassMixIn::class.java)
    .addMixIn(GenerasjonTilstandDto::class.java, SealedClassMixIn::class.java)
    .addMixIn(DokumenttypeDto::class.java, SealedClassMixIn::class.java)
    .addMixIn(VilkårsgrunnlagDto::class.java, SealedClassMixIn::class.java)
    .addMixIn(MedlemskapsvurderingDto::class.java, SealedClassMixIn::class.java)
    .addMixIn(InntektsopplysningDto::class.java, SealedClassMixIn::class.java)
    .addMixIn(InntekttypeDto::class.java, SealedClassMixIn::class.java)
    .addMixIn(UtbetaltDagDto::class.java, SealedClassMixIn::class.java)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
private class SealedClassMixIn