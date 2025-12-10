package no.nav.helse.dbscript

import java.io.BufferedReader
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
import no.nav.helse.dbscript.DbScript.ConnectionInfo

internal object AutomatiskOppkobling {
    fun start(block: (ConnectionInfo) -> Unit) {
        val proxyPort = java.net.ServerSocket(0).use { it.localPort }
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

    private suspend fun lookForConnectionDetailsAndPrintOutput(
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
                                connectionInfo = ConnectionInfo(it, Input.Epost(email))
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
}
