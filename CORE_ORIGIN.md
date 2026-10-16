# core のコピー元

`app/src/main/cpp/core/` と `core-tests/` は Hiragana IME for Windows
（`../hiragana-ime-win`）の `src/core/` と `tests/` をコピーしたものです。
2026-10-16 にコピーしました。`OnnxBert.*`、`tsf_test.cpp`、`furiganapad_tests.cpp`、`onnx_bert_test.cpp` はコピーしていません。

## Android 版で変えたところ

Windows 版にもそのまま戻せる変更です。

- `Text.h` / `Text.cpp`：`from_wide` / `to_wide` を `#ifdef _WIN32` で囲んだ（Android の wchar_t は32ビットのため）。
- `Dictionary.cpp`：`file_time_type` の count を `long long` にキャストした（libc++ では `__int128` になるため）。
- `Engine.h` / `Engine.cpp`：`Engine::insert_kana()` と `Engine::KANA_CYCLE` を足した（フリック入力と「゛゜小」キー用）。
- `Engine.h` / `Engine.cpp`：`set_shorter_candidates()` と `candidate_selected()` を足した。候補の列のうしろに、読みを短くした候補（うは → う羽）を並べる。既定では off なので、元の動きと golden テストは変わらない。
- `core-tests/AndroidEngineTests.cpp`：上の追加分のテスト（新しいファイル）。

## 同期のしかた

`tools/sync-core.ps1` を実行すると、Windows 版との違いが表示されます。
`-Apply` を付けると、Windows 版で変わったファイルをこちらへコピーします。

## ホストでのテスト

`core-tests/` は、このフォルダにある `ibus-hiragana/`（Esrille の元のソース。git には入れていません）を使います。

```
powershell -ExecutionPolicy Bypass -File tools\host-test.ps1 -Filter core_tests   # 単体テストだけ（数秒）
powershell -ExecutionPolicy Bypass -File tools\host-test.ps1                      # golden テストも（約5分、Python 3 が必要）
```
