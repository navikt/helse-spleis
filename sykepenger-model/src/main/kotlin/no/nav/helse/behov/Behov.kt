package no.nav.helse.behov

import no.nav.helse.hendelser.Hendelsetype
import java.time.LocalDateTime
import java.util.*

class Behov internal constructor(private val pakke: Pakke) {

    companion object {
        private const val BehovKey = "@behov"
        private const val IdKey = "@id"
        private const val OpprettetKey = "@opprettet"
        private const val LøsningsKey = "@løsning"


        fun nyttBehov(behov: List<Behovtype>, additionalParams: Map<String, Any>): Behov {
            val pakke = Pakke(additionalParams + mapOf(
                    BehovKey to behov.map { it.name },
                    IdKey to UUID.randomUUID().toString(),
                    OpprettetKey to LocalDateTime.now().toString()
            ))
            return Behov(pakke)
        }

        fun fromJson(json: String) =
                Behov(Pakke.fromJson(json).also {
                    it.requireKey(BehovKey)
                    it.requireKey(IdKey)
                    it.requireKey(OpprettetKey)
                })

    }

    fun behovType(): List<String> = requireNotNull(get(BehovKey))
    fun id(): UUID = UUID.fromString(pakke[IdKey] as String)
    fun opprettet(): LocalDateTime = LocalDateTime.parse(pakke[OpprettetKey] as String)

    fun hendelsetype() = Hendelsetype.valueOf(requireNotNull(this.get<String>("hendelse")))

    override fun toString() = "${behovType()}:${id()}"
    fun toJson(): String {
        return pakke.toJson()
    }

    fun løsBehov(løsning: Any) {
        pakke[LøsningsKey] = løsning
        pakke["final"] = true
    }

    fun erLøst(): Boolean {
        return (pakke["final"] as Boolean?) ?: false
    }

    fun løsning() =
            pakke[LøsningsKey]

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String): T? {
        return pakke[key] as T?
    }

    operator fun set(key: String, value: Any) {
        pakke[key] = value
    }
}
