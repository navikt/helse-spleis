package no.nav.helse.person

import java.util.*
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.tilstandsmaskin.TilstandType

class BehovBus {
    private val _behov = mutableListOf<Behov>()

    // eksponerer ikke en mutable list
    val behov get() = _behov.toList()

    internal fun registrerBehov(behov: Behov) {
        _behov.add(behov)
    }
}

class Behovsamler(
    private val bus: BehovBus,
    private val kontekster: List<Behov.Behovkontekst>
) {
    fun leggTilKontekst(behovkontekst: Behov.Behovkontekst): Behovsamler {
        val nyeKontekster = kontekster.takeWhile { it::class != behovkontekst::class } + behovkontekst
        return Behovsamler(bus, nyeKontekster)
    }

    fun utbetalingshistorikk(aktivitetslogg: IAktivitetslogg, periode: Periode) {
        aktivitetslogg.info("Trenger sykepengehistorikk fra Infotrygd")
        Aktivitet.Behov.utbetalingshistorikk(aktivitetslogg, periode)
        val behov = Behov(
            kontekster = kontekster.toSet(),
            data = Behov.Behovdetaljer.Utbetalingshistorikk(periode)
        )
        bus.registrerBehov(behov)
    }
}

data class Behov(val kontekster: Set<Behovkontekst>, val data: Behovdetaljer) {
    sealed interface Behovkontekst {
        data class Yrkesaktivitet(val yrkesaktivitet: Behandlingsporing.Yrkesaktivitet) : Behovkontekst
        data class Vedtaksperiode(val id: UUID) : Behovkontekst
        data class Behandling(val id: UUID) : Behovkontekst
        data class Vedtaksperiodetilstand(val tilstand: TilstandType) : Behovkontekst
        data class Utbetaling(val id: UUID) : Behovkontekst
    }
    sealed interface Behovdetaljer {
        data class Utbetalingshistorikk(val periode: Periode) : Behovdetaljer
    }
}
