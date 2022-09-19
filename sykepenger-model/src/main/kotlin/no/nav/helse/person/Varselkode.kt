package no.nav.helse.person

// Alle Varselkoder må følge formatet
internal const val varselkodeformat = "\\D{1}_\\D{2}_\\d{1,3}"
private val regex = "^$varselkodeformat$".toRegex()

enum class Varselkode(private val melding: String, private val deprekert: Boolean = false) {
    // SY: Sykmelding
    A_SY_1("Søknadsperioden kan ikke være eldre enn 6 måneder fra mottattidspunkt"),

    // SØ: Søknad
    A_SØ_1("Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger"),
    A_SØ_2("Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd"),
    A_SØ_4("Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling."),
    A_SØ_5("Utdanning oppgitt i perioden i søknaden."),
    A_SØ_6("Søknaden inneholder Permisjonsdager utenfor sykdomsvindu"),
    A_SØ_7("Søknaden inneholder egenmeldingsdager etter sykmeldingsperioden"),
    A_SØ_8("Søknaden inneholder Arbeidsdager utenfor sykdomsvindu"),
    A_SØ_9("Utenlandsopphold oppgitt i perioden i søknaden."),
    A_SØ_10("Det er oppgitt annen inntektskilde i søknaden. Vurder inntekt."),
    A_SØ_11("Den sykmeldte har oppgitt å ha andre arbeidsforhold med sykmelding i søknaden."),

    // IM: Inntektsmelding
    A_IM_1("Vi har mottatt en inntektsmelding i en løpende sykmeldingsperiode med oppgitt første/bestemmende fraværsdag som er ulik tidligere fastsatt skjæringstidspunkt."),
    A_IM_2("Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode."),

    /** Inntektsmeldingen og spleis er uenige om AGP */
    A_IM_3("Inntektsmeldingen og vedtaksløsningen er uenige om beregningen av arbeidsgiverperioden. Undersøk hva som er riktig arbeidsgiverperiode."),

    A_IM_4("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt."),
    A_IM_5("Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig."),
    A_IM_6("Sykmeldte har oppgitt ferie første dag i arbeidsgiverperioden."),

    // RE: Refusjon
    A_RE_1("Fant ikke refusjonsgrad for perioden. Undersøk oppgitt refusjon før du utbetaler."),

    // IT: Infotrygd
    A_IT_1("Det er utbetalt en periode i Infotrygd etter perioden du skal behandle nå. Undersøk at antall forbrukte dager og grunnlag i Infotrygd er riktig"),
    A_IT_2("Perioden er lagt inn i Infotrygd, men ikke utbetalt. Fjern fra Infotrygd hvis det utbetales via speil."),
    A_IT_3("Utbetaling i Infotrygd overlapper med vedtaksperioden"),
    A_IT_4("Det er registrert utbetaling på nødnummer"),
    A_IT_5("Mangler inntekt for første utbetalingsdag i en av infotrygdperiodene"),

    // VV: Vilkårsvurdering
    A_VV_1("Vi fant ugyldige arbeidsforhold i Aareg, burde sjekkes opp nærmere"),
    A_VV_2("Arbeidsgiver er ikke registrert i Aa-registeret."),
    A_VV_4("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold"),
    A_VV_5("Første utbetalingsdag er i Infotrygd og mellom 1. og 16. mai. Kontroller at riktig grunnbeløp er brukt."),
    A_VV_6("Minst én dag uten utbetaling på grunn av sykdomsgrad under 20 %. Vurder å sende vedtaksbrev fra Infotrygd"),
    A_VV_7("Bruker mangler nødvendig inntekt ved validering av Vilkårsgrunnlag"),

    // VV: Opptjeningsvurdering
    A_OV_1("Perioden er avslått på grunn av manglende opptjening"),
    A_OV_2("Opptjeningsvurdering må gjøres manuelt fordi opplysningene fra AA-registeret er ufullstendige"),

    // MV: Medlemskapsvurdering
    A_MV_1("Vurder lovvalg og medlemskap"),
    A_MV_2("Perioden er avslått på grunn av at den sykmeldte ikke er medlem av Folketrygden"),

    // IV: Inntektsvurdering
    A_IV_1("Bruker har flere inntektskilder de siste tre månedene enn arbeidsforhold som er oppdaget i Aa-registeret."),
    A_IV_2("Har mer enn 25 % avvik. Dette støttes foreløpig ikke i Speil. Du må derfor annullere periodene."),

    // SV: Sykepengegrunnlagsvurdering
    A_SV_1("Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag"),
    A_SV_2("Minst en arbeidsgiver inngår ikke i sykepengegrunnlaget"),

    // AY: Andre ytelser
    A_AY_1("Arena inneholdt en eller flere AAP-perioder med ugyldig fom/tom"),
    A_AY_2("Arena inneholdt en eller flere Dagpengeperioder med ugyldig fom/tom"),
    A_AY_3("Bruker har mottatt AAP innenfor 6 måneder før skjæringstidspunktet. Kontroller at brukeren har rett til sykepenger"),
    A_AY_4("Bruker har mottatt dagpenger innenfor 4 uker før skjæringstidspunktet. Kontroller om bruker er dagpengemottaker. Kombinerte ytelser støttes foreløpig ikke av systemet"),
    A_AY_5("Det er utbetalt foreldrepenger i samme periode."),
    A_AY_6("Det er utbetalt pleiepenger i samme periode."),
    A_AY_7("Det er utbetalt omsorgspenger i samme periode."),
    A_AY_8("Det er utbetalt opplæringspenger i samme periode."),
    A_AY_9("Det er institusjonsopphold i perioden. Vurder retten til sykepenger."),

    // SI: Simulering
    A_SI_1("Feil under simulering"),
    A_SI_2("Simulering av revurdert utbetaling feilet. Utbetalingen må annulleres"),

    // UT: Utbetaling
    A_UT_1("Utbetaling av revurdert periode ble avvist av saksbehandler. Utbetalingen må annulleres"),
    A_UT_2("Utbetalingen ble gjennomført, men med advarsel"),

    // OS: Oppdragsystemet
    A_OS_1("Utbetalingen forlenger et tidligere oppdrag som opphørte alle utbetalte dager. Sjekk simuleringen."),
    A_OS_2("Utbetalingens fra og med-dato er endret. Kontroller simuleringen"),
    A_OS_3("Endrer tidligere oppdrag. Kontroller simuleringen."),
    // RV: Revurdering
    A_RV_1("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden");

    init {
        require(this.name.matches(regex)) {"Ugyldig varselkode-format: ${this.name}"}
    }

    internal fun varsel(kontekster: List<SpesifikkKontekst>): Aktivitetslogg.Aktivitet.Varsel =
        Aktivitetslogg.Aktivitet.Varsel.opprett(kontekster, this, melding)

    override fun toString() = "${this.name}: $melding"
}
