# Nur diese Artefakte pushen

Diese Schritte committen und pushen ausschließlich das vorbereitete GitHub-Release-Paket.

## 1) Nur Release-Paket stagen

```bash
git add -f dist/github-release/2.0.0
```

## 2) Kontrollieren, dass nur gewünschte Dateien gestaged sind

```bash
git status --short
```

## 3) Commit erstellen

```bash
git commit -m "Prepare GitHub release bundle 2.0.0"
```

## 4) Push

```bash
git push origin HEAD
```

## Optional: Neuer Branch nur für Artefakte

```bash
git switch -c release/github-assets-2.0.0
git push -u origin release/github-assets-2.0.0
```
