package no.nav.helse.hendelser

import no.nav.helse.Toggle
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_2

class Medlemskapsvurdering(
    private val medlemskapstatus: Medlemskapstatus,
) {
    internal fun validert(aktivitetslogg: IAktivitetslogg): Medlemskapstatus {
        if (Toggle.MedlemskapsvurderingResultatBehov.enabled) {
            aktivitetslogg.info("Varsler om medlemskap gis ved beregning ut fra svaret fra sp-medlemskap")
            return medlemskapstatus
        }
        varsle(medlemskapstatus, aktivitetslogg)
        return medlemskapstatus
    }

    internal companion object {
        internal fun varsle(
            medlemskapstatus: Medlemskapstatus,
            aktivitetslogg: IAktivitetslogg,
        ) {
            when (medlemskapstatus) {
                Medlemskapstatus.Ja -> {
                    aktivitetslogg.info("Bruker er medlem av Folketrygden")
                }

                Medlemskapstatus.Nei -> {
                    aktivitetslogg.varsel(RV_MV_2)
                }

                Medlemskapstatus.UavklartMedBrukerspørsmål -> {
                    aktivitetslogg.varsel(RV_MV_1)
                }

                Medlemskapstatus.VetIkke -> {
                    aktivitetslogg.info("Bruker er VetIkke-medlem av Folketrygden")
                }
            }
        }
    }

    enum class Medlemskapstatus {
        Ja,
        Nei,
        VetIkke,
        UavklartMedBrukerspørsmål,
    }
}
