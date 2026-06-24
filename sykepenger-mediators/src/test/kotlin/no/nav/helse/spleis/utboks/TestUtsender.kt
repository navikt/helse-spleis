package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.Instant

internal class TestUtsender : Utsender() {
    val ok: MutableList<UtgåendeMelding> = mutableListOf()
    val feil: MutableList<UtgåendeMelding> = mutableListOf()

    override fun utførSending(utgåendeMeldinger: List<UtgåendeMelding>, sendt: Instant): Pair<List<UtgåendeMelding>, List<UtgåendeMelding>> {
        val (okMeldinger, feilmeldinger) = utgåendeMeldinger.partition { it.json.path("feil").isMissingOrNull() }
        ok.addAll(okMeldinger)
        feil.addAll(feilmeldinger)
        return okMeldinger to feilmeldinger
    }
}
