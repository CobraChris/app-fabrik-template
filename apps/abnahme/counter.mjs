export const MAX_COUNT = 9999;

export function parseCount(raw) {
  if (typeof raw !== 'string' || !/^(0|[1-9][0-9]{0,3})$/.test(raw)) return 0;
  const n = Number(raw);
  return Number.isSafeInteger(n) && n <= MAX_COUNT ? n : 0;
}

export function incrementCount(count) {
  if (!Number.isInteger(count) || count < 0 || count > MAX_COUNT) {
    throw new Error('Ungültiger Zählerstand.');
  }
  return Math.min(MAX_COUNT, count + 1);
}
