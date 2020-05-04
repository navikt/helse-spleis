package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import java.time.LocalDate
import java.util.*

internal object TestEvent : SykdomstidslinjeHendelse(UUID.randomUUID()) {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"

        override fun sykdomstidslinje(tom: LocalDate) = Sykdomstidslinje()
        override fun sykdomstidslinje() = Sykdomstidslinje()
        override fun nySykdomstidslinje() = NySykdomstidslinje()
        override fun nySykdomstidslinje(tom: LocalDate) = NySykdomstidslinje()
        override fun valider(periode: Periode) = Aktivitetslogg()
        override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = Unit
        override fun aktørId() = AKTØRID
        override fun fødselsnummer() = UNG_PERSON_FNR_2018
        override fun organisasjonsnummer() = ORGNUMMER
    }
