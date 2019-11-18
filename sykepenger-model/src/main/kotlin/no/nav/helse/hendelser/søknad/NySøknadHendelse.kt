package no.nav.helse.hendelser.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.UtenforOmfangException
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDateTime
import java.util.*

class NySøknadHendelse private constructor(hendelseId: String, private val søknad: Sykepengesøknad): ArbeidstakerHendelse, SykdomstidslinjeHendelse(hendelseId) {

    constructor(søknad: Sykepengesøknad) : this(UUID.randomUUID().toString(), søknad)

    companion object {
        fun fromJson(jsonNode: JsonNode): NySøknadHendelse {
            return NySøknadHendelse(
                    jsonNode["hendelseId"].textValue(),
                    Sykepengesøknad(jsonNode["søknad"])
            )
        }
    }

    override fun kanBehandles(): Boolean {
        return søknad.kanBehandles()
                && søknad.sykeperioder.all { it.sykmeldingsgrad == 100 }
    }

    override fun aktørId() =
            søknad.aktørId

    override fun organisasjonsnummer(): String =
            søknad.arbeidsgiver.orgnummer

    override fun rapportertdato(): LocalDateTime =
            søknad.opprettet

    override fun compareTo(other: SykdomstidslinjeHendelse): Int =
            søknad.opprettet.compareTo(other.rapportertdato())

    private val sykeperiodeTidslinje
        get(): List<Sykdomstidslinje> = søknad.sykeperioder
                .map { Sykdomstidslinje.sykedager(it.fom, it.tom, this) }

    override fun sykdomstidslinje() =
            sykeperiodeTidslinje.reduce { resultatTidslinje, delTidslinje ->
                if (resultatTidslinje.overlapperMed(delTidslinje)) throw UtenforOmfangException("Søknaden inneholder overlappende sykdomsperioder", this)
                resultatTidslinje + delTidslinje
            }

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Sykmelding

    override fun toJson(): JsonNode {
        return (super.toJson() as ObjectNode).apply {
            put("type", SykdomshendelseType.NySøknadMottatt.name)
            set<ObjectNode>("søknad", søknad.toJson())
        }
    }

}
