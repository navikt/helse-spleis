package no.nav.helse.spleis

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration

fun <T> retry(
    timeout: Duration = Duration.ofSeconds(5),
    whatToRetry: () -> T,
): T {
    val starttime = System.currentTimeMillis()
    lateinit var exception: Exception
    do {
        try {
            return whatToRetry()
        } catch (e: Exception) {
            exception = e
        }
        runBlocking { delay(100L) }
    } while ((System.currentTimeMillis() - starttime) < timeout.toMillis())
    throw RuntimeException("Gav opp å vente på OK resultat etter ${timeout.toMillis()} ms", exception)
}
