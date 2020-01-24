package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.hendelser.ModelSendtSøknad
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.util.*

abstract class SykdomstidslinjeHendelse(
    hendelseId: UUID,
    hendelsestype: Hendelsestype,
    aktivitetslogger: Aktivitetslogger
) : ArbeidstakerHendelse(hendelseId, hendelsestype, aktivitetslogger) {
    internal abstract fun sykdomstidslinje(): ConcreteSykdomstidslinje

    internal abstract fun nøkkelHendelseType(): Dag.NøkkelHendelseType

    abstract fun toJson(): String

    companion object Builder {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): SykdomstidslinjeHendelse {
            return objectMapper.readTree(json).let {
                when (val hendelsetype = Hendelsestype.valueOf(it["type"].textValue())) {
                    Hendelsestype.Inntektsmelding -> ModelInntektsmelding.fromJson(json)
                    Hendelsestype.NySøknad -> ModelNySøknad.fromJson(json)
                    Hendelsestype.SendtSøknad -> ModelSendtSøknad.fromJson(json)
                    else -> throw RuntimeException("kjenner ikke hendelsetypen $hendelsetype")
                }
            }
        }
    }
}
