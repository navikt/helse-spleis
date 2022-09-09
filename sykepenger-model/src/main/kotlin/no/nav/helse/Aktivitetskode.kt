package no.nav.helse

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.SpesifikkKontekst

enum class Aktivitetskode(private val melding: String) {
    A_INGEN("INGEN"),
    A_SY_1("Søknadsperioden kan ikke være eldre enn 6 måneder fra mottattidspunkt"),

    A_SØ_1("Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger"),
    A_SØ_2("Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd"),
    A_SØ_3("Søker er ikke gammel nok på søknadstidspunktet til å søke sykepenger uten fullmakt fra verge"),
    A_SØ_4("Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling."),
    A_SØ_5("Bruker har oppgitt at de har jobbet mindre enn sykmelding tilsier"),
    A_SØ_6("Søknaden inneholder en Papirsykmeldingsperiode"),
    A_SØ_7("Utdanning oppgitt i perioden i søknaden."),
    A_SØ_8("Søknaden inneholder Permisjonsdager utenfor sykdomsvindu"),
    A_SØ_9("Søknaden inneholder egenmeldingsdager etter sykmeldingsperioden"),
    A_SØ_10("Søknaden inneholder Arbeidsdager utenfor sykdomsvindu"),
    A_SØ_11("Utenlandsopphold oppgitt i perioden i søknaden."),
    A_SØ_12("Det er oppgitt annen inntektskilde i søknaden. Vurder inntekt."),
    A_SØ_13("Søknaden inneholder andre inntektskilder enn ANDRE_ARBEIDSFORHOLD"),
    A_SØ_14("Den sykmeldte har oppgitt å ha andre arbeidsforhold med sykmelding i søknaden."),

    A_IM_1("Vi har mottatt en inntektsmelding i en løpende sykmeldingsperiode med oppgitt første/bestemmende fraværsdag som er ulik tidligere fastsatt skjæringstidspunkt."),
    A_IM_2("Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode."),
    A_IM_3("Brukeren har opphold i naturalytelser"),
    A_IM_4("Inntektsmeldingen og vedtaksløsningen er uenige om beregningen av arbeidsgiverperioden. Undersøk hva som er riktig arbeidsgiverperiode."),
    A_IM_5("Inntektsmelding inneholder ikke beregnet inntekt"),

    A_IM_6("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: LovligFravaer"),
    A_IM_7("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: FravaerUtenGyldigGrunn"),
    A_IM_8("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: ArbeidOpphoert"),
    A_IM_9("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: BeskjedGittForSent"),
    A_IM_10("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: ManglerOpptjening"),
    A_IM_11("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: IkkeLoenn"),
    A_IM_12("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: BetvilerArbeidsufoerhet"),
    A_IM_13("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: IkkeFravaer"),
    A_IM_14("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: StreikEllerLockout"),
    A_IM_15("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: Permittering"),
    A_IM_16("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: FiskerMedHyre"),
    A_IM_17("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: Saerregler"),
    A_IM_18("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: FerieEllerAvspasering"),
    A_IM_19("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: IkkeFullStillingsandel"),
    A_IM_20("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: TidligereVirksomhet"),
    A_IM_21("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: ukjent årsak"),

    A_IM_22("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt."),
    A_IM_23("Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig."),

    SYKEPENGEGRUNNLAG_INNTEKT_ANTALL_MND(melding = "Forventer maks 3 inntektsmåneder"),
    SYKEPENGEGRUNNLAG_INNTEKT_FRILANSINNTEKT_OPPDAGET(melding = "Fant frilanserinntekt på en arbeidsgiver de siste 3 månedene"),

    A_RE_1("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler."),

    A_IT_1("Det er utbetalt en periode i Infotrygd etter perioden du skal behandle nå. Undersøk at antall forbrukte dager og grunnlag i Infotrygd er riktig"),
    A_IT_2("Perioden er lagt inn i Infotrygd, men ikke utbetalt. Fjern fra Infotrygd hvis det utbetales via speil."),
    A_IT_3("Utbetaling i Infotrygd overlapper med vedtaksperioden"),
    A_IT_4("Det er registrert utbetaling på nødnummer"),
    A_IT_5("Mangler inntekt for første utbetalingsdag i en av infotrygdperiodene"),

