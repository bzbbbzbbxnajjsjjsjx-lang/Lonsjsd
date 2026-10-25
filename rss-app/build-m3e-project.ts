// Builds the RSS app as an M3E Canvas project (the Doc JSON the editor opens).
// Every part gets its own explicit place on its screen, so nothing overlaps and
// nothing depends on the editor's measurements. The file is checked with the
// same validator the editor runs when a project is imported.
import { writeFileSync } from "node:fs";
import { PHONE_W, PHONE_H, FRAME_GAP, Doc, Frame, Group, Item, DEFAULT_THEME } from "/home/user/lnkiai/m3e-canvas/lib/tokens";
import { isProject, projectFileName } from "/home/user/lnkiai/m3e-canvas/lib/project";

const OUT = "/home/user/Lonsjsd/rss-app";
const MARGIN = 16;
const NAV_Y = PHONE_H - 80; // bottom navigation bar at the foot of every main screen

const screens = [
  { id: "stream", name: "Akış", note: "Ana ekran. Üstte başlık ve kaynak filtre çipleri, öne çıkan yazı, altta yazı kartları ve alt gezinme." },
  { id: "read", name: "Okuma", note: "Bir yazının okuma ekranı. Geri ok ile Akış'a döner. Kaydet ve Orijinali aç düğmeleri vardır." },
  { id: "saved", name: "Kaydedilenler", note: "Kaydedilmiş yazıların listesi. Her kart okuma ekranına gider." },
  { id: "feeds", name: "Kaynaklar", note: "Takip edilen RSS kaynaklarının listesi. Her satırda akışta gösterme anahtarı var. Sağ altta Kaynak ekle düğmesi yeni bağlantı diyaloğunu açar." },
  { id: "add", name: "Kaynak ekle", note: "Kaynaklar ekranının üstünde açılan diyalog. RSS veya Atom bağlantısı girilir ve Ekle ile kaydedilir." },
];

const frames: Frame[] = screens.map((s, i) => ({
  id: s.id,
  name: s.name,
  x: i * (PHONE_W + FRAME_GAP),
  y: 0,
  w: PHONE_W,
  h: PHONE_H,
  note: s.note,
}));
const frameOf = (id: string) => frames.find((f) => f.id === id)!;

let seq = 0;
const part = (p: Partial<Item> & Pick<Item, "kind" | "label" | "variant">): Item => ({ id: `p${++seq}`, icon: null, ...p });

const nav = (selected: number): Item =>
  part({
    kind: "bottomNav",
    label: "Gezinme",
    variant: "filled",
    selected,
    tabs: [
      { icon: "feed", label: "Akış" },
      { icon: "bookmark", label: "Kaydedilen" },
      { icon: "rss_feed", label: "Kaynaklar" },
    ],
    actions: {
      "tab:0": { to: "stream", transition: "fade" },
      "tab:1": { to: "saved", transition: "fade" },
      "tab:2": { to: "feeds", transition: "fade" },
    },
  });

const top = (label: string, extra: Partial<Item> = {}) => part({ kind: "topAppBar", label, variant: "text", ...extra });

const article = (label: string, supporting: string, variant: Item["variant"] = "elevated") =>
  part({ kind: "card", label, supporting, variant, noImage: true, action: { to: "read", transition: "slide" } });

