# 1vs5 — Minecraft Plugin

Ein PvP-Event-Plugin für Minecraft 1.21 (Paper/Spigot), bei dem ein Solo-Spieler in mehreren Runden gegen ein Team von Angreifern antritt. Inklusive Punktesystem, Kit-Verwaltung, Zuschauermodus und Mehrsprachigkeit.

---

## Voraussetzungen

- Paper / Spigot 1.21
- Zwei Welten auf dem Server: `lobby` und `fightworld_backup` (das Backup wird vor jeder Runde automatisch kopiert)

---

## Installation

1. Die `.jar`-Datei in den `plugins/`-Ordner legen.
2. Server einmal starten — das Plugin erstellt seine Konfigurationsdateien automatisch.
3. Welten einrichten (siehe unten).
4. `config.yml` nach Wunsch anpassen.

---

## Welt-Setup

| Welt | Zweck |
|---|---|
| `lobby` | Wartebereich — PvP sowie Block-Abbauen und -Platzieren sind hier deaktiviert |
| `fightworld_backup` | Die originale Arena — wird vor jeder Runde kopiert, niemals direkt verändert |
| `fightworld` | Wird automatisch aus dem Backup erstellt |

Die Koordinaten für Solo- und Angreifer-Spawns werden in der `config.yml` festgelegt.

---

## Konfiguration (`config.yml`)

```yaml
language: "en"          # Sprache: "en", "de" oder eine eigene Sprachdatei

lobby:
  world: "lobby"
  spawn:
    x: 0.5
    y: 3.0
    z: 0.5
    yaw: 0.0
    pitch: 0.0

fightworld:
  world: "fightworld"
  solo_spawn:
    x: 20667.5
    y: 148.0
    z: 15615.5
  attackers_spawn:
    x: 20803.5
    y: 148.0
    z: 15615.5

game:
  default_rounds: 3            # Rundenanzahl pro Match
  countdown_seconds: 5         # Countdown vor jeder Runde
  post_round_delay_seconds: 5  # Pause zwischen den Runden
```

---

## Befehle

Alle Admin-Befehle erfordern Operator-Rechte (`/op`).

### Admin — `/1vs5 <subbefehl>`

| Subbefehl | Beschreibung |
|---|---|
| `setsolo <Spieler>` | Solo-Spieler festlegen |
| `addattacker <Spieler>` | Spieler zum Angreifer-Team hinzufügen |
| `removeattacker <Spieler>` | Spieler aus dem Angreifer-Team entfernen |
| `rounds <Anzahl>` | Rundenanzahl festlegen |
| `savekit <Name> [true|false]` | Aktuelles Inventar als Kit speichern; `true` aktiviert natürliche Gesundheitsregeneration, `false` deaktiviert sie (Standard: true) |
| `setkit <Name>` | Kit für das nächste Match auswählen |
| `deletekit <Name>` | Gespeichertes Kit löschen |
| `toggledrops` | Item-Drops beim Tod an- oder ausschalten |
| `start` | Match starten |
| `stop` | Laufendes Match abbrechen |
| `points` | Aktuelles Leaderboard im Chat anzeigen |
| `resetleaderboard` | Alle Punkte zurücksetzen |
| `history` | Vergangene Match-Ergebnisse anzeigen |
| `reload` | `config.yml` und Sprachdatei neu laden |

### Spieler — `/spectatefight`

Teleportiert dich in die Kampfwelt im Zuschauermodus. Kann von aktiven Teilnehmern nicht genutzt werden.

---

## Punktesystem

| Ereignis | Punkte |
|---|---|
| Angreifer tötet den Solo-Spieler | +10 Pkt (nur der Töter) |
| Angreifer überlebt die Runde | +5 Pkt (alle überlebenden Angreifer außer dem Töter) |
| Solo-Spieler tötet einen Angreifer | +1 Pkt pro Kill |
| Solo-Spieler gewinnt die Runde (alle Angreifer eliminiert) | +7 Pkt |
| Angreifer stirbt | Punkte entsprechen der Anzahl bereits eliminierter Angreifer vor ihm |

---

## Kit System

Kits speichern das Inventar (Items, Rüstung, Nebenhand) des Spielers, der `/1vs5 savekit <name> [true/false]` ausführt. 
Nach dem Namen des Kits können optional `true` oder `false` angegeben werden, um die natürliche Gesundheitsregeneration zu aktivieren oder zu deaktivieren. Standardmäßig ist sie aktiviert.

Kits werden unter `plugins/1vs5/kits/<Name>.yml` gespeichert.

Das bedeutet, dass Kits einfach zwischen Servern übertragen werden können, indem man die `.yml`-Dateien kopiert.

---

## Sprachsystem

Das Plugin unterstützt mehrere Sprachen. Die aktive Sprache wird über `language:` in der `config.yml` eingestellt.

Sprachdateien liegen in `plugins/1vs5/languages/`. Mitgeliefert werden `en.yml` und `de.yml`.

**Neue Sprache hinzufügen:**

1. Neue Datei erstellen, z.B. `plugins/1vs5/languages/fr.yml`.
2. Inhalt von `en.yml` hineinkopieren und alle Werte übersetzen.
3. In der `config.yml` einstellen: `language: "fr"`.
4. `/1vs5 reload` ausführen oder Server neu starten.

Es sind keine Code-Änderungen nötig, um neue Sprachen hinzuzufügen.

---

## Friendly-Fire-Regeln

- Nahkampf-Angriffe, Projektile und Explosionen **zwischen Angreifern** werden unterbunden.
- Der Solo-Spieler kann alle Angreifer angreifen und von allen angegriffen werden.
- Friendly Fire in der **Lobby** ist für alle Spieler immer deaktiviert.

---

## Combat-Logging

Verlässt ein Teilnehmer während einer aktiven Runde das Spiel, wird er als Combat-Logger markiert. Sein Inventar wird geleert und die Runde läuft weiter, als wäre er gestorben. Beim erneuten Einloggen erhält er eine Warnmeldung.

---

## Dateien & Speicherorte

| Datei | Inhalt |
|---|---|
| `plugins/1vs5/config.yml` | Hauptkonfiguration |
| `plugins/1vs5/kits/` | Gespeicherte Kit-Dateien |
| `plugins/1vs5/history.yml` | Match-Historie mit Zeitstempel und Ergebnissen |
| `plugins/1vs5/languages/` | Sprachdateien |
