package no.nav.helse

fun main() {
    //val applicationBuilder = ApplicationBuilder(System.getenv())
    //applicationBuilder.start()
    ProbeApi().start()
    DataSourceBuilder(System.getenv()).migrate()
}
