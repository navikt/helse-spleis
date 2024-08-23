import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import no.nav.helse.dto.tilSpannerPersonDto
import no.nav.helse.person.Person
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
    override fun testSuccessful(context: ExtensionContext?) {
        // sjekk at vi har spanner-config
        val spannerConfigJson = {}.javaClass.getResource("/spanner_config.json")?.readText(Charsets.UTF_8)
        if (spannerConfigJson == null) Error("Fant ikke spanner_config.json. Finnes den i resources-mmappen?")
        val tree = objectMapper.readTree(spannerConfigJson)
        val map: Map<String, String> = objectMapper.treeToValue(tree)
        // sjekk at vi har feltene vi trenger i spanner-config
        check(map.containsKey("filplassering") && map.containsKey("person_lokal_uuid") && map.containsKey("spanner_frontend_port"))

        // fisk ut person på et vis og opprett SpannerDto
        val person = context!!.testInstance.get().get("person") as Person
        val dto = person.dto().tilSpannerPersonDto()

        // skriver spannerDto til filsystem
        val writeLocation = map["filplassering"] + "/jsonPersonLokalFraSpleis.json"
        val json = objectMapper.writeValueAsString(dto)
        val fil = File(writeLocation)
        if (!fil.exists()) {
            fil.createNewFile()
        }
        fil.printWriter().use { printer ->
            printer.println(json)
        }

        // Åpne i browser på din supercomputer
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            try {
                val port = map["spanner_frontend_port"]
                val uuid = map["person_lokal_uuid"]
                val uri = URI("http://localhost:$port/person/$uuid")
                desktop.browse(uri)
            } catch (excp: IOException) {
                excp.printStackTrace()
            } catch (excp: URISyntaxException) {
                excp.printStackTrace()
            }
        } else {
            Error("Desktop er ikke støttet")
        }
    }
}