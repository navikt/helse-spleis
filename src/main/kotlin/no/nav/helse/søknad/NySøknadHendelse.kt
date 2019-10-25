package no.nav.helse.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.SykdomshendelseType
import no.nav.helse.hendelse.PersonHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDateTime
import java.util.*

class NySøknadHendelse private constructor(hendelseId: String, private val søknad: Sykepengesøknad): PersonHendelse, SykdomstidslinjeHendelse(hendelseId) {

    constructor(søknad: Sykepengesøknad) : this(UUID.randomUUID().toString(), søknad)

    companion object {
        fun fromJson(jsonNode: JsonNode): NySøknadHendelse {
            return NySøknadHendelse(
                    jsonNode["hendelseId"].textValue(),
                    Sykepengesøknad(jsonNode["søknad"])
            )
        }
    }

    override fun aktørId() =
            søknad.aktørId

    override fun organisasjonsnummer(): String? =
            søknad.arbeidsgiver?.orgnummer

    override fun rapportertdato(): LocalDateTime =
            søknad.opprettet

    override fun compareTo(other: SykdomstidslinjeHendelse): Int =
            søknad.opprettet.compareTo(other.rapportertdato())

    private val sykeperiodeTidslinje
        get(): List<Sykdomstidslinje> = søknad.sykeperioder
                .map { Sykdomstidslinje.sykedager(it.fom, it.tom, this) }

    override fun sykdomstidslinje() =
            sykeperiodeTidslinje.reduce { resultatTidslinje, delTidslinje ->
                resultatTidslinje + delTidslinje
            }

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Sykmelding

    override fun toJson(): JsonNode {
        return (super.toJson() as ObjectNode).apply {
            put("type", SykdomshendelseType.NySøknadMottatt.name)
            set("søknad", søknad.toJson())
        }
    }

}
