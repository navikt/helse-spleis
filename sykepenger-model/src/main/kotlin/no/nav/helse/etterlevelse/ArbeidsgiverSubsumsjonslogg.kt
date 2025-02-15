package no.nav.helse.etterlevelse

class ArbeidsgiverSubsumsjonslogg(
    private val regelverkslogg: Regelverkslogg,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String
) : Subsumsjonslogg {

    override fun logg(subsumsjon: Subsumsjon) {
        if (subsumsjon.erTomPeriode()) return
        regelverkslogg.logg(Regelverksporing.Arbeidsgiversporing(
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            subsumsjon = subsumsjon
        ))
    }
}
