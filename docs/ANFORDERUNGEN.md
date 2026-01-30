Soldat, hab Acht! Du verlangst das strategische Fundament für unser gesamtes Arsenal. Eine Schlacht wird im Kopf des Planers gewonnen, lange bevor der erste Schuss – oder die erste Codezeile – fällt. Wenn wir die Anforderungen dieser Elite-Nutzer nicht mit chirurgischer Präzision erfüllen, bauen wir kein Meisterstück, sondern Spielzeug.

Hier ist die ArberCharts-Anforderungs-Doktrin. Ich habe die Bedürfnisse der Sektoren analysiert und in unumstößliche technische Zielvorgaben übersetzt.

1. Software-Entwickler (Die Architekten)
   Zero-Dependency Core: Keine "Transitiven Altlasten". Der Core muss ohne externe Libraries (außer der JVM) operieren.

Zero-GC Performance: Vollständige Vermeidung von Objekt-Allokationen im Render-Pfad. Wir fordern stabile 60 FPS bei Millionen von Datenpunkten.

Modulare Extension-Architektur: Nahtlose Integration in Spring Boot (Properties) und Swing (UIManager) über entkoppelte Module.

Fluide API: Ein Builder-Pattern, das typsicher ist und Fehlkonfigurationen zur Kompilierzeit verhindert.

2. Finanzdienstleister (Die Präzisions-Händler)
   Numerische Integrität: Rundungsgenauigkeit nach kaufmännischen Standards (Fixed-Point-Arithmetik statt unzuverlässigem Floating-Point).

High-Frequency Tick-Handling: Die Fähigkeit, Realtime-Datenströme ohne UI-Blockade zu visualisieren.

Deterministisches Rendering: Jeder Chart muss auf jedem System (Linux/Windows/Mac) bis auf den letzten Pixel identisch aussehen.

3. Militär & Polizei (Das Lagezentrum)
   Audit-Trail & Forensik: Jede Änderung des Viewports (Zoom/Pan) muss manipulationssicher im Export (PNG/PDF) versiegelt sein.

Tactical UI (Night-Vision Ready): Themes wie TACTICAL_DARK, die Kontraste maximieren und Blendeffekte in dunklen Gefechtsständen minimieren.

Crash-Fence Schutz: Bei Systemfehlern muss der letzte valide Datenstand (Snapshot) sofort wiederherstellbar und sichtbar sein.

Geringer Footprint: Maximale Leistung auf gehärteter, oft leistungsschwacher Hardware (Edge-Computing).

4. Forscher & Wissenschaftler (Die Entdecker)
   Publikationsqualität: Vektorgrafik-Export (PDF via PDFBox) mit voller DPI-Kontrolle für wissenschaftliche Poster und Fachzeitschriften.

Daten-Transparenz: Die Möglichkeit, jeden visualisierten Punkt per Tooltip auf seinen Ursprungswert zurückzuführen (Raw-Data-Access).

Mathematische Achsen-Skalierung: Logarithmische Skalen und präzise wissenschaftliche Notationen ohne Rundungsfehler.

5. Mediziner & Ärzte (Die Lebensretter)
   Echtzeit-Vitaldaten-Monitoring: Unterstützung für Scrolling-Charts (EKG/EEG-Style) mit absolut ruckelfreier Darstellung.

Fehler-Toleranz: Visuelle Warnsignale (Watermarks/Overlays), wenn Datenströme unterbrochen werden oder Grenzwerte überschritten sind.

Barrierefreiheit: Hohe Kontrastraten und Farbmuster, die auch für Rot-Grün-Schwächen (Daltonismus) interpretierbar bleiben.

Strategische Zusammenfassung der Anforderungen

Anforderung	Priorität	Zielgruppe	Technischer KPI
Zero-GC	Kritisch	Alle	< 0.1% GC Overhead bei Stress
Audit-Trail	Hoch	Militär, Polizei, Finanzen	Manipulationssichere iTXt/XMP Metadaten
Theming	Hoch	Entwickler, Ärzte	Vollständige Steuerung via .properties
Repaint-Region	Mittel	Alle	Minimierung der Dirty-Region auf < 10% Fläche
Ihr nächster Einsatzbefehl:

Die Anforderungen sind definiert. Um sie zu erfüllen, müssen wir die letzte Schwachstelle eliminieren.

Warmup-Validierung: Schließen Sie den MemoryStabilityTest mit der 2-Minuten-Warmup-Eichung ab.

Audit-Kompression: Verifizieren Sie, dass der Ring-Buffer die Forensik nicht gefährdet, sondern schützt.

Demo-Integration: Bauen Sie die Szenarien für "Finanz-Ticker", "Tactical-Map" und "Med-Monitor" in die neue Industrial-Demo ein.

General-Zitat:

"Ein Werkzeug ist nur so gut wie das Vertrauen, das der Nutzer in es setzt. Wir bauen keine Charts. Wir bauen die Wahrheit in Pixeln."