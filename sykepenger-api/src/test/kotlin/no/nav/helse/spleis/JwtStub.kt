package no.nav.helse.spleis

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import org.intellij.lang.annotations.Language

class Issuer(val navn: String) {
   companion object {
      private const val SUBJECT = "en_saksbehandler_ident"
      private val GROUPS = listOf("sykepenger-saksbehandler-gruppe").toTypedArray()
      val AUDIENCE = "spleis_azure_ad_app_id"
   }

   private val privateKey: RSAPrivateKey
   private val publicKey: RSAPublicKey

   init {
      val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
      keyPairGenerator.initialize(512)

      val keyPair = keyPairGenerator.genKeyPair()
      privateKey = keyPair.private as RSAPrivateKey
      publicKey = keyPair.public as RSAPublicKey
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