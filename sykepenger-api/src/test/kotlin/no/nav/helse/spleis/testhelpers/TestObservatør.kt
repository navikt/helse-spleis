package no.nav.helse.spleis.testhelpers

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.VedtaksperiodeEndretEvent
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.IdInnhenter
import org.junit.jupiter.api.fail

internal class TestObservatør : PersonObserver {
    lateinit var sisteVedtaksperiode: UUID

    private val tilstandsendringer = mutableMapOf<UUID, MutableList<TilstandType>>()
    private val utbetalteVedtaksperioder = mutableListOf<UUID>()
    private val vedtaksperioder = mutableMapOf<String, MutableSet<UUID>>()
    private val vedtaksperiodeendringer = mutableMapOf<UUID, MutableList<VedtaksperiodeEndretEvent>>()
    private val ventendeReplays = mutableListOf<Pair<String, UUID>>()
    fun hendelseider(vedtaksperiodeId: UUID) =
        vedtaksperiodeendringer[vedtaksperiodeId]?.last()?.hendelser ?: fail { "VedtaksperiodeId $vedtaksperiodeId har ingen hendelser tilknyttet" }

    fun sisteVedtaksperiode() = IdInnhenter { orgnummer -> vedtaksperioder.getValue(orgnummer).last() }
    fun vedtaksperiode(orgnummer: String, indeks: Int) = vedtaksperioder.getValue(orgnummer).toList()[indeks]

    fun ventendeReplays() = ventendeReplays.toList().also {
        ventendeReplays.clear()
    }

    override fun nyBehandling(event: PersonObserver.BehandlingOpprettetEvent) {
        sisteVedtaksperiode = event.vedtaksperiodeId
    }

    override fun inntektsmeldingReplay(
        personidentifikator: Personidentifikator,
        aktørId: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        skjæringstidspunkt: LocalDate,
        sammenhengendePeriode: Periode
    ) {
        ventendeReplays.add(organisasjonsnummer to vedtaksperiodeId)
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {
        vedtaksperiodeendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf(event) }.add(event)
        vedtaksperioder.getOrPut(event.organisasjonsnummer) { mutableSetOf() }.add(event.vedtaksperiodeId)
        if (event.gjeldendeTilstand != event.forrigeTilstand) {
            tilstandsendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf(TilstandType.START) }.add(event.gjeldendeTilstand)
        }
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(event.vedtaksperiodeId)
    }
}
