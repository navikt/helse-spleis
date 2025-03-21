package no.nav.helse

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.firstOrNull
import com.github.navikt.tbd_libs.sql_dsl.transaction
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode

fun main() {
    Personeditor.start()
}

internal object Personeditor {
    internal fun start() {
        println("## Velkommen til personeditoren")
        println(" - Dette blir gøyalt, hold deg fast 🎢")
        println(" - Du må selv koble opp 'nais postgres proxy' før du klasker i gang!")
        println(" - Og husk at du alltid kan skrive 'exit' for å putte en kniven i sideflesket mitt 🔪")

        val defaultPath = "${System.getenv("HOME")}/Desktop"
        println("## Fyll inn pathen")
        println(" - Pathen for hvor jeg skal legg filer. Default er '$defaultPath'")
        val path = ventPåInput(defaultPath) { kotlin.runCatching { Path.of(it) }.isSuccess }.removeSuffix("/")

        val workingDirectory = Path.of("$path/Personeditor")
        val backupDirectory = Path.of("$path/Personeditor/Backups").apply {
            Files.createDirectories(this)
        }

        println(" - Legger arbeidsfiler på '${workingDirectory}'")
        println(" - ..og lagrer backups på '${backupDirectory}'")

        val jdbcUrl = ventPåJdbcUrl()

        println("## Fyll inn fødselsnummer på personen det skal endres på")
        val fødselsnummer = ventPåInput { it.length == 11 && kotlin.runCatching { it.toLong() }.isSuccess }

        gåVidereVedJa("Ønsker du å gå videre å gå videre med å endre på '$fødselsnummer'? ⚠️", false)

        fådetpå(
            jdbcUrl = jdbcUrl,
            fødselsnummer = fødselsnummer,
            workingdirectory = workingDirectory,
            backupsdirectory = backupDirectory
        )
    }

    private fun fådetpå(jdbcUrl: String, fødselsnummer: String, workingdirectory: Path, backupsdirectory: Path) {
        val id = "${LocalDateTime.now()}-${fødselsnummer}-${UUID.randomUUID()}"
        val backupfil = File("${backupsdirectory}/$id.json")
        val resultatfil = File("${workingdirectory}/$id.json")

        dataSource(jdbcUrl).connection {
            transaction {
                val data = prepareStatement("SELECT data FROM person where fnr=? FOR UPDATE;").use { stmt ->
                    stmt.setLong(1, fødselsnummer.toLong())
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
                gåVidereVedJa("Er du ferdig? Husket å lagre? 💾", default = false)

                val resultat = resultatfil.somJson()

                println("## Dette er endringene du har gjort")
                println("\n${diff(data, resultat)}")

                gåVidereVedJa("Ser endringene bra ut? Nå er det no way back om du sier ja ⚠️", default = false)

                check(1 == prepareStatement("UPDATE person SET data=? WHERE fnr=?").use { stmt ->
                    stmt.setString(1, resultat)
                    stmt.setLong(2, fødselsnummer.toLong())
                    stmt.executeUpdate()
                }) { "forventet å oppdatere nøyaktig én rad" }

                println(" - Endringene dine er live ✅")
            }
        }
    }

    private fun ventPåInput(default: String? = null, valider: (input: String) -> Boolean): String {
        var svar: String?
        do {
            svar = readlnOrNull()?.lowercase()?.let { input ->
                if (input == "exit") error("💀 Avslutter prosessen")
                if (default != null && input.isEmpty()) return@let default
                if (!valider(input)) {
                    println("🙅 '$input' er ikke gyldig!")
                    return@let null
                }
                input
            }
        } while (svar == null)
        return svar
    }

    private fun ventPåJdbcUrl(): String {
        println("## Fyll inn databaseport. Defaulten er '5432'")
        val defaultPort = "5432"
        val port = ventPåInput(defaultPort) { it.length == 4 && kotlin.runCatching { it.toInt() }.isSuccess }
        val defaultEpost = hentEpostFraGCloud()
        when (defaultEpost) {
            null -> println("## Fyll inn brukernavn (epost)")
            else -> println("## Fyll inn brukernavn (epost). Defaulten er '$defaultEpost'")
        }
        val epost = ventPåInput(defaultEpost) { it.endsWith("@nav.no") }
        val jdbcUrl = "jdbc:postgresql://localhost:$port/spleis?user=${epost}"
        println(" - Bruker JdbcUrl '$jdbcUrl'")
        return jdbcUrl
    }

    private fun hentEpostFraGCloud(): String? {
        val identityToken = hentIdentityTokenFraGCloud() ?: return null
        return try {
            val (_, payload, _) = identityToken.split('.', limit = 3)
            val json = objectMapper.readTree(Base64.getDecoder().decode(payload))
            return json.path("email").asText().takeIf { it.lowercase().endsWith("@nav.no") }
        } catch (_: Exception) { null }
    }

    private fun hentIdentityTokenFraGCloud() = try {
        Runtime.getRuntime().exec(arrayOf("gcloud", "auth", "print-identity-token")).let {
            val token = it.inputReader().readText().trim()
            it.waitFor()
            token.takeUnless { it.isBlank() }
        }
    } catch (_: Exception) { null }

    private fun gåVidereVedJa(hva: String, default: Boolean) {
        val (defaultSvar, valg) = when (default) {
            true -> "y" to "[Yn]"
            false -> "n" to "[yN]"
        }
        println("## $hva? $valg")
        if (ventPåInput(defaultSvar) { it in setOf("y", "n") } == "y") return
        error("❌ Avslutter prosessen siden du svarte nei")
    }

    private fun åpneFil(fil: File) = try {
        Runtime.getRuntime().exec(arrayOf("idea", fil.absolutePath))
    } catch (_: Exception) {}

    private fun File.somJson(): String {
        val json = try { objectMapper.readTree(readText()).toString() } catch (feil: Exception) {
            throw IllegalArgumentException("❌ Du har laget en ugylig json!", feil)
        }
        val dto = try { SerialisertPerson(json).tilPersonDto() } catch (feil: Exception) {
            throw IllegalArgumentException("❌ Du har laget en ugylig person-json!", feil)
        }
        try { Person.gjenopprett(Regelverkslogg.EmptyLog, dto) } catch (feil: Exception) {
            throw IllegalArgumentException("❌ Du har laget en ugylig person-json!", feil)
        }
        return json
    }

    private fun dataSource(jdbcUrl: String) = try {
        HikariDataSource(HikariConfig().apply {
            this.maximumPoolSize = 1
            this.jdbcUrl = jdbcUrl
        })
    } catch (feil: Exception) {
        throw IllegalArgumentException("❌ Klarte ikke koble opp mot databasen. Har du startet proxyen?", feil)
    }

    private fun diff(gammel: String, ny: String) = JSONCompare.compareJSON(gammel, ny, JSONCompareMode.STRICT).message
        .replace("Expected", "Endret fra")
        .replace("values", "verdier")
        .replace("but got", "til")
        .replace("got", "til")
        .replace("but none found", "<slettet>")

    private val printer = DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
    }
    private val objectMapper = jacksonObjectMapper()
}
