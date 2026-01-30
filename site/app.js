const dictionary = {
  nav_home: { de: "Start", en: "Home" },
  nav_products: { de: "Produkte", en: "Products" },
  nav_solutions: { de: "Lösungen", en: "Solutions" },
  nav_technology: { de: "Technologie", en: "Technology" },
  nav_docs: { de: "Docs", en: "Docs" },
  nav_demo: { de: "Demo", en: "Demo" },
  nav_license: { de: "Lizenz", en: "Licensing" },
  nav_pricing: { de: "Lizenzierung", en: "Licensing" },
  nav_contact: { de: "Kontakt", en: "Contact" },
  nav_menu: { de: "Menü", en: "Menu" },
  hero_title: { de: "High-Performance-Charting-Engine für Java 25", en: "High-performance charting engine for Java 25" },
  hero_subtitle: {
    de: "158 Renderer. Zero-GC Pipeline. Swing-native, Spring Boot friendly, JDK 25 optimiert.",
    en: "158 renderers. Zero-GC pipeline. Swing-native, Spring Boot friendly, JDK 25 optimized."
  },
  cta_free: { de: "Core herunterladen", en: "Download core" },
  cta_business: { de: "Pricing ansehen", en: "View pricing" },
  cta_contact: { de: "Sales kontaktieren", en: "Contact Sales" },
  download_core: { de: "Core JAR herunterladen", en: "Download Core JAR" },
  download_demo: { de: "Demo JAR herunterladen", en: "Download Demo JAR" },
  problem_kicker: { de: "Das Problem", en: "The problem" },
  problem_title: { de: "Warum Standard-Diagramme in Produktion scheitern", en: "Why standard charts fail in production" },
  problem_card_title: { de: "Grenzen klassischer Charting-Bibliotheken", en: "Limits of classic charting libraries" },
  problem_body: { de: "In echten Produktionsumgebungen stossen Standard-Libraries an harte Grenzen.", en: "In real production environments, standard libraries hit hard limits." },
  problem_bullet_1: { de: "GC-Ruckler durch neue Objekte im Render-Loop", en: "GC stutters from new objects in the render loop" },
  problem_bullet_2: { de: "Feature-Lücken bei Spezial-Renderern", en: "Feature gaps for specialized renderers" },
  problem_bullet_3: { de: "Explodierende Kosten durch Lizenzmodelle pro Entwickler", en: "Exploding costs from per-developer licensing" },
  solution_kicker: { de: "Die Lösung", en: "The solution" },
  solution_title: { de: "ArberCharts", en: "ArberCharts" },
  solution_body_title: { de: "Maximale Performance (Zero-GC)", en: "Maximum performance (Zero-GC)" },
  solution_body: { de: "Unsere Rendering-Pipeline erzeugt im aktiven Betrieb keine neuen Objekte auf dem Heap. Ergebnis: deterministisches Verhalten und flüssige 60 FPS – auch bei Millionen von Datenpunkten.", en: "Our rendering pipeline creates no new heap objects during active drawing. Result: deterministic behavior and smooth 60 FPS, even with millions of points." },
  catalog_filter_all: { de: "Alle", en: "All" },
  catalog_filter_medical: { de: "Medizin & Lebenswissenschaften", en: "Medical & Life Sciences" },
  catalog_filter_financial: { de: "Finanz-Engineering", en: "Financial Engineering" },
  catalog_filter_analysis: { de: "Erweiterte Analyse & Statistik", en: "Advanced Analysis & Statistics" },
  catalog_medical_title: { de: "Medizin & Lebenswissenschaften", en: "Medical & Life Sciences" },
  catalog_medical_body: {
    de: "Visualisierung kritischer Vitaldaten mit Sweep-Erase-Modi für stabile Echtzeitdarstellung.",
    en: "Visualization of critical vital data with sweep-erase modes for stable real-time output."
  },
  catalog_medical_h1: { de: "ECG", en: "ECG" },
  catalog_medical_h2: { de: "EEG", en: "EEG" },
  catalog_medical_h3: { de: "Kapnographie", en: "Capnography" },
  catalog_medical_h4: { de: "Ventilator Waveforms", en: "Ventilator waveforms" },
  catalog_financial_title: { de: "Finanz-Engineering", en: "Financial Engineering" },
  catalog_financial_body: {
    de: "Analysieren Sie Märkte mit komplexen Overlays und nativen Indikatoren.",
    en: "Analyze markets with complex overlays and native indicators."
  },
  catalog_financial_h1: { de: "Candlestick (Hollow/Solid)", en: "Candlestick (hollow/solid)" },
  catalog_financial_h2: { de: "Ichimoku-Cloud", en: "Ichimoku Cloud" },
  catalog_financial_h3: { de: "Renko", en: "Renko" },
  catalog_financial_h4: { de: "Volume Profile", en: "Volume Profile" },
  catalog_analysis_title: { de: "Erweiterte Analyse & Statistik", en: "Advanced Analysis & Statistics" },
  catalog_analysis_body: {
    de: "Statistische Renderer für Dichten, Flüsse und grosse Datenmengen.",
    en: "Statistical renderers for distributions, flows, and large datasets."
  },
  catalog_analysis_h1: { de: "Violin Plots", en: "Violin plots" },
  catalog_analysis_h2: { de: "Sankey Pro", en: "Sankey Pro" },
  catalog_analysis_h3: { de: "Hexbin", en: "Hexbin" },
  catalog_analysis_h4: { de: "FFT (Fourier)", en: "FFT (Fourier)" },
  business_kicker: { de: "Business Case", en: "Business case" },
  business_title: { de: "Rechnen Sie selbst", en: "Do the math" },
  business_build_title: { de: "Build vs. Buy", en: "Build vs. buy" },
  business_build_body: { de: "Die Entwicklung eines spezialisierten Renderers dauert im Schnitt 3–5 Tage. Bei CHF 150/Std. kostet Sie das ca. CHF 6.000.", en: "Building a specialized renderer takes 3–5 days. At CHF 150/hour, that is about CHF 6,000." },
  business_alt: { de: "ArberCharts Alternative: Team- und Distributionslizenzen für 158 Renderer. Details auf der Lizenzseite.", en: "ArberCharts alternative: Team and distribution licensing for 158 renderers. See the licensing page." },
  business_roi: { de: "ROI: amortisiert sich nach der ersten gesparten Arbeitswoche – Distribution nur bei Endkunden-Auslieferung.", en: "ROI: pays back after the first saved work week — distribution licensing applies only when shipping to end customers." },
  callout_cta: { de: "Kontakt aufnehmen", en: "Contact us" },
  engineers_kicker: { de: "Von Ingenieuren", en: "Built by engineers" },
  engineers_title: { de: "Für Ingenieure", en: "For engineers" },
  engineers_java_title: { de: "Java 25, präzise & stabil", en: "Java 25, precise and stable" },
  engineers_java_body: { de: "Deterministische Frames, saubere Typen und API-Design für produktive Teams.", en: "Deterministic frames, clean types, and API design for productive teams." },
  arch_fluent_title: { de: "Fluent API mit ArberChartBuilder", en: "Fluent API with ArberChartBuilder" },
  engineers_i18n_title: { de: "Themes & i18n", en: "Themes and i18n" },
  engineers_i18n_body: { de: "Konsistente UI-Standards für internationale Produkte.", en: "Consistent UI standards for international products." },
  zero_kicker: { de: "Technical Excellence", en: "Technical excellence" },
  zero_title: { de: "Zero-GC Konzept", en: "Zero-GC concept" },
  zero_challenge_title: { de: "Die Herausforderung: Determinismus in Java", en: "The challenge: determinism in Java" },
  zero_challenge_body: {
    de: "In Echtzeitsystemen ist der Stop-the-World-GC der grösste Feind. Wenn im Render-Loop ständig neue Objekte entstehen, sind Pausen unvermeidlich.",
    en: "In real-time systems, stop-the-world GC is the biggest enemy. If the render loop creates new objects constantly, pauses are inevitable."
  },
  zero_approach_title: { de: "Der ArberCharts-Ansatz", en: "The ArberCharts approach" },
  zero_bullet_1: { de: "Allocation-Free Rendering: keine neuen Objekte im aktiven Zeichnen.", en: "Allocation-free rendering: no new objects during active drawing." },
  zero_bullet_2: { de: "Primitive-basierte Verarbeitung statt Boxing/Unboxing.", en: "Primitive-based processing instead of boxing/unboxing." },
  zero_bullet_3: { de: "Object Pooling für wiederverwendbare Ressourcen.", en: "Object pooling for reusable resources." },
  feature_solo_mode: {
    de: "Legend Solo-Mode: Fokussieren Sie Datenreihen durch intelligentes Dimming statt hartem Ausblenden.",
    en: "Legend solo mode: focus series with intelligent dimming instead of hard hiding."
  },
  zero_result: {
    de: "Ergebnis: glattes Schriftbild und flüssige Animationen ohne Mikroruckler – auch nach stundenlangem Dauerbetrieb.",
    en: "Result: crisp text and smooth animations without micro-stutters — even after hours of continuous operation."
  },
  showcase_kicker: { de: "Renderer-Showcase", en: "Renderer showcase" },
  showcase_title: { de: "Top-5 Highlights", en: "Top 5 highlights" },
  showcase_th_renderer: { de: "Renderer", en: "Renderer" },
  showcase_th_focus: { de: "Fokus", en: "Focus" },
  showcase_th_reason: { de: "Warum er den Unterschied macht", en: "Why it makes the difference" },
  docs_kicker: { de: "Dokumentation", en: "Documentation" },
  docs_title: { de: "Bridge-Dokumentation", en: "Bridge documentation" },
  docs_core_title: { de: "Core JavaDocs", en: "Core JavaDocs" },
  docs_core_body: { de: "Die offizielle API-Referenz fuer den headless Core.", en: "The official API reference for the headless core." },
  docs_core_cta: { de: "Core JavaDocs oeffnen", en: "Open core JavaDocs" },
  docs_bridge_title: { de: "Bridge-Dokumentation", en: "Bridge documentation" },
  docs_bridge_body: { de: "Plattform-Bridges fuer Swing, Server, Compose, Swift und Qt.", en: "Platform bridges for Swing, server, Compose, Swift, and Qt." },
  docs_swing_cta: { de: "Swing JavaDocs", en: "Swing JavaDocs" },
  docs_server_cta: { de: "Server JavaDocs", en: "Server JavaDocs" },
  docs_compose_cta: { de: "Compose Docs", en: "Compose docs" },
  docs_swift_cta: { de: "Swift Docs", en: "Swift docs" },
  docs_qt_cta: { de: "Qt Docs", en: "Qt docs" },
  support_policy_title: { de: "LTS Support-Policy", en: "LTS support policy" },
  support_policy_body: {
    de: "Unterstuetzt wird ausschliesslich ArberCharts v1.7.0 LTS. Aeltere Versionen sind abgekündigt und duerfen nicht fuer neue Releases verwendet werden. Produktionsanwendungen sind exklusiv mit v1.7.0 LTS auszuliefern.",
    en: "Only ArberCharts v1.7.0 LTS is supported. Older versions are deprecated and must not be used for new releases. Ship production applications exclusively on v1.7.0 LTS."
  },
  showcase_row1_name: { de: "Medical Sweep-Erase", en: "Medical sweep-erase" },
  showcase_row1_focus: { de: "Medizintechnik", en: "Medtech" },
  showcase_row1_reason: {
    de: "Simuliert das klassische Nachleuchten. Perfekt für EKG/EEG-Daten ohne flackerndes Neuzeichnen.",
    en: "Simulates classic phosphor persistence. Perfect for ECG/EEG without flickering redraws."
  },
  showcase_row2_name: { de: "SankeyPro", en: "SankeyPro" },
  showcase_row2_focus: { de: "Datenanalyse", en: "Data analysis" },
  showcase_row2_reason: {
    de: "Automatische Knotenplatzierung für maximale Lesbarkeit bei minimalen Kreuzungen.",
    en: "Automatic node placement for maximum readability with minimal crossings."
  },
  showcase_row3_name: { de: "Ichimoku-Cloud", en: "Ichimoku Cloud" },
  showcase_row3_focus: { de: "Finanzen", en: "Finance" },
  showcase_row3_reason: {
    de: "Komplexe Trendanalyse mit effizient gerenderter Cloud (Kumo) für Support/Resistance.",
    en: "Complex trend analysis with efficiently rendered cloud (Kumo) for support/resistance."
  },
  showcase_row4_name: { de: "Violin & RidgeLine", en: "Violin & ridgeline" },
  showcase_row4_focus: { de: "Statistik", en: "Statistics" },
  showcase_row4_reason: {
    de: "Macht Verteilungsdichten sichtbar und zeigt mehr als nur Mittelwerte.",
    en: "Reveals distribution density instead of just averages."
  },
  showcase_row5_name: { de: "Delaunay / Voronoi", en: "Delaunay / Voronoi" },
  showcase_row5_focus: { de: "Spezialisierte Analyse", en: "Specialized analysis" },
  showcase_row5_reason: {
    de: "Triangulation für räumliche Beziehungen, ideal für Geodaten und Materialanalysen.",
    en: "Triangulation for spatial relationships, ideal for geodata and materials analysis."
  },
  compare_kicker: { de: "Vergleich", en: "Comparison" },
  compare_title: { de: "ArberCharts vs. Standard", en: "ArberCharts vs standard" },
  compare_th_feature: { de: "Merkmal", en: "Feature" },
  compare_th_oss: { de: "Standard-Open-Source", en: "Standard open-source" },
  compare_th_enterprise: { de: "Enterprise-Konkurrenz", en: "Enterprise competition" },
  compare_row1_feature: { de: "Renderer-Vielfalt", en: "Renderer variety" },
  compare_row1_oss: { de: "10–20 (Basis-Charts)", en: "10–20 (basic charts)" },
  compare_row1_enterprise: { de: "50–80", en: "50–80" },
  compare_row1_ac: { de: "158 Renderer", en: "158 renderers" },
  compare_row2_feature: { de: "Speichereffizienz", en: "Memory efficiency" },
  compare_row2_oss: { de: "Hohe GC-Last", en: "High GC load" },
  compare_row2_enterprise: { de: "Mittel", en: "Medium" },
  compare_row2_ac: { de: "Zero-GC Optimized", en: "Zero-GC optimized" },
  compare_row3_feature: { de: "Java-Version", en: "Java version" },
  compare_row3_oss: { de: "Java 8 oder 11", en: "Java 8 or 11" },
  compare_row3_enterprise: { de: "Java 11 / 17", en: "Java 11 / 17" },
  compare_row3_ac: { de: "Java 25 Ready", en: "Java 25 ready" },
  compare_row4_feature: { de: "API-Design", en: "API design" },
  compare_row4_oss: { de: "Komplex / veraltet", en: "Complex / dated" },
  compare_row4_enterprise: { de: "Dokumentationsabhängig", en: "Documentation-dependent" },
  compare_row4_ac: { de: "Fluent API (ArberChartBuilder)", en: "Fluent API (ArberChartBuilder)" },
  compare_row5_feature: { de: "Lizenzkosten", en: "Licensing cost" },
  compare_row5_oss: { de: "Kostenlos (hoher Dev-Aufwand)", en: "Free (high dev effort)" },
  compare_row5_enterprise: { de: "Sehr hoch", en: "Very high" },
  compare_row5_ac: { de: "Fair & transparent", en: "Fair & transparent" },
  compare_row6_feature: { de: "Support", en: "Support" },
  compare_row6_oss: { de: "Community", en: "Community" },
  compare_row6_enterprise: { de: "Ticket-System / Callcenter", en: "Ticket system / call center" },
  compare_row6_ac: { de: "Direktkontakt zum Dev", en: "Direct access to dev" },
  gallery_kicker: { de: "Galerie", en: "Gallery" },
  gallery_title: { de: "Produkt-Impressionen", en: "Product impressions" },
  gallery_financial: { de: "Finanzcharts", en: "Financial charts" },
  gallery_medical: { de: "Medizinisches Monitoring", en: "Medical monitoring" },
  gallery_indicator: { de: "Marktindikatoren", en: "Market indicators" },
  gallery_flow: { de: "Flow-Analysen", en: "Flow analytics" },
  gallery_surface: { de: "Dichteflächen", en: "Density surfaces" },
  gallery_spatial: { de: "Räumliche Analyse", en: "Spatial analysis" },
  gallery_density: { de: "Verteilungsdichte", en: "Distribution density" },
  gallery_multivariate: { de: "Multivariate Analyse", en: "Multivariate analysis" },
  gallery_analysis: { de: "Signalanalyse", en: "Signal analysis" },
  gallery_network: { de: "Netzwerk-Analysen", en: "Network insights" },
  cta_title: { de: "Bereit für das nächste Level an Performance?", en: "Ready for the next level of performance?" },
  cta_body: { de: "Laden Sie die Demo herunter und erleben Sie 158 Renderer in Aktion. Kostenlos für Einzelentwickler.", en: "Download the demo and experience 158 renderers in action. Free for individual developers." },
  cta_download: { de: "Core-Framework herunterladen", en: "Download core framework" },
  cta_license: { de: "Lizenz erwerben", en: "Purchase license" },
  footer_rights: { de: "© 2026 Arber Gashi. All rights reserved.", en: "© 2026 Arber Gashi. All rights reserved." },
  footer_license: { de: "Proprietäre Software. Lizenzpflichtig für kommerzielle Distribution.", en: "Proprietary software. Licensed for commercial distribution." },
  footer_note: {
    de: "Proprietaere Software. Lizenziert fuer Entwicklung und kommerzielle Distribution.",
    en: "Proprietary software. Licensed for development and commercial distribution."
  },
  footer_tagline: { de: "High-Performance Java Charts für geschäftskritische Systeme.", en: "High-performance Java charts for mission-critical systems." },
  footer_legal: { de: "Rechtliches", en: "Legal" },
  footer_social: { de: "Social", en: "Social" },
  footer_imprint: { de: "Impressum", en: "Imprint" },
  footer_privacy: { de: "Datenschutz", en: "Privacy" },
  footer_github: { de: "GitHub", en: "GitHub" },
  footer_linkedin: { de: "LinkedIn", en: "LinkedIn" },
  footer_x: { de: "X", en: "X" },
  products_title: { de: "Produkte", en: "Products" },
  products_subtitle: { de: "v1.7.0 LTS \"Surgeon\" — Die zertifizierte High-Performance Engine für geschäftskritische Java-Applikationen.", en: "v1.7.0 LTS \"Surgeon\" — The certified high-performance engine for mission-critical Java applications." },
  products_core_title: { de: "Core Framework", en: "Core framework" },
  products_core_body: { de: "API für Entwickler, 158 Renderer, Zero-GC Render-Loop.", en: "Developer API, 158 renderers, zero-GC render loop." },
  products_core_b1: { de: "Zertifizierte Stabilität (100s Stress-Test bestanden).", en: "Certified stability (100s stress test passed)." },
  products_core_b2: { de: "Lock-free Circular Models für Echtzeit-Integrität.", en: "Lock-free circular models for real-time integrity." },
  products_core_b3: { de: "Zero-GC Render-Loop & Flyweight-Theme-Engine.", en: "Zero-GC render loop & flyweight theme engine." },
  products_core_cta: { de: "Core JAR", en: "Core JAR" },
  products_demo_title: { de: "Demo-App", en: "Demo app" },
  products_demo_body: { de: "Referenz-Panels für Financial und Medical sowie Spezial-Renderer.", en: "Reference panels for financial, medical, and specialized renderers." },
  products_demo_b1: { de: "Showcase für 158 Renderer.", en: "Showcase for 158 renderers." },
  products_demo_b2: { de: "Best-Practice Layouts und Interaktionen.", en: "Best-practice layouts and interactions." },
  products_demo_b3: { de: "Ideal für Evaluierung und Präsentation.", en: "Ideal for evaluation and presentation." },
  products_demo_cta: { de: "Demo JAR", en: "Demo JAR" },
  products_kicker: { de: "Produkte", en: "Products" },
  product_kicker: { de: "Produkt", en: "Product" },
  product_title: { de: "ArberCharts Core Framework", en: "ArberCharts Core Framework" },
  product_subtitle: {
    de: "Eine High-Performance, präsentationstaugliche Diagramm-Engine für moderne Java-Anwendungen.",
    en: "A high-performance, presentation-grade charting engine for modern Java applications."
  },
  product_features: { de: "Core-Faehigkeiten", en: "Core Capabilities" },
  product_features_title: { de: "Gebaut fuer Geschwindigkeit, Praezision und Klarheit.", en: "Built for speed, precision, and clarity." },
  product_features_note: { de: "Fokus auf vorhersagbare Ausgabe und stabiles Rendering.", en: "Focused on predictable output and stable rendering." },
  product_perf_title: { de: "Performance im Massstab", en: "Performance at scale" },
  product_perf_body: { de: "Zero-Allocation Rendering, deterministische Grids, optimierte Paint-Pipeline.", en: "Zero-allocation rendering, deterministic grids, optimized paint pipeline." },
  product_fluent_title: { de: "Fluent API", en: "Fluent API" },
  product_fluent_body: { de: "Builder-API mit klaren Defaults.", en: "Builder-style API with clear defaults." },
  product_legend_title: { de: "Legenden & Crosshair", en: "Legends & crosshair" },
  product_legend_body: { de: "Legenden, Crosshair-Overlays, praezise Tooltips.", en: "Legends, crosshair overlays, precise tooltips." },
  product_renderers: { de: "Renderer-Bibliothek", en: "Renderer Library" },
  product_renderers_title: { de: "Ueber 140 Renderer-Familien.", en: "Over 140 renderer families." },
  product_renderers_note: { de: "Jeder Renderer liefert Default-Themes und Demo-Presets.", en: "Each renderer ships with default themes and demo presets." },
  product_domains_title: { de: "Domain-Abdeckung", en: "Domain coverage" },
  product_domains_body: {
    de: "Financial-, Statistik-, Medical-, Analyse-, Spezial- und Circular-Renderer.",
    en: "Financial, statistical, medical, analysis, specialized, and circular renderers."
  },
  product_financial: { de: "Financial", en: "Financial" },
  product_financial_body: { de: "Candlestick, Renko, MACD, Ichimoku und fortgeschrittene Indikatoren.", en: "Candlestick, Renko, MACD, Ichimoku, and advanced indicators." },
  product_medical: { de: "Medical", en: "Medical" },
  product_medical_body: { de: "ECG, NIRS, Ventilator-Wellenformen und praezise klinische Grids.", en: "ECG, NIRS, ventilator waveforms, and precise clinical grids." },
  product_specialized: { de: "Spezialisiert", en: "Specialized" },
  product_specialized_body: {
    de: "Sunburst, Sankey, Parallel Coordinates und multi-dimensionale Visuals.",
    en: "Sunburst, Sankey, Parallel Coordinates, and multi-dimensional visuals."
  },
  product_downloads_title: { de: "Developer-Downloads", en: "Developer Downloads" },
  product_downloads_body: {
    de: "Geben Sie die Core-Bibliothek an Entwickler und die Demo an Stakeholder weiter.",
    en: "Provide the core library to developers and the demo to stakeholders."
  },
  product_callout_title: { de: "Brauchen Sie einen massgeschneiderten Rollout?", en: "Need a tailored rollout?" },
  product_callout_body: { de: "Wir bieten Onboarding, Integrationen und Enterprise-Supportpakete.", en: "We offer onboarding, integrations, and enterprise support packages." },
  portfolio_view: { de: "Produkt ansehen", en: "View product" },
  portfolio_core: { de: "Das Core-Charting-Framework fuer Java-Entwickler.", en: "The core charting framework for Java developers." },
  portfolio_demo_title: { de: "Demo-App", en: "Demo App" },
  portfolio_demo_body: { de: "Referenz-Panels fuer Evaluation, QA und Stakeholder-Review.", en: "Reference panels for evaluation, QA, and stakeholder review." },
  portfolio_demo_link: { de: "Demo ansehen", en: "View demo" },
  demo_kicker: { de: "Demo App", en: "Demo app" },
  demo_title: { de: "Ein Premium-Showcase für Entscheider.", en: "A premium showcase for decision makers." },
  demo_subtitle: {
    de: "Erleben Sie die v1.7.0 LTS \"Surgeon\" in Aktion. 158 Renderer, flüssiges 60 FPS Rendering und industrielle Praezision auf dem Desktop.",
    en: "Experience v1.7.0 LTS \"Surgeon\" live. 158 renderers, smooth 60 FPS rendering, and industrial precision on the desktop."
  },
  demo_download: { de: "Demo JAR herunterladen", en: "Download demo JAR" },
  demo_downloads_title: { de: "Downloads", en: "Downloads" },
  demo_downloads_body: { de: "Laden Sie das Demo JAR fuer alle Betriebssysteme mit kompatibler JRE herunter.", en: "Grab the demo JAR for any OS with a compatible JRE." },
  demo_downloads_jar_body: { de: "Fuer Windows, Linux und macOS mit kompatibler JRE.", en: "For Windows, Linux, and macOS with a compatible JRE." },
  demo_sections: { de: "Showcase", en: "Showcase" },
  demo_sections_title: { de: "Jede Domaene ist vertreten.", en: "Every domain is represented." },
  demo_financial_title: { de: "Financial", en: "Financial" },
  demo_financial_body: { de: "Candlestick, Renko, MACD, Ichimoku und Indikatoren.", en: "Candlestick, Renko, MACD, Ichimoku, and indicators." },
  demo_medical_title: { de: "Medical", en: "Medical" },
  demo_medical_body: {
    de: "Echtzeit-EKG mit dem neuen Lock-free Circular Model und sub-pixelgenauen Grids.",
    en: "Real-time ECG with the new lock-free circular model and sub-pixel grids."
  },
  demo_specialized_title: { de: "Specialized", en: "Specialized" },
  demo_specialized_body: {
    de: "Sankey Pro, Parallel Coordinates und komplexe Heatmaps ohne GC-Lags.",
    en: "Sankey Pro, parallel coordinates, and complex heatmaps without GC lag."
  },
  demo_gallery_kicker: { de: "Galerie", en: "Gallery" },
  demo_gallery_title: { de: "Screens aus der Demo App.", en: "Screens from the demo app." },
  demo_gallery_body: { de: "Ersetzen Sie diese mit echten Screenshots aus dem aktuellen Build.", en: "Replace these with real captures from your current demo build." },
  demo_gallery_financial: { de: "Financial Indikatoren und Overlays.", en: "Financial indicators and overlays." },
  demo_gallery_medical: { de: "Medizinische Wellenformen mit praezisen Grids.", en: "Medical waveforms with precise grids." },
  demo_gallery_specialized: { de: "Spezialisierte Renderer fuer Advanced Analytics.", en: "Specialized renderers for advanced analytics." },
  demo_run: { de: "Start", en: "Run" },
  demo_run_title: { de: "In Sekunden startklar.", en: "Launch in seconds." },
  demo_run_note: {
    de: "Optimiert fuer JRE 21 bis JRE 25. Keine Installation notwendig.",
    en: "Optimized for JRE 21 through JRE 25. No installation required."
  },
  products_highlight_kicker: { de: "Highlights", en: "Highlights" },
  products_highlight_title: { de: "Was ArberCharts liefert", en: "What ArberCharts delivers" },
  products_highlight_perf_title: { de: "Zero-GC Rendering", en: "Zero-GC rendering" },
  products_highlight_perf_body: { de: "Optimierte Memory-Barriers garantieren flüssige 60 FPS ohne GC-Interruption.", en: "Optimized memory barriers deliver smooth 60 FPS without GC interruptions." },
  products_highlight_count_title: { de: "158 Renderer", en: "158 renderers" },
  products_highlight_count_body: { de: "Von Medical-Sweeps bis Financial Overlays und Spezial-Analysen.", en: "From medical sweeps to financial overlays and specialized analysis." },
  products_highlight_api_title: { de: "Fluent API", en: "Fluent API" },
  products_highlight_api_body: { de: "ArberChartBuilder verbindet Models, Renderer und Themes in wenigen Zeilen.", en: "ArberChartBuilder connects models, renderers, and themes in a few lines." },
  products_highlight_export_title: { de: "Export Pipeline", en: "Export pipeline" },
  products_highlight_export_body: { de: "PNG, SVG und PDF Export für Reports und Distribution.", en: "PNG, SVG, and PDF export for reports and distribution." },
  products_highlight_models_title: { de: "Model Builder", en: "Model builder" },
  products_highlight_models_body: { de: "Spezialisierte Modelle für Financial, Medical und Statistical Daten.", en: "Specialized models for financial, medical, and statistical data." },
  products_highlight_ui_title: { de: "UI & Interaktion", en: "UI and interaction" },
  products_highlight_ui_body: { de: "Neu in v1.7.0 LTS: Legend Solo-Mode mit intelligentem Layer-Dimming.", en: "New in v1.7.0 LTS: legend solo mode with intelligent layer dimming." },
  products_highlight_domains_title: { de: "Domain Suites", en: "Domain suites" },
  products_highlight_domains_body: { de: "Medical und Financial Renderer mit Industry-Grade Features.", en: "Medical and financial renderers with industry-grade features." },
  products_highlight_special_title: { de: "Specialized Visuals", en: "Specialized visuals" },
  products_highlight_special_body: { de: "Sankey, Sunburst, Delaunay, Voronoi und mehr.", en: "Sankey, sunburst, Delaunay, Voronoi, and more." },
  products_examples_kicker: { de: "Beispiele", en: "Examples" },
  products_examples_title: { de: "Produkt-Integration in der Praxis", en: "Product integration in practice" },
  products_example_basic_title: { de: "Standard Chart", en: "Standard chart" },
  products_example_fin_title: { de: "Financial Overlay", en: "Financial overlay" },
  products_example_med_title: { de: "Medical Sweep", en: "Medical sweep" },
  products_stack_kicker: { de: "Integration", en: "Integration" },
  products_stack_title: { de: "i18n, Themes, Swing und Spring Boot", en: "i18n, themes, Swing, and Spring Boot" },
  products_stack_steps_kicker: { de: "Schritte", en: "Steps" },
  products_stack_steps_title: { de: "In 3 Schritten produktiv", en: "Productive in 3 steps" },
  products_stack_step1_title: { de: "Dependency hinzufügen", en: "Add the dependency" },
  products_stack_step1_body: { de: "Core via Maven/Gradle einbinden und Version pinnen.", en: "Add core via Maven/Gradle and pin the version." },
  products_stack_step2_title: { de: "Renderer konfigurieren", en: "Configure renderers" },
  products_stack_step2_body: { de: "ModelBuilder + ArberChartBuilder für Layer, Themes und Layout.", en: "Use ModelBuilder + ArberChartBuilder for layers, themes, and layout." },
  products_stack_step3_title: { de: "UI integrieren", en: "Integrate the UI" },
  products_stack_step3_body: { de: "Diagramme in Swing-Views, Docking-Layouts oder modulare Apps einbinden.", en: "Embed charts in Swing views, docking layouts, or modular apps." },
  products_stack_i18n_title: { de: "i18n", en: "i18n" },
  products_stack_i18n_body: {
    de: "Ressourcenbasierte Lokalisierung für UI und Renderer-Labels.",
    en: "Resource-based localization for UI and renderer labels."
  },
  products_stack_theme_title: { de: "Themes", en: "Themes" },
  products_stack_theme_body: {
    de: "Standardisierte Theme-API für Dark/Light und CI-konforme Farben.",
    en: "Standardized theme API for dark/light and CI-compliant colors."
  },
  products_stack_platform_title: { de: "Swing & Spring Boot", en: "Swing & Spring Boot" },
  products_stack_platform_body: {
    de: "Bewährt in Swing-Frontends und Spring Boot Anwendungen.",
    en: "Proven in Swing front-ends and Spring Boot applications."
  },
  products_docs_title: { de: "JavaDocs (v1.7.0 LTS)", en: "JavaDocs (v1.7.0 LTS)" },
  products_docs_body: { de: "Vollständige API-Referenz inklusive technischer Notes zu Concurrency und Rendering.", en: "Full API reference including technical notes on concurrency and rendering." },
  products_docs_cta: { de: "Dokumentation durchsuchen", en: "Browse documentation" },
  products_docs_swing_cta: { de: "Swing Bridge JavaDocs", en: "Swing bridge JavaDocs" },
  products_docs_server_cta: { de: "Server Bridge JavaDocs", en: "Server bridge JavaDocs" },
  products_docs_compose_cta: { de: "Compose Bridge Docs", en: "Compose bridge docs" },
  products_docs_swift_cta: { de: "Swift Bridge Docs", en: "Swift bridge docs" },
  products_docs_qt_cta: { de: "Qt Bridge Docs", en: "Qt bridge docs" },
  products_support_policy_title: { de: "Support-Policy v1.7.0 LTS", en: "v1.7.0 LTS support policy" },
  products_support_policy_body: {
    de: "Aeltere Versionen werden nicht mehr unterstuetzt. Kunden liefern ihre Anwendungen ausschliesslich mit ArberCharts v1.7.0 LTS aus.",
    en: "Older versions are no longer supported. Customers must ship their applications exclusively with ArberCharts v1.7.0 LTS."
  },
  solutions_title: { de: "Solutions & Integration", en: "Solutions & integration" },
  solutions_subtitle: { de: "v1.7.0 LTS \"Surgeon\" — Zertifizierte Use-cases und industrielle Integration fur geschaftskritische Systeme.", en: "v1.7.0 LTS \"Surgeon\" — Certified use cases and industrial integration for mission-critical systems." },
  solutions_kicker: { de: "Use Cases", en: "Use cases" },
  solutions_cases_title: { de: "Branchen-Lösungen", en: "Industry solutions" },
  solutions_medical_title: { de: "Medical & Life Sciences", en: "Medical & life sciences" },
  solutions_medical_body: { de: "Sweep-Erase für EKG/EEG, stabile Vitaldaten-Visualisierung ohne Systemlast.", en: "Sweep-erase for ECG/EEG, stable vital data visualization without system load." },
  solutions_medical_b1: { de: "Sweep-Erase EKG/EEG mit Lock-free Data-Safety (v1.7.0 LTS).", en: "Sweep-erase ECG/EEG with lock-free data safety (v1.7.0 LTS)." },
  solutions_medical_b2: { de: "Ventilator, Ultrasound M-Mode, NIRS — Zero-GC Pipeline.", en: "Ventilator, ultrasound M-mode, NIRS — Zero-GC pipeline." },
  solutions_medical_b3: { de: "HighPrecisionCrosshair fur sub-pixelgenaue Messungen.", en: "HighPrecisionCrosshair for sub-pixel measurements." },
  solutions_financial_title: { de: "Financial Engineering", en: "Financial engineering" },
  solutions_financial_body: { de: "Kagi, Renko, Ichimoku Cloud, Volume Profile und präzise Overlays.", en: "Kagi, Renko, Ichimoku Cloud, Volume Profile, and precise overlays." },
  solutions_financial_b1: { de: "Candlestick, Heikin-Ashi, Kagi, Renko.", en: "Candlestick, Heikin-Ashi, Kagi, Renko." },
  solutions_financial_b2: { de: "Ichimoku Cloud, Volume Profile, Pivot Points.", en: "Ichimoku Cloud, Volume Profile, Pivot Points." },
  solutions_financial_b3: { de: "MACD, RSI, Stochastic, ATR.", en: "MACD, RSI, Stochastic, ATR." },
  solutions_analysis_title: { de: "Advanced Analysis & Statistics", en: "Advanced analysis & statistics" },
  solutions_analysis_body: { de: "Violin Plots, FFT, Hexbin, Sankey Pro für Datenanalyse auf Enterprise-Level.", en: "Violin plots, FFT, Hexbin, Sankey Pro for enterprise-grade analytics." },
  solutions_analysis_b1: { de: "Sankey/Alluvial, Chord, Network.", en: "Sankey/Alluvial, Chord, Network." },
  solutions_analysis_b2: { de: "FFT, Regression, Anomaly Detection.", en: "FFT, regression, anomaly detection." },
  solutions_analysis_b3: { de: "Violin, RidgeLine, Hexbin.", en: "Violin, RidgeLine, Hexbin." },
  solutions_industry_title: { de: "Industrial Analytics", en: "Industrial analytics" },
  solutions_industry_body: { de: "Operational Analytics für Produktion, Energie und Qualität.", en: "Operational analytics for production, energy, and quality." },
  solutions_industry_b1: { de: "Heatmaps, Regelkarten, Pareto.", en: "Heatmaps, control charts, Pareto." },
  solutions_industry_b2: { de: "Delaunay/Voronoi für räumliche Daten.", en: "Delaunay/Voronoi for spatial data." },
  solutions_industry_b3: { de: "Dashboards für Prozessfluss und KPIs.", en: "Dashboards for process flow and KPIs." },
  solutions_engineering_title: { de: "Engineering Systems", en: "Engineering systems" },
  solutions_engineering_body: { de: "Systemdiagnostik, Simulationen und multivariate Analysen.", en: "System diagnostics, simulations, and multivariate analysis." },
  solutions_engineering_b1: { de: "Parallel Coordinates, Radar, Scatter.", en: "Parallel coordinates, radar, scatter." },
  solutions_engineering_b2: { de: "Vectorfield, Delaunay, Voronoi.", en: "Vectorfield, Delaunay, Voronoi." },
  solutions_engineering_b3: { de: "Automotive, Robotics, QA.", en: "Automotive, robotics, QA." },
  solutions_enterprise_title: { de: "Enterprise Operations", en: "Enterprise operations" },
  solutions_enterprise_body: { de: "Überwachung komplexer Prozesse, Abhängigkeiten und Flüsse.", en: "Monitoring complex processes, dependencies, and flows." },
  solutions_enterprise_b1: { de: "Sankey, Network, Dependency Wheel.", en: "Sankey, network, dependency wheel." },
  solutions_enterprise_b2: { de: "Pareto, Regelkarten, KPI-Layer.", en: "Pareto, control charts, KPI layers." },
  solutions_enterprise_b3: { de: "Reporting und Export für Stakeholder.", en: "Reporting and export for stakeholders." },
  solutions_examples_kicker: { de: "Beispiele", en: "Examples" },
  solutions_examples_title: { de: "Konkrete Anwendungsszenarien", en: "Concrete application scenarios" },
  solutions_example1_title: { de: "Intensivstation Monitoring", en: "ICU monitoring" },
  solutions_example1_body: { de: "Mehrkanal-EKG mit Sweep-Erase und schnellen Zooms.", en: "Multi-channel ECG with sweep-erase and fast zooms." },
  solutions_example1_b1: { de: "ECG/EEG + Ventilator in einer Ansicht.", en: "ECG/EEG plus ventilator in a single view." },
  solutions_example1_b2: { de: "Crosshair & Tooltip für präzise Messung.", en: "Crosshair and tooltip for precise measurements." },
  solutions_example1_b3: { de: "Export als PNG/PDF für Berichte.", en: "Export as PNG/PDF for reports." },
  solutions_example2_title: { de: "Trading Terminal", en: "Trading terminal" },
  solutions_example2_body: { de: "Multi-Layer Marktansicht mit professionellen Overlays.", en: "Multi-layer market view with professional overlays." },
  solutions_example2_b1: { de: "Candlestick + Ichimoku + Volume.", en: "Candlestick + Ichimoku + volume." },
  solutions_example2_b2: { de: "Indikatoren: MACD, RSI, ATR.", en: "Indicators: MACD, RSI, ATR." },
  solutions_example2_b3: { de: "Theme-Switch für Day/Night.", en: "Theme switch for day/night." },
  solutions_example3_title: { de: "Engineering Analytics", en: "Engineering analytics" },
  solutions_example3_body: { de: "Forschung und Produktion in einer Oberfläche.", en: "Research and production in one interface." },
  solutions_example3_b1: { de: "FFT und Signalverlauf nebeneinander.", en: "FFT and signal trace side by side." },
  solutions_example3_b2: { de: "Heatmap + Control Chart für Abweichungen.", en: "Heatmap + control chart for deviations." },
  solutions_example3_b3: { de: "Daten in Echtzeit nachgeführt.", en: "Data streamed in real time." },
  solutions_integrate_kicker: { de: "Integration", en: "Integration" },
  solutions_integrate_title: { de: "Einbindung in eigene Software", en: "Embed into your software" },
  solutions_integrate_intro: { de: "Kurze, robuste Integration von Desktop-Swing bis Spring Boot.", en: "Fast, robust integration from Swing desktop to Spring Boot." },
  solutions_swing_title: { de: "Swing Desktop Integration", en: "Swing desktop integration" },
  solutions_swing_body: { de: "ArberCharts fügt sich in jede Swing-Oberfläche ein.", en: "ArberCharts fits into any Swing UI." },
  solutions_swing_b1: { de: "Panel in JFrame, Tabs oder Docking-Layouts.", en: "Panel in JFrame, tabs, or docking layouts." },
  solutions_swing_b2: { de: "Themes und Grid-Layer frei kombinierbar.", en: "Themes and grid layers are freely combinable." },
  solutions_stream_title: { de: "Realtime Streaming (Zero-GC)", en: "Realtime streaming (Zero-GC)" },
  solutions_stream_body: { de: "Dank der v1.7.0 LTS Memory-Barriers bleibt die UI auch bei 1000 Hz Datenrate stabil.", en: "Thanks to v1.7.0 LTS memory barriers, the UI stays stable even at 1000 Hz data rates." },
  solutions_stream_b1: { de: "Modelle aktualisieren statt neu instanziieren.", en: "Update models instead of re-instantiating." },
  solutions_stream_b2: { de: "Repaints gezielt steuern.", en: "Control repaints deliberately." },
  solutions_export_title: { de: "Export Pipeline", en: "Export pipeline" },
  solutions_export_body: { de: "PNG, SVG oder PDF für Reports und Kunden.", en: "PNG, SVG, or PDF for reports and customers." },
  solutions_spring_title: { de: "Spring Boot", en: "Spring Boot" },
  solutions_spring_body: { de: "ArberCharts lässt sich problemlos in Spring Boot Services integrieren und als UI-Modul ausliefern.", en: "ArberCharts integrates cleanly into Spring Boot services and can be delivered as a UI module." },
  solutions_spring_b1: { de: "Services liefern Daten, UI rendert live.", en: "Services provide data, UI renders live." },
  solutions_spring_b2: { de: "Ideal für modulare Desktop-Stacks.", en: "Ideal for modular desktop stacks." },
  solutions_theme_title: { de: "Themes & i18n", en: "Themes & i18n" },
  solutions_theme_body: { de: "Light/Dark, eigene Paletten, Lokalisierung.", en: "Light/dark, custom palettes, localization." },
  solutions_download_title: { de: "Download Core", en: "Download core" },
  solutions_download_body: { de: "Core Framework für Entwickler und Integrationsteams.", en: "Core framework for developers and integration teams." },
  solutions_download_cta: { de: "ArberCharts Core v1.7.0 LTS herunterladen", en: "Download ArberCharts Core v1.7.0 LTS" },
  solutions_gallery_kicker: { de: "Galerie", en: "Gallery" },
  solutions_gallery_title: { de: "Loesungen in der Praxis", en: "Solutions in practice" },
  solutions_gallery_body: { de: "Visuelle Beispiele fuer spezialisierte Integrationen.", en: "Visual examples of specialized integrations." },
  solutions_gallery_medical: { de: "Patienten-Monitoring (ICU)", en: "Patient monitoring (ICU)" },
  solutions_gallery_financial: { de: "Marktanalyse & Indikatoren", en: "Market analysis & indicators" },
  solutions_gallery_analysis: { de: "Statistische Dichteverteilung", en: "Statistical density distribution" },
  solutions_gallery_industrial: { de: "Industrielle Prozessueberwachung", en: "Industrial process monitoring" },
  solutions_gallery_sankey: { de: "Sankey Pro", en: "Sankey Pro" },
  solutions_gallery_radar: { de: "Radar", en: "Radar" },
  solutions_gallery_ichimoku: { de: "Ichimoku", en: "Ichimoku" },
  solutions_gallery_voronoi: { de: "Voronoi", en: "Voronoi" },
  solutions_gallery_hexbin: { de: "Hexbin", en: "Hexbin" },
  solutions_gallery_macd: { de: "MACD", en: "MACD" },
  solutions_gallery_boxplot: { de: "Boxplot", en: "Boxplot" },
  technology_title: { de: "Technologie", en: "Technology" },
  technology_subtitle: {
    de: "Die Architektur hinter 158 Renderern: Java 25 Native Performance, Zero-GC und Lock-free Concurrency.",
    en: "The architecture behind 158 renderers: Java 25 native performance, zero-GC, and lock-free concurrency."
  },
  tech_stack_kicker: { de: "Der Stack", en: "The stack" },
  tech_stack_title: { de: "Fuer die Ewigkeit gebaut", en: "Built for eternity" },
  tech_stack_java_title: { de: "Java 25 Ready", en: "Java 25 ready" },
  tech_stack_java_body: { de: "Nutzt moderne JVM-Features fuer maximale Ausfuehrungsgeschwindigkeit.", en: "Uses modern JVM features for maximum execution speed." },
  tech_stack_zerogc_title: { de: "Zero-GC Pipeline", en: "Zero-GC pipeline" },
  tech_stack_zerogc_body: { de: "Vermeidung von Objekterzeugung im Render-Loop verhindert GC-Pausen.", en: "Avoiding object creation in the render loop prevents GC pauses." },
  tech_stack_lockfree_title: { de: "Lock-free Modelle", en: "Lock-free models" },
  tech_stack_lockfree_body: { de: "Thread-safe Datenverarbeitung ohne Performance-Einbussen durch Mutexe.", en: "Thread-safe data processing without performance hits from mutexes." },
  tech_zero_title: { de: "Zero-GC Rendering Pipeline", en: "Zero-GC rendering pipeline" },
  tech_zero_body: { de: "Flyweight-Pattern und primitive Caches eliminieren Heap-Allokationen im Render-Loop. JMH-validiert unter 1e-5 B/op.", en: "Flyweight patterns and primitive caches remove heap allocations in the render loop. JMH-validated below 1e-5 B/op." },
  tech_model_title: { de: "Optimistic Concurrency", en: "Optimistic concurrency" },
  tech_model_body: { de: "Sequence-Guards für das Circular-Modell sichern konsistente Reads, während Writer mit 1000Hz+ streamen.", en: "Sequence guards for the circular model keep reads consistent while writers stream at 1000Hz+." },
  tech_jdk_title: { de: "JDK 25 & Concurrency", en: "JDK 25 & concurrency" },
  tech_jdk_body: { de: "VarHandles und Memory-Barriers für lock-free Datenmodelle. Zertifiziert durch 100s-Stress-Tests ohne Datenkorruption.", en: "VarHandles and memory barriers power lock-free data models. Certified by 100s stress tests without corruption." },
  tech_docs_kicker: { de: "JavaDocs", en: "JavaDocs" },
  tech_docs_title: { de: "Core API Documentation", en: "Core API documentation" },
  tech_docs_body: { de: "Die JavaDocs für das Core-Modul sind online verfügbar.", en: "Core JavaDocs are available online." },
  tech_docs_cta: { de: "Dokumentation öffnen", en: "Open documentation" },
  tech_docs_swing_cta: { de: "Swing Bridge JavaDocs", en: "Swing bridge JavaDocs" },
  tech_docs_server_cta: { de: "Server Bridge JavaDocs", en: "Server bridge JavaDocs" },
  tech_docs_compose_cta: { de: "Compose Bridge Docs", en: "Compose bridge docs" },
  tech_docs_swift_cta: { de: "Swift Bridge Docs", en: "Swift bridge docs" },
  tech_docs_qt_cta: { de: "Qt Bridge Docs", en: "Qt bridge docs" },
  bridge_compose_title: { de: "Compose Bridge", en: "Compose bridge" },
  bridge_compose_body: { de: "Integration von ArberCharts in JetBrains Compose Desktop.", en: "Integration of ArberCharts into JetBrains Compose Desktop." },
  bridge_swift_title: { de: "Swift Bridge", en: "Swift bridge" },
  bridge_swift_body: { de: "Native Integration fuer Swift-basierte Frontends ueber das C-Bridge-Modul.", en: "Native integration for Swift-based frontends via the C bridge module." },
  bridge_qt_title: { de: "Qt Bridge", en: "Qt bridge" },
  bridge_qt_body: { de: "Qt-Integration fuer C++/QML-Frontends mit dem headless Core.", en: "Qt integration for C++/QML frontends using the headless core." },
  bridge_docs_body: { de: "Siehe User Guide und Migration Guide fuer Nutzungsmuster und API-Vertraege.", en: "See the User Guide and Migration Guide for usage patterns and API contracts." },
  bridge_core_javadoc_cta: { de: "Core JavaDocs", en: "Core JavaDocs" },
  bridge_back_cta: { de: "Zurueck zu Produkten", en: "Back to products" },
  pricing_title: { de: "Faire Preise für High-End Performance.", en: "Fair pricing for high-end performance." },
  pricing_subtitle: {
    de: "v1.7.0 LTS \"Surgeon\" — Industrielle Praezision fuer jedes Team. Kostenlos fuer Einzelentwickler & Forschung.",
    en: "v1.7.0 LTS \"Surgeon\" — industrial precision for every team. Free for individual developers and research."
  },
  pricing_th_feature: { de: "Merkmal", en: "Feature" },
  pricing_th_dev: { de: "Einzelentwickler", en: "Individual" },
  pricing_th_startup: { de: "Startup", en: "Startup" },
  pricing_th_pro: { de: "Professional", en: "Professional" },
  pricing_th_enterprise: { de: "Enterprise", en: "Enterprise" },
  pricing_row_price: { de: "Preis", en: "Price" },
  pricing_row_price_dev: { de: "CHF 0", en: "CHF 0" },
  pricing_row_price_startup: { de: "CHF 499", en: "CHF 499" },
  pricing_row_price_pro: { de: "CHF 1'299", en: "CHF 1'299" },
  pricing_row_price_enterprise: { de: "CHF 3'999", en: "CHF 3'999" },
  pricing_row_unit: { de: "Einheit", en: "Unit" },
  pricing_row_unit_dev: { de: "Einzelentwickler", en: "Individual developer" },
  pricing_row_unit_startup: { de: "Pro Produkt / Jahr", en: "Per product / year" },
  pricing_row_unit_pro: { de: "Unlimitierte Entwickler", en: "Unlimited developers" },
  pricing_row_unit_enterprise: { de: "Unlimitierte Produkte", en: "Unlimited products" },
  pricing_row_target: { de: "Zielgruppe", en: "Target" },
  pricing_row_target_dev: { de: "Einzelentwickler", en: "Individual developers" },
  pricing_row_target_startup: { de: "Kleine Unternehmen", en: "Small companies" },
  pricing_row_target_pro: { de: "Mittelstand / Teams", en: "Mid-market / teams" },
  pricing_row_target_enterprise: { de: "Konzern / OEM", en: "Enterprise / OEM" },
  pricing_row_support: { de: "Support", en: "Support" },
  pricing_row_support_dev: { de: "Community", en: "Community" },
  pricing_row_support_startup: { de: "E-Mail", en: "Email" },
  pricing_row_support_pro: { de: "Priority (48h)", en: "Priority (48h)" },
  pricing_row_support_enterprise: { de: "VIP (24h) + SLA", en: "VIP (24h) + SLA" },
  pricing_row_updates: { de: "Updates", en: "Updates" },
  pricing_row_updates_dev: { de: "Inklusive (inkl. v1.7.0 LTS Surgeon & kuenftige Major Releases)", en: "Included (v1.7.0 LTS Surgeon and future major releases)" },
  pricing_row_updates_startup: { de: "Inklusive (inkl. v1.7.0 LTS Surgeon & kuenftige Major Releases)", en: "Included (v1.7.0 LTS Surgeon and future major releases)" },
  pricing_row_updates_pro: { de: "Inklusive (inkl. v1.7.0 LTS Surgeon & kuenftige Major Releases)", en: "Included (v1.7.0 LTS Surgeon and future major releases)" },
  pricing_row_updates_enterprise: { de: "Inklusive (inkl. v1.7.0 LTS Surgeon & kuenftige Major Releases)", en: "Included (v1.7.0 LTS Surgeon and future major releases)" },
  pricing_notice_title: { de: "Lizenz-Überblick", en: "License overview" },
  pricing_notice_body: {
    de: "Einzelentwickler entwickeln kostenlos. Für Teams und kommerzielle Distribution gelten Lizenzpakete.",
    en: "Individual developers build for free. Team and distribution licensing applies for commercial use."
  },
  pricing_notice_b1: { de: "Einzelentwickler: freie Nutzung der Core-v1.7.0 LTS fuer Evaluation und private Projekte.", en: "Individuals: free use of core v1.7.0 LTS for evaluation and private projects." },
  pricing_notice_b4: { de: "Studierende, Universitäten und Hochschulen: kostenfreie Nutzung für Lehre und Forschung.", en: "Students, universities, and higher education institutions: free use for teaching and research." },
  pricing_notice_b2: { de: "Kommerzielle Sicherheit: Alle v1.7.0 LTS Concurrency-Fixes sind in Team-Lizenzen enthalten.", en: "Commercial safety: all v1.7.0 LTS concurrency fixes are included in team licenses." },
  pricing_notice_b3: { de: "Wartung: Aktive Lizenznehmer erhalten Zugriff auf kuenftige v1.4.x Optimierungen.", en: "Maintenance: active licensees get access to future v1.4.x optimizations." },
  pricing_note_title: { de: "Transparente Jahreslizenzen", en: "Transparent annual licenses" },
  pricing_note_body: {
    de: "Alle Preise verstehen sich als jährliche Gebühr. Darin enthalten sind sämtliche Updates auf neue Versionen (inkl. Major-Releases) sowie technischer Support.",
    en: "All prices are annual fees. They include all updates to new versions (including major releases) and technical support."
  },
  pricing_note_lockin_title: { de: "Kein Vendor Lock-in:", en: "No vendor lock-in:" },
  pricing_note_lockin_body: {
    de: "Sollten Sie Ihre Lizenz nicht erneuern, bleibt die zum Zeitpunkt des Ablaufens aktuelle Version von ArberCharts in Ihren bereits ausgelieferten Produkten voll funktionsfähig. Sie zahlen lediglich für den Zugang zu neuen Funktionen und Support.",
    en: "If you do not renew, the version current at expiry remains fully functional in your shipped products. You only pay for access to new features and support."
  },
  contact_title: { de: "Kontakt", en: "Contact" },
  contact_subtitle: { de: "Fragen zur v1.7.0 LTS, individuellen Integrationen oder Lizenz-Modellen.", en: "Questions about v1.7.0 LTS, custom integrations, or licensing models." },
  contact_direct_title: { de: "Direktkontakt", en: "Direct contact" },
  contact_direct_body: { de: "Arber Gashi · Chief Software Architect", en: "Arber Gashi · Chief Software Architect" },
  contact_location: { de: "Schweiz", en: "Switzerland" },
  contact_request_title: { de: "Anfrage", en: "Request" },
  contact_request_body: {
    de: "Schicken Sie uns eine kurze Beschreibung Ihres Use-Cases fuer eine massgeschneiderte Beratung.",
    en: "Send us a short description of your use case for tailored advice."
  },
  contact_email_cta: { de: "E-Mail senden", en: "Send email" }
  ,
  imprint_title: { de: "Impressum", en: "Imprint" },
  imprint_subtitle: { de: "Angaben gemäss schweizer Recht.", en: "Information according to Swiss law." },
  imprint_owner_title: { de: "Inhaber", en: "Owner" },
  imprint_owner_body: { de: "Arber Gashi · Chief Software Architect & Inhaber", en: "Arber Gashi · Chief Software Architect & Owner" },
  imprint_country: { de: "Schweiz", en: "Switzerland" },
  imprint_contact_title: { de: "Kontakt", en: "Contact" },
  imprint_web: { de: "www.arbergashi.com", en: "www.arbergashi.com" },
  imprint_legal_title: { de: "Rechtlicher Hinweis", en: "Legal notice" },
  imprint_legal_body: { de: "Verantwortlich fuer die Inhalte dieser Website.", en: "Responsible for the content of this website." },
  imprint_copyright_title: { de: "Urheberrecht", en: "Copyright" },
  imprint_copyright_body: { de: "Alle Inhalte unterliegen dem Urheberrecht.", en: "All content is subject to copyright." },
  privacy_title: { de: "Datenschutz", en: "Privacy" },
  privacy_subtitle: { de: "Informationen gemäss DSGVO und schweizer DSG.", en: "Information according to GDPR and Swiss DPA." },
  privacy_controller_title: { de: "Verantwortlicher", en: "Controller" },
  privacy_controller_body: { de: "Arber Gashi · Schweiz", en: "Arber Gashi · Switzerland" },
  privacy_scope_title: { de: "Datensparsamkeit", en: "Data minimization" },
  privacy_scope_body: {
    de: "Diese Website verarbeitet nur technisch notwendige Daten. Kein Tracking.",
    en: "This website processes only technically necessary data. No tracking."
  },
  privacy_logs_title: { de: "Server-Logs", en: "Server logs" },
  privacy_logs_body: { de: "Technische Logdaten (z.B. IP, Zeit, Browser) werden zur Sicherheit und Fehleranalyse gespeichert.", en: "Technical log data (e.g., IP, time, browser) is stored for security and troubleshooting." },
  privacy_rights_title: { de: "Ihre Rechte", en: "Your rights" },
  privacy_rights_body: { de: "Sie haben das Recht auf Auskunft, Berichtigung und Löschung Ihrer Daten.", en: "You have the right to access, correct, and delete your data." },
  privacy_third_title: { de: "Kein Tracking", en: "No tracking" },
  privacy_third_body: {
    de: "Wir nutzen keine Cookies von Drittanbietern oder Analyse-Tools.",
    en: "We do not use third-party cookies or analytics tools."
  },
  privacy_contact_title: { de: "Kontakt", en: "Contact" },
  privacy_contact_body: { de: "Bei Fragen zum Datenschutz kontaktieren Sie uns per E-Mail.", en: "For privacy questions, contact us by email." }
};

