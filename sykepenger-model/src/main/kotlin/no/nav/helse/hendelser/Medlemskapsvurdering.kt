package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Periodetype.FORLENGELSE
import no.nav.helse.person.Periodetype.INFOTRYGDFORLENGELSE

class Medlemskapsvurdering(
    internal val medlemskapstatus: Medlemskapstatus
) {
    internal fun valider(aktivitetslogg: Aktivitetslogg, periodetype: Periodetype): Aktivitetslogg {
        when (medlemskapstatus) {
            Medlemskapstatus.Ja -> aktivitetslogg.info("Bruker er medlem av Folketrygden")
            Medlemskapstatus.VetIkke -> {
                val melding = "Vurder lovvalg og medlemskap"
                if (periodetype in listOf(INFOTRYGDFORLENGELSE, FORLENGELSE)) aktivitetslogg.info(melding)
                else aktivitetslogg.warn(melding)
            }
            Medlemskapstatus.Nei -> aktivitetslogg.error("Bruker er ikke medlem av Folketrygden")
        }
        return aktivitetslogg
    }

    enum class Medlemskapstatus {
        Ja, Nei, VetIkke
    }
}
