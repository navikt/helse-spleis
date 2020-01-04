package no.nav.helse.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import java.time.LocalDateTime
import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    private val hendelseId: UUID,
    private val hendelsetype: Hendelsetype
) : Comparable<ArbeidstakerHendelse> {

    enum class Hendelsetype {
        Ytelser,
        Vilkårsgrunnlag,
        ManuellSaksbehandling,
        Utbetaling,
        Inntektsmelding,
        NySøknad,
        SendtSøknad,
        Påminnelse
    }

    fun hendelseId() = hendelseId
    fun hendelsetype() = hendelsetype

    open fun kanBehandles() = true

    abstract fun rapportertdato(): LocalDateTime

    abstract fun aktørId(): String
    abstract fun fødselsnummer(): String
    abstract fun organisasjonsnummer(): String
    abstract fun toJson(): String

    override fun compareTo(other: ArbeidstakerHendelse) = this.rapportertdato().compareTo(other.rapportertdato())

    override fun equals(other: Any?) =
        other is ArbeidstakerHendelse && other.hendelseId == this.hendelseId

    override fun hashCode() = hendelseId.hashCode()

    companion object Builder {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): ArbeidstakerHendelse {
            return objectMapper.readTree(json).let {
                when (val hendelsetype = Hendelsetype.valueOf(it["type"].textValue())) {
                    Hendelsetype.Inntektsmelding -> Inntektsmelding.fromJson(json)
                    Hendelsetype.NySøknad -> NySøknad.fromJson(json)
                    Hendelsetype.SendtSøknad -> SendtSøknad.fromJson(json)
                    Hendelsetype.Ytelser -> Ytelser.fromJson(json)
                    Hendelsetype.ManuellSaksbehandling -> ManuellSaksbehandling.fromJson(json)
                    else -> throw RuntimeException("kjenner ikke hendelsetypen $hendelsetype")
                }
            }
        }
    }
}