function applyLanguage(lang) {
  document.querySelectorAll('[data-i18n]').forEach((el) => {
    const key = el.dataset.i18n;
    if (dictionary[key]) {
      el.textContent = dictionary[key][lang];
    }
  });
  document.querySelectorAll('.lang-btn').forEach((btn) => {
    btn.classList.toggle('is-active', btn.dataset.lang === lang);
  });
  localStorage.setItem('lang', lang);
  document.documentElement.lang = lang;
}

const storedLang = localStorage.getItem('lang') || 'en';
applyLanguage(storedLang);

document.querySelectorAll('.lang-btn').forEach((btn) => {
  btn.addEventListener('click', () => applyLanguage(btn.dataset.lang));
});

const navToggle = document.querySelector('.nav-toggle');
const nav = document.querySelector('.nav');
const backdrop = document.querySelector('.mobile-backdrop');

if (navToggle && nav) {
  navToggle.addEventListener('click', () => {
    const isOpen = nav.classList.toggle('is-open');
    navToggle.classList.toggle('is-open', isOpen);
    if (backdrop) backdrop.classList.toggle('is-visible', isOpen);
  });
}

if (backdrop && nav && navToggle) {
  backdrop.addEventListener('click', () => {
    nav.classList.remove('is-open');
    navToggle.classList.remove('is-open');
    backdrop.classList.remove('is-visible');
  });
}