// A screen is a list of rows. A row is one part, or several parts side by side.
type Row = { y: number; items: Item[]; axis?: "x" | "y" };
const screenRows: Record<string, Row[]> = {
  stream: [
    { y: 0, items: [top("Akış", { icon: "search", icon2: "refresh" })] },
    {
      y: 76,
      axis: "x",
      items: [
        part({ kind: "chip", label: "Tümü", variant: "filled", checked: true }),
        part({ kind: "chip", label: "Hacker News", variant: "outlined" }),
        part({ kind: "chip", label: "The Verge", variant: "outlined" }),
      ],
    },
    {
      y: 128,
      items: [
        part({
          kind: "card",
          label: "Yapay zeka araçları işleri değiştiriyor",
          supporting: "Hacker News · 1 saat önce",
          variant: "filled",
          imagePos: "top",
          imageSize: 200,
          action: { to: "read", transition: "expand" },
        }),
      ],
    },
    { y: 452, items: [article("Açık kaynak derleyiciler yeni sürüm", "BBC Teknoloji · 2 saat önce")] },
    { y: 566, items: [article("Katı hal bataryalar seri üretimde", "The Verge · 3 saat önce", "tonal")] },
    { y: NAV_Y, items: [nav(0)] },
  ],
  read: [
    { y: 0, items: [top("Haber", { icon: "arrow_back", icon2: "bookmark_border", actions: { icon: { to: "back", transition: "slideLeft" } } })] },
    { y: 96, items: [part({ kind: "text", label: "Yapay zeka araçları", variant: "text", bold: true })] },
    { y: 128, items: [part({ kind: "text", label: "gündelik işleri değiştiriyor", variant: "text", bold: true })] },
    { y: 190, items: [part({ kind: "text", label: "Yeni nesil asistanlar", variant: "text" })] },
    { y: 214, items: [part({ kind: "text", label: "kod yazmaktan takvime", variant: "text" })] },
    { y: 238, items: [part({ kind: "text", label: "her alanda iş akışlarını", variant: "text" })] },
    { y: 262, items: [part({ kind: "text", label: "yeniden tanımlıyor.", variant: "text" })] },
    {
      y: 316,
      axis: "x",
      items: [
        part({ kind: "button", label: "Orijinali aç", variant: "filled", icon: "open_in_new", action: { to: "link", transition: "none", url: "https://news.ycombinator.com/" } }),
        part({ kind: "button", label: "Kaydet", variant: "tonal", icon: "bookmark_border", toggle: { icon: "bookmark", label: "Kaydedildi" } }),
      ],
    },
  ],
  saved: [
    { y: 0, items: [top("Kaydedilenler", { icon: "search", icon2: "sort" })] },
    { y: 84, items: [article("Yapay zeka araçları işleri değiştiriyor", "Hacker News · 1 saat önce")] },
    { y: 204, items: [article("Katı hal bataryalar seri üretimde", "The Verge · 3 saat önce", "tonal")] },
    { y: NAV_Y, items: [nav(1)] },
  ],
  feeds: [
    { y: 0, items: [top("Kaynaklar", { icon2: "more_vert" })] },
    { y: 84, items: [part({ kind: "listItem", label: "Hacker News", supporting: "hnrss.org/frontpage", icon: "rss_feed", variant: "tonal", switch: true, checked: true })] },
    { y: 164, items: [part({ kind: "listItem", label: "The Verge", supporting: "theverge.com/rss", icon: "rss_feed", variant: "tonal", switch: true, checked: true })] },
    { y: 244, items: [part({ kind: "listItem", label: "BBC Teknoloji", supporting: "feeds.bbci.co.uk", icon: "rss_feed", variant: "tonal", switch: true, checked: true })] },
    { y: NAV_Y - 72, items: [part({ kind: "extendedFab", label: "Kaynak ekle", icon: "add", variant: "tonal", action: { to: "add", transition: "slideUp" } })] },
    { y: NAV_Y, items: [nav(2)] },
  ],
  add: [
    { y: 220, items: [part({ kind: "dialog", label: "Kaynak ekle", supporting: "RSS veya Atom bağlantısını yapıştırın.", variant: "elevated" })] },
  ],
};

const groups: Group[] = [];
for (const frame of frames) {
  for (const [i, row] of (screenRows[frame.id] ?? []).entries()) {
    const x = frame.x + MARGIN;
    if (row.axis === "x") {
      // a row of parts side by side: one group, laid out on the x axis
      groups.push({ id: `${frame.id}-r${i}`, x, y: frame.y + row.y, axis: "x", items: row.items });
    } else {
      // every part stands alone at its own height, so none can cover another
      for (const item of row.items) {
        groups.push({ id: `${frame.id}-r${i}-${item.id}`, x: frame.x + MARGIN, y: frame.y + row.y, axis: "y", items: [item] });
      }
    }
  }
}
// the extended FAB sits at the bottom right, and the dialog is centered
for (const g of groups) {
  if (g.items[0].kind === "extendedFab") g.x = frameOf("feeds").x + PHONE_W - 150;
  if (g.items[0].kind === "dialog") g.x = frameOf("add").x + 40;
}

const doc: Doc = {
  groups,
  frames,
  paletteKey: "purple",
  frame: "phone",
  platform: "android",
  title: "Akış RSS",
  brief:
    "Material 3 Expressive ile yapılmış bir RSS ve Atom okuyucu. Üstte başlık, kaynak filtre çipleri, öne çıkan yazı ve yazı kartları var. Alt gezinme Akış, Kaydedilenler ve Kaynaklar arasında geçiş sağlar. Yazılar kaydedilebilir ve orijinal siteye açılabilir. Kaynaklar listesinden yeni RSS bağlantısı eklenir.",
  theme: { ...DEFAULT_THEME, dark: false, bothModes: true, emphasized: true, motion: "expressive", shape: "rounded" },
};

if (!isProject(doc)) throw new Error("the editor would reject this project");

const file = `${OUT}/${projectFileName(doc)}`;
writeFileSync(file, JSON.stringify(doc, null, 2) + "\n");
console.log("written:", file, "groups:", groups.length);
