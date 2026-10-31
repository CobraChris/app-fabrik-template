import { STORE_KEY, addIdea, removeIdea, parseIdeas } from './core.mjs';

const form = document.querySelector('#idea-form');
const input = document.querySelector('#idea-title');
const list = document.querySelector('#idea-list');
const count = document.querySelector('#count');
const empty = document.querySelector('#empty-state');
const feedback = document.querySelector('#form-feedback');
let ideas = [];

try { ideas = parseIdeas(localStorage.getItem(STORE_KEY)); }
catch { feedback.textContent = 'Gerätespeicher nicht verfügbar. Änderungen gelten nur bis zum Neuladen.'; }

function save() {
  try { localStorage.setItem(STORE_KEY, JSON.stringify(ideas)); }
  catch { feedback.textContent = 'Speichern nicht verfügbar. Die Ideen bleiben nur bis zum Neuladen erhalten.'; }
}

function render() {
  list.replaceChildren();
  count.textContent = ideas.length + (ideas.length === 1 ? ' Idee' : ' Ideen');
  empty.hidden = ideas.length > 0;
  for (const title of ideas) {
    const li = document.createElement('li');
    const text = document.createElement('span');
    text.textContent = title;
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = 'Entfernen';
    button.setAttribute('aria-label', 'Idee ' + title + ' entfernen');
    button.addEventListener('click', () => {
      ideas = removeIdea(ideas, title);
      feedback.textContent = '';
      save(); render();
    });
    li.append(text, button);
    list.append(li);
  }
}

form.addEventListener('submit', event => {
  event.preventDefault();
  try {
    ideas = addIdea(ideas, input.value);
    input.value = '';
    feedback.textContent = 'Idee hinzugefügt.';
    save(); render(); input.focus();
  } catch (error) {
    feedback.textContent = error.message;
  }
});
render();

if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
  window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').catch(() => {}));
}
