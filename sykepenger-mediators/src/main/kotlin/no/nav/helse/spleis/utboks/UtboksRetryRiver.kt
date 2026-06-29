package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.utboks.Utboks.Companion.fireAndForget
import org.slf4j.LoggerFactory

internal class UtboksRetryRiver(
    rapidsConnection: RapidsConnection,
    private val utboksDao: UtboksDao
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            precondition { it.requireAny("@event_name", listOf("minutt", "spleis_utboks_retry")) }
            validate {
                it.interestedIn("personidentifikatorer")
            }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext, metadata: MessageMetadata, meterRegistry: MeterRegistry) {
        try {
            val fraMelding = packet["personidentifikatorer"].map { Personidentifikator(it.asText()) }.toSet()

            val personerMedUsendteMeldinger = when (fraMelding.isNotEmpty()) {
                true -> fraMelding
                else -> utboksDao.personerMedUsendteMeldinger()
            }

            sikkerlogg.info("Sender ut personpåminnelse for ${personerMedUsendteMeldinger.size} personer som har usendte meldinger i utboksen.")

            personerMedUsendteMeldinger.forEach { personidentifikator ->
                val personpåminnelse = UtgåendeMelding.nyRapidmelding(
                    personidentifikator = personidentifikator,
                    eventName = "person_påminnelse",
                    innhold = emptyMap()
                )
                sikkerlogg.info("Sender personpåminnelse for ${personidentifikator}\n\t${personpåminnelse.json}")
                context.fireAndForget(personpåminnelse)
            }
        } catch (throwable: Throwable) {
            sikkerlogg.error("Feil ved retry av meldinger i utboks", throwable)
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
