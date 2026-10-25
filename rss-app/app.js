import '@material/web/button/filled-button.js';
import '@material/web/button/filled-tonal-button.js';
import '@material/web/button/text-button.js';
import '@material/web/iconbutton/icon-button.js';
import '@material/web/fab/fab.js';
import '@material/web/chips/chip-set.js';
import '@material/web/chips/filter-chip.js';
import '@material/web/list/list.js';
import '@material/web/list/list-item.js';
import '@material/web/dialog/dialog.js';
import '@material/web/textfield/outlined-text-field.js';
import '@material/web/progress/linear-progress.js';
import '@material/web/labs/navigationbar/navigation-bar.js';
import '@material/web/labs/navigationtab/navigation-tab.js';
import '@material/web/labs/card/elevated-card.js';
import '@material/web/labs/card/filled-card.js';

// Public CORS proxy, used only when a feed does not allow direct browser requests.
const PROXY = 'https://api.allorigins.win/raw?url=';

const DEFAULT_FEEDS = [
  { id: 'hn', title: 'Hacker News', url: 'https://hnrss.org/frontpage' },
  { id: 'verge', title: 'The Verge', url: 'https://www.theverge.com/rss/index.xml' },
  { id: 'bbc', title: 'BBC Teknoloji', url: 'https://feeds.bbci.co.uk/news/technology/rss.xml' },
];

const TITLES = { stream: 'Akış', saved: 'Kaydedilenler', feeds: 'Kaynaklar' };
const KEYS = { feeds: 'rss.feeds', items: 'rss.items', saved: 'rss.saved', read: 'rss.read' };
const MAX_ITEMS = 150;

const store = {
  get(key, fallback) {
    try {
      const value = localStorage.getItem(key);
      return value ? JSON.parse(value) : fallback;
    } catch {
      return fallback;
    }
  },
  set(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      // Storage unavailable (private mode, blocked site data): keep working in memory.
    }
  },
};

// The article open in the reader dialog.
let articleId = null;

const state = {
  view: 'stream',
  filter: 'all',
  feeds: store.get(KEYS.feeds, DEFAULT_FEEDS),
  items: store.get(KEYS.items, []),
  saved: store.get(KEYS.saved, {}),
  read: new Set(store.get(KEYS.read, [])),
};

