"""Checks that the release signing in app/build.gradle.kts, release.yml, build.yml and CONTRIBUTING.md fit together."""
import os
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read(*path):
    with open(os.path.join(ROOT, *path), encoding="utf-8") as f:
        return f.read()


class ReleaseTests(unittest.TestCase):
    def test_the_key_and_its_password_have_the_same_names_everywhere(self):
        gradle = read("app", "build.gradle.kts")
        release = read(".github", "workflows", "release.yml")
        build = read(".github", "workflows", "build.yml").split("\n  app:", 1)[1]
        guide = read("CONTRIBUTING.md")
        for variable in ("UPLOAD_KEYSTORE", "UPLOAD_KEYSTORE_PASSWORD"):
            self.assertIn(f'providers.environmentVariable("{variable}")', gradle)
            self.assertIn(f"{variable}=", build)
        self.assertIn('export UPLOAD_KEYSTORE="$RUNNER_TEMP/upload.jks"', release)
        for secret in ("UPLOAD_KEYSTORE_BASE64", "UPLOAD_KEYSTORE_PASSWORD"):
            self.assertIn(f"${{{{ secrets.{secret} }}}}", release)
            self.assertIn(f"gh secret set {secret} ", guide)
        self.assertIn('keyAlias = "upload"', gradle)
        self.assertIn("-alias upload", build)
        self.assertIn("-alias upload", guide)

    def test_both_workflows_build_the_bundle_and_check_its_signature(self):
        for workflow in ("release.yml", "build.yml"):
            text = read(".github", "workflows", workflow)
            self.assertIn(":app:bundleRelease", text, workflow)
            # jarsigner also exits 0 for an unsigned file.
            self.assertIn("jarsigner -verify app/build/outputs/bundle/release/app-release.aab | grep -x 'jar verified.'",
                          text, workflow)
        release = read(".github", "workflows", "release.yml")
        self.assertIn("on: workflow_dispatch", release)
        self.assertIn('[ "$GITHUB_REF" = refs/heads/main ] ||', release)


if __name__ == "__main__":
    unittest.main()
