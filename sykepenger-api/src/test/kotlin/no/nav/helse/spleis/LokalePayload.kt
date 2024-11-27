package no.nav.helse.spleis

import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.Payload
import java.time.Instant
import java.util.Date

class LokalePayload(
    claims: Map<String, String>,
) : Payload {
    private val claims = claims.mapValues { LokaleClaim(it.value) }

    override fun getIssuer(): String = "lokal utsteder"

    override fun getSubject(): String = "lokal subjekt"

    override fun getAudience(): List<String> = listOf("lokal publikum")

    override fun getExpiresAt(): Date = Date.from(Instant.MAX)

    override fun getNotBefore(): Date = Date.from(Instant.EPOCH)

    override fun getIssuedAt(): Date = Date.from(Instant.now())

    override fun getId(): String = "lokal id"

    override fun getClaim(name: String): Claim = claims.getValue(name)

    override fun getClaims(): Map<String, Claim> = claims
}

private class LokaleClaim(
    private val verdi: String,
) : Claim {
    override fun isNull() = false

    override fun isMissing() = false

    override fun asBoolean() = true

    override fun asInt() = 0

    override fun asLong() = 0L

    override fun asDouble() = 0.0

    override fun asString() = verdi

    override fun asDate() = Date.from(Instant.EPOCH)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> asArray(clazz: Class<T>?) = emptyArray<Any>() as Array<T>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> asList(clazz: Class<T>?) = emptyList<Any>() as List<T>

    override fun asMap() = emptyMap<String, Any>()

    override fun <T : Any?> `as`(clazz: Class<T>?) = throw NotImplementedError()
}
