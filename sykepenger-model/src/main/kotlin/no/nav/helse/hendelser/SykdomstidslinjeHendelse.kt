package no.nav.helse.hendelser

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.nesteDag
import no.nav.helse.person.Behandlinger

sealed class SykdomstidslinjeHendelse protected constructor(
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
) : ArbeidstakerHendelse(fødselsnummer, aktørId, organisasjonsnummer), SykdomshistorikkHendelse {
    private val håndtertAv = mutableSetOf<UUID>()
    private var nesteFraOgMed: LocalDate = LocalDate.MIN

    override fun oppdaterFom(other: Periode): Periode {
        // strekker vedtaksperioden tilbake til å måte første dag
        val førsteDag = sykdomstidslinje().førsteDag()
        return other.oppdaterFom(førsteDag)
    }

    internal fun noenHarHåndtert() = håndtertAv.isNotEmpty()

    internal abstract fun erRelevant(other: Periode): Boolean

    internal fun vurdertTilOgMed(dato: LocalDate) {
        nesteFraOgMed = dato.nesteDag
        trimSykdomstidslinje(nesteFraOgMed)
    }

    protected open fun trimSykdomstidslinje(fom: LocalDate) {}

    internal fun periode(): Periode {
        return sykdomstidslinje().periode() ?: LocalDate.MIN.somPeriode()
    }

    override fun equals(other: Any?): Boolean = other is SykdomstidslinjeHendelse
        && this.metadata.meldingsreferanseId == other.metadata.meldingsreferanseId

    internal fun leggTil(vedtaksperiodeId: UUID, behandlinger: Behandlinger): Boolean {
        håndtertAv.add(vedtaksperiodeId)
        // return behandlinger.oppdaterDokumentsporing(dokumentsporing())
        return true
    }

    override fun hashCode(): Int {
        return metadata.meldingsreferanseId.hashCode()
    }
}