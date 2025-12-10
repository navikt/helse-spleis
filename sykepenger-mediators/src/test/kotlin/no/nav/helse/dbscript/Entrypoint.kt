package no.nav.helse.dbscript

fun main() {
    Entrypoint.start()
}

internal object Entrypoint {
    private val tilgjengeligseScript = mapOf(
        1 to Personeditor,
        2 to HoppOverMelding
    )

    fun start() {
        println("## Velkommen til spleis sine DbScripts!")
        println(" - Husk at du alltid kan skrive 'exit' for å putte en kniven i sideflesket mitt 🔪")
        println()

        println("Hvordan vil du koble deg til databasen?")
        println(" - [1] Scriptet kobler meg opp (default)")
        println(" - [2] Jeg er allerede koblet til & vil oppgi connection info manuelt")
        val valgtConnectionmåte = Input.ventPåInput("1") { it.toIntOrNull() in setOf(1, 2) }.toInt()
        println()

        val scriptWrapper = when (valgtConnectionmåte) {
            1 -> AutomatiskOppkobling::start
            2 -> ManuellOppkobling::start
            else -> error("Hvordan kom vi hit?")
        }

        println("Her er de tilgjengelige scriptene. Velg det du vil kjøre.")
        tilgjengeligseScript.forEach { (valg, script) ->
            println(" - [$valg] ${script::class.simpleName}: ${script.beskrivelse}")
        }

        val valgtScript = Input.ventPåInput { it.toIntOrNull() in tilgjengeligseScript.keys }.toInt()
        println()

        scriptWrapper { connectionInfo ->
            tilgjengeligseScript.getValue(valgtScript).start(connectionInfo)
        }
    }
}
