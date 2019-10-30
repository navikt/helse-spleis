package no.nav.helse.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.SykdomshendelseType
import no.nav.helse.person.domain.PersonHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import java.time.LocalDateTime
import java.util.*

class SendtSøknadHendelse private constructor(hendelseId: String, private val søknad: Sykepengesøknad) : PersonHendelse, SykdomstidslinjeHendelse(hendelseId) {
    constructor(søknad: Sykepengesøknad) : this(UUID.randomUUID().toString(), søknad)

    companion object {

        fun fromJson(jsonNode: JsonNode): SendtSøknadHendelse {
            return SendtSøknadHendelse(
                    jsonNode["hendelseId"].textValue(),
                    Sykepengesøknad(jsonNode["søknad"])
            )
        }
    }

    override fun aktørId() =
            søknad.aktørId

    override fun nøkkelHendelseType() =
            Søknad

    override fun organisasjonsnummer(): String? =
            søknad.arbeidsgiver?.orgnummer

    override fun rapportertdato(): LocalDateTime =
            søknad.opprettet

    override fun compareTo(other: SykdomstidslinjeHendelse): Int =
            søknad.opprettet.compareTo(other.rapportertdato())

    private val sykeperiodeTidslinje
        get(): List<Sykdomstidslinje> = søknad.sykeperioder
                .map { Sykdomstidslinje.sykedager(it.fom, it.tom, this) }

    private val egenmeldingsTidslinje
        get(): List<Sykdomstidslinje> = søknad.egenmeldinger
                .map { Sykdomstidslinje.egenmeldingsdager(it.fom, it.tom, this) }

    private val ferieTidslinje
        get(): List<Sykdomstidslinje> = søknad.fraværsperioder
                .filter { it.type == Sykepengesøknad.Fraværstype.FERIE }
                .map { Sykdomstidslinje.ferie(it.fom, it.tom, this) }

    private val permisjonTidslinje
        get(): List<Sykdomstidslinje> = søknad.fraværsperioder
                .filter { it.type == Sykepengesøknad.Fraværstype.PERMISJON }
                .map { Sykdomstidslinje.permisjonsdager(it.fom, it.tom, this) }

    private val arbeidGjenopptattTidslinje
        get(): List<Sykdomstidslinje> = søknad.arbeidGjenopptatt
                ?.let { listOf(Sykdomstidslinje.ikkeSykedager(it, søknad.tom, this)) }
                ?: emptyList()

    private val studiedagertidslinje = søknad.utdanningsperioder.map {
        Sykdomstidslinje.studiedager(it.fom, søknad.tom, this)
    }

    override fun sykdomstidslinje() =
            (sykeperiodeTidslinje + egenmeldingsTidslinje + ferieTidslinje + arbeidGjenopptattTidslinje + studiedagertidslinje + permisjonTidslinje)
                    .reduce { resultatTidslinje, delTidslinje ->
                        resultatTidslinje + delTidslinje
                    }

    override fun toJson(): JsonNode {
        return (super.toJson() as ObjectNode).apply {
            put("type", SykdomshendelseType.SendtSøknadMottatt.name)
            set("søknad", søknad.toJson())
        }
    }

}
