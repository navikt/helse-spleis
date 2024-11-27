package no.nav.helse.hendelser

import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_2

class Medlemskapsvurdering(
    internal val medlemskapstatus: Medlemskapstatus
) {
    internal fun valider(aktivitetslogg: IAktivitetslogg): Boolean {
        return when (medlemskapstatus) {
            Medlemskapstatus.Ja -> {
                aktivitetslogg.info("Bruker er medlem av Folketrygden")
                true
            }

            Medlemskapstatus.Nei -> {
                aktivitetslogg.varsel(RV_MV_2)
                false
            }

            Medlemskapstatus.UavklartMedBrukerspørsmål -> {
                aktivitetslogg.varsel(RV_MV_1)
                true
            }

            Medlemskapstatus.VetIkke -> {
                aktivitetslogg.info("Bruker er VetIkke-medlem av Folketrygden")
                true
            }
        }
    }

    enum class Medlemskapstatus {
        Ja, Nei, VetIkke, UavklartMedBrukerspørsmål
    }
}
