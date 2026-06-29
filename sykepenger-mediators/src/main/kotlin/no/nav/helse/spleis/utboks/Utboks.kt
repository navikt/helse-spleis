package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import java.sql.Connection
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import org.slf4j.LoggerFactory

internal class Utboks(private val utsender: Utsender, private val innkommendeMelding: HendelseMessage, private val utboksDao: UtboksDao) {
    private val personidentifikator = Personidentifikator(innkommendeMelding.meldingsporing.fødselsnummer)
    private val utgåendeMeldinger = mutableListOf<UtgåendeMelding>()
    private var tilstand: Tilstand = Tilstand.Åpen

    fun nyMelding(block: (personidentifikator: Personidentifikator) -> UtgåendeMelding) {
        val utgåendeMelding = block(personidentifikator)
        require(utgåendeMelding.key == null || utgåendeMelding.key == personidentifikator.toString()) { "Kan ikke sende ut meldinger for andre i denne utboksen!" }
        tilstand.nyMelding(utgåendeMelding, this)
    }

    fun lagre(connection: Connection) {
        nyMelding {
            UtgåendeMelding.nyRapidmelding(
                personidentifikator = personidentifikator,
                eventName = "melding_om_melding_håndtert",
                innhold = mapOf(
                    "originalt_event_name" to "${innkommendeMelding.navn}",
                    "original_id" to "${innkommendeMelding.meldingsporing.id.id}"
                )
            )
        }
        tilstand = Tilstand.Lukket
        sikkerLogg.info("Lagrer ${utgåendeMeldinger.size} meldinger fra utboksen")
        // TODO: Lagre i db
    }

    fun send() {
        sikkerLogg.info("Sender ${utgåendeMeldinger.size} meldinger fra utboksen")
        innkommendeMelding.logOutgoingMessages(sikkerLogg, utgåendeMeldinger.size)
        val kvittering = utsender.send(utgåendeMeldinger)
        kvittering.ok.loggSending()
        // TODO: Marker OK-meldingene sendt i DB
    }

    private fun List<UtgåendeMelding>.loggSending() {
        forEach { utgåendeMelding ->
            when (utgåendeMelding.eventName) {
                "behov" -> {
                    val behov = utgåendeMelding.json.path("@behov").map { it.asText() }
                    sikkerLogg.info("sender behov til ${utgåendeMelding.mottaker.name} (${behov.joinToString()}):\n\t${utgåendeMelding.json}")
                }
                else -> sikkerLogg.info("sender ${utgåendeMelding.eventName} til ${utgåendeMelding.mottaker.name}:\n\t${utgåendeMelding.json}")
            }
        }
    }

    private sealed interface Tilstand {
        fun nyMelding(melding: UtgåendeMelding, utboks: Utboks)

        data object Åpen: Tilstand {
            override fun nyMelding(melding: UtgåendeMelding, utboks: Utboks) {
                utboks.utgåendeMeldinger.add(melding.copy(
                    json = melding.json.apply {
                        putObject("@forårsaket_av").apply {
                            put("id", utboks.innkommendeMelding.meldingsporing.id.id.toString())
                            put("opprettet", utboks.innkommendeMelding.opprettet.toString())
                            put("event_name", utboks.innkommendeMelding.navn)
                            utboks.innkommendeMelding.behov?.let { behov ->
                                putArray("behov").apply {
                                    addAll(behov)
                                }
                            }
                        }
                    }
                ))
            }
        }
        data object Lukket: Tilstand {
            override fun nyMelding(melding: UtgåendeMelding, utboks: Utboks) {
                error("Utboksen er lukket, kan ikke legge til melding")
            }
        }
    }

    internal companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        // Sendes utom utboks, "uviktige" meldinger, eller annet som gjør at de ikke kan/ikke gir mening å sendes via utboks
        internal fun MessageContext.fireAndForget(melding: UtgåendeMelding) = try {
            this.publish(listOf(OutgoingMessage(key = melding.key, body = melding.json.toString())))
        } catch (_: Exception) {}
    }
}
