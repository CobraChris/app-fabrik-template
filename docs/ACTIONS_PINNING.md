# GitHub Actions – feste Commit-Referenzen und Pflege

**Geprüft am 07.11.2026.** Dies ist ein sicherheitskritischer Änderungsvorschlag.
Betroffene Workflows dürfen **erst nach einer gesonderten Sicherheitsfreigabe**
zusammengeführt werden. Kein automatischer Merge auf der Grundlage grüner CI.

## Was geändert wird
Alle externen GitHub Actions in den sieben Workflows unter
`.github/workflows/` werden statt auf bewegliche Versions-Tags auf die
**zum Prüfzeitpunkt aufgelösten vollständigen Git-Commit-SHAs** gebunden.

Im Code bleiben die ursprünglichen Major-Tags als Kommentar `# v4` usw.
lesbar. Keine Workflow-Auslöser, Job-Permissions, Secret-Verweise, Inputs,
Befehle oder Produktiv-Release-Regeln werden geändert.

| GitHub Action | Vorheriges Tag | Aufgelöster Commit-SHA |
| --- | --- | --- |
| `actions/checkout` | `v4` | `11d5960a326750d5838078e36cf38b85af677262` |
| `actions/setup-node` | `v4` | `49933ea5288caeca8642d1e84afbd3f7d6820020` |
| `actions/setup-java` | `v4` | `cf277c60eb25467037889841efdb72551f06f6c3` |
| `actions/configure-pages` | `v5` | `983d7736d9b0ae728b81ab479565c72886d7745b` |
| `actions/upload-pages-artifact` | `v3` | `56afc609e74202658d3ffba0e8f6dda462b719fa` |
| `actions/deploy-pages` | `v4` | `d6db90164ac5ed86f2b6aed7e0febac5b3c0c03e` |
| `actions/upload-artifact` | `v4` | `ea165f8d65b6e75b540449e92b4886f43607fa02` |
| `actions/download-artifact` | `v4` | `d3f86a106a0bac45b974a628896c90dbdf5c8093` |
| `gradle/actions (setup-gradle)` | `v4` | `ed408507eac070d1f99cc633dbcf757c94c7933a` |
| `openai/codex-action` | `v1` | `bdf19a4a223ec2549a3e2274a0cf61556bc07675` |

**Ermittlung:** GitHub REST `GET /repos/OWNER/REPO/git/ref/tags/TAG`,
bei annotierten Tags dereferenziert über
`GET /repos/OWNER/REPO/git/tags/SHA`, bis das Zielobjekt ein Commit
war; anschließend Commit über GitHub REST nachgewiesen.
Für `gradle/actions@v4` führte die Auflösung durch **zwei** annotierte
Git-Tags (einschließlich `v4.4.3`) zum tatsächlichen Commit.
Ein annotiertes Tag-Objekt ist **kein** ausführbarer Commit-Pin.

## Verschärfte CI
`scripts/security-check.mjs` blockiert nach der Änderung jeden neuen
externen `uses:`-Verweis, dessen Version kein vollständiger 40-stelliger
Commit-SHA ist. Lokale `./`-Actions bleiben zulässig.
Der Parser ist ein einfacher Textscanner und **kein** vollständiger
GitHub-Actions-/YAML-Validator. `docker://`-Referenzen werden derzeit
vom GitHub-Action-Pinning ausgenommen; sie müssen separat überprüft werden.

## Grenzen der PR-Tests
- Der normale PR-CI-Test kann den PWA-Build und die neue Policy prüfen.
- Manuelle/privilegierte `workflow_dispatch`-, GitHub-Pages- und
  Android-Signier-/Release-Pfade werden von dieser PR-CI **nicht**
  vollständig durchlaufen.
- Noch kein echter signierter Android-Beta-Release-Nachweis mit gepinnten
  Workflows und kein echter Deployment-Nachweis auf `main` für diese Änderung.
- Nach gezieltem Review zunächst kontrollierte Beta-/Smoke-Tests, dann
  produktive Änderungen. Bei Fehlern revertieren auf die vorherige
  Workflow-Version, **ohne** Secrets zu rotieren.

## Pflege nach der Freigabe
Ein SHA-Pin aktualisiert sich nicht automatisch. Neue Sicherheitsfixes
der Action-Hersteller erfordern einen **neuen geprüften PR**:
Tag/Release prüfen, bis zum Commit dereferenzieren, Änderungen zwischen
alter und neuer Commit-Version prüfen, CI und Anwendungs-Smoke-Tests
ausführen und anschließend einzeln freigeben.

Andere Repositories der App-Fabrik haben **eigene** Workflows und werden
durch diesen PR nicht geändert. Insbesondere der **private**
GitHub-App-Admin-Controller wird separat und nur nach Security-Review
angepasst.
