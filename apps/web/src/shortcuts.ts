export type Shortcut = { id: string; label: string; url: string; category: string };
export type Backup = { version: 1; shortcuts: Shortcut[] };
const KEY = 'goreecloud.launcher.shortcuts.v1';
const MAX_SHORTCUTS = 200;
const MAX_BYTES = 256_000;

export function sanitizeUrl(value: string): string {
  if (typeof value !== 'string' || value.length > 2048) throw new Error('URL must be under 2048 characters');
  let url: URL;
  try { url = new URL(value.trim()); } catch { throw new Error('Use an absolute https:// or http:// URL'); }
  if (!['https:', 'http:'].includes(url.protocol) || !url.hostname || url.username || url.password) {
    throw new Error('Only http/https URLs without embedded credentials are accepted');
  }
  return url.href;
}

function label(value: unknown, max: number, field: string): string {
  if (typeof value !== 'string' || value.trim().length < 1 || value.trim().length > max) {
    throw new Error(field + ' must be 1–' + max + ' characters');
  }
  return value.trim();
}

export function createShortcut(value: { label: string; url: string; category: string }): Shortcut {
  return { id: crypto.randomUUID(), label: label(value.label, 80, 'Name'),
    url: sanitizeUrl(value.url), category: label(value.category || 'General', 32, 'Category') };
}

export function parseBackup(text: string): Shortcut[] {
  if (new TextEncoder().encode(text).length > MAX_BYTES) throw new Error('Backup exceeds 256 KB');
  let raw: unknown;
  try { raw = JSON.parse(text); } catch { throw new Error('Backup is not valid JSON'); }
  if (!raw || typeof raw !== 'object') throw new Error('Invalid backup');
  const backup = raw as Partial<Backup>;
  if (backup.version !== 1 || !Array.isArray(backup.shortcuts) || backup.shortcuts.length > MAX_SHORTCUTS) {
    throw new Error('Expected a version 1 backup containing at most 200 entries');
  }
  const ids = new Set<string>();
  return backup.shortcuts.map((item: unknown) => {
    if (!item || typeof item !== 'object') throw new Error('Invalid bookmark');
    const obj = item as Record<string, unknown>;
    const id = label(obj.id, 100, 'ID');
    if (!/^[a-zA-Z0-9._:-]+$/.test(id) || ids.has(id)) throw new Error('Invalid or duplicate bookmark ID');
    ids.add(id);
    return { id, label: label(obj.label, 80, 'Name'), url: sanitizeUrl(obj.url as string),
      category: label(obj.category, 32, 'Category') };
  });
}

export function readShortcuts(storage: Pick<Storage, 'getItem'>): Shortcut[] {
  try {
    const value = storage.getItem(KEY);
    return value === null ? [] : parseBackup(value);
  } catch { return []; }
}

export function saveShortcuts(storage: Pick<Storage, 'setItem'>, items: Shortcut[]): void {
  if (items.length > MAX_SHORTCUTS) throw new Error('Maximum 200 shortcuts');
  storage.setItem(KEY, JSON.stringify({ version: 1, shortcuts: items } satisfies Backup));
}

export function exportShortcuts(items: Shortcut[]): string {
  return JSON.stringify({ version: 1, shortcuts: items } satisfies Backup, null, 2);
}
