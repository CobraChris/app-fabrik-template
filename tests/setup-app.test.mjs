import test from 'node:test';
import assert from 'node:assert/strict';
import { configFromInputs } from '../scripts/setup-app.mjs';

const inputs = {
  APP_NAME: 'Mein Fitness-Tracker',
  APP_SHORT_NAME: 'Fitness',
  APP_DESCRIPTION: 'Trainingspläne auf dem Handy',
  APP_THEME_COLOR: '#101622',
  APP_ACCENT_COLOR: '#b1e0ff',
  APP_ICON_LETTER: 'F',
};

test('Handy-Assistent übersetzt Eingaben in die App-Konfiguration', () => {
  assert.deepEqual(configFromInputs(inputs), {
    name: 'Mein Fitness-Tracker',
    shortName: 'Fitness',
    description: 'Trainingspläne auf dem Handy',
    themeColor: '#101622',
    accentColor: '#b1e0ff',
    iconLetter: 'F',
  });
});

test('Ungültige Eingaben werden abgewiesen', () => {
  assert.throws(() => configFromInputs({ ...inputs, APP_NAME: ' ' }), /name/);
  assert.throws(() => configFromInputs({ ...inputs, APP_THEME_COLOR: '#123' }), /themeColor/);
  assert.throws(() => configFromInputs({ ...inputs, APP_ICON_LETTER: '<' }), /iconLetter/);
  assert.throws(() => configFromInputs({ ...inputs, APP_DESCRIPTION: '\nabc' }), /description/);
});

test('Leerzeichen an Rändern werden entfernt', () => {
  assert.equal(configFromInputs({ ...inputs, APP_NAME: '  Neue App  ' }).name, 'Neue App');
});
