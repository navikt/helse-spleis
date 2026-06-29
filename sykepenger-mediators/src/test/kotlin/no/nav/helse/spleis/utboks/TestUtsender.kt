package no.nav.helse.spleis.utboks

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.Instant

internal interface TestUtsenderObservatør {
    fun okMelding(melding: UtgåendeMelding)
}

internal class TestUtsender() : Utsender() {
    private val observatører = mutableListOf<TestUtsenderObservatør>()
    val ok: MutableList<UtgåendeMelding> = mutableListOf()
    val feil: MutableList<UtgåendeMelding> = mutableListOf()

    fun nyObservatør(observatør: TestUtsenderObservatør) {
        observatører.add(observatør)
    }

    override fun utførSending(utgåendeMeldinger: List<UtgåendeMelding>, sendt: Instant): Pair<List<UtgåendeMelding>, List<UtgåendeMelding>> {
        val (okMeldinger, feilmeldinger) = utgåendeMeldinger.partition { it.json.path("feil").isMissingOrNull() }
        observatører.forEach { observatør ->
            okMeldinger.forEach { observatør.okMelding(it) }
        }
        ok.addAll(okMeldinger)
        feil.addAll(feilmeldinger)
        return okMeldinger to feilmeldinger
    }
}
