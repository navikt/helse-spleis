# Flere inntektsmeldinger på kort tid

## Scenario

Én arbeidsgiver sender to inntektsmeldinger med to minutters mellomrom. Hva skjer da?

### datagrunnlag

| datagrunnlag | AGP             | Refusjon | Sendetidspunkt |
|--------------|-----------------|----------|----------------|
| IM 1         | 01.01 til 16.01 | Full     | 15:30          |
| IM 2         | 02.01 til 17.01 | Delvis   | 15:32          |

### dagens observerte oppførsel

| ArbeidsforholdID IM 1 | Arbeidsforhold IM 2 | Varsler              | Brukt dager fra | Brukt inntekt/refusjon fra |
|-----------------------|---------------------|----------------------|-----------------|----------------------------|
| Ikke satt             | Ikke satt           | RV_IM_4 og RV_IM 22  | IM 1            | IM 2                       |
| Satt til noe          | Ikke satt           | RV_IM_4 og RV_IM 22  | IM 1            | IM 2                       |
| Ikke satt             | Satt til noe        | RV_IM_4 og RV_IM 22  | IM 1            | IM 2                       |
| Satt til noe          | Satt til noe        | RV_IM_4 og RV_IM 22  | IM 1            | IM 2                       |

### ønsket oppførsel

| ArbeidsforholdID IM 1 | Arbeidsforhold IM 2 | Varsler | Brukt dager fra | Brukt inntekt/refusjon fra |
|-----------------------|---------------------|---------|-----------------|----------------------------|
| Ikke satt             | Ikke satt           | ???     | ???             | ???                        |
| Satt til noe          | Ikke satt           | ???     | ???             | ???                        |
| Ikke satt             | Satt til noe        | ???     | ???             | ???                        |
| Satt til noe          | Satt til noe        | ???     | ???             | ???                        |

Hvem har lyst til å fylle inn spørsmålstegnene?