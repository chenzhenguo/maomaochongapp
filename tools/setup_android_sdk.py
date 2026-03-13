from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
import time
import zipfile
from pathlib import Path
from urllib.request import Request, build_opener, ProxyHandler


CMDLINE_TOOLS_ZIP_URL = (
    "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
)


def _download(url: str, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    opener = build_opener(ProxyHandler({}))  # ignore broken local proxy env vars
    for attempt in range(1, 6):
        existing = dest.stat().st_size if dest.exists() else 0
        headers = {"User-Agent": "maomaochongapp-android-sdk-setup/1.0"}
        if existing:
            headers["Range"] = f"bytes={existing}-"

        req = Request(url, headers=headers)
        try:
            with opener.open(req, timeout=120) as r:
                # If server does not honor Range, restart the download.
                if existing and getattr(r, "status", None) == 200:
                    existing = 0

                total = r.headers.get("Content-Length")
                total_int = (existing + int(total)) if total and total.isdigit() else None

                mode = "ab" if existing else "wb"
                with dest.open(mode) as f:
                    downloaded = existing
                    while True:
                        chunk = r.read(1024 * 1024)
                        if not chunk:
                            break
                        f.write(chunk)
                        downloaded += len(chunk)
                        if total_int:
                            pct = int(downloaded * 100 / total_int)
                            print(
                                f"\rDownloading cmdline-tools: {pct:3d}% ({downloaded}/{total_int})",
                                end="",
                            )
                        else:
                            print(f"\rDownloading cmdline-tools: {downloaded} bytes", end="")
            print()
            return
        except Exception as e:  # noqa: BLE001 - network stack raises varied exceptions
            if attempt == 5:
                raise
            wait_s = min(30, 2**attempt)
            print(f"\nDownload error ({type(e).__name__}): {e}. Retrying in {wait_s}s...")
            time.sleep(wait_s)


def _ensure_cmdline_tools(sdk_root: Path) -> Path:
    sdkmanager = sdk_root / "cmdline-tools" / "latest" / "bin" / "sdkmanager.bat"
    if sdkmanager.exists():
        return sdkmanager

    downloads = sdk_root / "_downloads"
    zip_path = downloads / "commandlinetools.zip"
    downloads.mkdir(parents=True, exist_ok=True)

    for attempt in (1, 2):
        if not zip_path.exists() or zip_path.stat().st_size < 10_000_000:
            print(f"Downloading: {CMDLINE_TOOLS_ZIP_URL}")
        _download(CMDLINE_TOOLS_ZIP_URL, zip_path)
        try:
            with zipfile.ZipFile(zip_path) as zf:
                # Validate zip integrity.
                if zf.testzip() is not None:
                    raise zipfile.BadZipFile("Zip integrity test failed")
            break
        except zipfile.BadZipFile:
            if attempt == 2:
                raise
            print("Detected a corrupt/partial cmdline-tools zip; re-downloading...")
            try:
                zip_path.unlink()
            except OSError:
                pass

    latest_dir = sdk_root / "cmdline-tools" / "latest"
    latest_dir.mkdir(parents=True, exist_ok=True)

    tmp_root = sdk_root / "_tmp"
    tmp_root.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="android-cmdline-tools-", dir=str(tmp_root)) as td:
        td_path = Path(td)
        with zipfile.ZipFile(zip_path) as zf:
            zf.extractall(td_path)

        inner = td_path / "cmdline-tools"
        if not inner.exists():
            raise RuntimeError("Unexpected cmdline-tools zip layout (missing cmdline-tools/ folder).")

        if latest_dir.exists():
            shutil.rmtree(latest_dir)
        shutil.copytree(inner, latest_dir)

    if not sdkmanager.exists():
        raise RuntimeError(f"sdkmanager not found after install: {sdkmanager}")
    return sdkmanager


def _run_sdkmanager(sdkmanager_bat: Path, sdk_root: Path, args: list[str], *, input_text: str | None = None) -> None:
    env = os.environ.copy()
    env.pop("JAVA_HOME", None)  # avoid stale JAVA_HOME breaking sdkmanager
    cmd = [str(sdkmanager_bat), f"--sdk_root={sdk_root}", *args]
    print("Running:", " ".join(cmd))
    p = subprocess.Popen(
        cmd,
        stdin=subprocess.PIPE if input_text is not None else None,
        stdout=sys.stdout,
        stderr=sys.stderr,
        env=env,
    )
    if input_text is not None:
        assert p.stdin is not None
        p.stdin.write(input_text.encode("utf-8", errors="ignore"))
        p.stdin.close()
    rc = p.wait()
    if rc != 0:
        raise RuntimeError(f"sdkmanager failed with exit code {rc}: {' '.join(cmd)}")


def _accept_licenses(sdkmanager_bat: Path, sdk_root: Path) -> None:
    # Feed a bunch of "y" answers for licenses.
    _run_sdkmanager(sdkmanager_bat, sdk_root, ["--licenses"], input_text=("y\n" * 200))


def main() -> int:
    home = Path.home()
    default_root = home / ".codex" / "memories" / "android-sdk"
    sdk_root = Path(os.environ.get("ANDROID_SDK_ROOT", str(default_root))).resolve()

    print(f"Android SDK root: {sdk_root}")
    sdk_root.mkdir(parents=True, exist_ok=True)

    sdkmanager_bat = _ensure_cmdline_tools(sdk_root)
    _accept_licenses(sdkmanager_bat, sdk_root)

    packages = [
        "platform-tools",
        "platforms;android-35",
        "build-tools;35.0.0",
    ]
    _run_sdkmanager(sdkmanager_bat, sdk_root, packages)

    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
