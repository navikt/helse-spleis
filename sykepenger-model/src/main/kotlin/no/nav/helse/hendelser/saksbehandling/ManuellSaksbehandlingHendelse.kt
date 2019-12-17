package no.nav.helse.hendelser.saksbehandling

import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.Hendelsetype
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sak.VedtaksperiodeHendelse

class ManuellSaksbehandlingHendelse(private val manuellSaksbehandling: Behov) : ArbeidstakerHendelse,
    VedtaksperiodeHendelse {

    fun saksbehandler(): String = requireNotNull(manuellSaksbehandling["saksbehandlerIdent"])

    fun utbetalingGodkjent(): Boolean =
            (manuellSaksbehandling.løsning() as Map<*, *>?)?.get("godkjent") == true

    override fun vedtaksperiodeId(): String = manuellSaksbehandling.vedtaksperiodeId()

    override fun aktørId(): String = manuellSaksbehandling.aktørId()

    override fun fødselsnummer(): String = manuellSaksbehandling.fødselsnummer()

    override fun organisasjonsnummer(): String = manuellSaksbehandling.organisasjonsnummer()

    override fun opprettet() = requireNotNull(manuellSaksbehandling.besvart())

    override fun hendelsetype(): Hendelsetype {
        return Hendelsetype.ManuellSaksbehandling
    }

    override fun toJson(): String {
        return manuellSaksbehandling.toJson()
    }
}
