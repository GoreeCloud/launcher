import { describe, expect, test } from 'vitest';
import { createShortcut, exportShortcuts, parseBackup, sanitizeUrl, readShortcuts, saveShortcuts } from './shortcuts';

describe('shortcut boundaries', () => {
  test('accepts https links and normalizes', () => expect(sanitizeUrl(' https://example.org ')).toBe('https://example.org/'));
  test.each(['javascript:alert(1)', 'data:text/plain,secret', 'file:///etc/passwd', 'https://user:pass@site.test'])(
    'rejects unsafe URL %s', (value) => expect(() => sanitizeUrl(value)).toThrow()
  );
  test('rejects duplicate backup IDs', () => {
    const s = createShortcut({ label: 'Docs', url: 'https://example.org', category: 'Work' });
    expect(() => parseBackup(exportShortcuts([s, s]))).toThrow('duplicate');
  });
  test('rejects oversized backups', () => expect(() => parseBackup(' '.repeat(256_001))).toThrow());
  test('round trips a valid backup', () => {
    const s = createShortcut({ label: 'Docs', url: 'https://example.org', category: 'Work' });
    expect(parseBackup(exportShortcuts([s]))).toEqual([s]);
  });
  test('survives denied browser storage', () => {
    expect(readShortcuts({ getItem: () => { throw new Error('denied'); } })).toEqual([]);
    const saved: string[] = [];
    saveShortcuts({ setItem: (_, value) => { saved.push(value); } }, []);
    expect(saved).toHaveLength(1);
  });
});
