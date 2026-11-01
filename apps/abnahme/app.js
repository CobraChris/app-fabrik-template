import { incrementCount, parseCount, MAX_COUNT } from './counter.mjs';

const countOutput = document.querySelector('#count');
const message = document.querySelector('#status');
const storageKey = 'app-fabrik-abnahme-count-v1:' + new URL('./', location.href).pathname;
let stored = true;
let value = 0;

try {
  value = parseCount(localStorage.getItem(storageKey));
} catch {
  stored = false;
}

function render() {
  countOutput.textContent = String(value);
}

function save() {
  try {
    localStorage.setItem(storageKey, String(value));
    stored = true;
    message.textContent = value === MAX_COUNT
      ? 'Maximaler Testwert erreicht.'
      : 'Gespeichert. Lade die Seite neu, um die Speicherung zu prüfen.';
  } catch {
    stored = false;
    message.textContent = 'Speicherzugriff blockiert. Der Wert bleibt nur bis zum Neuladen erhalten.';
  }
}

document.querySelector('#increment').addEventListener('click', () => {
  value = incrementCount(value);
  render();
  save();
});

document.querySelector('#reset').addEventListener('click', () => {
  value = 0;
  render();
  save();
});

render();
if (!stored) message.textContent = 'Gerätespeicher nicht verfügbar. Teste nur die Anzeige.';
else message.textContent = 'Zähler geladen. Die Werte bleiben ausschließlich lokal.';

if ('serviceWorker' in navigator && (location.protocol === 'https:' || location.hostname === 'localhost')) {
  window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').catch(() => {
    message.textContent = 'Offline-Installation wird vom Browser nicht unterstützt; der Zähler funktioniert weiterhin.';
  }));
}
