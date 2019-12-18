package no.nav.helse.sak

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import java.time.LocalDateTime
import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    private val hendelseId: UUID,
    private val hendelsetype: Hendelsetype
) {
    enum class Hendelsetype {
        Ytelser,
        ManuellSaksbehandling,
        Utbetaling,
        Inntektsmelding,
        NySøknad,
        SendtSøknad,
        Påminnelse
    }

    // old enums, replaced by Hendelsetype.
    // kept around to patch json when deserializing
    internal enum class SykdomshendelseType {
        SendtSøknadMottatt,
        NySøknadMottatt,
        InntektsmeldingMottatt
    }

    fun hendelseId() = hendelseId
    fun hendelsetype() = hendelsetype

    abstract fun opprettet(): LocalDateTime

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String
    abstract fun organisasjonsnummer(): String

    open fun kanBehandles() = true

    abstract fun toJson(): String

    override fun equals(other: Any?) =
        other is ArbeidstakerHendelse && other.hendelseId == this.hendelseId

    override fun hashCode() = hendelseId.hashCode()

    companion object Builder {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val inntektsmeldingtyper = listOf(
            Hendelsetype.Inntektsmelding.name,
            SykdomshendelseType.InntektsmeldingMottatt.name
        )

        private val nySøknadtyper = listOf(
            Hendelsetype.NySøknad.name,
            SykdomshendelseType.NySøknadMottatt.name
        )
        private val sendtSøknadtyper = listOf(
            Hendelsetype.SendtSøknad.name,
            SykdomshendelseType.SendtSøknadMottatt.name
        )

        fun fromJson(json: String): ArbeidstakerHendelse {
            return objectMapper.readTree(json).let {
                when (val hendelsetype = it["type"].textValue()) {
                    in inntektsmeldingtyper -> Inntektsmelding.fromJson(json)
                    in nySøknadtyper -> NySøknad.fromJson(json)
                    in sendtSøknadtyper -> SendtSøknad.fromJson(json)
                    Hendelsetype.Ytelser.name -> Ytelser.fromJson(json)
                    Hendelsetype.ManuellSaksbehandling.name -> ManuellSaksbehandling.fromJson(json)
                    else -> throw RuntimeException("kjenner ikke hendelsetypen $hendelsetype")
                }
            }
        }
    }
}
