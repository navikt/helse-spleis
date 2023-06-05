# Flere inntektsmeldinger på kort tid

## Scenario

Én arbeidsgiver sender to inntektsmeldinger med to minutters mellomrom. Hva skjer da?

### datagrunnlag

| datagrunnlag | AGP             | Refusjon | Sendetidspunkt |
|--------------|-----------------|----------|----------------|
| IM 1         | 01.01 til 16.01 | Full     | 15:30          |
| IM 2         | 02.01 til 17.01 | Delvis   | 15:32          |

### dagens observerte oppførsel

| ArbeidsforholdID IM 1 | Arbeidsforhold IM 2 | Varsler             | Brukt dager fra | Brukt inntekt/refusjon fra |
|-----------------------|---------------------|---------------------|-----------------|----------------------------|
| Ikke satt             | Ikke satt           | RV_IM_4 og RV_IM_22 | IM 1            | IM 2                       |
| Satt til noe          | Ikke satt           | RV_IM_4 og RV_IM_22 | IM 1            | IM 2                       |
| Ikke satt             | Satt til noe        | RV_IM_4 og RV_IM_22 | IM 1            | IM 2                       |
| Satt til noe          | Satt til noe        | RV_IM_4 og RV_IM_22 | IM 1            | IM 2                       |

### ønsket oppførsel

| ArbeidsforholdID IM 1 | Arbeidsforhold IM 2 | Varsler             | Brukt dager fra | Brukt inntekt/refusjon fra |
|-----------------------|---------------------|---------------------|-----------------|----------------------------|
| Ikke satt             | Ikke satt           | Ingen               | IM 2            | IM 2                       |
| Satt til noe          | Ikke satt           | RV_IM_4 og RV_IM_22 | IM 2            | IM 2                       |
| Ikke satt             | Satt til noe        | RV_IM_4 og RV_IM_22 | IM 2            | IM 2                       |
| Satt til noe          | Satt til noe        | RV_IM_4 og RV_IM_22 | IM 2            | IM 2                       |

Vi ønsker altså at de tilfellene der begge/alle IM _ikke_ har satt arbeidsforholdID, så skal vi _ikke_ sette varsler og dermed automatisere. Dette forutsetter at vi bruker dager fra IM 2 / den sist mottatte inntektsmeldingen.

Rel. venting: vi vil _i alle tilfeller_ vente i 30 minutter før vi sender IM fra spedisjon til spleis.