    A_VV_1("Vi fant ugyldige arbeidsforhold i Aareg, burde sjekkes opp nærmere"),
    A_VV_2("Arbeidsgiver er ikke registrert i Aa-registeret."),
    A_VV_3("Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig."),
    A_VV_4("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"),
    A_VV_5("Første utbetalingsdag er i Infotrygd og mellom 1. og 16. mai. Kontroller at riktig grunnbeløp er brukt."),
    A_VV_6("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %. Vurder å sende vedtaksbrev fra Infotrygd"),
    A_VV_7("Bruker mangler nødvendig inntekt ved validering av Vilkårsgrunnlag"),

    A_OV_1("Perioden er avslått på grunn av manglende opptjening"),
    A_OV_2("Opptjeningsvurdering må gjøres manuelt fordi opplysningene fra AA-registeret er ufullstendige"),

    INNTEKTSVURDERING_INNTEKT_ANTALL_MND("Forventer 12 eller færre inntektsmåneder"),
    INNTEKTSVURDERING_FLERE_INNTEKTER_ENN_ARBEIDSFORHOLD("Bruker har flere inntektskilder de siste tre månedene enn arbeidsforhold som er oppdaget i Aa-registeret."),
    INNTEKTSVURDERING_FOR_HØYT_AVVIK("Har mer enn 25 % avvik"),

    MEDLEMSKAPSVURDERING_UAVKLART_MEDLEMSKAP("Vurder lovvalg og medlemskap"),
    MEDLEMSKAPSVURDERING_IKKE_MEDLEMSKAP("Perioden er avslått på grunn av at den sykmeldte ikke er medlem av Folketrygden"),

    A_MV_1("Vurder lovvalg og medlemskap"),
    A_MV_2("Perioden er avslått på grunn av at den sykmeldte ikke er medlem av Folketrygden"),

    A_IV_1("Bruker har flere inntektskilder de siste tre månedene enn arbeidsforhold som er oppdaget i Aa-registeret."),
    A_IV_2("Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene."),
    A_IV_3("Har mer enn 25 % avvik"),

    A_SV_1("Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag"),
    A_SV_2("Minst en arbeidsgiver inngår ikke i sykepengegrunnlaget"),

    A_AY_1("Arena inneholdt en eller flere AAP-perioder med ugyldig fom/tom"),
    A_AY_2("Arena inneholdt en eller flere Dagpengeperioder med ugyldig fom/tom"),
    A_AY_3("Bruker har mottatt AAP innenfor 6 måneder før skjæringstidspunktet. Kontroller at brukeren har rett til sykepenger"),
    A_AY_4("Bruker har mottatt dagpenger innenfor 4 uker før skjæringstidspunktet. Kontroller om bruker er dagpengemottaker. Kombinerte ytelser støttes foreløpig ikke av systemet"),
    A_AY_5("Det er utbetalt foreldrepenger i samme periode."),
    A_AY_6("Det er utbetalt pleiepenger i samme periode."),
    A_AY_7("Det er utbetalt omsorgspenger i samme periode."),
    A_AY_8("Det er utbetalt opplæringspenger i samme periode."),
    A_AY_9("Det er institusjonsopphold i perioden. Vurder retten til sykepenger."),

    A_SI_1("Feil under simulering"),

    A_UT_1("Utbetaling av revurdert periode ble avvist av saksbehandler. Utbetalingen må annulleres"),

    A_OS_1("Utbetalingen forlenger et tidligere oppdrag som opphørte alle utbetalte dager. Sjekk simuleringen."),
    A_OS_2("Utbetalingens fra og med-dato er endret. Kontroller simuleringen"),
    A_OS_3("Endrer tidligere oppdrag. Kontroller simuleringen."),

    A_RV_1("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden");

    internal fun varsel(kontekster: List<SpesifikkKontekst>): Aktivitetslogg.Aktivitet.Varsel {
        return Aktivitetslogg.Aktivitet.Varsel.opprett(kontekster, melding)
    }

    internal fun funksjonellFeil(kontekster: List<SpesifikkKontekst>): Aktivitetslogg.Aktivitet.FunksjonellFeil {
        return Aktivitetslogg.Aktivitet.FunksjonellFeil.opprett(kontekster, melding)
    }
}