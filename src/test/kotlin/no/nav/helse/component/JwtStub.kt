package no.nav.helse.component

import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.*
import kotlinx.io.core.*
import java.security.*
import java.security.interfaces.*
import java.util.*

class JwtStub(private val issuer: String, private val wireMockServer: WireMockServer) {

   private val privateKey: RSAPrivateKey
   private val publicKey: RSAPublicKey

   init {
      val client = WireMock.create().port(wireMockServer.port()).build()
      WireMock.configureFor(client)

      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(512)

      val keyPair = keyPairGenerator.genKeyPair()
      privateKey = keyPair.private as RSAPrivateKey
      publicKey = keyPair.public as RSAPublicKey
   }

   fun createTokenFor(group: String, audience: String? = null): String {
      val algorithm = Algorithm.RSA256(publicKey, privateKey)

      return JWT.create()
         .withIssuer(issuer)
         .withAudience(audience ?: "el_cliento")
         .withKeyId("key-1234")
         .withSubject("Da Usah")
         .withArrayClaim("groups", arrayOf("someothergroup1", group, "someothergroup2"))
         .sign(algorithm)
   }

   fun stubbedJwkProvider() = WireMock.get(WireMock.urlPathEqualTo("/jwks")).willReturn(
      WireMock.okJson("""
{
    "keys": [
        {
            "kty": "RSA",
            "alg": "RS256",
            "kid": "key-1234",
            "e": "${String(Base64.getEncoder().encode(publicKey.publicExponent.toByteArray()))}",
            "n": "${String(Base64.getEncoder().encode(publicKey.modulus.toByteArray()))}"
        }
    ]
}
""".trimIndent())
   )

   fun stubbedConfigProvider() = WireMock.get(WireMock.urlPathEqualTo("/config")).willReturn(
      WireMock.okJson("""
{
    "jwks_uri": "${wireMockServer.baseUrl()}/jwks",
    "token_endpoint": "${wireMockServer.baseUrl()}/token",
    "issuer": "$issuer"
}
""".trimIndent())
   )
}
