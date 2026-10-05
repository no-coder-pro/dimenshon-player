import os
import sys
import shutil
import subprocess
import time
from pathlib import Path

def print_banner(text):
    print("\n" + "=" * 60)
    print(f"  {text}")
    print("=" * 60)

def find_gradle_cmd(project_root):
    is_windows = os.name == 'nt'
    gradlew_name = "gradlew.bat" if is_windows else "gradlew"
    gradlew_path = project_root / gradlew_name

    if gradlew_path.exists():
        if not is_windows:
            os.chmod(gradlew_path, 0o755)
        return str(gradlew_path.resolve())

    # Check PATH
    gradle_in_path = shutil.which("gradle")
    if gradle_in_path:
        return gradle_in_path

    # Check Gradle user home wrapper dists on Windows
    user_home = Path.home()
    dists_dir = user_home / ".gradle" / "wrapper" / "dists"
    if dists_dir.exists():
        for bat in dists_dir.rglob("gradle.bat" if is_windows else "gradle"):
            if bat.is_file():
                return str(bat.resolve())

    return None

def safe_remove(path):
    if not path.exists():
        return
    try:
        if path.is_file() or path.is_symlink():
            path.unlink()
        elif path.is_dir():
            shutil.rmtree(path, ignore_errors=True)
    except Exception as e:
        print(f"  [Warning] Could not fully remove {path.name}: {e}")

def main():
    project_root = Path(__file__).resolve().parent
    os.chdir(project_root)

    print_banner("DIMEN SHON 8D - AUTOMATED APK BUILD & CLEANUP")
    print(f"Project Directory: {project_root}")

    gradle_cmd = find_gradle_cmd(project_root)
    if not gradle_cmd:
        print("\n[Error] Gradle or gradlew executable could not be found.")
        sys.exit(1)

    print(f"Using Gradle: {gradle_cmd}")

    build_type = "Release"
    task = "assembleRelease"
    if "--debug" in sys.argv:
        build_type = "Debug"
        task = "assembleDebug"

    print(f"\n[1/3] Building {build_type} APK with '{task}'...")
    start_time = time.time()

    cmd = [gradle_cmd, task]
    result = subprocess.run(cmd, cwd=project_root)

    if result.returncode != 0:
        print("\n[Error] Build failed! Check the Gradle error log above.")
        sys.exit(result.returncode)

    elapsed = time.time() - start_time
    print(f"\n[+] Build finished successfully in {elapsed:.1f}s!")

    # Locate generated APK
    apk_search_dir = project_root / "app" / "build" / "outputs" / "apk" / build_type.lower()
    found_apk = None
    if apk_search_dir.exists():
        for f in apk_search_dir.glob("*.apk"):
            found_apk = f
            break

    if not found_apk:
        # Fallback search anywhere in app/build
        for f in (project_root / "app" / "build").rglob("*.apk"):
            found_apk = f
            break

    if not found_apk or not found_apk.exists():
        print("\n[Error] Could not locate output APK file.")
        sys.exit(1)

    # Destination in root
    dest_apk_name = "dimen_shon.apk"
    dest_apk = project_root / dest_apk_name
    shutil.copy2(found_apk, dest_apk)
    apk_size_mb = dest_apk.stat().st_size / (1024 * 1024)

    print("\n[2/3] Output APK Saved:")
    print(f"  -> File : {dest_apk.name}")
    print(f"  -> Path : {dest_apk.resolve()}")
    print(f"  -> Size : {apk_size_mb:.2f} MB ({dest_apk.stat().st_size:,} bytes)")

    # Clean temporary files to keep repository pristine for GitHub
    print("\n[3/3] Cleaning up temporary build artifacts for GitHub...")

    # Stop Gradle daemons to release file locks on Windows
    subprocess.run([gradle_cmd, "--stop"], cwd=project_root, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(1)

    cleanup_targets = [
        project_root / "build",
        project_root / "app" / "build",
        project_root / ".gradle",
        project_root / ".cxx",
    ]

    for target in cleanup_targets:
        if target.exists():
            safe_remove(target)
            print(f"  - Removed: {target.name}")

    print("\n" + "=" * 60)
    print("  BUILD & CLEANUP COMPLETE!")
    print(f"  Target APK Ready: {dest_apk_name} ({apk_size_mb:.2f} MB)")
    print("  Repository is clean & ready for Git commit/push!")
    print("=" * 60 + "\n")

if __name__ == "__main__":
    main()
