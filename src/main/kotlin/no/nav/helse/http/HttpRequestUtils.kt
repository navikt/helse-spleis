package no.nav.helse.http

import arrow.core.Try
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.httpGet
import org.json.JSONObject

fun String.getJson() =
   Try {
      toJson(this.httpGet().responseString())
   }.toEither()

private fun toJson(fuelResult: ResponseResultOf<String>): JSONObject {
   val (request, response, result) = fuelResult
   return when (response.statusCode) {
      200  -> JSONObject(result.component1())
      else -> throw Exception("got status ${response.statusCode} from ${request.url.toExternalForm()}.")
   }
}
