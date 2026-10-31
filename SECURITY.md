# Sicherheit

- Keine persönlichen oder geschäftlichen Daten in öffentlichen Repositories hinterlegen.
- API-Schlüssel ausschließlich als GitHub Actions Secrets speichern; niemals in Quelldateien.
- Die PWA nutzt für Ideen ausschließlich lokalen Browser-Speicher.
- codex-proposal.yml läuft nur auf manuelle Anforderung. Der Codex-Schritt erhält nur Lesezugriff auf GitHub; der Schreibschritt läuft in einem getrennten Job und öffnet einen Pull Request ohne Auto-Merge.
- KI-generierten Code vor dem Merge prüfen, insbesondere JavaScript, Netzwerkaufrufe und Datenzugriffe.
- Die GitHub-Einstellung „Allow GitHub Actions to create and approve pull requests“ wird für automatische PR-Erstellung benötigt und sollte bewusst freigegeben werden.
- Vor echten Projekten Branch Protection oder Rulesets mit erfolgreichen Checks und bei Bedarf Review-Regeln aktivieren.
- GitHub Pages ist kein vertrauliches Hosting; auch die Website eines privaten Projekts kann öffentlich sein.
- GitHub Actions Workflows bei produktivem Einsatz auf unveränderliche Commit-SHAs pinnen.
