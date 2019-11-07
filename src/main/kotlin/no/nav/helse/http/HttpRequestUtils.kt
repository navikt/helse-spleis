package no.nav.helse.http

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private val objectMapper = ObjectMapper()

fun String.getJson(): JsonNode {
   val (responseCode, responseBody) = this.fetchUrl()

   if (responseCode >= 300 || responseBody == null) {
      throw Exception("got status $responseCode from ${this}.")
   }
   return objectMapper.readTree(responseBody)
}

private fun String.fetchUrl() = with(URL(this).openConnection() as HttpURLConnection) {
      requestMethod = "GET"

      val stream: InputStream? = if (responseCode < 300) this.inputStream else this.errorStream
      responseCode to stream?.bufferedReader()?.readText()
   }
