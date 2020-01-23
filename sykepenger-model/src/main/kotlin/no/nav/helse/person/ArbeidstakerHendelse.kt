package no.nav.helse.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.*
import java.time.LocalDateTime
import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    private val hendelseId: UUID,
    private val hendelsestype: Hendelsestype
) : Comparable<ArbeidstakerHendelse>, IAktivitetslogger {

    enum class Hendelsestype {
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
    fun hendelsetype() = hendelsestype

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

    override fun info(melding: String, vararg params: Any) {}

    override fun warn(melding: String, vararg params: Any) {}

    override fun error(melding: String, vararg params: Any) {}

    override fun severe(melding: String, vararg params: Any): Nothing { Aktivitetslogger().severe(melding, params) }

    override fun hasMessages(): Boolean { return false }

    override fun hasErrors(): Boolean { return false }

    override fun addAll(other: Aktivitetslogger, label: String) {}

    override fun expectNoErrors(): Boolean { return true }

    companion object Builder {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): ArbeidstakerHendelse {
            return objectMapper.readTree(json).let {
                when (val hendelsetype = Hendelsestype.valueOf(it["type"].textValue())) {
                    Hendelsestype.Inntektsmelding -> ModelInntektsmelding.fromJson(json)
                    Hendelsestype.NySøknad -> ModelNySøknad.fromJson(json)
                    Hendelsestype.SendtSøknad -> ModelSendtSøknad.fromJson(json)
                    Hendelsestype.Ytelser -> ModelYtelser.fromJson(json)
                    Hendelsestype.ManuellSaksbehandling -> ModelManuellSaksbehandling.fromJson(json)
                    else -> throw RuntimeException("kjenner ikke hendelsetypen $hendelsetype")
                }
            }
        }
    }
}
