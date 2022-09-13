package no.nav.helse.person

// Alle Varselkoder må følge formatet
private val kodeFormat = "^\\S_\\S{2}_\\d{1,3}$".toRegex()

enum class Varselkode(private val melding: String) {

    /** Inntektsmeldingen og spleis er uenige om AGP */
    V_IM_1("Inntektsmeldingen og vedtaksløsningen er uenige om beregningen av arbeidsgiverperioden. Undersøk hva som er riktig arbeidsgiverperiode.");


    init {
        require(this.name.matches(kodeFormat)) {"Ugyldig varselkode-format: ${this.name}"}
    }

    internal fun varsel(kontekster: List<SpesifikkKontekst>) =
        Aktivitetslogg.Aktivitet.Varsel.opprett(kontekster, melding)
}
