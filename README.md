# Naze Vault

**Personal Secure Vault + File Manager + Project Workspace**

Aplikasi Android native (Kotlin + Jetpack Compose + Material 3) untuk menyimpan dan
mengelola file penting secara lokal di perangkat — tanpa server/backend, tanpa
upload cloud otomatis.

## Fitur

- **Vault** — file manager lengkap: buat/rename/copy/cut/paste/move/duplicate/
  delete, multi-select, search, sort, breadcrumb navigation, favorite.
- **Projects** — scaffold struktur project (Empty / Web / Android / Node.js /
  Python / React / Custom) langsung menjadi folder & file nyata.
- **Secrets** — API key, token, password, env var tersimpan terenkripsi
  AES-256-GCM lewat Android Keystore. Tidak pernah plaintext di disk atau log.
- **File Viewer** — syntax-aware text/code editor, JSON pretty-print, image
  viewer (zoom/pan), video & audio player, PDF viewer (Android `PdfRenderer`,
  tanpa library tambahan), ZIP content browser, dan fallback "Open with..."
  untuk format yang tidak didukung langsung.
- **ZIP Manager** — create, extract, dan lihat isi ZIP (dengan proteksi
  zip-slip), berjalan di background thread.
- **Vault Lock** — PIN (terenkripsi Keystore, bukan plaintext) + Biometric
  (sidik jari/wajah) + auto-lock setelah beberapa menit di background.
- **Backup** — export/import seluruh vault sebagai file `.zip` lewat Storage
  Access Framework. Tidak ada upload otomatis ke cloud mana pun.

## Arsitektur & prinsip

- **Local-first**: semua file disimpan di direktori privat aplikasi
  (`getExternalFilesDir()` dengan fallback ke `filesDir`), sehingga **tidak
  memerlukan permission storage** sama sekali di Android 11+.
- **Minimal dependency**: tidak memakai Room/SQLite — favorites & recent
  activity disimpan sebagai JSON kecil (`IndexStore`). Tidak ada networking
  library karena aplikasi memang tidak butuh internet (`INTERNET` permission
  pun tidak dideklarasikan).
- **Import file** memakai Storage Access Framework (system file picker), bukan
  permission storage klasik.
- **Enkripsi**: satu AES-256-GCM key yang tinggal di Android Keystore
  (`CryptoManager`), dipakai bersama oleh Secrets Vault dan penyimpanan PIN.

## Cara build

### Lewat GitHub Actions (disarankan)

1. Push repo ini ke GitHub.
2. Buka tab **Actions** → workflow **"Build Naze Vault APK"**.
3. Jalankan manual lewat **workflow_dispatch**, atau otomatis saat push ke
   `main`/`master`.
4. Setelah selesai, unduh APK dari **Artifacts** → `naze-vault-debug.apk`.

### Catatan tentang Gradle Wrapper

Project ini disiapkan di lingkungan sandbox tanpa akses internet, sehingga file
biner `gradle/wrapper/gradle-wrapper.jar` **tidak bisa ikut dibuat/di-commit**
dari sini (naskah `gradlew`/`gradlew.bat` teks biasa sudah ada, tapi jar-nya
tidak). Workflow CI sudah menangani ini secara otomatis: pada run pertama, ia
akan menjalankan `gradle wrapper --gradle-version 8.9` untuk membuat
`gradle-wrapper.jar` sebelum memanggil `./gradlew`.

Kalau kamu ingin build lewat **Android Studio** di komputer sendiri (yang
punya akses internet), cukup buka project ini sekali di Android Studio —
Android Studio akan otomatis melengkapi `gradle-wrapper.jar` yang hilang, atau
jalankan manual di terminal:

```
gradle wrapper --gradle-version 8.9
```

Setelah itu, hapus baris `gradle/wrapper/gradle-wrapper.jar` dari `.gitignore`
dan commit jar tersebut supaya build berikutnya (termasuk di GitHub Actions)
lebih cepat dan tidak bergantung pada instalasi Gradle sistem.

## Keterbatasan yang perlu diketahui

Project ini dibuat di lingkungan tanpa akses internet/emulator, jadi belum
bisa dijalankan `./gradlew assembleDebug` secara langsung di sini untuk
verifikasi compile 100% bersih. Semua kode sudah ditinjau manual untuk
kesalahan sintaks/import, tapi **run pertama di GitHub Actions adalah
verifikasi sesungguhnya**. Jika ada error Gradle/Kotlin di log Actions,
salin error tersebut dan minta diperbaiki.

Simplifikasi yang disengaja (bisa dikembangkan lebih lanjut):
- Kunci Keystore untuk Secrets tidak diberi `setUserAuthenticationRequired`,
  jadi dekripsi tidak memaksa autentikasi biometrik per-akses — gerbang utama
  keamanannya adalah PIN/biometric di Lock Screen. Untuk keamanan lebih tinggi,
  ini bisa ditambahkan di `CryptoManager`.
- Player audio/video pakai `MediaPlayer`/`VideoView` bawaan Android (bukan
  ExoPlayer) agar dependency tetap minimal.
- Detail file (`File Details`) ditampilkan lewat metadata di header viewer,
  belum berupa dialog "Details" terpisah dari daftar file.

## Struktur project

```
NazeVault/
├── .github/workflows/build.yml
├── app/src/main/java/com/naze/vault/
│   ├── data/            # FileRepository, IndexStore, ProjectBuilder, model
│   ├── security/        # CryptoManager, SecretsRepository, VaultLockManager
│   ├── ui/
│   │   ├── components/  # Breadcrumb, FAB, FileListItem
│   │   ├── screens/     # Lock, Dashboard, FileBrowser, FileViewer, Secrets, ...
│   │   └── theme/       # Color, Type, Theme (dark-only, Naze branding)
│   └── util/            # FileUtils, ZipUtils
└── app/src/main/res/     # themes, colors, launcher icon, xml configs
```

Package: `com.naze.vault` · minSdk 30 (Android 11+) · targetSdk/compileSdk 34.
