package no.nav.helse.utbetalingslinjer

enum class Fagområde(val verdi: String) {
    SykepengerRefusjon("SPREF"),
    Sykepenger("SP");

    override fun toString() = verdi
}
