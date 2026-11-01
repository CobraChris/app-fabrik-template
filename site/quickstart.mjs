import { focusActions, normalizeProject, openTasksCount } from './projects.mjs';

/**
 * Ein kleiner rein lokaler Wegweiser, keine Aufgabenverwaltung.
 * Der Stand wird immer aus bereits vorhandenen Ideen/Projekten abgeleitet.
 * Neue Checklisten, zusätzliche Häkchen oder externe API-Abfragen sind unnötig.
 */
export function quickstartState(rawProjects, briefs) {
  if (!Array.isArray(rawProjects) || !Array.isArray(briefs)) {
    throw new Error('Projekte und Ideen müssen Listen sein.');
  }
  const projects = rawProjects.map(normalizeProject);
  const ideaCount = briefs.length;
  const activeCount = projects.filter(project => project.status !== 'Fertig').length;
  const todoCount = projects.reduce((count, project) => count + openTasksCount(project.todos), 0);
  const summary = {
    projects: projects.length,
    ideas: ideaCount,
    activeProjects: activeCount,
    openTodos: todoCount,
  };

  if (projects.length === 0 && ideaCount === 0) {
    return {
      kind: 'empty',
      title: 'Richte deine App-Fabrik ein',
      description: 'Übernimm deine öffentlichen GitHub-Projekte oder beginne unten mit einer neuen App-Idee. Private Repositories kannst du manuell ergänzen.',
      button: 'Repositories übernehmen',
      target: 'repo-import-panel',
      summary,
    };
  }

  if (projects.length === 0) {
    return {
      kind: 'ideas',
      title: 'Aus deiner Idee wird eine App',
      description: 'Du hast bereits App-Ideen gespeichert. Über „Mit GitHub weiterarbeiten“ kannst du daraus ein neues Repository vorbereiten.',
      button: 'App-Ideen ansehen',
      target: 'ideas-title',
      summary,
    };
  }

  const focus = focusActions(projects, 1)[0];
  if (focus) {
    return {
      kind: 'focus',
      title: 'Dein nächster Entwicklungsschritt',
      description: focus.repository + ': ' + focus.title,
      button: 'Zur nächsten Aufgabe',
      target: 'focus-title',
      summary,
    };
  }

  if (activeCount) {
    return {
      kind: 'projects',
      title: 'Lege deinen nächsten Schritt fest',
      description: 'Du hast aktive Projekte, aber aktuell keine offenen Aufgaben. Öffne ein Projekt und ergänze dort einen nächsten Schritt oder eine konkrete Aufgabe.',
      button: 'Projekte ansehen',
      target: 'project-list',
      summary,
    };
  }

  return {
    kind: 'complete',
    title: 'Bereit für die nächste App-Idee',
    description: 'Alle erfassten Projekte sind als fertig markiert. Du kannst eine neue App-Idee anlegen oder bestehende Projekte wieder öffnen.',
    button: 'Neue App-Idee erfassen',
    target: 'idea-title',
    summary,
  };
}
