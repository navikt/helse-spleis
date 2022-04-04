package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.TilstandType
import no.nav.helse.rapids_rivers.RapidsConnection
import org.junit.jupiter.api.fail
import org.slf4j.LoggerFactory

internal class TestRapid : RapidsConnection() {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val log = LoggerFactory.getLogger(TestRapid::class.java)
    }

    private val messages = mutableListOf<Pair<String?, String>>()
    internal val inspektør get() = RapidInspektør(messages.toList())

    internal fun reset() {
        messages.clear()
    }

    fun sendTestMessage(message: Pair<*, String>) = sendTestMessage(message.second)

    fun sendTestMessage(message: String) {
        log.info("sending message:\n\t$message")
        notifyMessage(message, this)
    }

    override fun publish(message: String) {
        messages.add(null to message)
    }

    override fun publish(key: String, message: String) {
        messages.add(key to message)
    }

    override fun rapidName(): String {
        return "Testrapid"
    }

    override fun start() {}
    override fun stop() {}

    class RapidInspektør(private val messages: List<Pair<String?, String>>) {
        private val jsonmeldinger = mutableMapOf<Int, JsonNode>()
        private val vedtaksperiodeIder
            get() = mutableSetOf<UUID>().apply {
                events("vedtaksperiode_endret") {
                    this.add(UUID.fromString(it.path("vedtaksperiodeId").asText()))
                }
            }

        private val forkastedeVedtaksperiodeIder
            get() = mutableMapOf<UUID, String>().apply {
                events("vedtaksperiode_forkastet") {
                    val id = UUID.fromString(it.path("vedtaksperiodeId").asText())
                    this[id] = it.path("tilstand").asText()
                }
            }

        private val tilstander
            get() = mutableMapOf<UUID, MutableList<String>>().apply {
                events("vedtaksperiode_endret") {
                    val id = UUID.fromString(it.path("vedtaksperiodeId").asText())
                    this.getOrPut(id) { mutableListOf() }.add(it.path("gjeldendeTilstand").asText())
                }
            }

        private val utbetalinger = mutableSetOf<UUID>().apply {
            events("utbetaling_endret") {
                add(UUID.fromString(it.path("utbetalingId").asText()))
            }
        }

        private val utbetalingtilstander
            get() = mutableMapOf<UUID, MutableList<String>>().apply {
                events("utbetaling_endret") {
                    val id = UUID.fromString(it.path("utbetalingId").asText())
                    this.getOrPut(id) { mutableListOf(it.path("forrigeStatus").asText()) }
                        .add(it.path("gjeldendeStatus").asText())
                }
            }

        private val utbetalingtyper
            get() = mutableMapOf<UUID, String>().apply {
                events("utbetaling_endret") {
                    val id = UUID.fromString(it.path("utbetalingId").asText())
                    this[id] = it.path("type").asText()
                }
            }

        private val forkastedeTilstander
            get() = tilstander.filter { it.key in forkastedeVedtaksperiodeIder }

        private val tilstanderUtenForkastede
            get() = tilstander.filter { it.key !in forkastedeVedtaksperiodeIder }

        private val behov
            get() = mutableMapOf<UUID, MutableList<Pair<Aktivitetslogg.Aktivitet.Behov.Behovtype, TilstandType>>>().apply {
                events("behov") {
                    val vedtaksperiodeIdString = it.path("vedtaksperiodeId")
                        .takeIf { id -> !id.isMissingNode }
                        ?.asText() ?: return@events

                    val id = UUID.fromString(vedtaksperiodeIdString)
                    val tilstand = TilstandType.valueOf(it.path("tilstand").asText())
                    this.getOrPut(id) { mutableListOf() }.apply {
                        it.path("@behov").onEach {
                            add(Aktivitetslogg.Aktivitet.Behov.Behovtype.valueOf(it.asText()) to tilstand)
                        }
                    }
                }
            }

        private val behovmeldinger
            get() = mutableListOf<Pair<Aktivitetslogg.Aktivitet.Behov.Behovtype, JsonNode>>().apply {
                events("behov") { message ->
                    message.path("@behov").onEach {
                        add(Aktivitetslogg.Aktivitet.Behov.Behovtype.valueOf(it.asText()) to message)
                    }
                }
            }

        private fun events(name: String, onEach: (JsonNode) -> Unit) = messages.forEachIndexed { indeks, _ ->
            val message = melding(indeks)
            if (name == message.path("@event_name").asText()) onEach(message)
        }

        internal fun behovtypeSisteMelding(behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
            melding(antall() - 1)["@behov"][0].asText() == behovtype.toString()

        val vedtaksperiodeteller get() = vedtaksperiodeIder.size

        fun melding(indeks: Int) = jsonmeldinger.getOrPut(indeks) { objectMapper.readTree(messages[indeks].second) }
        fun antall() = messages.size

        fun siste(name: String) = meldinger(name).last()

        fun meldinger(name: String) = messages.mapIndexed { indeks, _ -> melding(indeks) }
            .filter { name == it.path("@event_name").asText() }

        fun vedtaksperiodeId(indeks: Int) = vedtaksperiodeIder.elementAt(indeks)
        fun utbetalingtilstander(utbetalingIndeks: Int) =
            utbetalinger.elementAt(utbetalingIndeks).let { utbetalingId ->
                utbetalingtilstander[utbetalingId]?.toList()
            } ?: emptyList()
        fun utbetalingtype(utbetalingIndeks: Int) =
            utbetalinger.elementAt(utbetalingIndeks).let { utbetalingId ->
                utbetalingtyper[utbetalingId]
            } ?: fail { "Finner ikke utbetaling" }
        fun utbetalingId(utbetalingIndeks: Int) = utbetalinger.elementAt(utbetalingIndeks)

        fun tilstander(vedtaksperiodeId: UUID) = tilstander[vedtaksperiodeId]?.toList() ?: emptyList()
        fun tilstanderUtenForkastede(vedtaksperiodeId: UUID) = tilstanderUtenForkastede[vedtaksperiodeId]?.toList() ?: emptyList()
        fun forkastedeTilstander(vedtaksperiodeId: UUID) = forkastedeTilstander[vedtaksperiodeId]?.toList() ?: emptyList()

        fun harEtterspurteBehov(vedtaksperiodeIndeks: Int, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
            behov[vedtaksperiodeId(vedtaksperiodeIndeks)]?.any { it.first == behovtype } ?: false

        fun etterspurteBehov(behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
            behovmeldinger.last { it.first == behovtype }.second

        fun alleEtterspurteBehov(behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
            behovmeldinger.filter { it.first == behovtype }.map { it.second }

        fun tilstandForEtterspurteBehov(vedtaksperiodeIndeks: Int, behovtype: Aktivitetslogg.Aktivitet.Behov.Behovtype) =
            behov.getValue(vedtaksperiodeId(vedtaksperiodeIndeks)).last { it.first == behovtype }.second
    }
}
