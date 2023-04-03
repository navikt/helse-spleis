package no.nav.helse.spleis

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*
import org.intellij.lang.annotations.Language

class Issuer(private val navn: String) {
   private val privateKey: RSAPrivateKey
   private val publicKey: RSAPublicKey

   init {
      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(512)

      val keyPair = keyPairGenerator.genKeyPair()
      privateKey = keyPair.private as RSAPrivateKey
      publicKey = keyPair.public as RSAPublicKey
   }

   companion object {
      private const val SUBJECT = "en_saksbehandler_ident"
      private val GROUPS = listOf("sykepenger-saksbehandler-gruppe").toTypedArray()
      val AUDIENCE = "spleis_azure_ad_app_id"
   }

   fun createToken(audience: String = AUDIENCE): String {
      val algorithm = Algorithm.RSA256(publicKey, privateKey)

      return JWT.create()
         .withIssuer(navn)
         .withAudience(audience)
         .withKeyId("key-1234")
         .withSubject(SUBJECT)
         .withArrayClaim("groups", GROUPS)
         .sign(algorithm)
   }

   @Language("JSON")
   val jwks = """
   {
       "keys": [
           {
               "kty": "RSA",
               "alg": "RS256",
               "kid": "key-1234",
               "e": "${Base64.getUrlEncoder().encodeToString(publicKey.publicExponent.toByteArray())}",
               "n": "${Base64.getUrlEncoder().encodeToString(publicKey.modulus.toByteArray())}"
           }
       ]
   }
   """
}

class JwtStub(private val navn: String, private val wireMockServer: WireMockServer) {

   private val issuer = Issuer(navn)

   init {
      val client = WireMock.create().port(wireMockServer.port()).build()
      WireMock.configureFor(client)
   }

   fun createTokenFor(audience: String = Issuer.AUDIENCE) =
      issuer.createToken(audience)

   fun stubbedJwkProvider() = WireMock.get(WireMock.urlPathEqualTo("/jwks")).willReturn(WireMock.okJson(issuer.jwks))

   fun stubbedConfigProvider() = WireMock.get(WireMock.urlPathEqualTo("/config")).willReturn(
      WireMock.okJson("""
      {
          "jwks_uri": "${wireMockServer.baseUrl()}/jwks",
          "token_endpoint": "${wireMockServer.baseUrl()}/token",
          "issuer": "$navn"
      }
      """
      )
   )
}
