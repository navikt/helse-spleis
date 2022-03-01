package no.nav.helse.hendelser

import no.nav.helse.person.IAktivitetslogg

class Medlemskapsvurdering(
    internal val medlemskapstatus: Medlemskapstatus
) {
    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        return when (medlemskapstatus) {
            Medlemskapstatus.Ja -> {
                aktivitetslogg.info("Bruker er medlem av Folketrygden")
                true
            }
            Medlemskapstatus.VetIkke -> {
                aktivitetslogg.warn("Vurder lovvalg og medlemskap")
                true
            }
            Medlemskapstatus.Nei -> {
                aktivitetslogg.warn("Perioden er avslått på grunn av at den sykmeldte ikke er medlem av Folketrygden")
                false
            }
        }
    }

    enum class Medlemskapstatus {
        Ja, Nei, VetIkke
    }
}
