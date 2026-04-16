package no.nav.helse.dsl

import java.util.UUID
import kotlin.collections.single
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spill_av_im.Forespørsel
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.assertNotNull

class Behovshåndterer(private val behovsoppsamler: Behovsoppsamler): EventSubscription {
    private val tilstander = mutableMapOf<UUID, TilstandType>()
    private val hånderteInntektsmeldinger = mutableSetOf<UUID>()

    fun utbetalingsdetaljer(orgnummer: String): List<Behovsoppsamler.Behovsdetaljer.Utbetaling> {
        return behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.Utbetaling>()
            .filter { it.organisasjonsnummer == orgnummer }
            .groupBy { "${it.utbetalingId}-${it.fagsystemId}" }
            // velger bare siste behov per utbetalingId-fagsystemId-kombinasjon for å håndtere at vedtaksperioden kan ha blitt påminnet og produsert behovet flere ganger
            .mapValues { (_, utbetalingsdetaljer) -> utbetalingsdetaljer.last() }
            .values
            .toList()
            .also { if (it.isEmpty()) fail("Forventet at det skal være spurt om utbetaling, men det var det ikke!") }
    }

    fun simuleringsdetaljer(vedtaksperiodeId: UUID) =
        behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.Simulering>().filter { it.vedtaksperiodeId == vedtaksperiodeId }
            .also { if (it.isEmpty()) fail("Forventet at det skal være spurt om simulering, men det var det ikke!") }

    fun godkjenningsdetaljer(vedtaksperiodeId: UUID): Behovsoppsamler.Behovsdetaljer.Godkjenning {
        val godkjenningsdetaljer = behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.Godkjenning>().filter { it.vedtaksperiodeId == vedtaksperiodeId }
        assert(godkjenningsdetaljer.size == 1) { "Forventet at det skulle være forspurt nøyaktig én godkjenning. Fant ${godkjenningsdetaljer.size}"}
        return godkjenningsdetaljer.single()
    }

    fun feriepengerutbetalingsdetaljer() =
        behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.Feriepengeutbetaling>()
            .also { if (it.isEmpty()) fail("Forventet at det skal være spurt om feriepengerutbetaling, men det var det ikke!") }

    fun harForespurtHistorikkFraInfotrygd(vedtaksperiodeId: UUID) =
        behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InitiellHistorikFraInfotrygd>()
            .any { it.vedtaksperiodeId == vedtaksperiodeId }

    fun bekreftForespurtVilkårsprøving(vedtaksperiodeId: UUID) =
        assertNotNull(behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InformasjonTilVilkårsprøving>().filter { it.vedtaksperiodeId == vedtaksperiodeId }) {
            "Forventet at det skulle være forspurt informasjon til vilkårsprøving. Vedtaksperioden er i tilstand ${tilstander[vedtaksperiodeId]}"
        }

    fun bekreftForespurtBeregningAvSelvstendig(vedtaksperiodeId: UUID) =
        assertNotNull(behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InformasjonTilBeregningAvSelvstendig>().filter { it.vedtaksperiodeId == vedtaksperiodeId }) {
            "Forventet at det skulle være forspurt informasjon til beregning av selvstendig periode. Vedtaksperioden er i tilstand ${tilstander[vedtaksperiodeId]}"
        }

    fun bekreftForespurtBeregningAvArbeidstaker(vedtaksperiodeId: UUID) =
        assertNotNull(behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InformasjonTilBeregningAvSelvstendig>().filter { it.vedtaksperiodeId == vedtaksperiodeId }) {
            "Forventet at det skulle være forspurt informasjon til beregning av arbeidstaker-periode. Vedtaksperioden er i tilstand ${tilstander[vedtaksperiodeId]}"
        }

    override fun vedtaksperiodeEndret(event: EventSubscription.VedtaksperiodeEndretEvent) {
        tilstander[event.vedtaksperiodeId] = event.gjeldendeTilstand
    }

    override fun inntektsmeldingHåndtert(event: EventSubscription.InntektsmeldingHåndtertEvent) {
        hånderteInntektsmeldinger.add(event.meldingsreferanseId)
    }

    fun <T> håndterForespørslerOmReplayAvInntektsmeldingSomFølgeAv(
        operasjon: () -> T?,
        håndterForespørsel: (forespørsel: Forespørsel, alleredeHåndterteInntektsmeldinger: Set<UUID>) -> Unit
    ): T? {
        val før = behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InntektsmeldingReplay>().toSet()
        val verdi = operasjon()
        val etter = behovsoppsamler.behovsdetaljer<Behovsoppsamler.Behovsdetaljer.InntektsmeldingReplay>().toSet() - før
        etter.forEach { replayDetaljer ->
            håndterForespørsel(replayDetaljer.forespørsel, hånderteInntektsmeldinger.toSet())
            behovsoppsamler.besvart(replayDetaljer)
        }
        return verdi
    }
}
