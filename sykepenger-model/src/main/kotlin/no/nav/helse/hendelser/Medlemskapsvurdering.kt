package no.nav.helse.hendelser

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Varselkode.RV_MV_1

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
                aktivitetslogg.varsel(RV_MV_1)
                true
            }
            Medlemskapstatus.Nei -> {
                aktivitetslogg.varsel("Perioden er avslått på grunn av at den sykmeldte ikke er medlem av Folketrygden")
                false
            }
        }
    }

    enum class Medlemskapstatus {
        Ja, Nei, VetIkke
    }
}
