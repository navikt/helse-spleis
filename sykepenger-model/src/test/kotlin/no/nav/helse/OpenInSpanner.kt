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
import java.util.UUID
import no.nav.helse.dto.tilSpannerPersonDto
import no.nav.helse.inspectors.personLogg
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.AktivitetsloggVisitor
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
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
        val person = context!!.testInstance.get().get("person") as Person
        val spannerPerson = person.dto().tilSpannerPersonDto()
        val personJson = objectMapper.writeValueAsString(spannerPerson)

        // aktivitetsloggen krever litt mer greier ettersom spanner henter den fra sparsom på ekte
        val aktivtetsloggV2Json = SugUtAlleAktivitetene(person.personLogg)

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
            check(response.statusCode() == 201) { "Det var sprøtt, fikk http status ${response.statusCode()} fra Spannerish. Kanskje du ikke er nais device?"}
        }
        // Men vi kan se på den i ansatt.dev da, så kan den deles med folk uten naisdevice
        val urlEncodedTestName = URLEncoder.encode(testnavn, "UTF-8")
        val urlEncodedErrorName = errorMsg?.let { URLEncoder.encode(it, "UTF-8")}
        if (urlEncodedErrorName != null) {
            return URI("https://spannerish.ansatt.dev.nav.no/person/$uuid?testnavn=$urlEncodedTestName?error=$urlEncodedErrorName")
        }
        return URI("https://spannerish.ansatt.dev.nav.no/person/$uuid?testnavn=$urlEncodedTestName")
    }

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
        val liste = mutableListOf<AktivitetDto>()

        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitInfo(
                id: UUID,
                kontekster: List<SpesifikkKontekst>,
                aktivitet: Aktivitet.Info,
                melding: String,
                tidsstempel: String
            ) {
                liste.add(AktivitetDto(
                    id = 0,
                    nivå = "INFO",
                    tekst = melding,
                    tidsstempel = tidsstempel,
                    kontekster = kontekster.associateBy({ it.kontekstType }, {it.kontekstMap})
                ))
            }

            override fun visitVarsel(
                id: UUID,
                kontekster: List<SpesifikkKontekst>,
                aktivitet: Aktivitet.Varsel,
                kode: Varselkode?,
                melding: String,
                tidsstempel: String
            ) {
                liste.add(AktivitetDto(
                    id = 0,
                    nivå = "VARSEL",
                    tekst = melding,
                    tidsstempel = tidsstempel,
                    kontekster = kontekster.associateBy({ it.kontekstType }, {it.kontekstMap})
                ))
            }

            override fun visitBehov(
                id: UUID,
                kontekster: List<SpesifikkKontekst>,
                aktivitet: Aktivitet.Behov,
                type: Aktivitet.Behov.Behovtype,
                melding: String,
                detaljer: Map<String, Any?>,
                tidsstempel: String
            ) {
                liste.add(AktivitetDto(
                    id = 0,
                    nivå = "BEHOV",
                    tekst = melding,
                    tidsstempel = tidsstempel,
                    kontekster = kontekster.associateBy({ it.kontekstType }, {it.kontekstMap})
                ))
            }

            override fun visitLogiskFeil(
                id: UUID,
                kontekster: List<SpesifikkKontekst>,
                aktivitet: Aktivitet.LogiskFeil,
                melding: String,
                tidsstempel: String
            ) {
                liste.add(AktivitetDto(
                    id = 0,
                    nivå = "LOGISK_FEIL",
                    tekst = melding,
                    tidsstempel = tidsstempel,
                    kontekster = kontekster.associateBy({ it.kontekstType }, {it.kontekstMap})
                ))
            }

            override fun visitFunksjonellFeil(
                id: UUID,
                kontekster: List<SpesifikkKontekst>,
                aktivitet: Aktivitet.FunksjonellFeil,
                melding: String,
                tidsstempel: String
            ) {
                liste.add(AktivitetDto(
                    id = 0,
                    nivå = "FUNKSJONELL_FEIL",
                    tekst = melding,
                    tidsstempel = tidsstempel,
                    kontekster = kontekster.associateBy({ it.kontekstType }, {it.kontekstMap})
                ))
            }
        })

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