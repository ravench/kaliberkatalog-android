"""Checks on tools/play_listing.py without the network: what it would write, and the signature of its token request."""
import base64
import os
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(__file__))
import play_listing  # noqa: E402


class PlayListingTests(unittest.TestCase):
    def test_every_language_in_store_goes_to_google_play(self):
        self.assertEqual(set(play_listing.LOCALES.values()), set(os.listdir(play_listing.STORE)))
        for lang in set(play_listing.LOCALES.values()):
            texts = play_listing.listing(lang)
            self.assertEqual(texts["title"], "Kaliberkatalog", lang)
            self.assertTrue(all(texts.values()) and all(t == t.strip() for t in texts.values()), lang)

    def test_screenshots_in_their_order_and_the_icon_and_feature_graphic_once(self):
        with tempfile.TemporaryDirectory() as screens:
            os.mkdir(os.path.join(screens, "en"))
            for name in ("2-calibre.png", "10-later.txt", "1-drawing.png"):
                open(os.path.join(screens, "en", name), "w").close()
            names = {kind: [os.path.basename(p) for p in paths]
                     for kind, paths in play_listing.images(screens, "en-GB", "en-GB").items()}
            self.assertEqual(names, {"phoneScreenshots": ["1-drawing.png", "2-calibre.png"], "icon": ["icon.png"],
                                     "featureGraphic": ["feature-graphic.png"]})
            self.assertEqual(list(play_listing.images(screens, "en-US", "de-DE")), ["phoneScreenshots"])

    def test_only_what_differs_is_written(self):
        want = {"title": "Kaliberkatalog", "shortDescription": "Short", "fullDescription": "Full"}
        self.assertEqual(play_listing.text_changes({"language": "de-DE", **want}, want), [])
        self.assertEqual(play_listing.text_changes({**want, "fullDescription": "Old"}, want), ["fullDescription"])
        self.assertEqual(play_listing.text_changes({}, want), list(play_listing.FIELDS))
        with tempfile.TemporaryDirectory() as tmp:
            a, b = os.path.join(tmp, "a.png"), os.path.join(tmp, "b.png")
            for path in (a, b):
                with open(path, "w") as f:
                    f.write(path)
            have = [{"id": "1", "sha256": play_listing.sha256(a)}, {"id": "2", "sha256": play_listing.sha256(b)}]
            self.assertTrue(play_listing.same_images(have, [a, b]))
            self.assertFalse(play_listing.same_images(have, [b, a]))  # the order is the listing's
            self.assertFalse(play_listing.same_images(have[:1], [a, b]))
            self.assertTrue(play_listing.same_images([], []))

    def test_the_token_request_is_signed_with_the_service_accounts_key(self):
        account = {"client_email": "play-listing@api-project.iam.gserviceaccount.com",
                   "token_uri": "https://oauth2.googleapis.com/token"}
        self.assertEqual(play_listing.claims(account, 1000),
                         {"iss": account["client_email"], "scope": "https://www.googleapis.com/auth/androidpublisher",
                          "aud": account["token_uri"], "iat": 1000, "exp": 4600})
        with tempfile.TemporaryDirectory() as tmp:
            key, public, signature = (os.path.join(tmp, n) for n in ("key.pem", "public.pem", "signature"))
            subprocess.run(["openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048", "-out", key],
                           check=True, capture_output=True)
            subprocess.run(["openssl", "pkey", "-in", key, "-pubout", "-out", public], check=True, capture_output=True)
            with open(key) as f:
                signed = play_listing.sign("header.claims", f.read())
            with open(signature, "wb") as f:
                f.write(signed)
            # RS256: a PKCS #1 v1.5 signature over SHA-256, which the public key verifies.
            verified = subprocess.run(["openssl", "dgst", "-sha256", "-verify", public, "-signature", signature],
                                      input=b"header.claims", capture_output=True)
            self.assertEqual(verified.stdout.strip(), b"Verified OK")
            self.assertEqual(len(signed), 256)
            self.assertNotIn("=", play_listing.b64url(signed))
            self.assertEqual(base64.urlsafe_b64decode(play_listing.b64url(signed) + "=="), signed)

    def test_the_workflow_runs_it_from_main_with_the_secret_that_contributing_md_sets(self):
        def read(*path):
            with open(os.path.join(play_listing.ROOT, *path), encoding="utf-8") as f:
                return f.read()
        workflow = read(".github", "workflows", "store-screens.yml")
        step = workflow.split("- name: Google Play listing", 1)[1]
        self.assertIn("PLAY_SERVICE_ACCOUNT_JSON: ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}", step)
        self.assertIn("gh secret set PLAY_SERVICE_ACCOUNT_JSON ", read("CONTRIBUTING.md"))
        self.assertIn('[ "$GITHUB_REF" = refs/heads/main ] ||', step)
        self.assertIn("python3 tools/play_listing.py app/build/store-screens $APPLY", step)


if __name__ == "__main__":
    unittest.main()
