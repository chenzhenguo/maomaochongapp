from __future__ import annotations

import io
import os
import re
import sys
import zipfile
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, build_opener, ProxyHandler


ROOT = Path(__file__).resolve().parents[1]
WRAPPER_PROPS = ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties"
WRAPPER_JAR = ROOT / "gradle" / "wrapper" / "gradle-wrapper.jar"


def _read_distribution_url() -> str:
    if not WRAPPER_PROPS.exists():
        raise FileNotFoundError(f"Missing {WRAPPER_PROPS}")

    text = WRAPPER_PROPS.read_text(encoding="utf-8", errors="replace")
    m = re.search(r"(?m)^\s*distributionUrl\s*=\s*(.+?)\s*$", text)
    if not m:
        raise RuntimeError(f"distributionUrl not found in {WRAPPER_PROPS}")

    url = m.group(1).strip()
    return url.replace("\\:", ":")


def _extract_gradle_version(dist_url: str) -> str:
    m = re.search(r"gradle-([0-9]+(?:\.[0-9]+){1,2})-(?:bin|all)\.zip$", dist_url)
    if not m:
        raise RuntimeError(f"Could not parse Gradle version from distributionUrl: {dist_url}")
    return m.group(1)


def _download_wrapper_jar_from_gradle_github(version: str) -> bytes:
    parts = version.split(".")
    tag_candidates: list[str] = []
    if len(parts) == 2:
        tag_candidates.extend([f"v{version}.0", f"v{version}"])
    else:
        tag_candidates.append(f"v{version}")

    opener = build_opener(ProxyHandler({}))  # ignore ALL_PROXY/HTTP(S)_PROXY in sandbox
    last_err: Exception | None = None
    for tag in tag_candidates:
        url = f"https://raw.githubusercontent.com/gradle/gradle/{tag}/gradle/wrapper/gradle-wrapper.jar"
        try:
            with opener.open(Request(url, headers={"User-Agent": "maomaochongapp-wrapper-fix/1.0"}), timeout=60) as r:
                data = r.read()
            with zipfile.ZipFile(io.BytesIO(data)) as jar_zf:
                if "org/gradle/wrapper/GradleWrapperMain.class" not in jar_zf.namelist():
                    raise RuntimeError(f"Downloaded wrapper jar from {url} is missing GradleWrapperMain")
            return data
        except HTTPError as e:
            last_err = e
            if e.code == 404:
                continue
            raise
        except Exception as e:  # noqa: BLE001 - keep a single failure to report at the end
            last_err = e
            continue

    raise RuntimeError(f"Failed to download gradle-wrapper.jar for {version} from GitHub ({tag_candidates}).") from last_err


def _download(url: str, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    opener = build_opener(ProxyHandler({}))  # ignore ALL_PROXY/HTTP(S)_PROXY in sandbox
    req = Request(url, headers={"User-Agent": "maomaochongapp-wrapper-fix/1.0"})

    with opener.open(req, timeout=60) as r:
        total = r.headers.get("Content-Length")
        total_int = int(total) if total and total.isdigit() else None
        # Write directly to dest. On some locked-down Windows setups the final rename can fail
        # even when the download succeeded, so we rely on zip validation + re-download instead.
        with dest.open("wb") as f:
            downloaded = 0
            while True:
                chunk = r.read(1024 * 1024)
                if not chunk:
                    break
                f.write(chunk)
                downloaded += len(chunk)
                if total_int:
                    pct = int(downloaded * 100 / total_int)
                    print(f"\rDownloading Gradle dist: {pct:3d}% ({downloaded}/{total_int} bytes)", end="")
                else:
                    print(f"\rDownloading Gradle dist: {downloaded} bytes", end="")
        print()


def _find_wrapper_main_jar_bytes(dist_zip: Path) -> bytes:
    with zipfile.ZipFile(dist_zip) as zf:
        names = zf.namelist()
        plugin = next((n for n in names if re.search(r"lib/plugins/gradle-wrapper-[^/]+\.jar$", n)), None)
        shared = next((n for n in names if re.search(r"lib/gradle-wrapper-shared-[^/]+\.jar$", n)), None)
        cli = next((n for n in names if re.search(r"lib/gradle-cli-[^/]+\.jar$", n)), None)
        if not plugin or not shared or not cli:
            raise RuntimeError("Could not locate required wrapper jars (plugin/shared/cli) in the distribution zip.")

        plugin_bytes = zf.read(plugin)
        shared_bytes = zf.read(shared)
        cli_bytes = zf.read(cli)

    out = io.BytesIO()
    seen: set[str] = set()
    with zipfile.ZipFile(out, "w", compression=zipfile.ZIP_DEFLATED) as out_zf:
        # Wrapper is launched via `-classpath ... org.gradle.wrapper.GradleWrapperMain`,
        # so the manifest is not used, but keeping a minimal one is conventional.
        out_zf.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\n")
        seen.add("META-INF/MANIFEST.MF")

        def copy_from(jar_bytes: bytes) -> None:
            with zipfile.ZipFile(io.BytesIO(jar_bytes)) as in_zf:
                for info in in_zf.infolist():
                    name = info.filename
                    if name.endswith("/"):
                        continue
                    upper = name.upper()
                    if upper == "META-INF/MANIFEST.MF":
                        continue
                    if upper.startswith("META-INF/") and upper.endswith((".SF", ".DSA", ".RSA", ".EC")):
                        continue
                    if name in seen:
                        continue
                    out_zf.writestr(name, in_zf.read(name))
                    seen.add(name)

        # Shared first so plugin can override if needed.
        copy_from(shared_bytes)
        copy_from(cli_bytes)
        copy_from(plugin_bytes)

    jar_bytes = out.getvalue()
    with zipfile.ZipFile(io.BytesIO(jar_bytes)) as jar_zf:
        required = [
            "org/gradle/wrapper/GradleWrapperMain.class",
            "org/gradle/wrapper/IDownload.class",
            "org/gradle/cli/CommandLineParser.class",
        ]
        missing = [p for p in required if p not in jar_zf.namelist()]
        if missing:
            raise RuntimeError(f"Built wrapper jar is missing: {', '.join(missing)}")

    return jar_bytes


def main() -> int:
    dist_url = _read_distribution_url()
    version = _extract_gradle_version(dist_url)
    jar_bytes = _download_wrapper_jar_from_gradle_github(version)
    WRAPPER_JAR.write_bytes(jar_bytes)
    print(f"Updated: {WRAPPER_JAR}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
