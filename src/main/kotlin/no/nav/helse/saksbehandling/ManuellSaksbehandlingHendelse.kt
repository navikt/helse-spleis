package no.nav.helse.saksbehandling

import no.nav.helse.hendelse.SakskompleksHendelse
import no.nav.helse.person.domain.PersonHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDateTime
import java.util.*

class ManuellSaksbehandlingHendelse private constructor(hendelseId: String, private val manuellSaksbehandling: ManuellSaksbehandling) : PersonHendelse, SakskompleksHendelse, SykdomstidslinjeHendelse(hendelseId) {
    constructor(manuellSaksbehandling: ManuellSaksbehandling) : this(UUID.randomUUID().toString(), manuellSaksbehandling)

    fun utbetalingGodkjent() = manuellSaksbehandling.utbetalingGodkjent

    override fun sakskompleksId() =
            manuellSaksbehandling.sakskompleksId

    override fun aktørId(): String {
        TODO("not implemented")
    }

    override fun organisasjonsnummer(): String? {
        TODO("not implemented")
    }

    override fun nøkkelHendelseType(): Dag.NøkkelHendelseType {
        TODO("not implemented")
    }

    override fun rapportertdato(): LocalDateTime {
        TODO("not implemented")
    }

    override fun sykdomstidslinje(): Sykdomstidslinje {
        TODO("not implemented")
    }
}