const $ = (selector) => document.querySelector(selector);
const esc = (value) =>
  String(value ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

const rtf = new Intl.RelativeTimeFormat('tr', { numeric: 'auto' });

function timeAgo(ts) {
  if (!ts) return '';
  const diff = (ts - Date.now()) / 1000;
  if (Math.abs(diff) > 7 * 86400) {
    return new Date(ts).toLocaleDateString('tr', { day: 'numeric', month: 'short' });
  }
  const units = [['day', 86400], ['hour', 3600], ['minute', 60]];
  const [unit, seconds] = units.find(([, s]) => Math.abs(diff) >= s) ?? ['minute', 60];
  return rtf.format(Math.round(diff / seconds), unit);
}

function stripHtml(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  return (doc.body.textContent || '').replace(/\s+/g, ' ').trim();
}

async function fetchText(url) {
  for (const target of [url, PROXY + encodeURIComponent(url)]) {
    try {
      const response = await fetch(target);
      if (response.ok) return await response.text();
    } catch {
      // Try the next route.
    }
  }
  throw new Error(`Kaynak alınamadı: ${url}`);
}

// Reads RSS (<item>) and Atom (<entry>) documents.
function parseFeed(xml, feed) {
  const doc = new DOMParser().parseFromString(xml, 'text/xml');
  if (doc.getElementsByTagName('parsererror').length) {
    throw new Error(`Geçersiz akış: ${feed.url}`);
  }

  const title = doc.querySelector('channel > title, feed > title')?.textContent.trim() || feed.title;
  const nodes = [...doc.getElementsByTagName('item'), ...doc.getElementsByTagName('entry')];

  const items = nodes
    .slice(0, 30)
    .map((el) => {
      const text = (...names) => {
        for (const name of names) {
          const value = el.getElementsByTagName(name)[0]?.textContent.trim();
          if (value) return value;
        }
        return '';
      };
      const linkEl = el.getElementsByTagName('link')[0];
      const link = linkEl?.getAttribute('href') || linkEl?.textContent.trim() || '';
      const image =
        el.querySelector('enclosure[type^="image"]')?.getAttribute('url') ||
        el.getElementsByTagName('media:content')[0]?.getAttribute('url') ||
        '';
      const date = new Date(text('pubDate', 'published', 'updated', 'dc:date'));

      return {
        id: link || text('title'),
        title: text('title'),
        link,
        summary: stripHtml(text('description', 'summary', 'content:encoded', 'content')).slice(0, 320),
        image,
        date: isNaN(date) ? null : date.getTime(),
        feedId: feed.id,
        source: title,
      };
    })
    .filter((item) => item.title && item.link);

  return { title, items };
}

function mergeItems(list) {
  const unique = new Map();
  for (const item of list) {
    if (!unique.has(item.id)) unique.set(item.id, item);
  }
  return [...unique.values()].sort((a, b) => (b.date ?? 0) - (a.date ?? 0)).slice(0, MAX_ITEMS);
}

async function refresh() {
  if (!state.feeds.length) return render();

  $('#progress').hidden = false;
  $('#refresh').disabled = true;

  const results = await Promise.allSettled(
    state.feeds.map(async (feed) => parseFeed(await fetchText(feed.url), feed)),
  );

  const fresh = [];
  const failed = [];
  results.forEach((result, i) => {
    const feed = state.feeds[i];
    if (result.status === 'fulfilled') {
      fresh.push(...result.value.items);
      feed.title = result.value.title;
    } else {
      failed.push(feed.title);
    }
  });

  // Keep cached articles when every source failed.
  if (fresh.length) {
    state.items = mergeItems(fresh);
    store.set(KEYS.items, state.items);
  }
  store.set(KEYS.feeds, state.feeds);

  $('#progress').hidden = true;
  $('#refresh').disabled = false;
  render();
  if (failed.length) toast(`Yüklenemedi: ${failed.join(', ')}`);
}

function itemById(id) {
  return state.items.find((item) => item.id === id) ?? state.saved[id];
}

function visibleItems() {
  if (state.view === 'saved') {
    return Object.values(state.saved).sort((a, b) => (b.date ?? 0) - (a.date ?? 0));
  }
  const ids = new Set(state.feeds.map((feed) => feed.id));
  return state.items.filter(
    (item) => ids.has(item.feedId) && (state.filter === 'all' || item.feedId === state.filter),
  );
}

function card(item) {
  const saved = Object.hasOwn(state.saved, item.id);
  const read = state.read.has(item.id);
  return `
    <md-elevated-card data-id="${esc(item.id)}" tabindex="0" role="button" aria-label="${esc(item.title)}" style="${read ? 'opacity:.78' : ''}">
      <div class="card-body">
        ${item.image ? `<img src="${esc(item.image)}" alt="" loading="lazy" referrerpolicy="no-referrer">` : ''}
        <h3>${esc(item.title)}</h3>
        <p>${esc(item.summary)}</p>
        <div class="card-foot">
          <span class="source">${esc(item.source)}</span>
          <span class="when">${esc(timeAgo(item.date))}</span>
          <md-icon-button data-save="${esc(item.id)}" class="${saved ? 'on' : ''}" aria-label="${saved ? 'Kayıttan çıkar' : 'Kaydet'}" toggle ${saved ? 'selected' : ''}>
            <span class="material-symbols-rounded">bookmark</span>
            <span slot="selected" class="material-symbols-rounded">bookmark</span>
          </md-icon-button>
        </div>
      </div>
    </md-elevated-card>`;
}

function hero(item) {
  return `
    <md-filled-card data-id="${esc(item.id)}" tabindex="0" role="button" aria-label="${esc(item.title)}">
      <div class="hero-body">
        <span class="source">${esc(item.source)} · ${esc(timeAgo(item.date))}</span>
        <h2>${esc(item.title)}</h2>
        <p>${esc(item.summary)}</p>
      </div>
    </md-filled-card>`;
}

function renderChips() {
  const chips = [{ id: 'all', title: 'Tümü' }, ...state.feeds];
  $('#chips').innerHTML = chips
    .map((chip) => `<md-filter-chip label="${esc(chip.title)}" data-filter="${esc(chip.id)}" ${state.filter === chip.id ? 'selected' : ''}></md-filter-chip>`)
    .join('');
}

function renderFeeds() {
  $('#feeds').innerHTML = state.feeds
    .map(
      (feed) => `
      <md-list-item type="text">
        <span slot="headline">${esc(feed.title)}</span>
        <span slot="supporting-text">${esc(feed.url)}</span>
        <span slot="start" class="material-symbols-rounded">rss_feed</span>
        <md-icon-button slot="end" data-remove="${esc(feed.id)}" aria-label="${esc(feed.title)} kaynağını kaldır">
          <span class="material-symbols-rounded">delete</span>
        </md-icon-button>
      </md-list-item>`,
    )
    .join('');
}

function setEmpty(message) {
  const empty = $('#empty');
  empty.hidden = !message;
  empty.innerHTML = message ? `<span class="material-symbols-rounded">inbox</span>${esc(message)}` : '';
}

function render() {
  $('#title').textContent = TITLES[state.view];
  $('#nav').activeIndex = ['stream', 'saved', 'feeds'].indexOf(state.view);
  $('#add').hidden = state.view !== 'feeds';
  $('#chips').hidden = state.view !== 'stream';
  $('#hero').hidden = state.view !== 'stream';
  $('#grid').hidden = state.view === 'feeds';
  $('#feeds').hidden = state.view !== 'feeds';

  if (state.view === 'feeds') {
    renderFeeds();
    setEmpty(state.feeds.length ? '' : 'Henüz kaynak yok. Kaynak ekle ile bir RSS bağlantısı ekleyin.');
    return;
  }

  renderChips();
  const items = visibleItems();
  const isStream = state.view === 'stream';

  $('#hero').innerHTML = isStream && items[0] ? hero(items[0]) : '';
  $('#grid').innerHTML = (isStream ? items.slice(1) : items).map(card).join('');

  if (!items.length) {
    setEmpty(
      state.view === 'saved'
        ? 'Henüz kaydedilmiş bir yazı yok.'
        : state.feeds.length
          ? 'Gösterilecek yazı yok. Yenilemeyi deneyin.'
          : 'Henüz kaynak yok. Kaynaklar sekmesinden bir RSS bağlantısı ekleyin.',
    );
  } else {
    setEmpty('');
  }
}

function toast(message) {
  const el = $('#toast');
  el.textContent = message;
  el.classList.add('show');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => el.classList.remove('show'), 2600);
}

