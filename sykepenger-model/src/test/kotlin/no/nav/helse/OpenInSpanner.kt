import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
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
        val spannerPerson = person.dto().tilSpannerPersonDto()
        val personJson = objectMapper.writeValueAsString(spannerPerson)
        // aktivitetsloggen krever litt mer greier ettersom spanner henter den fra sparsom på ekte
        val aktivtetsloggV2Json = SugUtAlleAktivitetene(person.personLogg)

        // trikser inn aktivitetsloggen. Dette burde sikkert gjøres på en bedre måte
        val json = "{" + aktivtetsloggV2Json.drop(1).dropLast(1) + "," + personJson.drop(1)

        // skriver spannerDto til filsystem
        val writeLocation = map["filplassering"] + "/jsonPersonLokalFraSpleis.json"
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