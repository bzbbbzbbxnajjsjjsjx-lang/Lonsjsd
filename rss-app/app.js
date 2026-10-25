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
      // Storage is unavailable (private mode, blocked site data). The app keeps working in memory.
    }
  },
};

// The article currently shown in the reader dialog.
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

// Reads both RSS (<item>) and Atom (<entry>) documents.
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

  // Keep the cached articles when every source failed.
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
    <article class="card${read ? ' is-read' : ''}" data-id="${esc(item.id)}" tabindex="0">
      ${item.image ? `<img class="card-img" src="${esc(item.image)}" alt="" loading="lazy" referrerpolicy="no-referrer">` : ''}
      <h3>${esc(item.title)}</h3>
      <p>${esc(item.summary)}</p>
      <div class="card-foot">
        <div class="card-meta"><span>${esc(item.source)}</span><span>${esc(timeAgo(item.date))}</span></div>
        <button class="icon-btn save${saved ? ' on' : ''}" data-save="${esc(item.id)}" aria-label="${saved ? 'Kayıttan çıkar' : 'Kaydet'}">
          <span class="ms${saved ? ' fill' : ''}">bookmark</span>
        </button>
      </div>
    </article>`;
}

function heroCard(item) {
  return `
    <article class="hero${item.image ? ' has-img' : ''}" data-id="${esc(item.id)}" tabindex="0">
      ${item.image ? `<div class="hero-media"><img src="${esc(item.image)}" alt="" referrerpolicy="no-referrer"></div>` : ''}
      <div class="hero-body">
        <div class="eyebrow"><span>${esc(item.source)}</span><span>·</span><span>${esc(timeAgo(item.date))}</span></div>
        <h2>${esc(item.title)}</h2>
        <p>${esc(item.summary)}</p>
      </div>
    </article>`;
}

function renderChips() {
  const chips = [{ id: 'all', title: 'Tümü' }, ...state.feeds];
  $('#chips').innerHTML = chips
    .map((chip) => `<button class="chip" data-filter="${esc(chip.id)}" aria-pressed="${state.filter === chip.id}">${esc(chip.title)}</button>`)
    .join('');
}

function renderFeeds() {
  $('#feeds').innerHTML = state.feeds
    .map(
      (feed) => `
      <div class="row">
        <div class="avatar">${esc(feed.title.charAt(0).toUpperCase())}</div>
        <div class="info"><strong>${esc(feed.title)}</strong><small>${esc(feed.url)}</small></div>
        <button class="icon-btn" data-remove="${esc(feed.id)}" aria-label="${esc(feed.title)} kaynağını kaldır"><span class="ms">delete</span></button>
      </div>`,
    )
    .join('');
}

function setEmpty(message) {
  const empty = $('#empty');
  empty.hidden = !message;
  empty.innerHTML = message ? `<span class="ms">inbox</span>${esc(message)}` : '';
}

function render() {
  $('#title').textContent = TITLES[state.view];
  document.querySelectorAll('.nav button').forEach((button) => {
    if (button.dataset.view === state.view) button.setAttribute('aria-current', 'page');
    else button.removeAttribute('aria-current');
  });

  $('#add').hidden = state.view !== 'feeds';
  $('#chips').hidden = state.view !== 'stream';
  $('#feeds').hidden = state.view !== 'feeds';

  if (state.view === 'feeds') {
    renderFeeds();
    setEmpty(state.feeds.length ? '' : 'Henüz kaynak yok. Kaynak ekle düğmesiyle bir RSS bağlantısı ekleyin.');
    return;
  }

  renderChips();
  const items = visibleItems();
  const isStream = state.view === 'stream';

  $('#hero').innerHTML = isStream && items[0] ? heroCard(items[0]) : '';
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

  if (id === articleId) syncSaveButton();
  render();
}

function syncSaveButton() {
  const saved = Object.hasOwn(state.saved, articleId);
  $('#a-save').innerHTML = `<span class="ms${saved ? ' fill' : ''}">bookmark</span>${saved ? 'Kaydedildi' : 'Kaydet'}`;
}

function openArticle(id) {
  const item = itemById(id);
  if (!item) return;

  state.read.add(id);
  store.set(KEYS.read, [...state.read].slice(-500));

  articleId = id;
  $('#a-meta').textContent = `${item.source} · ${timeAgo(item.date)}`;
  $('#a-title').textContent = item.title;
  $('#a-summary').textContent = item.summary || 'Özet yok. Tam yazı için orijinali açın.';
  $('#a-open').href = item.link;
  syncSaveButton();

  render();
  $('#article').showModal();
}

function removeFeed(id) {
  state.feeds = state.feeds.filter((feed) => feed.id !== id);
  if (state.filter === id) state.filter = 'all';
  store.set(KEYS.feeds, state.feeds);
  render();
  toast('Kaynak kaldırıldı');
}

function openAdd() {
  $('#add-form').reset();
  $('#add-error').textContent = '';
  $('#add-dialog').showModal();
  $('#add-url').focus();
}

async function addFeed(event) {
  event.preventDefault();
  const submit = $('#add-submit');
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

  submit.disabled = true;
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
    submit.disabled = false;
  }
}

document.addEventListener('click', (event) => {
  const target = event.target;

  const save = target.closest('[data-save]');
  if (save) return toggleSave(save.dataset.save);

  const remove = target.closest('[data-remove]');
  if (remove) return removeFeed(remove.dataset.remove);

  const filter = target.closest('[data-filter]');
  if (filter) {
    state.filter = filter.dataset.filter;
    return render();
  }

  const view = target.closest('[data-view]');
  if (view) {
    state.view = view.dataset.view;
    window.scrollTo({ top: 0 });
    return render();
  }

  const card = target.closest('[data-id]');
  if (card) openArticle(card.dataset.id);
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Enter' && event.target.matches('[data-id]')) {
    openArticle(event.target.dataset.id);
  }
});

$('#refresh').addEventListener('click', refresh);
$('#add').addEventListener('click', openAdd);
$('#add-cancel').addEventListener('click', () => $('#add-dialog').close());
$('#add-form').addEventListener('submit', addFeed);
$('#a-save').addEventListener('click', () => toggleSave(articleId));

render();
refresh();
