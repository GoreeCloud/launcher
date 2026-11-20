import { useMemo, useRef, useState } from 'react';
import { createShortcut, exportShortcuts, parseBackup, readShortcuts, saveShortcuts, type Shortcut } from './shortcuts';

const initial = (): Shortcut[] => readShortcuts(window.localStorage);
const colors = ['#BFB8FF', '#8BE7F1', '#FFB79F', '#A9E0B3'];
const categoryColor = (value: string): string => colors[Array.from(value).reduce((sum, c) => sum + c.charCodeAt(0), 0) % colors.length];
function updateStorage(next: Shortcut[]): string {
  try { saveShortcuts(window.localStorage, next); return 'Saved on this device'; }
  catch { return 'Browser storage is unavailable. Export your shortcuts to keep a copy.'; }
}
export function App() {
  const [items, setItems] = useState<Shortcut[]>(initial);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('All');
  const [openEditor, setOpenEditor] = useState(false);
  const [editing, setEditing] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [group, setGroup] = useState('General');
  const [feedback, setFeedback] = useState('');
  const file = useRef<HTMLInputElement>(null);

  const categories = useMemo(() => ['All', ...Array.from(new Set(items.map(x => x.category))).sort()], [items]);
  const shown = useMemo(() => items.filter(x =>
    (category === 'All' || x.category === category) &&
    (x.label + ' ' + x.category + ' ' + x.url).toLocaleLowerCase().includes(search.trim().toLocaleLowerCase())
  ).sort((a, b) => a.label.localeCompare(b.label)), [items, category, search]);

  const persist = (next: Shortcut[]) => { setItems(next); setFeedback(updateStorage(next)); };
  const openCreate = () => { setEditing(null); setName(''); setUrl(''); setGroup('General'); setOpenEditor(true); setFeedback(''); };
  const openEdit = (item: Shortcut) => { setEditing(item.id); setName(item.label); setUrl(item.url); setGroup(item.category); setOpenEditor(true); setFeedback(''); };
  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    try {
      const created = createShortcut({ label: name, url, category: group });
      persist(editing ? items.map(x => x.id === editing ? { ...created, id: editing } : x) : [...items, created]);
      setOpenEditor(false);
    } catch (error) { setFeedback(error instanceof Error ? error.message : 'Invalid shortcut'); }
  };
  const remove = (item: Shortcut) => {
    if (!window.confirm('Remove "' + item.label + '" from this browser?')) return;
    persist(items.filter(x => x.id !== item.id));
  };
  const backup = () => {
    const blob = new Blob([exportShortcuts(items)], { type: 'application/json' });
    const href = URL.createObjectURL(blob);
    const anchor = document.createElement('a'); anchor.href = href; anchor.download = 'goreecloud-launcher-shortcuts.json';
    anchor.click(); URL.revokeObjectURL(href); setFeedback('Unencrypted backup exported. Store it securely.');
  };
  const restore = async (selected?: File) => {
    if (!selected) return;
    if (selected.size > 256_000) { setFeedback('Backup exceeds 256 KB'); return; }
    try {
      const parsed = parseBackup(await selected.text());
      if (!window.confirm('Replace all local shortcuts with ' + parsed.length + ' imported entries?')) return;
      persist(parsed); setCategory('All');
    } catch (error) { setFeedback(error instanceof Error ? error.message : 'Invalid backup'); }
    finally { if (file.current) file.current.value = ''; }
  };
  return <div className="shell">
    <aside className="rail">
      <a className="identity" href="#main" aria-label="GoreeCloud Launcher home"><span className="symbol">G</span><span><strong>GoreeCloud</strong><small>Launcher / Web</small></span></a>
      <nav aria-label="Main navigation"><a className="nav-active" href="#main">⌂ <span>Dashboard</span></a><a href="#shortcuts">▦ <span>Shortcuts</span></a><a href="#privacy">◈ <span>Privacy</span></a></nav>
      <p className="rail-note">Your links stay in this browser. No account or telemetry.</p>
    </aside>
    <main id="main">
      <header className="top"><span className="breadcrumb">Workspace <span aria-hidden="true">/</span> Personal dashboard</span><span className="local-indicator">● Local-only</span></header>
      <section className="hero" aria-labelledby="welcome"><div className="hero-copy"><div className="eyebrow">YOUR SPACE, YOUR WAY</div><h1 id="welcome">Everything in its place.</h1><p>Your personal launchpad for the places and tools that matter. Private by default, ready when you are.</p><button className="primary" onClick={openCreate}>＋ Add shortcut</button></div><div aria-hidden="true" className="hero-illustration"><div className="orbit orbit-one"></div><div className="orbit orbit-two"></div><span className="hero-symbol">G</span></div></section>
      <section className="workspace" id="shortcuts" aria-labelledby="links-heading">
        <div className="section-heading"><div><div className="eyebrow">WORKSPACE</div><h2 id="links-heading">Your shortcuts <span className="count">{items.length}</span></h2></div><div className="actions"><button className="secondary" onClick={backup} disabled={!items.length}>Export JSON</button><button className="secondary" onClick={() => file.current?.click()}>Import JSON</button><input className="sr-only" type="file" ref={file} accept=".json,application/json" aria-label="Import shortcuts from JSON file" onChange={e => void restore(e.target.files?.[0])}/></div></div>
        <div className="filters"><label className="search"><span aria-hidden="true">⌕</span><input aria-label="Search your shortcuts" placeholder="Search shortcuts..." value={search} onChange={e=>setSearch(e.target.value)} /></label><label className="sr-only" htmlFor="category">Filter category</label><select id="category" value={categories.includes(category) ? category : 'All'} onChange={e => setCategory(e.target.value)}>{categories.map(x => <option key={x}>{x}</option>)}</select></div>
        {shown.length ? <div className="tiles">{shown.map(item => <article className="tile" key={item.id}><a className="tile-link" href={item.url} target="_blank" rel="noopener noreferrer" referrerPolicy="no-referrer" aria-label={'Open ' + item.label + ' in new tab'}><span className="tile-icon" style={{ backgroundColor: categoryColor(item.category) }}>{item.label.slice(0,1).toUpperCase()}</span><strong>{item.label}</strong><small>{item.category}</small><span className="hostname">{new URL(item.url).hostname}</span></a><div className="tile-actions"><button aria-label={'Edit ' + item.label} onClick={()=>openEdit(item)}>Edit</button><button aria-label={'Remove ' + item.label} onClick={()=>remove(item)}>Remove</button></div></article>)}</div> :
        <div className="empty"><div aria-hidden="true" className="empty-icon">▦</div><h3>{items.length ? 'No matching shortcuts' : 'Your workspace starts here'}</h3><p>{items.length ? 'Try another search or category.' : 'Add a favorite site to create your personal launchpad.'}</p><button className="secondary" onClick={items.length ? ()=>{setSearch('');setCategory('All');} : openCreate}>{items.length ? 'Clear filters' : 'Add your first shortcut'}</button></div>}
      </section>
      <footer id="privacy"><span>GoreeCloud Launcher · Development preview</span><span>Privacy-first · Offline configuration</span></footer>
    </main>
    {openEditor && <div className="modal-backdrop" onClick={() => setOpenEditor(false)}><div className="dialog" role="dialog" aria-modal="true" aria-labelledby="editor-title" onClick={e => e.stopPropagation()}><div className="dialog-top"><h2 id="editor-title">{editing ? 'Edit shortcut' : 'Add shortcut'}</h2><button className="close" aria-label="Close editor" onClick={()=>setOpenEditor(false)}>✕</button></div><form onSubmit={submit}><label>Name<input autoFocus required maxLength={80} value={name} onChange={e=>setName(e.target.value)} placeholder="My favorite app" /></label><label>Web address<input required type="url" maxLength={2048} value={url} onChange={e=>setUrl(e.target.value)} placeholder="https://example.org" /></label><label>Category<input required maxLength={32} value={group} onChange={e=>setGroup(e.target.value)} placeholder="General" /></label><div className="dialog-actions"><button className="secondary" type="button" onClick={()=>setOpenEditor(false)}>Cancel</button><button className="primary" type="submit">Save shortcut</button></div></form></div></div>}
    <div className="sr-only" role="status" aria-live="polite">{feedback}</div>
    {feedback && <p className="toast" role="status">{feedback}</p>}
  </div>;
}
