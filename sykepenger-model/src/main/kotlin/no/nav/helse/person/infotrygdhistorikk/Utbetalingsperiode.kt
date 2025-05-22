package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import no.nav.helse.dto.deserialisering.InfotrygdArbeidsgiverutbetalingsperiodeInnDto
import no.nav.helse.dto.deserialisering.InfotrygdPersonutbetalingsperiodeInnDto
import no.nav.helse.dto.serialisering.InfotrygdArbeidsgiverutbetalingsperiodeUtDto
import no.nav.helse.dto.serialisering.InfotrygdPersonutbetalingsperiodeUtDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Prosentdel.Companion.HundreProsent
import no.nav.helse.økonomi.Økonomi

sealed class Utbetalingsperiode(
    val orgnr: String,
    fom: LocalDate,
    tom: LocalDate
) : Infotrygdperiode(fom, tom) {
    override fun sykdomstidslinje(kilde: Hendelseskilde): Sykdomstidslinje {
        return Sykdomstidslinje.sykedager(periode.start, periode.endInclusive, HundreProsent, kilde)
    }

    override fun utbetalingstidslinje() =
        Utbetalingstidslinje.Builder().apply {
            periode.forEach { dag -> nyDag(this, dag) }
        }.build()

    private fun nyDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate) {
        if (dato.erHelg()) return builder.addHelg(dato, Økonomi.ikkeBetalt())
        builder.addNAVdag(dato, Økonomi.ikkeBetalt())
    }

    override fun gjelder(orgnummer: String) = orgnummer == this.orgnr

    override fun funksjoneltLik(other: Infotrygdperiode): Boolean {
        if (!super.funksjoneltLik(other)) return false
        other as Utbetalingsperiode
        return this.orgnr == other.orgnr && this.periode.start == other.periode.start
    }
}

class ArbeidsgiverUtbetalingsperiode(orgnr: String, fom: LocalDate, tom: LocalDate) :
    Utbetalingsperiode(orgnr, fom, tom) {

    internal fun dto() = InfotrygdArbeidsgiverutbetalingsperiodeUtDto(
        orgnr = orgnr,
        periode = periode.dto()
    )

    internal companion object {
        internal fun gjenopprett(dto: InfotrygdArbeidsgiverutbetalingsperiodeInnDto): ArbeidsgiverUtbetalingsperiode {
            val periode = Periode.gjenopprett(dto.periode)
            return ArbeidsgiverUtbetalingsperiode(
                orgnr = dto.orgnr,
                fom = periode.start,
                tom = periode.endInclusive
            )
        }
    }
}

class PersonUtbetalingsperiode(orgnr: String, fom: LocalDate, tom: LocalDate) :
    Utbetalingsperiode(orgnr, fom, tom) {

    internal fun dto() = InfotrygdPersonutbetalingsperiodeUtDto(
        orgnr = orgnr,
        periode = periode.dto()
    )

    internal companion object {
        internal fun gjenopprett(dto: InfotrygdPersonutbetalingsperiodeInnDto): PersonUtbetalingsperiode {
            val periode = Periode.gjenopprett(dto.periode)
            return PersonUtbetalingsperiode(
                orgnr = dto.orgnr,
                fom = periode.start,
                tom = periode.endInclusive
            )
        }
    }
}
