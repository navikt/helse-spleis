package no.nav.helse.sykepengehistorikk

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelse.PersonHendelse
import no.nav.helse.hendelse.SakskompleksHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDateTime
import java.util.*

class SykepengehistorikkHendelse private constructor(hendelseId: String, private val sykepengehistorikk: Sykepengehistorikk): PersonHendelse,
        SakskompleksHendelse, SykdomstidslinjeHendelse(hendelseId) {


    constructor(sykepengehistorikk: Sykepengehistorikk) : this(UUID.randomUUID().toString(), sykepengehistorikk)

    // forenkling for å kaste ut flest mulige saker. Vil ikke være relevant når vi faktisk regner maksdato
    private val seksMånederIDager = 6*31

    override fun sakskompleksId() =
        sykepengehistorikk.sakskompleksId

    override fun aktørId() =
        sykepengehistorikk.aktørId

    override fun rapportertdato(): LocalDateTime {
        TODO("not implemented")
    }

    fun påvirkerSakensMaksdato(sakensTidslinje: Sykdomstidslinje) =
        sykdomstidslinje().antallDagerMellom(sakensTidslinje) <= seksMånederIDager

    override fun organisasjonsnummer(): String? =
        sykepengehistorikk.organisasjonsnummer

    override fun sykdomstidslinje() =
        sykepengehistorikk.perioder.fold( Sykdomstidslinje.tomTidslinje()) { aggregate, periode ->
            aggregate + Sykdomstidslinje.sykedager(
                periode.fom,
                periode.tom,
                this
            )
        }

    override fun nøkkelHendelseType(): Dag.NøkkelHendelseType {
        TODO("not implemented")
    }

    override fun toJson(): JsonNode {
        throw NotImplementedError("Sykepengehistorikk skal ikke persisteres")
    }
}
