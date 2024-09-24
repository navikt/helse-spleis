package no.nav.helse.etterlevelse

enum class KontekstType {
    Fødselsnummer,
    Organisasjonsnummer,
    Vedtaksperiode,
    Sykmelding,
    Søknad,
    Inntektsmelding,
    OverstyrTidslinje,
    OverstyrInntekt,
    OverstyrRefusjon,
    OverstyrArbeidsgiveropplysninger,
    OverstyrArbeidsforhold,
    SkjønnsmessigFastsettelse,
    AndreYtelser
}

data class Subsumsjonskontekst(
    val type: KontekstType,
    val verdi: String,
    val forelder: Subsumsjonskontekst? = null
) {
    fun barn(kontekst: Subsumsjonskontekst): Subsumsjonskontekst {
        check(kontekster().none { it.type == kontekst.type }) { "denne kontekst-kjeden består allerede av ${kontekst.type}" }
        return kontekst.copy(forelder = this)
    }

    fun kontekster(): List<Subsumsjonskontekst> {
        var kontekster = mutableListOf<Subsumsjonskontekst>()
        var nåværende: Subsumsjonskontekst? = this
        while (nåværende != null) {
            kontekster.add(nåværende)
            nåværende = nåværende.forelder
        }
        return kontekster.toList()
    }
}