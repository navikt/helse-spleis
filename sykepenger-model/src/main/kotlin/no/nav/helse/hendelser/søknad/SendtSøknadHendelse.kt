package no.nav.helse.hendelser.søknad

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import java.util.*

class SendtSøknadHendelse private constructor(hendelseId: String, søknad: JsonNode) : SøknadHendelse(hendelseId, SykdomshendelseType.SendtSøknadMottatt, søknad) {

    constructor(søknad: JsonNode) : this(UUID.randomUUID().toString(), søknad)

    companion object {
        fun fromJson(jsonNode: JsonNode): SendtSøknadHendelse {
            return SendtSøknadHendelse(jsonNode["hendelseId"].textValue(), jsonNode["søknad"])
        }
    }

    override fun nøkkelHendelseType() = Søknad

    override fun kanBehandles(): Boolean {
        return super.kanBehandles()
                && sykeperioder.all { (it.faktiskGrad ?: it.sykmeldingsgrad) == 100 }
                && sendtNav != null
                && fom >= sendtNav.toLocalDate().minusMonths(3).withDayOfMonth(1)
    }

    private val sykeperiodeTidslinje
        get(): List<ConcreteSykdomstidslinje> = sykeperioder
                .map { ConcreteSykdomstidslinje.sykedager(it.fom, it.tom, this) }

    private val egenmeldingsTidslinje
        get(): List<ConcreteSykdomstidslinje> = egenmeldinger
                .map { ConcreteSykdomstidslinje.egenmeldingsdager(it.fom, it.tom, this) }

    private val ferieTidslinje
        get(): List<ConcreteSykdomstidslinje> = fraværsperioder
                .filter { it.type == Fraværstype.FERIE }
                .map { ConcreteSykdomstidslinje.ferie(it.fom, it.tom, this) }

    private val permisjonTidslinje
        get(): List<ConcreteSykdomstidslinje> = fraværsperioder
                .filter { it.type == Fraværstype.PERMISJON }
                .map { ConcreteSykdomstidslinje.permisjonsdager(it.fom, it.tom, this) }

    private val arbeidGjenopptattTidslinje
        get(): List<ConcreteSykdomstidslinje> = arbeidGjenopptatt
                ?.let { listOf(ConcreteSykdomstidslinje.ikkeSykedager(it, tom, this)) }
                ?: emptyList()

    private val studiedagertidslinje = utdanningsperioder.map {
        ConcreteSykdomstidslinje.studiedager(it.fom, tom, this)
    }

    override fun sykdomstidslinje() =
            (sykeperiodeTidslinje + egenmeldingsTidslinje + ferieTidslinje + arbeidGjenopptattTidslinje + studiedagertidslinje + permisjonTidslinje)
                    .reduce { resultatTidslinje, delTidslinje ->
                        resultatTidslinje + delTidslinje
                    }
}
