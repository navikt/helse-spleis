package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigStart : Vedtaksperiodetilstand {
    override val type = TilstandType.SELVSTENDIG_START
}
