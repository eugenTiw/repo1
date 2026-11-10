# Fernziel (Arbeitstitel)

Eine ruhige, minimalistische Android-App zum langfristigen Verfolgen von Zielen.
Leitgedanke: **Deine Daten bleiben bei dir.** Die App arbeitet vollständig offline.

## Was die App kann

- **Ziele** mit Lebensbereich, optionalem Zieltermin und einem Kriterium („Erreicht, wenn …").
- **Handlungen** je Ziel mit Häufigkeit: täglich, x-mal pro Woche oder x-mal pro Monat.
- **Heute:** To-do-Liste für den Tag. Tägliche Handlungen erscheinen automatisch.
- **Diese Woche:** Wochenhandlungen per Tipp einem Wochentag zuteilen; vergangene Tage lassen sich nachträglich abhaken.
- **Diesen Monat:** Monatshandlungen für heute einplanen.
- Heller und dunkler Modus, Deutsch und Englisch.

## Datenschutz als Prinzip

- Die App fordert **keine Berechtigungen** an, auch keinen Internetzugriff. Das Manifest entfernt die Internetberechtigung ausdrücklich, falls eine Bibliothek sie mitbringen sollte.
- Alle Daten liegen in einer lokalen SQLite-Datenbank im privaten App-Speicher.
- Cloud-Backups der App-Daten sind abgeschaltet (`allowBackup="false"`).
- Keine Werbung, kein Tracking, keine Analyse-Bibliotheken. Einzige Abhängigkeiten sind die offiziellen AndroidX-Bibliotheken.

## Testversion installieren

Bei jeder Änderung auf `main` baut GitHub automatisch eine Testversion:

1. Auf dem Android-Gerät die Seite **Releases** dieses Repositorys öffnen und unter „Testversion" die Datei `fernziel-test.apk` laden.
2. Die Datei öffnen und die Installation aus dieser Quelle einmalig erlauben.

Die Testversion heißt intern `com.eugentiw.fernziel.test` und ist mit einem öffentlichen Testschlüssel signiert. Sie lässt sich mit neueren Testversionen aktualisieren, ist aber **nicht** die Play-Store-Version.

## Technik

- Kotlin, Jetpack Compose, minSdk 26, targetSdk 36
- Datenhaltung: `SQLiteOpenHelper` (ohne zusätzliche Datenbank-Bibliothek)
- Build: Gradle 8.14, Android Gradle Plugin 8.13, GitHub Actions (`.github/workflows/build.yml`)

Lokal bauen (Android Studio oder Kommandozeile mit Android SDK):

```
./gradlew assembleDebug
```

## Offene Punkte bis zum Play Store

- Endgültiger App-Name und endgültige App-ID
- Export und Import der Daten in eine lokale Datei (Sicherung ohne Cloud)
- Eigener, geheimer Signaturschlüssel und signiertes App Bundle
- Datenschutzerklärung, Store-Eintrag, geschlossener Test mit 12 Testern über 14 Tage
