# Akış · RSS

RSS ve Atom okuyucu. Arayüz Material 3 Expressive bileşenleri ve tokenlarıyla yapıldı
(`@material/web` 2.5.0, `labs/gb` M3 tokenları). Derleme adımı yok.

## Çalıştırma

İnternet bağlantısı gerekir: bileşenler ve fontlar CDN'den yüklenir.

```sh
cd rss-app
python3 -m http.server 8000
```

Sonra `http://localhost:8000` adresini açın.

## Dosyalar

- `index.html`: import map (sürümleri sabitlenmiş Lit ve Material Web), tema tokenları, sayfa iskeleti
- `app.js`: RSS/Atom okuma, kaydetme, okundu işareti, kaynak ekleme ve silme
- `styles.css`: yalnızca yerleşim; renk, köşe, tipografi ve hareket tokenlardan gelir
- `m3e-canvas Akış RSS.json`, `m3e-canvas-preview.png`, `build-m3e-project.ts`: M3E Canvas tasarım dosyası, önizleme ve üretici betik

## Kullanılan Material 3 Expressive bileşenleri

`md-navigation-bar` / `md-navigation-tab`, `md-fab` (extended), `md-filter-chip`, `md-elevated-card`,
`md-filled-card` (`--md-sys-shape-corner-xl-increased`), `md-dialog`, `md-outlined-text-field`,
`md-list-item`, `md-icon-button` (toggle), `md-filled-button`, `md-filled-tonal-button`, `md-text-button`,
`md-linear-progress`.

## Özellikler

- Akış: öne çıkan yazı, kaynak filtresi, yazı kartları
- Kaydedilenler: sonra okumak için kaydedilen yazılar
- Kaynaklar: RSS/Atom bağlantısı ekleme ve kaldırma
- Okundu işareti ve açık/koyu tema (sistem ayarı)
- Veriler tarayıcıda (`localStorage`) saklanır

## Notlar

- Kaynaklar önce doğrudan alınır. İzin vermeyen sitelerde `app.js` içindeki `PROXY` adresi (`api.allorigins.win`) kullanılır. Üretimde kendi proxy'nizi koymanız önerilir.
