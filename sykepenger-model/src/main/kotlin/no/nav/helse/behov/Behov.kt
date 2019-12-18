package no.nav.helse.behov

import no.nav.helse.sak.ArbeidstakerHendelse
import java.time.LocalDateTime
import java.util.*

class Behov internal constructor(private val pakke: Pakke) {

    companion object {
        private const val BehovKey = "@behov"
        private const val IdKey = "@id"
        private const val OpprettetKey = "@opprettet"
        private const val BesvartKey = "@besvart"
        private const val LøsningsKey = "@løsning"
        private const val FinalKey = "@final"

        private const val HendelsetypeKey = "hendelse"
        private const val AktørIdKey = "aktørId"
        private const val FødselsnummerKey = "fødselsnummer"
        private const val OrganisasjonsnummerKey = "organisasjonsnummer"
        private const val VedtaksperiodeIdKey = "vedtaksperiodeId"

        fun nyttBehov(
            hendelsetype: ArbeidstakerHendelse.Hendelsetype,
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

    fun opprettet() = LocalDateTime.parse(pakke[OpprettetKey] as String)

    fun besvart(): LocalDateTime? {
        return pakke[BesvartKey]?.let { LocalDateTime.parse(it as String) }
    }

    fun hendelsetype(): ArbeidstakerHendelse.Hendelsetype {
        return ArbeidstakerHendelse.Hendelsetype.valueOf(requireNotNull(get<String>(HendelsetypeKey)))
    }

    fun aktørId(): String {
        return requireNotNull(get<String>(AktørIdKey))
    }

    fun fødselsnummer(): String {
        return requireNotNull(get<String>(FødselsnummerKey))
    }

    fun organisasjonsnummer(): String {
        return requireNotNull(get<String>(OrganisasjonsnummerKey))
    }

    fun vedtaksperiodeId(): String {
        return requireNotNull(get<String>(VedtaksperiodeIdKey))
    }

    override fun toString() = "${behovType()}:${id()}"

    fun toJson(): String {
        return pakke.toJson()
    }

    fun erLøst(): Boolean {
        return (pakke[FinalKey] as Boolean?) ?: false
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
