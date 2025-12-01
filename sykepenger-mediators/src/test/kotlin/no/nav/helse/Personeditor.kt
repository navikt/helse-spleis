package no.nav.helse

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.sql_dsl.connection
import com.github.navikt.tbd_libs.sql_dsl.firstOrNull
import com.github.navikt.tbd_libs.sql_dsl.transaction
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.BufferedReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode

fun main() {
    println("## Fyll inn fødselsnummer på personen det skal endres på")
    val fødselsnummer = ventPåInput { it.length == 11 && runCatching { it.toLong() }.isSuccess }

    startDbProxy { connectionInfo ->
        println("Mottok connection info: $connectionInfo")
        println("========================================")
        println()
        Personeditor(fødselsnummer, connectionInfo).start()
    }
}

internal class Personeditor(
    private val fødselsnummer: String,
    private val connectionInfo: ConnectionInfo
) {
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

        gåVidereVedJa("Ønsker du å gå videre å gå videre med å endre på '$fødselsnummer'? ⚠️", false)

        println("## Beskriv _hvorfor_ du gjør denne endringen (for auditlog) - minst 15 makreller lang 🤏")
        val beskrivelse = ventPåInput { it.trim().length >= 15 }

        fådetpå(
            jdbcUrl = connectionInfo.jdbcUrl,
            fødselsnummer = fødselsnummer,
            epost = connectionInfo.epost,
            beskrivelse = beskrivelse,
            workingdirectory = workingDirectory,
            backupsdirectory = backupDirectory
        )
    }

    private fun fådetpå(jdbcUrl: String, fødselsnummer: String, epost: String, beskrivelse: String, workingdirectory: Path, backupsdirectory: Path) {
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
                val diff = diff(data, resultat)
                println("\n$diff")

                gåVidereVedJa("Ser endringene bra ut? Nå er det no way back om du sier ja ⚠️", default = false)

                check(1 == prepareStatement("UPDATE person SET data=? WHERE fnr=?").use { stmt ->
                    stmt.setString(1, resultat)
                    stmt.setLong(2, fødselsnummer.toLong())
                    stmt.executeUpdate()
                }) { "forventet å oppdatere nøyaktig én rad ved oppdatering av person" }

                check(1 == prepareStatement("INSERT INTO auditlog (personidentifikator, epost, diff, beskrivelse) VALUES (?,?,?,?)").use { stmt ->
                    stmt.setString(1, fødselsnummer)
                    stmt.setString(2, epost)
                    stmt.setString(3, diff)
                    stmt.setString(4, beskrivelse)
                    stmt.executeUpdate()
                }) { "forventet å oppdatere nøyaktig én rad ved auditlogging" }

                println(" - Endringene dine er live ✅")
            }
        }
    }

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
        val dto = try { SerialisertPerson(json, SerialisertPerson.gjeldendeVersjon()).tilPersonDto() } catch (feil: Exception) {
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
        .replace("Unexpected", "Nytt felt")

    private val printer = DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
    }
    private val objectMapper = jacksonObjectMapper()
}

internal data class ConnectionInfo(val jdbcUrl: String, val epost: String)

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

private fun startDbProxy(port: Int? = null, block: (ConnectionInfo) -> Unit) {
    val proxyPort = port ?: java.net.ServerSocket(0).use { it.localPort }
    val stdoutChannel = Channel<String>(Channel.UNLIMITED)
    val stderrChannel = Channel<String>(Channel.UNLIMITED)
    val connectionInfoChannel = Channel<ConnectionInfo>(Channel.CONFLATED)
    val unauthenticatedChannel = Channel<Boolean>(Channel.CONFLATED)
    val stopSignal = Channel<Boolean>(Channel.RENDEZVOUS)
    val proxyComplectionChannel = Channel<Int>(Channel.CONFLATED)

    runBlocking {
        val streamJob = launch(Dispatchers.IO) {
            lookForConnectionDetailsAndPrintOutput(stdoutChannel, stderrChannel, connectionInfoChannel, unauthenticatedChannel)
        }

        println("Starter proxy")

        var connected = false
        do {
            val dbproxyJob = dbproxyJob(proxyPort, stopSignal, proxyComplectionChannel, stdoutChannel, stderrChannel)

            println("Venter på proxyen …")

            select {
                connectionInfoChannel.onReceive { connectionInfo ->
                    connected = true
                    println("OK, ting er oppe og går!")
                    try {
                        block(connectionInfo)
                    } finally {
                        stopSignal.send(true)

                        dbproxyJob.cancel()
                        streamJob.cancel()

                        println("Avslutter")
                    }
                }
                proxyComplectionChannel.onReceive { exitCode ->
                    println("Prosessen 'nais postgres proxy' avsluttet med kode $exitCode")
                }
                unauthenticatedChannel.onReceive { _ ->
                    println("DU ER IKKE LOGGET INN, prøver å fikse det for deg!")
                    Runtime.getRuntime().exec(arrayOf("nais", "login")).waitFor()
                    delay(500)
                }
            }
        } while (!connected)
    }
}

