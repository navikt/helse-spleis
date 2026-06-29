package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import java.sql.Connection
import no.nav.helse.Personidentifikator
import no.nav.helse.Toggle
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

        if (Toggle.BrukUtboks.enabled) {
            utboksDao.lagre(connection, utgåendeMeldinger, innkommendeMelding.meldingsporing.id.id)
        }
    }

    fun send() {
        innkommendeMelding.logOutgoingMessages(sikkerLogg, utgåendeMeldinger.size)
        when (Toggle.BrukUtboks.enabled) {
            true -> sendFraDao()
            false -> {
                sikkerLogg.info("Sender ${utgåendeMeldinger.size} meldinger fra utboksen")
                val kvittering = utsender.send(utgåendeMeldinger)
                kvittering.ok.loggSending()
            }
        }
    }

    private fun sendFraDao() {
        // Her henter vi alt for DB, inkludert de vi akkurat lagret ettersom det kan være
        // andre usendte meldinger for samme person som må sendes før de vi har genrert nå for å sikre rett rekkefølge.
        val produsertNå = utgåendeMeldinger.map { it.id }.toSet()

        utboksDao.usendte(personidentifikator) { usendteMeldinger ->
            // For å sjekke at resendingen funker så unnlater vi alltid å sende "vedtaksperioder_venter" med en gang.
            // .. da skal de sendes neste gang vi håndterer en melding på personen.
            val sendNå = usendteMeldinger.filterNot { usendtMelding ->
                usendtMelding.eventName == "vedtaksperioder_venter" && produsertNå.any { it == usendtMelding.id }
            }
            sikkerLogg.info("Sender ${sendNå.size} meldinger fra utboksen")

            utsender.send(sendNå).also { kvittering ->
                kvittering.ok.loggSending()
                val sendtNå = kvittering.ok.map { it.id }.toSet()
                sendtNå.filterNot { it in produsertNå }.takeUnless { it.isEmpty() }?.let { gamleMeldinger ->
                    sikkerLogg.info("Sendte ${gamleMeldinger.size} melding(er) som ikke ble produsert nå: ${gamleMeldinger.joinToString()}")
                }
            }
        }
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
