package no.nav.helse

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong

// Genererer garantert unike, syntaktisk gyldige (11 sifre) fødselsnumre til bruk i tester.
// Trygt å bruke på tvers av parallelle tester mot samme (delte) database, siden ingen
// to kall (heller ikke fra ulike tråder) returnerer samme verdi innad i én JVM-kjøring.
object Fødselsnumre {
    // dNNNNN som førsifre sørger for at genererte fødselsnumre ikke kolliderer med
    // håndskrevne test-fødselsnumre (som normalt starter med en gyldig dato, ddMMyy)
    private const val PREFIKS = 9

    // Tilfeldig startverdi (i stedet for en fast 1) gjør at separate JVM-kjøringer mot samme
    // (gjenbrukte) testdatabase praktisk talt aldri kolliderer med fødselsnumre skrevet av en
    // tidligere kjøring — f.eks. når Testcontainers-databasen gjenbrukes på tvers av kjøringer
    // (`.withReuse(true)`), slik den gjør fra IntelliJ. getAndIncrement() er fortsatt atomisk og
    // monotont stigende, så unikhet innad i én kjøring er fortsatt garantert.
    private val neste = AtomicLong(ThreadLocalRandom.current().nextLong(1, 9_000_000_000L))

    fun nytt(): String {
        val løpenummer = neste.getAndIncrement()
        check(løpenummer < 10_000_000_000L) { "Gikk tom for unike fødselsnumre til tester" }
        return "$PREFIKS" + løpenummer.toString().padStart(10, '0')
    }
}

fun nyttFødselsnummer() = Fødselsnumre.nytt()
