package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.AnnenYtelseSomKanOppdatereHistorikk.Companion.HvorforIkkeOppdatereHistorikk.FLERE_IKKE_SAMMENHENGENDE_INNSLAG
import no.nav.helse.hendelser.AnnenYtelseSomKanOppdatereHistorikk.Companion.HvorforIkkeOppdatereHistorikk.GRADERT_YTELSE
import no.nav.helse.hendelser.AnnenYtelseSomKanOppdatereHistorikk.Companion.HvorforIkkeOppdatereHistorikk.HAR_VEDTAKSPERIODE_RETT_ETTER
import no.nav.helse.hendelser.AnnenYtelseSomKanOppdatereHistorikk.Companion.HvorforIkkeOppdatereHistorikk.IKKE_I_HALEN_AV_VEDTAKSPERIODE
import no.nav.helse.hendelser.AnnenYtelseSomKanOppdatereHistorikk.Companion.HvorforIkkeOppdatereHistorikk.INGEN_YTELSE
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

abstract class AnnenYtelseSomKanOppdatereHistorikk {

    companion object {
        internal enum class HvorforIkkeOppdatereHistorikk {
            INGEN_YTELSE,
            GRADERT_YTELSE,
            FLERE_IKKE_SAMMENHENGENDE_INNSLAG,
            HAR_VEDTAKSPERIODE_RETT_ETTER,
            IKKE_I_HALEN_AV_VEDTAKSPERIODE,
        }

        internal fun List<GradertPeriode>.skalOppdatereHistorikkIHalen(vedtaksperiode: Periode, vedtaksperiodeRettEtter: Periode?): Pair<Boolean, HvorforIkkeOppdatereHistorikk?> {
            if (this.isEmpty()) return false to INGEN_YTELSE
            if (vedtaksperiodeRettEtter != null) return false to HAR_VEDTAKSPERIODE_RETT_ETTER
            val sammenhengendePerioder = this.map { it.periode }.grupperSammenhengendePerioder()
            if (sammenhengendePerioder.size > 1) return false to FLERE_IKKE_SAMMENHENGENDE_INNSLAG
            val ytelseperiode = sammenhengendePerioder.single()
            val fullstendigOverlapp = ytelseperiode == vedtaksperiode
            val ytelseIHalen = vedtaksperiode.overlapperMed(ytelseperiode) && ytelseperiode.slutterEtter(vedtaksperiode.endInclusive)
            if (!fullstendigOverlapp && !ytelseIHalen) return false to IKKE_I_HALEN_AV_VEDTAKSPERIODE
            if (this.any { it.grad != 100 }) return false to GRADERT_YTELSE
            return true to null
        }
    }

    internal fun skalOppdatereHistorikk(
        aktivitetslogg: IAktivitetslogg,
        ytelse: AnnenYtelseSomKanOppdatereHistorikk,
        vedtaksperiode: Periode,
        vedtaksperiodeRettEtter: Periode?
    ): Boolean {
        val (skalOppdatereHistorikk, hvorforIkke) = ytelse.skalOppdatereHistorikk(vedtaksperiode, vedtaksperiodeRettEtter)
        if (hvorforIkke !in listOf(null, INGEN_YTELSE)) {
            aktivitetslogg.info("Legger ikke til ${ytelse.javaClass.simpleName.lowercase()} i historikken fordi $hvorforIkke")
        }
        return skalOppdatereHistorikk.also {
            if (it) aktivitetslogg.info("Legger til ${ytelse.javaClass.simpleName.lowercase()} i historikken")
        }
    }

    internal abstract fun skalOppdatereHistorikk(vedtaksperiode: Periode, vedtaksperiodeRettEtter: Periode? = null): Pair<Boolean, HvorforIkkeOppdatereHistorikk?>
    internal abstract fun sykdomstidslinje(meldingsreferanseId: UUID, registrert: LocalDateTime): Sykdomstidslinje

}