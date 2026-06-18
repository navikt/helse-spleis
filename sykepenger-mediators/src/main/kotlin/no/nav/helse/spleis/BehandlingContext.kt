package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.spleis.utboks.Utboks

internal data class BehandlingContext(
    val messageContext: MessageContext
) {
    val utboks = Utboks()
}
