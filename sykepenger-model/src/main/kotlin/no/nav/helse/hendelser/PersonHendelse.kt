package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.AktivitetsloggMappingPort
import no.nav.helse.person.aktivitetslogg.AktivitetsloggObserver
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Subaktivitetskontekst
import no.nav.helse.person.aktivitetslogg.Varselkode

internal class FunksjonelleFeilTilVarsler(private val other: IAktivitetslogg) : IAktivitetslogg by other {
    override fun funksjonellFeil(kode: Varselkode) = varsel(kode)

    override fun barn() = FunksjonelleFeilTilVarsler(other.barn())

    internal companion object {
        internal fun wrap(hendelse: PersonHendelse, block: () -> Unit) = hendelse.wrap(::FunksjonelleFeilTilVarsler, block)
    }
}

abstract class PersonHendelse protected constructor(
    private val meldingsreferanseId: UUID,
    protected val fødselsnummer: String,
    protected val aktørId: String,
    private var aktivitetslogg: IAktivitetslogg
) : IAktivitetslogg, Aktivitetskontekst {

    init {
        aktivitetslogg.kontekst(this)
    }

    internal fun wrap(other: (IAktivitetslogg) -> IAktivitetslogg, block: () -> Unit): IAktivitetslogg {
        val kopi = aktivitetslogg
        aktivitetslogg = other(aktivitetslogg)
        block()
        aktivitetslogg = kopi
        return this
    }

    fun aktørId() = aktørId
    fun fødselsnummer() = fødselsnummer

    fun meldingsreferanseId() = meldingsreferanseId

    final override fun toSpesifikkKontekst() = this.javaClass.canonicalName.split('.').last().let {
        SpesifikkKontekst(it, mapOf(
            "meldingsreferanseId" to meldingsreferanseId().toString(),
            "aktørId" to aktørId(),
            "fødselsnummer" to fødselsnummer()
        ) + kontekst())
    }

    protected open fun kontekst(): Map<String, String> = emptyMap()

    fun toLogString() = aktivitetslogg.toString()

    override fun info(melding: String, vararg params: Any?) = aktivitetslogg.info(melding, *params)
    override fun varsel(melding: String) = aktivitetslogg.varsel(melding)
    override fun varsel(kode: Varselkode) = aktivitetslogg.varsel(kode)
    override fun behov(type: Aktivitet.Behov.Behovtype, melding: String, detaljer: Map<String, Any?>) =
        aktivitetslogg.behov(type, melding, detaljer)
    override fun funksjonellFeil(kode: Varselkode) = aktivitetslogg.funksjonellFeil(kode)
    override fun logiskFeil(melding: String, vararg params: Any?) = aktivitetslogg.logiskFeil(melding, *params)
    override fun harAktiviteter() = aktivitetslogg.harAktiviteter()
    override fun harVarslerEllerVerre() = aktivitetslogg.harVarslerEllerVerre()
    override fun harFunksjonelleFeilEllerVerre() = aktivitetslogg.harFunksjonelleFeilEllerVerre()
    override fun aktivitetsteller() = aktivitetslogg.aktivitetsteller()
    override fun behov() = aktivitetslogg.behov()
    override fun barn() = aktivitetslogg.barn()
    override fun kontekst(kontekst: Aktivitetskontekst) = aktivitetslogg.kontekst(kontekst)
    override fun kontekst(kontekst: Subaktivitetskontekst) = aktivitetslogg.kontekst(kontekst)
    override fun kontekster() = aktivitetslogg.kontekster()
    override fun toMap(mapper: AktivitetsloggMappingPort) = aktivitetslogg.toMap(mapper)

    override fun register(observer: AktivitetsloggObserver) {
        aktivitetslogg.register(observer)
    }
}
