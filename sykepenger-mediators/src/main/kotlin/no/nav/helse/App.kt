package no.nav.helse

import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
fun main() {
    val applicationBuilder = ApplicationBuilder(System.getenv())
    applicationBuilder.start()
}
