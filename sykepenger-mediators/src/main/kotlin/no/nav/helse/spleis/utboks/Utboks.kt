package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import java.sql.Connection
import kotlin.system.measureTimeMillis
import no.nav.helse.Personidentifikator
import org.slf4j.LoggerFactory

internal class Utboks(
    private val utsender: Utsender,
    private val innkommendeMelding: InnkommendeMelding,
    private val utboksDao: UtboksDao
) {
    private val personidentifikator = innkommendeMelding.personidentifikator
    private val utgåendeMeldinger = mutableListOf<UtgåendeMelding>()
    private var tilstand: Tilstand = Tilstand.Åpen

    fun nyMelding(block: (personidentifikator: Personidentifikator) -> UtgåendeMelding) {
        val utgåendeMelding = block(personidentifikator)
        require(utgåendeMelding.key == null || utgåendeMelding.key == personidentifikator.toString()) { "Kan ikke sende ut meldinger for andre i denne utboksen!" }
        tilstand.nyMelding(utgåendeMelding, this)
    }

    private fun leggTilMeldingOmMeldingHåndtert() {
        if (utgåendeMeldinger.any { it.eventName == "melding_om_melding_ikke_håndtert_fordi_person_ikke_funnet" }) return
        nyMelding {
            UtgåendeMelding.nyRapidmelding(
                personidentifikator = personidentifikator,
                eventName = "melding_om_melding_håndtert",
                innhold = mapOf(
                    "originalt_event_name" to innkommendeMelding.navn,
                    "original_id" to "${innkommendeMelding.meldingsreferanseId.id}"
                )
            )
        }
    }

    fun lagre(connection: Connection) {
        leggTilMeldingOmMeldingHåndtert()
        tilstand = Tilstand.Lukket
        val tidsbruk = measureTimeMillis {
            utboksDao.lagre(connection, utgåendeMeldinger.map { Utboksmelding.BeholdEtterSending(it) }, innkommendeMelding.meldingsreferanseId.id)
        }
        sikkerLogg.info("Brukte ${tidsbruk}ms å lagre ${utgåendeMeldinger.size} meldinger i utboksen.")
    }

    fun send() {
        val tidsbruk = measureTimeMillis {
            utboksDao.usendte(personidentifikator) { usendteMeldinger ->
                sikkerLogg.info("som følge av ${innkommendeMelding.navn} id=${innkommendeMelding.meldingsreferanseId.id} sendes ${usendteMeldinger.size} meldinger for fnr=${personidentifikator}")
                utsender.send(usendteMeldinger).also { kvittering ->
                    kvittering.ok.loggSending()
                    // Logger meldinger som ble sendt nå men som ikke ble produsert nå
                    val produsertNå = utgåendeMeldinger.map { it.id }.toSet()
                    val sendtNå = kvittering.ok.map { it.id }.toSet()
                    sendtNå.filterNot { it in produsertNå }.takeUnless { it.isEmpty() }?.let { gamleMeldinger ->
                        sikkerLogg.info("Sendte ${gamleMeldinger.size} melding(er) som ikke ble produsert nå: ${gamleMeldinger.joinToString()}")
                    }
                }
            }
        }
        sikkerLogg.info("Brukte ${tidsbruk}ms å sende meldinger fra utboksen.")

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
                            put("id", utboks.innkommendeMelding.meldingsreferanseId.id.toString())
                            put("opprettet", utboks.innkommendeMelding.opprettet.toString())
                            put("event_name", utboks.innkommendeMelding.navn)
                            utboks.innkommendeMelding.behov?.let { behov ->
                                putArray("behov").apply {
                                    behov.forEach(::add)
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
