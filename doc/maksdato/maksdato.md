Diagram 1 — Tilstandsmaskin (state machine per dag i tidslinjen)

```mermaid
    stateDiagram-v2
    direction LR

    [*] --> Initiell

    Initiell --> Syk : betalbarDag\n→ start ny maksdatosak\n(treårsvindu = dag - 3 år)

    Syk --> Syk : betalbarDag\n(inkrementer betalteDager)
    Syk --> Karantene : betalbarDag\n(248 dager brukt)
    Syk --> KaranteneOver67 : betalbarDag\n(60 dager over 67 brukt)
    Syk --> Opphold : oppholdsdag
    Syk --> OppholdFri : fridag

    Opphold --> Syk : betalbarDag\n(+ forskyv treårsvindu)
    Opphold --> Opphold : oppholdsdag/helg\n(< 182 dager opphold)
    Opphold --> Initiell : oppholdsdag\n(≥ 182 dager opphold)

    OppholdFri --> Syk : betalbarDag\n(uten dekrement)
    OppholdFri --> OppholdFri : fridag\n(< 182 dager)
    OppholdFri --> Opphold : oppholdsdag\n(< 182 dager)
    OppholdFri --> Initiell : fridag/oppholdsdag\n(≥ 182 dager)

    Karantene --> Karantene : betalbarDag/avvistDag\n(avslag- SykepengedagerOppbrukt, < 182)
    Karantene --> KaranteneTilstrekkeligOppholdNådd : helg/ferie\n(≥ 182 dager opphold)
    Karantene --> Initiell : oppholdsdag\n(≥ 182 dager opphold)

    KaranteneOver67 --> KaranteneOver67 : betalbarDag/avvistDag\n(avslag- SykepengedagerOppbruktOver67)
    KaranteneOver67 --> KaranteneTilstrekkeligOppholdNådd : helg/ferie (≥ 182)
    KaranteneOver67 --> Initiell : oppholdsdag (≥ 182)

    KaranteneTilstrekkeligOppholdNådd --> KaranteneTilstrekkeligOppholdNådd : betalbarDag/avvistDag\n(avslag- NyVilkårsprøvingNødvendig)
    KaranteneTilstrekkeligOppholdNådd --> Initiell : oppholdsdag

    note right of Initiell
        Arkiverer forrige maksdatosak
        Starter ny (tom) sak
    end note

    state "Alle tilstander" as alle
    state fork <<fork>>
    [*] --> fork
    fork --> ForGammel : dato ≥ 70-årsdag
    fork --> Død : dato > dødsdato


    ForGammel --> ForGammel : betalbarDag/helg\n(avslag- Over70)
    Død --> Død : betalbarDag/helg\n(avslag- EtterDødsdato)
```

-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

Diagram 2 — Beregning av maksdato (fra Maksdatokontekst.beregnMaksdato)

```mermaid 
flowchart TD
    A([beregnMaksdato\ncalled for a vedtaksperiode]) --> B[Finn forrigeVirkedag\n= siste betalte dag\neller siste vurderte virkedag]

    B --> C[Beregn ordinærRett\n= forrigeVirkedag\n+ gjenstående dager under 67 år]
    B --> D[Beregn begrensetRett\n= max&#40forrigeVirkedag, 67-årsdag som virkedag&#41\n+ gjenstående dager over 67 år]
    
    C --> E{Sammenlign\nordnærRett vs\nbegrensetRett vs\n70-årsdag}
    D --> E
    
    E -->|ordinærRett ≤ begrensetRett| F[Maksdato = ordinærRett\nBestemmelse = ORDINÆR_RETT\nKvote: 248 ukedager]
    E -->|begrensetRett ≤ virkedag før 70-årsdag| G[Maksdato = begrensetRett\nBestemmelse = BEGRENSET_RETT\nKvote: 60 ukedager etter 67 år]
    E -->|ellers| H[Maksdato = virkedag før 70-årsdag\nBestemmelse = SYTTI_ÅR]
    
    F --> I{Dødsdato\nkjent?}
    G --> I
    H --> I
    
    I -->|Ja| J[Maksdato = min&#40beregnet, dødsdato&#41]
    I -->|Nei| K([BeregnetMaksdato\nmed maksdato og\ngjenståendeDager])
    J --> K
    
    style F fill:#c8e6c9
    style G fill:#fff9c4
    style H fill:#ffccbc
    style J fill:#e1bee7

```
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

Oppsummering av flyten
```
┌───────────────────────────┬────────────────────────────────────────────────────────────────────────────────────┐
│ Konsept                   │ Verdi                                                                              │
├───────────────────────────┼────────────────────────────────────────────────────────────────────────────────────┤
│ Maks dager (ordinær)      │ 248 ukedager (§ 8-12)                                                              │
├───────────────────────────┼────────────────────────────────────────────────────────────────────────────────────┤
│ Maks dager over 67 år     │ 60 ukedager (§ 8-51)                                                               │
├───────────────────────────┼────────────────────────────────────────────────────────────────────────────────────┤
│ Tilstrekkelig opphold     │ 26 uker = 182 dager                                                                │
├───────────────────────────┼────────────────────────────────────────────────────────────────────────────────────┤
│ Treårsvindu               │ Kun betalte dager siste 3 år teller                                                │
├───────────────────────────┼────────────────────────────────────────────────────────────────────────────────────┤
│ Opphold med bare fridager │ Teller IKKE mot 182-dagersregelen for ny rettighet – kun opphold → Opphold-state   │
├───────────────────────────┼────────────────────────────────────────────────────────────────────────────────────┤
│ Initiell-tilstand         │ Arkiverer gjeldende sak og starter ny tom Maksdatokontekst                         │
└───────────────────────────┴────────────────────────────────────────────────────────────────────────────────────┘
```

Kjerne-ideen: For hver dag i tidslinjen kjøres staten fremover. Betalbare dager forbruker kvoten; opphold akkumuleres. Etter 182 dagers opphold nullstilles rettigheten (Initiell). Til slutt beregnes selve
maksdatoen ved å se fremover basert på gjenværende kvote fra siste betalte dag.