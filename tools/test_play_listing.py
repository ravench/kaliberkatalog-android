"""Checks on tools/play_listing.py without the network: what it would write, and the signature of its token request."""
import base64
import contextlib
import hashlib
import io
import os
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, os.path.dirname(__file__))
import play_listing  # noqa: E402


class FakePlay:
    """The Google Play Developer API's edits as play_listing.call uses them, with the listing in memory."""

    def __init__(self, listings):
        self.listings, self.images, self.log, self.asks_for_the_flag = listings, {}, [], False

    def __call__(self, access, method, url, body=None, data=None, content_type="application/json"):
        path = url.split(play_listing.PACKAGE)[1]
        self.log.append(f"{method} {path}")
        if path == "/edits":
            return {"id": "7"}
        if path == "/edits/7/details":
            return {"defaultLanguage": "de-DE"}
        if path == "/edits/7/listings":
            return {"listings": list(self.listings.values())}
        if path.startswith("/edits/7:commit"):
            if self.asks_for_the_flag and "changesNotSentForReview=true" not in path:
                raise play_listing.ApiError("POST /edits/7:commit: 400 Please set the query parameter "
                                            "changesNotSentForReview to true.")
            return {}
        if path == "/edits/7":
            return {}
        locale, *kind = path.split("?")[0].split("/listings/")[1].split("/")
        if not kind:
            self.listings[locale] = body
            return body
        assert locale in self.listings, "images for a language without a listing"
        images = self.images.setdefault((locale, kind[0]), [])
        if method == "GET":
            return {"images": list(images)} if images else {}
        if method == "DELETE":
            images.clear()
        else:
            images.append({"sha256": hashlib.sha256(data).hexdigest()})
        return {}


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

    def test_a_dry_run_writes_nothing_and_an_upload_only_what_differs(self):
        with tempfile.TemporaryDirectory() as screens:
            for lang in set(play_listing.LOCALES.values()):
                os.mkdir(os.path.join(screens, lang))
                for name in ("1-drawing.png", "2-calibre.png"):
                    with open(os.path.join(screens, lang, name), "w") as f:
                        f.write(lang + name)
            for name in ("icon.png", "feature-graphic.png"):
                with open(os.path.join(screens, name), "w") as f:
                    f.write(name)
            # A new app: the default language has only its title, and a video from the Play Console.
            play = FakePlay({"de-DE": {"language": "de-DE", "title": "Kaliberkatalog", "video": "https://youtu.be/x"}})

            def run(*flags):
                play.log.clear()
                with mock.patch.object(play_listing, "call", play), \
                        mock.patch.object(play_listing, "token", lambda account: "token"), \
                        mock.patch.dict(os.environ, {"PLAY_SERVICE_ACCOUNT_JSON": "{}"}), \
                        contextlib.redirect_stdout(io.StringIO()) as out:
                    play_listing.main(["play_listing.py", screens, *flags])
                return out.getvalue(), [call for call in play.log if not call.startswith("GET ")]

            nothing = ["POST /edits", "DELETE /edits/7"]
            out, writes = run()
            self.assertIn("- en-GB: title, shortDescription, fullDescription (new language)", out)
            self.assertIn("14 changes (dry run, nothing written)", out)
            self.assertEqual(writes, nothing)

            out, writes = run("--apply")
            self.assertIn("\n14 changes\n", out)
            self.assertEqual(sum(call.startswith("PUT ") for call in writes), 6)
            self.assertEqual(sum("uploadType=media" in call for call in writes), 6 * 2 + 2)
            self.assertEqual(writes[-1], "POST /edits/7:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW")
            self.assertEqual(play.listings["de-DE"]["video"], "https://youtu.be/x")
            self.assertEqual(play.listings["fr-FR"], {"language": "fr-FR", **play_listing.listing("fr")})
            english = [os.path.join(screens, "en", name) for name in ("1-drawing.png", "2-calibre.png")]
            self.assertTrue(play_listing.same_images(play.images["en-GB", "phoneScreenshots"], english))
            self.assertEqual(sorted(kind for locale, kind in play.images if locale == "en-US"), ["phoneScreenshots"])

            self.assertEqual(run("--apply"), ("\n0 changes\n", nothing))

            # A new screenshot replaces the language's old ones. Changes waiting in the Play Console to be sent for
            # review: the commit joins them.
            with open(english[1], "w") as f:
                f.write("a new screenshot")
            play.listings["it-IT"]["title"] = "Old"
            play.asks_for_the_flag = True
            out, writes = run("--apply")
            self.assertTrue(play_listing.same_images(play.images["en-GB", "phoneScreenshots"], english))
            self.assertEqual(writes[-2:], ["POST /edits/7:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW",
                                           "POST /edits/7:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW"
                                           "&changesNotSentForReview=true"])
            self.assertEqual(play.listings["it-IT"]["title"], "Kaliberkatalog")

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
