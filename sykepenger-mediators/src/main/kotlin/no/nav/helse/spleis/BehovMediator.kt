package no.nav.helse.spleis

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behov.BehovType
import no.nav.helse.behov.partisjoner
import no.nav.helse.hendelser.HendelseObserver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import java.time.LocalDateTime
import java.util.*

internal class BehovMediator(
    private val rapidsConnection: RapidsConnection,
    private val sikkerLogg: Logger
) : HendelseObserver {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
    }

    private val behov = mutableListOf<BehovType>()

    override fun onBehov(behov: BehovType) {
        this.behov.add(behov)
    }

    internal fun finalize(hendelse: ArbeidstakerHendelse) {
        if (behov.isEmpty()) return

        behov.partisjoner().let {
            it.map { behovspakke ->
                val id = UUID.randomUUID()
                id to mutableMapOf(
                    "@event_name" to "behov",
                    "@opprettet" to LocalDateTime.now(),
                    "@id" to id
                ).apply { putAll(behovspakke) }
            }.also {
                sikkerLogg.info("sender ${it.size} behovshendelser med id = {}", it.map { it.first })
            }.forEach { (id, event) ->
                sikkerLogg.info("sender {} som {}", id, event)
                rapidsConnection.publish(hendelse.fødselsnummer(), event.toJson())
            }
        }
    }

    internal fun håndter(hendelse: ArbeidstakerHendelse) {
        hendelse.behov().groupBy { it.kontekst() }.forEach { (kontekst, behov) ->
            val behovsliste = mutableListOf<String>()
            val id = UUID.randomUUID()
            mutableMapOf(
                "@event_name" to "behov",
                "@opprettet" to LocalDateTime.now(),
                "@id" to id,
                "@behov" to behovsliste
            ).apply {
                putAll(kontekst)
                behov.forEach {
                    require(it.type.name !in behovsliste) { "Kan ikke produsere samme behov $it.type.name på samme kontekst" }
                    require(it.detaljer().filterKeys { this.containsKey(it) }.isEmpty()) { "Kan ikke produsere behov med duplikate detaljer" }
                    behovsliste.add(it.type.name)
                    putAll(it.detaljer())
                }

                sikkerLogg.info("sender {} som {}", id, this.toJson())
                rapidsConnection.publish(hendelse.fødselsnummer(), this.toJson())
            }
        }
    }

    private fun Map<String, Any>.toJson() = objectMapper.writeValueAsString(this)

}
