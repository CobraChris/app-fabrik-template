import { writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { validateConfig } from './build.mjs';

export function configFromInputs(env) {
  return validateConfig({
    name: env.APP_NAME,
    shortName: env.APP_SHORT_NAME,
    description: env.APP_DESCRIPTION,
    themeColor: env.APP_THEME_COLOR,
    accentColor: env.APP_ACCENT_COLOR,
    iconLetter: env.APP_ICON_LETTER,
  });
}

async function main() {
  const config = configFromInputs(process.env);
  const filename = fileURLToPath(new URL('../app.config.json', import.meta.url));
  await writeFile(filename, JSON.stringify(config, null, 2) + '\n');
  console.log('App-Konfiguration validiert und gespeichert: ' + path.basename(filename));
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch(error => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
