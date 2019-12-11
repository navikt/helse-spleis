package no.nav.helse.hendelser.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import java.time.LocalDateTime
import java.util.*

class SendtSøknadHendelse private constructor(hendelseId: String, private val søknad: Sykepengesøknad) : ArbeidstakerHendelse, SykdomstidslinjeHendelse(hendelseId) {
    constructor(søknad: Sykepengesøknad) : this(UUID.randomUUID().toString(), søknad)

    companion object {

        fun fromJson(jsonNode: JsonNode): SendtSøknadHendelse {
            return SendtSøknadHendelse(
                    jsonNode["hendelseId"].textValue(),
                    Sykepengesøknad(jsonNode["søknad"])
            )
        }
    }

    override fun aktørId() = søknad.aktørId

    override fun fødselsnummer() = søknad.fnr

    override fun nøkkelHendelseType() = Søknad

    override fun kanBehandles(): Boolean {
        return søknad.kanBehandles()
                && søknad.sykeperioder.all { (it.faktiskGrad ?: it.sykmeldingsgrad) == 100 }
                && søknad.sendtNav != null
                && søknad.fom >= søknad.sendtNav.toLocalDate().minusMonths(3).withDayOfMonth(1)
    }

    override fun organisasjonsnummer(): String =
            søknad.arbeidsgiver.orgnummer

    override fun rapportertdato(): LocalDateTime =
            søknad.opprettet

    override fun compareTo(other: SykdomstidslinjeHendelse): Int =
            søknad.opprettet.compareTo(other.rapportertdato())

    private val sykeperiodeTidslinje
        get(): List<ConcreteSykdomstidslinje> = søknad.sykeperioder
                .map { ConcreteSykdomstidslinje.sykedager(it.fom, it.tom, this) }

    private val egenmeldingsTidslinje
        get(): List<ConcreteSykdomstidslinje> = søknad.egenmeldinger
                .map { ConcreteSykdomstidslinje.egenmeldingsdager(it.fom, it.tom, this) }

    private val ferieTidslinje
        get(): List<ConcreteSykdomstidslinje> = søknad.fraværsperioder
                .filter { it.type == Sykepengesøknad.Fraværstype.FERIE }
                .map { ConcreteSykdomstidslinje.ferie(it.fom, it.tom, this) }

    private val permisjonTidslinje
        get(): List<ConcreteSykdomstidslinje> = søknad.fraværsperioder
                .filter { it.type == Sykepengesøknad.Fraværstype.PERMISJON }
                .map { ConcreteSykdomstidslinje.permisjonsdager(it.fom, it.tom, this) }

    private val arbeidGjenopptattTidslinje
        get(): List<ConcreteSykdomstidslinje> = søknad.arbeidGjenopptatt
                ?.let { listOf(ConcreteSykdomstidslinje.ikkeSykedager(it, søknad.tom, this)) }
                ?: emptyList()

    private val studiedagertidslinje = søknad.utdanningsperioder.map {
        ConcreteSykdomstidslinje.studiedager(it.fom, søknad.tom, this)
    }

    override fun sykdomstidslinje() =
            (sykeperiodeTidslinje + egenmeldingsTidslinje + ferieTidslinje + arbeidGjenopptattTidslinje + studiedagertidslinje + permisjonTidslinje)
                    .reduce { resultatTidslinje, delTidslinje ->
                        resultatTidslinje + delTidslinje
                    }

    override fun toJson(): JsonNode {
        return (super.toJson() as ObjectNode).apply {
            put("type", SykdomshendelseType.SendtSøknadMottatt.name)
            set<ObjectNode>("søknad", søknad.toJson())
        }
    }

}
