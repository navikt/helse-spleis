package no.nav.helse.saksbehandling

import no.nav.helse.behov.Behov
import no.nav.helse.hendelse.SakskompleksHendelse
import no.nav.helse.person.domain.ArbeidstakerHendelse

class ManuellSaksbehandlingHendelse(private val manuellSaksbehandling: Behov) : ArbeidstakerHendelse, SakskompleksHendelse {

    fun saksbehandler() =
            manuellSaksbehandling.get<String>("saksbehandler")!!

    fun utbetalingGodkjent(): Boolean =
            (manuellSaksbehandling.løsning() as Map<String, Any>?)?.getValue("godkjent") as Boolean? ?: false

    override fun sakskompleksId() =
            manuellSaksbehandling.get<String>("sakskompleksId")!!

    override fun aktørId() =
            manuellSaksbehandling.get<String>("aktørId")!!

    override fun organisasjonsnummer() =
            manuellSaksbehandling.get<String>("organisasjonsnummer")!!
}
