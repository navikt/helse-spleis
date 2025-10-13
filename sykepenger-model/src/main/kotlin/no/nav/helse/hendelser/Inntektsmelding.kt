package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.forrigeDag
import no.nav.helse.førsteArbeidsdag
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.mapWithNext
import no.nav.helse.nesteDag
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Person
import no.nav.helse.person.Sykmeldingsperioder
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_23
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.refusjon.Refusjonsservitør
import no.nav.helse.økonomi.Inntekt

class Inntektsmelding(
    meldingsreferanseId: MeldingsreferanseId,
    private val refusjon: Refusjon,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
    beregnetInntekt: Inntekt,
    arbeidsgiverperioder: List<Periode>,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: BegrunnelseForReduksjonEllerIkkeUtbetalt?,
    private val opphørAvNaturalytelser: List<OpphørAvNaturalytelse>,
    private val harFlereInntektsmeldinger: Boolean,
    private val førsteFraværsdag: LocalDate?,
    mottatt: LocalDateTime
) : Hendelse {

    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = ARBEIDSGIVER,
        innsendt = mottatt,
        registrert = mottatt,
        automatiskBehandling = false
    )

    private val grupperteArbeidsgiverperioder = arbeidsgiverperioder.grupperSammenhengendePerioder()
    private val dager by lazy {
        DagerFraInntektsmelding(
            arbeidsgiverperioder = grupperteArbeidsgiverperioder,
            førsteFraværsdag = førsteFraværsdag,
            mottatt = metadata.registrert,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
            harFlereInntektsmeldinger = harFlereInntektsmeldinger,
            opphørAvNaturalytelser = opphørAvNaturalytelser,
            hendelse = this
        )
    }

    init {
        val count = arbeidsgiverperioder.flatten().count()
        check(count <= 16) {
            "antall arbeidsgiverperiodedager kan ikke være mer enn 16 dager: var $count"
        }
    }

    private val kompensertFørsteFraværsdag: LocalDate by lazy {
        if (førsteFraværsdag != null && (grupperteArbeidsgiverperioder.isEmpty() || førsteFraværsdag > grupperteArbeidsgiverperioder.last().endInclusive.nesteDag)) førsteFraværsdag
        else grupperteArbeidsgiverperioder.maxOf { it.start }
    }

    // dagen inntekten gjelder for er "dag nr 17", slik at ikke en auu-periode håndterer inntekten
    internal val datoForHåndteringAvInntekt = if (begrunnelseForReduksjonEllerIkkeUtbetalt == null) {
        listOfNotNull(grupperteArbeidsgiverperioder.lastOrNull()?.endInclusive?.nesteDag, førsteFraværsdag).max()
    } else {
        kompensertFørsteFraværsdag
    }

    private val refusjonsdato: LocalDate by lazy {
        if (førsteFraværsdag == null) grupperteArbeidsgiverperioder.maxOf { it.start }
        else grupperteArbeidsgiverperioder.map { it.start }.plus(førsteFraværsdag).max()
    }

    internal val refusjonsservitør get() = Refusjonsservitør.fra(refusjon.refusjonstidslinje(refusjonsdato, metadata.meldingsreferanseId, metadata.innsendt))

    private var håndtertInntekt = false
    val dokumentsporing = Dokumentsporing.inntektsmeldingInntekt(meldingsreferanseId)

    internal val inntektsdata = Inntektsdata(metadata.meldingsreferanseId, kompensertFørsteFraværsdag, beregnetInntekt, metadata.registrert)

    internal fun korrigertInntekt() = ArbeidstakerFaktaavklartInntekt(
        id = UUID.randomUUID(),
        inntektsdata = inntektsdata,
        inntektsopplysningskilde = Arbeidstakerinntektskilde.Arbeidsgiver
    )

    internal fun inntektHåndtert() {
        håndtertInntekt = true
    }

    @JvmInline
    value class BegrunnelseForReduksjonEllerIkkeUtbetalt private constructor(private val begrunnelse: String) {
        init { check(begrunnelse.isNotBlank()) }
        override fun toString() = begrunnelse
        internal fun valider(aktivitetslogg: IAktivitetslogg, hulleteArbeidsgiverperiode: Boolean) {
            aktivitetslogg.info("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: $begrunnelse")
            if (hulleteArbeidsgiverperiode) aktivitetslogg.funksjonellFeil(RV_IM_23)
            when (begrunnelse) {
                in ikkeStøttedeBegrunnelserForReduksjon -> aktivitetslogg.funksjonellFeil(RV_IM_8)
                "FerieEllerAvspasering" -> aktivitetslogg.varsel(Varselkode.RV_IM_25)
                else -> aktivitetslogg.varsel(RV_IM_8)
            }
        }
        companion object {
            private val ikkeStøttedeBegrunnelserForReduksjon = setOf(
                "BetvilerArbeidsufoerhet",
                "FiskerMedHyre",
                "StreikEllerLockout",
                "FravaerUtenGyldigGrunn",
                "BeskjedGittForSent",
                "IkkeLoenn"
            )
            fun fraInnteksmelding(imBegrunnelse: String?) = when {
                imBegrunnelse.isNullOrBlank() -> null
                else -> BegrunnelseForReduksjonEllerIkkeUtbetalt(imBegrunnelse)
            }

        }
    }

    data class OpphørAvNaturalytelse(
        val beløp: Inntekt,
        val fom: LocalDate,
        val naturalytelse: String
    )

    data class Refusjon(
        val beløp: Inntekt?,
        val opphørsdato: LocalDate?,
        val endringerIRefusjon: List<EndringIRefusjon> = emptyList()
    ) {

        internal fun refusjonstidslinje(refusjonsdato: LocalDate, meldingsreferanseId: MeldingsreferanseId, tidsstempel: LocalDateTime): Beløpstidslinje {
            val kilde = Kilde(meldingsreferanseId, ARBEIDSGIVER, tidsstempel)

            val opphørIRefusjon = opphørsdato?.let {
                val sisteRefusjonsdag = maxOf(it, refusjonsdato.forrigeDag)
                EndringIRefusjon(Inntekt.INGEN, sisteRefusjonsdag.nesteDag)
            }

            val hovedopplysning = EndringIRefusjon(beløp ?: Inntekt.INGEN, refusjonsdato).takeUnless { it.endringsdato == opphørIRefusjon?.endringsdato }

            val gyldigeEndringer = endringerIRefusjon
                .filter { it.endringsdato > refusjonsdato }
                .filter { it.endringsdato < (opphørIRefusjon?.endringsdato ?: LocalDate.MAX) }
                .distinctBy { it.endringsdato }

            val alleRefusjonsopplysninger = listOfNotNull(hovedopplysning, *gyldigeEndringer.toTypedArray(), opphørIRefusjon).sortedBy { it.endringsdato }

            check(alleRefusjonsopplysninger.isNotEmpty()) { "Inntektsmeldingen inneholder ingen refusjonsopplysninger. Hvordan er dette mulig?" }

            return alleRefusjonsopplysninger.mapWithNext { nåværende, neste ->
                val fom = nåværende.endringsdato
                val tom = neste?.endringsdato?.forrigeDag ?: fom
                Beløpstidslinje.fra(fom til tom, nåværende.beløp, kilde)
            }.reduce(Beløpstidslinje::plus)
        }

        data class EndringIRefusjon(val beløp: Inntekt, val endringsdato: LocalDate)
    }

    internal fun dager(): DagerFraInntektsmelding {
        return dager
    }

    internal fun ferdigstill(
        aktivitetslogg: IAktivitetslogg,
        person: Person,
        forkastede: List<Periode>,
        sykmeldingsperioder: Sykmeldingsperioder
    ) {
        if (håndtertInntekt) return // Definisjonen av om en inntektsmelding er håndtert eller ikke er at vi har håndtert inntekten i den... 🤡
        dager.inntektsmeldingIkkeHåndtert(aktivitetslogg, person, forkastede, sykmeldingsperioder)
    }

}
