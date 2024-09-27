package no.nav.helse.etterlevelse

import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.etterlevelse.MaskinellJurist.SubsumsjonEvent.Companion.paragrafVersjonFormaterer

class MaskinellJurist private constructor(
    private val parent: MaskinellJurist?,
    private val kontekster: List<Subsumsjonskontekst>
) : Subsumsjonslogg {

    private val subsumsjoner = mutableListOf<Subsumsjon>()

    constructor() : this(null, emptyList())

    override fun logg(subsumsjon: Subsumsjon) {
        sjekkKontekster()
        if (tomPeriode(subsumsjon)) return
        leggTil(subsumsjon.copy(kontekster = kontekster))
    }

    private fun tomPeriode(subsumsjon: Subsumsjon): Boolean {
        if (subsumsjon.type != Subsumsjon.Subsumsjonstype.PERIODISERT) return false
        if ("perioder" !in subsumsjon.output) return true
        val perioder = subsumsjon.output["perioder"] as List<*>
        return perioder.isEmpty()
    }

    private fun leggTil(subsumsjon: Subsumsjon) {
        subsumsjoner.add(subsumsjon)
        parent?.leggTil(subsumsjon)
    }

    private fun sjekkKontekster() {
        val kritiskeTyper = setOf(KontekstType.Fødselsnummer, KontekstType.Organisasjonsnummer)
        check(kritiskeTyper.all { kritiskType ->
            kontekster.count { it.type == kritiskType } == 1
        }) {
            "en av $kritiskeTyper mangler/har duplikat:\n${kontekster.joinToString(separator = "\n")}"
        }
        // todo: sjekker for mindre enn 1 også ettersom noen subsumsjoner skjer på arbeidsgivernivå. det burde vi forsøke å flytte/fikse slik at
        // alt kan subsummeres i kontekst av en behandling.
        check(kontekster.count { it.type == KontekstType.Vedtaksperiode } <= 1) {
            "det er flere kontekster av ${KontekstType.Vedtaksperiode}:\n${kontekster.joinToString(separator = "\n")}"
        }
    }

    fun medFødselsnummer(personidentifikator: String) =
        kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Fødselsnummer, personidentifikator)))

    fun medOrganisasjonsnummer(organisasjonsnummer: String) =
        kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Organisasjonsnummer, organisasjonsnummer)))

    fun medVedtaksperiode(vedtaksperiodeId: UUID, hendelseIder: List<Subsumsjonskontekst>) =
        kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Vedtaksperiode, vedtaksperiodeId.toString())) + hendelseIder)

    fun medInntektsmelding(inntektsmeldingId: UUID) = kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Inntektsmelding, inntektsmeldingId.toString())))

    private fun kopierMedKontekst(kontekster: List<Subsumsjonskontekst>) =
        MaskinellJurist(this, this.kontekster + kontekster)

    fun subsumsjoner() = subsumsjoner.toList()

    fun events() = subsumsjoner.map { subsumsjon ->
        SubsumsjonEvent(
            sporing = subsumsjon.kontekster
                .filterNot { it.type == KontekstType.Fødselsnummer }
                .groupBy({ it.type }) { it.verdi },
            lovverk = subsumsjon.lovverk,
            ikrafttredelse = paragrafVersjonFormaterer.format(subsumsjon.versjon),
            paragraf = subsumsjon.paragraf.ref,
            ledd = subsumsjon.ledd?.nummer,
            punktum = subsumsjon.punktum?.nummer,
            bokstav = subsumsjon.bokstav?.ref,
            input = subsumsjon.input,
            output = subsumsjon.output,
            utfall = subsumsjon.utfall.name
        )
    }

    data class SubsumsjonEvent(
        val id: UUID = UUID.randomUUID(),
        val sporing: Map<KontekstType, List<String>>,
        val lovverk: String,
        val ikrafttredelse: String,
        val paragraf: String,
        val ledd: Int?,
        val punktum: Int?,
        val bokstav: Char?,
        val input: Map<String, Any>,
        val output: Map<String, Any>,
        val utfall: String,
    ) {

        companion object {
            val paragrafVersjonFormaterer = DateTimeFormatter.ISO_DATE
        }
    }
}