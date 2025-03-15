import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.format.DateTimeFormatter
import java.util.*
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dto.tilSpannerPersonDto
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(SpannerEtterTestInterceptor::class)
annotation class OpenInSpanner

class SpannerEtterTestInterceptor : TestWatcher {

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
    }

    override fun testFailed(context: ExtensionContext?, cause: Throwable?) {
        val errorMsg = cause.toString()
        openTheSpanner(context, errorMsg)
    }

    override fun testSuccessful(context: ExtensionContext?) {
        openTheSpanner(context)
    }

    internal fun openTheSpanner(context: ExtensionContext?, errorMsg: String? = null) {
        // fisk ut person på et vis og opprett SpannerDto
        val (person, personlogg) = when (val testcontext = context!!.testInstance.get()) {
            is AbstractDslTest -> testcontext.testperson.person to testcontext.testperson.personlogg
            is AbstractEndToEndTest -> testcontext.person to testcontext.personlogg
            else -> error("Kjenner ikke til testcontext-typen, og kan derfor ikke finne testperson!")
        }
        val spannerPerson = person.dto().tilSpannerPersonDto()
        val personJson = objectMapper.writeValueAsString(spannerPerson)

        // aktivitetsloggen krever litt mer greier ettersom spanner henter den fra sparsom på ekte
        val aktivtetsloggV2Json = SugUtAlleAktivitetene(personlogg)

        // trikser inn aktivitetsloggen. Dette burde sikkert gjøres på en bedre måte
        val json = "{" + aktivtetsloggV2Json.drop(1).dropLast(1) + "," + personJson.drop(1)

        // poster tullegutten til spannerish
        val testnavn = context.testMethod.get().name
        val uri = lastOppTilSpannerish(json, testnavn, errorMsg)

        // Åpne i browser på din supercomputer
        åpneBrowser(uri)
    }

    private fun lastOppTilSpannerish(json: String, testnavn: String, errorMsg: String?): URI {
        val uuid = UUID.randomUUID()
        HttpClient.newHttpClient().use { client ->
            // Må laste opp mot intern.dev pga. en Microsoft-redirect på ansatt.dev. Så må ha naisdevicen på
            val request = HttpRequest.newBuilder(URI("https://spannerish.intern.dev.nav.no/api/person/$uuid"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            check(response.statusCode() == 201) { "Det var sprøtt, fikk http status ${response.statusCode()} fra Spannerish. Kanskje du ikke er nais device?" }
        }
        // Litt gøyale query parametre til Spannerish
        val queryString = mapOf("testnavn" to testnavn, "error" to errorMsg).queryString

        // Men vi kan se på den i ansatt.dev da, så kan den deles med folk uten naisdevice
        return URI("https://spannerish.ansatt.dev.nav.no/person/$uuid${queryString}")
    }

    private val Map<String, String?>.queryString
        get() = this
            .filterNot { (_, value) -> value.isNullOrBlank() }.entries
            .joinToString("&") { (key, value) -> "${key}=${URLEncoder.encode(value, "UTF-8")}" }
            .let { if (it.isBlank()) "" else "?$it" }

    private fun åpneBrowser(uri: URI) {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            try {
                desktop.browse(uri)
            } catch (excp: IOException) {
                excp.printStackTrace()
            } catch (excp: URISyntaxException) {
                excp.printStackTrace()
            }
        } else {
            error("Desktop er ikke støttet")
        }
    }

    private fun SugUtAlleAktivitetene(aktivitetslogg: Aktivitetslogg): String {
        val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val liste = aktivitetslogg.aktiviteter.map {
            when (it) {
                is Aktivitet.Behov -> AktivitetDto(
                    id = 0,
                    nivå = "BEHOV",
                    tekst = it.melding,
                    tidsstempel = it.tidsstempel.format(tidsstempelformat),
                    kontekster = it.kontekster.associateBy({ it.kontekstType }, { it.kontekstMap })
                )

                is Aktivitet.FunksjonellFeil -> AktivitetDto(
                    id = 0,
                    nivå = "FUNKSJONELL_FEIL",
                    tekst = it.melding,
                    tidsstempel = it.tidsstempel.format(tidsstempelformat),
                    kontekster = it.kontekster.associateBy({ it.kontekstType }, { it.kontekstMap })
                )

                is Aktivitet.Info -> AktivitetDto(
                    id = 0,
                    nivå = "INFO",
                    tekst = it.melding,
                    tidsstempel = it.tidsstempel.format(tidsstempelformat),
                    kontekster = it.kontekster.associateBy({ it.kontekstType }, { it.kontekstMap })
                )

                is Aktivitet.LogiskFeil -> AktivitetDto(
                    id = 0,
                    nivå = "LOGISK_FEIL",
                    tekst = it.melding,
                    tidsstempel = it.tidsstempel.format(tidsstempelformat),
                    kontekster = it.kontekster.associateBy({ it.kontekstType }, { it.kontekstMap })
                )

                is Aktivitet.Varsel -> AktivitetDto(
                    id = 0,
                    nivå = "VARSEL",
                    tekst = it.melding,
                    tidsstempel = it.tidsstempel.format(tidsstempelformat),
                    kontekster = it.kontekster.associateBy({ it.kontekstType }, { it.kontekstMap })
                )
            }
        }

        val aktiviteter = mapOf("aktiviteter" to liste)
        val aktivitetsloggV2 = mapOf("aktivitetsloggV2" to aktiviteter)
        val aktivtetsloggV2Json = objectMapper.writeValueAsString(aktivitetsloggV2)
        return aktivtetsloggV2Json
    }

    private data class AktivitetDto(
        val id: Int,
        val nivå: String,
        val tekst: String,
        val tidsstempel: String,
        val kontekster: Map<String, Any>
    )

}
