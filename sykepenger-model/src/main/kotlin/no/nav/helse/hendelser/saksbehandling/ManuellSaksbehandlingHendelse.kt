package no.nav.helse.hendelser.saksbehandling

import no.nav.helse.behov.Behov
import no.nav.helse.sak.VedtaksperiodeHendelse
import no.nav.helse.sak.ArbeidstakerHendelse

class ManuellSaksbehandlingHendelse(private val manuellSaksbehandling: Behov) : ArbeidstakerHendelse,
    VedtaksperiodeHendelse {

    fun saksbehandler() =
            manuellSaksbehandling.get<String>("saksbehandlerIdent")!!

    fun utbetalingGodkjent(): Boolean =
            (manuellSaksbehandling.løsning() as Map<String, Boolean>?)?.getValue("godkjent")?: false

    override fun vedtaksperiodeId() =
            manuellSaksbehandling.get<String>("sakskompleksId")!!

    override fun aktørId() =
            manuellSaksbehandling.get<String>("aktørId")!!

    override fun organisasjonsnummer() =
            manuellSaksbehandling.get<String>("organisasjonsnummer")!!
}
