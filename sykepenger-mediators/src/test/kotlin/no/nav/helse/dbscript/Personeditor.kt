package no.nav.helse.dbscript

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.sql_dsl.firstOrNull
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID
import kotlin.use
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode

internal object Personeditor: DbScript() {
    override val beskrivelse = "Laster inn og låser en person i spleis for manuell redigering"

    override fun start(connectionInfo: ConnectionInfo) {
        println("## Velkommen til personeditoren")
        println(" - Dette blir gøyalt, hold deg fast 🎢")

        println("## Fyll inn fødselsnummer på personen det skal endres på")
        val fødselsnummer = Input.ventPåFødselsnummer()
        println()

        val defaultPath = "${System.getenv("HOME")}/Desktop"
        println("## Fyll inn pathen")
        println(" - Pathen for hvor jeg skal legg filer. Default er '$defaultPath'")
        val path = Input.ventPåInput(defaultPath) { kotlin.runCatching { Path.of(it) }.isSuccess }.removeSuffix("/")

        val workingDirectory = Path.of("$path/Personeditor")
        val backupDirectory = Path.of("$path/Personeditor/Backups").apply {
            Files.createDirectories(this)
        }

        println(" - Legger arbeidsfiler på '${workingDirectory}'")
        println(" - ..og lagrer backups på '${backupDirectory}'")

        Input.gåVidereVedJa("Ønsker du å gå videre å gå videre med å endre på '${fødselsnummer.verdi}'? ⚠️", false)
        println()

        println("## Beskriv _hvorfor_ du gjør denne endringen (for auditlog) - minst 15 makreller lang 🤏")
        val beskrivelse = Input.ventPåBeskrivelse()
        println()

        fådetpå(
            connectionInfo = connectionInfo,
            fødselsnummer = fødselsnummer,
            beskrivelse = beskrivelse,
            workingdirectory = workingDirectory,
            backupsdirectory = backupDirectory
        )
    }

    private fun fådetpå(connectionInfo: ConnectionInfo, fødselsnummer: Input.Fødselsnummer, beskrivelse: Input.Beskrivelse, workingdirectory: Path, backupsdirectory: Path) {
        val id = "${LocalDateTime.now()}-${fødselsnummer}-${UUID.randomUUID()}"
        val backupfil = File("${backupsdirectory}/$id.json")
        val resultatfil = File("${workingdirectory}/$id.json")

        databaseTransaksjon(connectionInfo) {
            val data = prepareStatement("SELECT data FROM person where fnr=? FOR UPDATE;").use { stmt ->
                stmt.setLong(1, fødselsnummer.verdi.toLong())
                stmt.executeQuery().use { rs ->
                    rs.firstOrNull { row -> row.getString("data") }
                }
            } ?: error("❌ Fant ikke person med fnr $fødselsnummer")
            with(backupfil) {
                createNewFile()
                writeText(data)
            }
            with(resultatfil) {
                createNewFile()
                writeText(objectMapper.writer(printer).writeValueAsString(objectMapper.readTree(data)))
            }

            println("## Nå er vi klar får å endre personen her 🥷")
            println(" - 🚨 Husk at personen nå er låst i databasen, så ikke gå på lunsj nå 🌯")
            println(" - Jeg har lagd en backup på om det skulle gå helt til skogen 🌳")
            println("   > ${backupfil.path}")
            println(" - Endre på filen, og husk å lagre før du går videre! 💾")
            println("   > ${resultatfil.path}")
            println("   > Jeg forsøker å åpne den for deg i IntelliJ, men lover ingenting")

            åpneFil(resultatfil)
            Input.gåVidereVedJa("Er du ferdig? Husket å lagre? 💾", default = false)
            println()

            val resultat = resultatfil.somJson()

            println("## Dette er endringene du har gjort")
            val diff = diff(data, resultat)
            println("\n$diff")

            Input.gåVidereVedJa("Ser endringene bra ut? Nå er det no way back om du sier ja ⚠️", default = false)
            println()

            check(1 == prepareStatement("UPDATE person SET data=? WHERE fnr=?").use { stmt ->
                stmt.setString(1, resultat)
                stmt.setLong(2, fødselsnummer.verdi.toLong())
                stmt.executeUpdate()
            }) { "forventet å oppdatere nøyaktig én rad ved oppdatering av person" }

            audit(fødselsnummer, connectionInfo.epost, diff, beskrivelse)

            println(" - Endringene dine er live ✅")
        }
    }

    private fun åpneFil(fil: File) = try {
        Runtime.getRuntime().exec(arrayOf("idea", fil.absolutePath))
    } catch (_: Exception) {}

    private fun File.somJson(): String {
        val json = try { objectMapper.readTree(readText()).toString() } catch (feil: Exception) {
            throw IllegalArgumentException("❌ Du har laget en ugylig json!", feil)
        }
        val dto = try { SerialisertPerson(json, SerialisertPerson.gjeldendeVersjon()).tilPersonDto() } catch (feil: Exception) {
            throw IllegalArgumentException("❌ Du har laget en ugylig person-json!", feil)
        }
        try { Person.gjenopprett(Regelverkslogg.EmptyLog, dto) } catch (feil: Exception) {
            throw IllegalArgumentException("❌ Du har laget en ugylig person-json!", feil)
        }
        return json
    }

    private fun diff(gammel: String, ny: String) = JSONCompare.compareJSON(gammel, ny, JSONCompareMode.STRICT).message
        .replace("Expected", "Endret fra")
        .replace("values", "verdier")
        .replace("but got", "til")
        .replace("got", "til")
        .replace("but none found", "<slettet>")
        .replace("Unexpected", "Nytt felt")

    private val printer = DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
    }

    private val objectMapper = jacksonObjectMapper()
}
