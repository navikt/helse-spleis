package no.nav.helse.utbetalingslinjer

enum class Fagområde(
    val verdi: String,
    val klassekode: Klassekode
) {
    SykepengerRefusjon("SPREF", Klassekode.RefusjonIkkeOpplysningspliktig),
    Sykepenger("SP", Klassekode.SykepengerArbeidstakerOrdinær);

    override fun toString() = verdi
}
