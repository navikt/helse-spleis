package no.nav.helse.hendelser

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Periodetype.FORLENGELSE
import no.nav.helse.person.Periodetype.INFOTRYGDFORLENGELSE

class Medlemskapsvurdering(
    internal val medlemskapstatus: Medlemskapstatus
) {
    internal fun valider(aktivitetslogg: IAktivitetslogg, periodetype: Periodetype): Boolean {
        return when (medlemskapstatus) {
            Medlemskapstatus.Ja -> {
                aktivitetslogg.info("Bruker er medlem av Folketrygden")
                true
            }
            Medlemskapstatus.VetIkke -> {
                val melding = "Vurder lovvalg og medlemskap"
                if (periodetype in listOf(INFOTRYGDFORLENGELSE, FORLENGELSE)) {
                    aktivitetslogg.info(melding)
                }
                else aktivitetslogg.warn(melding)
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
