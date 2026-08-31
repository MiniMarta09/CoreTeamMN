# CoreTeam

**Small Business Management App** — Applicazione Android per la gestione operativa e il benessere aziendale.

Progetto realizzato per il corso di Programmazione Mobile, Corso di Laurea Triennale in Ingegneria Informatica, Università di Genova (DIBRIS).

**Progetto realizzato da:** Marta Nasso, Gabriele Bordo

---

## Obiettivo

CoreTeam nasce per aiutare le piccole e medie aziende nella gestione operativa quotidiana, offrendo al tempo stesso funzionalità dedicate alla tutela della salute e del benessere dei dipendenti.

L'app distingue due ruoli utente, con permessi e viste differenti:
- **Dipendente** — accesso standard alle funzionalità operative e personali
- **Capo** — accesso amministratore, con strumenti aggiuntivi di gestione del team

---

## Stack tecnologico

- **Linguaggio:** Kotlin
- **Piattaforma:** Android
- **Build system:** Gradle (Kotlin DSL)
- **Backend:** Firebase
  - Firebase Authentication (Email/Password + Google Sign-In)
  - Cloud Firestore (dati applicativi e ruoli utente)

---

## Funzionalità principali

### Login e Autenticazione
Accesso tramite email e password o account Google. I ruoli (Dipendente/Capo) sono memorizzati esclusivamente su Firestore e determinano le viste e i permessi disponibili.

### Home Page
Punto di accesso centrale con sei sezioni principali (Diario, Eventi, Turni, Team, Bacheca, Richieste) e barra di navigazione inferiore con sezione profilo integrata.

### Profilo
Visualizzazione e modifica delle informazioni personali, con possibilità di logout.

### Eventi
Calendario lavorativo con creazione di eventi:
- visibili a tutti i dipendenti
- visibili solo all'utente che li inserisce

### Bacheca
Spazio condiviso per pubblicare post visibili a tutto il team, utile per segnalazioni e comunicazioni interne (con reazioni like/dislike).

### Team
Elenco dei dipendenti organizzato per settore, con search bar a filtro real-time e possibilità di contattare i colleghi via email.

### Richieste
Gestione di quattro tipologie di richieste:
- Ferie
- Permesso ingresso
- Permesso uscita
- Smart-working

Ogni richiesta resta in stato "In Attesa" fino ad accettazione o rifiuto da parte del Capo.

### Turni
Due sezioni distinte:
- **Pubblica** — orari di lavoro per ogni settore aziendale
- **Privata** — turni assegnati all'utente corrente

### Diario
Feedback mensile personale su tre parametri chiave (stress, rapporto con i colleghi, soddisfazione), con dashboard personale che include grafico a barre e statistiche riassuntive (punto di forza, area di miglioramento, partecipazione, mese migliore).

---

## Funzionalità riservate al ruolo Capo

- **Accettazione Richieste** — visualizzazione di tutte le richieste dei dipendenti con possibilità di accettarle o rifiutarle
- **Generazione Turni (Smart Scheduling)** — algoritmo di scheduling automatico che genera i turni per tutti i settori tenendo conto di:
  - disponibilità reali dei dipendenti
  - vincoli contrattuali e prevenzione di sovrapposizioni
  - equilibrio tra presenza in sede e smart-working
  - workflow di controllo: selezione settimana → anteprima → pubblicazione
- **Dashboard Diario Aziendale** — visualizzazione aggregata dei feedback mensili di tutto il team, con statistiche e trend nel tempo

---

## Struttura del progetto

```
CoreTeam/
├── app/                    # Codice sorgente dell'applicazione Android
├── gradle/                 # Wrapper e configurazione Gradle
├── build.gradle.kts        # Configurazione build del progetto
├── settings.gradle.kts     # Configurazione moduli
├── gradle.properties
├── gradlew / gradlew.bat   # Gradle wrapper scripts
└── CoreTeamProject.zip
```

---

## Come avviare il progetto

1. Clona la repository:
   ```bash
   git clone https://github.com/G48ri3l3/CoreTeam.git
   ```
2. Apri il progetto in Android Studio.
3. Collega il progetto a un Firebase project (Authentication + Firestore abilitati) inserendo il file `google-services.json` nella cartella `app/`.
4. Sincronizza Gradle ed esegui l'app su un emulatore o dispositivo fisico.

---

## Documentazione

La presentazione completa del progetto è disponibile nel file [`presentazioneCoreteam.pdf`](./presentazioneCoreteam.pdf), che illustra obiettivi, funzionalità e architettura dell'applicazione.

---

## Contesto accademico

Progetto sviluppato per il corso di Programmazione Mobile, Ingegneria Informatica, Università di Genova — DIBRIS.