document.querySelectorAll('.nav a').forEach((link) => {
  link.addEventListener('click', () => {
    if (!nav || !navToggle || !backdrop) return;
    nav.classList.remove('is-open');
    navToggle.classList.remove('is-open');
    backdrop.classList.remove('is-visible');
  });
});

const filterButtons = document.querySelectorAll('.filter-btn');
const filterCards = document.querySelectorAll('[data-filter]');

if (filterButtons.length && filterCards.length) {
  filterButtons.forEach((button) => {
    button.addEventListener('click', () => {
      const target = button.dataset.filter;
      filterButtons.forEach((btn) => btn.classList.toggle('is-active', btn === button));
      filterCards.forEach((card) => {
        const highlight = target === 'all' || card.dataset.filter === target;
        card.classList.toggle('is-muted', !highlight);
      });
    });
  });
}

const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

function initRevealAnimations() {
  if (prefersReducedMotion) return;

  const hasGsap = typeof window.gsap !== 'undefined';

  if (hasGsap) {
    const hero = document.querySelector('.hero');
    if (hero) {
      const tl = window.gsap.timeline({ defaults: { ease: 'power2.out', duration: 0.7 } });
      tl.from(hero.querySelector('h1'), { y: 16, opacity: 0 })
        .from(hero.querySelector('p'), { y: 12, opacity: 0, duration: 0.55 }, '-=0.4')
        .from(hero.querySelectorAll('.hero-cta .btn'), { y: 10, opacity: 0, stagger: 0.06, duration: 0.5 }, '-=0.3');
    }

    const revealTargets = [];
    document.querySelectorAll('.section-title, .card, .gallery-item, .media-slot, .image-strip, .table').forEach((el) => {
      revealTargets.push(el);
    });

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          window.gsap.fromTo(
            entry.target,
            { y: 14, opacity: 0, scale: entry.target.classList.contains('card') ? 0.99 : 1 },
            { y: 0, opacity: 1, scale: 1, duration: 0.6, ease: 'power2.out' }
          );
          observer.unobserve(entry.target);
        });
      },
      { rootMargin: '0px 0px -12% 0px', threshold: 0.2 }
    );

    revealTargets.forEach((el) => observer.observe(el));
    return;
  }

  const revealElements = new Set();

  const register = (el, { delay = 0, variant = '', duration } = {}) => {
    if (!el) return;
    el.classList.add('reveal');
    if (variant) el.classList.add(variant);
    if (duration) el.style.setProperty('--reveal-duration', `${duration}s`);
    el.style.setProperty('--delay', `${delay}s`);
    revealElements.add(el);
  };

  const hero = document.querySelector('.hero');
  if (hero) {
    register(hero.querySelector('h1'), { delay: 0, variant: 'slow' });
    register(hero.querySelector('p'), { delay: 0.08, variant: 'soft' });
    hero.querySelectorAll('.hero-cta .btn').forEach((btn, idx) => {
      register(btn, { delay: 0.16 + idx * 0.06, variant: 'soft' });
    });
  }

  document.querySelectorAll('.section-title').forEach((el, idx) => {
    register(el, { delay: Math.min(idx * 0.05, 0.4), variant: 'soft' });
  });

  document.querySelectorAll('.grid').forEach((grid) => {
    const children = Array.from(grid.children);
    children.forEach((child, idx) => {
      const isCard = child.classList.contains('card');
      const isGallery = child.classList.contains('gallery-item');
      const variant = isGallery ? 'soft' : 'scale';
      const delay = Math.min(idx * 0.06, 0.45);
      register(child, { delay, variant, duration: isCard ? 0.8 : 0.7 });
    });
  });

  document.querySelectorAll('.media-slot, .image-strip, .table').forEach((el) => {
    register(el, { delay: 0.1, variant: 'soft' });
  });

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        }
      });
    },
    { rootMargin: '0px 0px -12% 0px', threshold: 0.2 }
  );

  revealElements.forEach((el) => observer.observe(el));
}

initRevealAnimations();

function initCardGlow() {
  const cards = document.querySelectorAll('.card');
  if (!cards.length) return;

  cards.forEach((card) => {
    card.addEventListener('pointermove', (event) => {
      const rect = card.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      card.style.setProperty('--mouse-x', `${x}px`);
      card.style.setProperty('--mouse-y', `${y}px`);
    });

    card.addEventListener('pointerleave', () => {
      card.style.setProperty('--mouse-x', '50%');
      card.style.setProperty('--mouse-y', '50%');
    });
  });
}

initCardGlow();