function toggleSave(id) {
  const item = itemById(id);
  if (!item) return;

  if (Object.hasOwn(state.saved, id)) {
    delete state.saved[id];
    toast('Kayıttan çıkarıldı');
  } else {
    state.saved[id] = item;
    toast('Kaydedildi');
  }
  store.set(KEYS.saved, state.saved);

  if (id === articleId) syncReaderSave();
  render();
}

function syncReaderSave() {
  const saved = Object.hasOwn(state.saved, articleId);
  $('#r-save').textContent = saved ? 'Kaydedildi' : 'Kaydet';
}

function openArticle(id) {
  const item = itemById(id);
  if (!item) return;

  state.read.add(id);
  store.set(KEYS.read, [...state.read].slice(-500));

  articleId = id;
  $('#r-meta').textContent = `${item.source} · ${timeAgo(item.date)}`;
  $('#r-title').textContent = item.title;
  $('#r-summary').textContent = item.summary || 'Özet yok. Tam yazı için orijinali açın.';
  $('#r-open').href = item.link;
  syncReaderSave();

  render();
  $('#reader').show();
}

function removeFeed(id) {
  state.feeds = state.feeds.filter((feed) => feed.id !== id);
  if (state.filter === id) state.filter = 'all';
  store.set(KEYS.feeds, state.feeds);
  render();
  toast('Kaynak kaldırıldı');
}

function openAdd() {
  $('#add-url').value = '';
  $('#add-error').textContent = '';
  $('#add-dialog').show();
}

async function addFeed() {
  const error = $('#add-error');
  const raw = $('#add-url').value.trim();

  let url;
  try {
    url = new URL(/^https?:\/\//i.test(raw) ? raw : `https://${raw}`).href;
  } catch {
    error.textContent = 'Geçerli bir bağlantı girin.';
    return;
  }
  if (state.feeds.some((feed) => feed.url === url)) {
    error.textContent = 'Bu kaynak zaten ekli.';
    return;
  }

  $('#add-submit').disabled = true;
  error.textContent = 'Kaynak kontrol ediliyor…';
  try {
    const feed = { id: `f${Date.now().toString(36)}`, title: url, url };
    const parsed = parseFeed(await fetchText(url), feed);
    feed.title = parsed.title;

    state.feeds.push(feed);
    state.items = mergeItems([...parsed.items, ...state.items]);
    store.set(KEYS.feeds, state.feeds);
    store.set(KEYS.items, state.items);

    $('#add-dialog').close();
    render();
    toast(`${feed.title} eklendi`);
  } catch {
    error.textContent = 'Bu bağlantı okunamadı. RSS veya Atom olduğundan emin olun.';
  } finally {
    $('#add-submit').disabled = false;
  }
}

document.addEventListener('click', (event) => {
  const target = event.target;

  const save = target.closest('[data-save]');
  if (save) return toggleSave(save.dataset.save);

  const remove = target.closest('[data-remove]');
  if (remove) return removeFeed(remove.dataset.remove);

  const filterChip = target.closest('md-filter-chip[data-filter]');
  if (filterChip) {
    state.filter = filterChip.dataset.filter;
    return render();
  }

  const card = target.closest('[data-id]');
  if (card) openArticle(card.dataset.id);
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Enter' && event.target.matches?.('[data-id]')) {
    openArticle(event.target.dataset.id);
  }
});

$('#nav').addEventListener('navigation-bar-activated', (event) => {
  const tab = event.detail.tab;
  state.view = tab.dataset.view ?? 'stream';
  window.scrollTo({ top: 0 });
  render();
});

$('#refresh').addEventListener('click', refresh);
$('#add').addEventListener('click', openAdd);
$('#add-cancel').addEventListener('click', () => $('#add-dialog').close());
$('#add-submit').addEventListener('click', addFeed);
$('#add-form').addEventListener('submit', (event) => event.preventDefault());
$('#r-save').addEventListener('click', () => toggleSave(articleId));

render();
refresh();
