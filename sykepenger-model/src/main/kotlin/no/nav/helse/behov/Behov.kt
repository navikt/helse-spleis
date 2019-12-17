package no.nav.helse.behov

import no.nav.helse.hendelser.Hendelsetype
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.VedtaksperiodeHendelse
import java.time.LocalDateTime
import java.util.*

class Behov internal constructor(private val pakke: Pakke) : ArbeidstakerHendelse, VedtaksperiodeHendelse {

    companion object {
        private const val BehovKey = "@behov"
        private const val IdKey = "@id"
        private const val OpprettetKey = "@opprettet"
        private const val BesvartKey = "@besvart"
        private const val LøsningsKey = "@løsning"

        private const val HendelsetypeKey = "hendelse"
        private const val AktørIdKey = "aktørId"
        private const val FødselsnummerKey = "fødselsnummer"
        private const val OrganisasjonsnummerKey = "organisasjonsnummer"
        private const val VedtaksperiodeIdKey = "vedtaksperiodeId"

        fun nyttBehov(
            hendelsetype: Hendelsetype,
            behov: List<Behovtype>,
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String,
            vedtaksperiodeId: UUID,
            additionalParams: Map<String, Any>
        ): Behov {
            val pakke = Pakke(
                additionalParams + mapOf(
                    BehovKey to behov.map { it.name },
                    IdKey to UUID.randomUUID().toString(),
                    OpprettetKey to LocalDateTime.now().toString(),
                    HendelsetypeKey to hendelsetype.name,
                    AktørIdKey to aktørId,
                    FødselsnummerKey to fødselsnummer,
                    OrganisasjonsnummerKey to organisasjonsnummer,
                    VedtaksperiodeIdKey to vedtaksperiodeId.toString()
                )
            )
            return Behov(pakke)
        }

        fun fromJson(json: String) =
            Behov(Pakke.fromJson(json).also {
                it.requireKey(BehovKey)
                it.requireKey(IdKey)
                it.requireKey(OpprettetKey)
                it.requireKey(HendelsetypeKey)
                it.requireKey(AktørIdKey)
                it.requireKey(FødselsnummerKey)
                it.requireKey(OrganisasjonsnummerKey)
                it.requireKey(VedtaksperiodeIdKey)
            })

    }

    fun behovType(): List<String> = requireNotNull(get(BehovKey))
    fun id(): UUID = UUID.fromString(pakke[IdKey] as String)

    override fun opprettet() = LocalDateTime.parse(pakke[OpprettetKey] as String)

    fun besvart(): LocalDateTime? {
        return pakke[BesvartKey]?.let { LocalDateTime.parse(it as String) }
    }

    override fun hendelsetype(): Hendelsetype {
        return Hendelsetype.valueOf(requireNotNull(get<String>(HendelsetypeKey)))
    }

    override fun aktørId(): String {
        return requireNotNull(get<String>(AktørIdKey))
    }

    override fun fødselsnummer(): String {
        return requireNotNull(get<String>(FødselsnummerKey))
    }

    override fun organisasjonsnummer(): String {
        return requireNotNull(get<String>(OrganisasjonsnummerKey))
    }

    override fun vedtaksperiodeId(): String {
        return requireNotNull(get<String>(VedtaksperiodeIdKey))
    }

    override fun toString() = "${behovType()}:${id()}"

    override fun toJson(): String {
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