private suspend fun CoroutineScope.lookForConnectionDetailsAndPrintOutput(
    stdoutChannel: ReceiveChannel<String>,
    stderrChannel: ReceiveChannel<String>,
    connectionInfoChannel: SendChannel<ConnectionInfo>,
    unauthenticatedChannel: SendChannel<Boolean>
) {
    val connectionStringRegex = "jdbc:\\S+".toRegex()
    val emailRegex = "user=(\\S+)".toRegex()

    var connectionInfo: ConnectionInfo? = null
    var listening = false

    try {
        while (true) {
            select {
                stdoutChannel.onReceive { line ->
                    println("[STDOUT] $line")

                    if (connectionInfo == null) {
                        connectionStringRegex.find(line)?.groupValues?.singleOrNull()?.also {
                            val email = emailRegex.find(it)!!.groupValues[1]
                            connectionInfo = ConnectionInfo(it, email)
                        }
                    }

                    if (!listening) {
                        listening = line.startsWith("Listening on ")
                    }

                    if (listening && connectionInfo != null) {
                        connectionInfoChannel.send(connectionInfo)
                    }
                }
                stderrChannel.onReceive { line ->
                    if (line.contains("missing active user")) {
                        unauthenticatedChannel.send(true)
                    }
                    println("[STDERR] $line")
                }
            }
        }
    } catch (err: Exception) {
        println("Feil i streamJob: ${err.message}")
    }
}

private fun CoroutineScope.dbproxyJob(
    port: Int,
    stopSignal: ReceiveChannel<Boolean>,
    completionChannel: SendChannel<Int>,
    stdoutChannel: SendChannel<String>,
    stderrChannel: SendChannel<String>
): Job {
    return async(Dispatchers.IO) {
        try {
            startProcessAndStreamOutput(arrayOf("nais", "postgres", "proxy", "--port", "$port", "spleis"), stopSignal, completionChannel, stdoutChannel, stderrChannel)
        } catch (err: Exception) {
            println("Feil i dbproxyJob: ${err.message}")
        }
    }
}

private suspend fun CoroutineScope.startProcessAndStreamOutput(
    cmd: Array<String>,
    stopSignal: ReceiveChannel<Boolean>,
    completionChannel: SendChannel<Int>,
    stdoutChannel: SendChannel<String>,
    stderrChannel: SendChannel<String>
) {
    val process = Runtime.getRuntime().exec(cmd)

    val stdoutJob = consumeReader(process.inputReader(), stdoutChannel)
    val stderrJob = consumeReader(process.errorReader(), stderrChannel)

    val processJob = launch(Dispatchers.IO) {
        completionChannel.send(process.waitFor())
    }
    val stopJob = launch(Dispatchers.IO) {
        // Vent på stoppsignal
        stopSignal.receive()
        process.destroy()
    }

    select {
        processJob.onJoin {
            stopJob.cancel()
            stdoutJob.cancel()
            stderrJob.cancel()
        }
        stopJob.onJoin {
            processJob.cancel()
            stdoutJob.cancel()
            stderrJob.cancel()
        }
    }
}

private fun CoroutineScope.consumeReader(reader: BufferedReader, output: SendChannel<String>): Job {
    return launch(Dispatchers.IO) {
        reader.use { reader ->
            reader.forEachLine { line ->
                launch { output.send(line) }
            }
        }
    }
}
