# Akış · RSS

Material 3 Expressive ile yapılmış, bağımlılığı olmayan bir RSS/Atom okuyucu. Derleme adımı yok.

## Çalıştırma

```sh
cd rss-app
python3 -m http.server 8000
```

Sonra `http://localhost:8000` adresini açın. Bir klasör olarak herhangi bir statik barındırmaya da konabilir.

## Özellikler

- Akış: en yeni yazılar, öne çıkan büyük kart ve kaynak filtresi
- Kaydedilenler: sonra okumak için kaydedilen yazılar
- Kaynaklar: RSS veya Atom bağlantısı ekleme ve kaldırma
- Okundu işareti, koyu ve açık tema (sistem ayarını izler)
- Kaynaklar ve yazılar tarayıcıda (`localStorage`) saklanır

## Notlar

- Kaynaklar doğrudan tarayıcıdan alınır. Bu izin vermeyen sitelerde `app.js` içindeki `PROXY` adresi üzerinden bir CORS proxy'si kullanılır. Üretimde kendi proxy'nizi koymanız önerilir.
- İkonlar ve yazı tipi Google Fonts'tan yüklenir.
