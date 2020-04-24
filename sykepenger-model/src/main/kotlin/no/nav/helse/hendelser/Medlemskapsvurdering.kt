package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg

class Medlemskapsvurdering(
    internal val medlemskapstatus: Medlemskapstatus
) {
    internal fun valider(aktivitetslogg: Aktivitetslogg): Aktivitetslogg {
        when (medlemskapstatus) {
            Medlemskapstatus.Ja -> aktivitetslogg.info("Bruker er medlem av Folketrygden")
            Medlemskapstatus.VetIkke -> aktivitetslogg.warn("Lovvalg og medlemskap må vurderes manuelt")
            Medlemskapstatus.Nei -> aktivitetslogg.error("Bruker er ikke medlem av Folketrygden")
        }
        return aktivitetslogg
    }

    enum class Medlemskapstatus {
        Ja, Nei, VetIkke
    }
}
