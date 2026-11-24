"""The Google Play store listing: the texts in store/<lang>/ and StoreScreensTest's images, through the Google Play
Developer API (.github/workflows/store-screens.yml, Run workflow with «Google Play listing»).

    python3 tools/play_listing.py app/build/store-screens [--apply]

Needs PLAY_SERVICE_ACCOUNT_JSON, the JSON key of a service account that the Play Console has invited with «Manage
store presence» (CONTRIBUTING.md). Without --apply it only lists what would change. Writes, in one edit:
- each language's title, short and full description;
- its phone screenshots, from its language's folder, in their numbered order;
- the icon and the feature graphic, on the default language's listing, which the other languages show.
The API has no field for the feature graphic's alt text, the App content declarations or the category: those stay in
the Play Console. The API finds the app by its package name but can't create it: that's done in the Play Console,
which needs no uploaded bundle for it.
"""
import argparse
import base64
import hashlib
import json
import os
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STORE = os.path.join(ROOT, "store")
PACKAGE = "ch.giotta.kaliberkatalog"
API = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}"
UPLOAD = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PACKAGE}"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"
# Google Play's languages for the folders in store/ (CONTRIBUTING.md): English goes to the US and the UK.
LOCALES = {"de-DE": "de", "en-US": "en", "en-GB": "en", "fr-FR": "fr", "it-IT": "it", "es-ES": "es"}
FIELDS = {"title": "title", "shortDescription": "short_description", "fullDescription": "full_description"}


class ApiError(Exception):
    pass


def listing(lang, store=STORE):
    """A language's texts as the API's Listing has them, from store/<lang>/<field>.txt."""
    out = {}
    for api, field in FIELDS.items():
        with open(os.path.join(store, lang, field + ".txt"), encoding="utf-8") as f:
            out[api] = f.read().strip()
    return out


def images(screens, locale, default):
    """A language's image files by the API's image type: its screenshots, and on the default language also the icon
    and the feature graphic."""
    folder = os.path.join(screens, LOCALES[locale])
    out = {"phoneScreenshots": sorted(os.path.join(folder, f) for f in os.listdir(folder) if f.endswith(".png"))}
    if locale == default:
        out["icon"] = [os.path.join(screens, "icon.png")]
        out["featureGraphic"] = [os.path.join(screens, "feature-graphic.png")]
    return out


def text_changes(have, want):
    """The fields of `want` that differ from the Play Console's listing `have` ({} for a language it lacks)."""
    return [field for field in FIELDS if have.get(field) != want[field]]


def sha256(path):
    with open(path, "rb") as f:
        return hashlib.sha256(f.read()).hexdigest()


def same_images(have, paths):
    """Whether the Play Console has exactly these files, in this order (`have`: the API's images)."""
    return [image.get("sha256") for image in have] == [sha256(path) for path in paths]


def claims(account, now):
    """What the service account asks Google's token endpoint for: access to this API, for an hour."""
    return {"iss": account["client_email"], "scope": SCOPE, "aud": account["token_uri"], "iat": now, "exp": now + 3600}


def b64url(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def sign(signed, private_key):
    """An RS256 signature of `signed` with the service account's PEM key; openssl signs, as for Kaliberfinder's App
    Store Connect token."""
    with tempfile.TemporaryDirectory() as tmp:
        key = os.path.join(tmp, "key.pem")
        with open(os.open(key, os.O_WRONLY | os.O_CREAT, 0o600), "w") as f:
            f.write(private_key)
        return subprocess.run(["openssl", "dgst", "-sha256", "-sign", key], input=signed.encode(),
                              capture_output=True, check=True).stdout


def token(account, now=None):
    """An access token for the API, in exchange for a JWT signed with the service account's key."""
    header = b64url(json.dumps({"alg": "RS256", "typ": "JWT"}).encode())
    signed = f"{header}.{b64url(json.dumps(claims(account, int(now or time.time()))).encode())}"
    body = urllib.parse.urlencode({"grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
                                   "assertion": f"{signed}.{b64url(sign(signed, account['private_key']))}"})
    try:
        with urllib.request.urlopen(account["token_uri"], data=body.encode(), timeout=60) as response:
            return json.load(response)["access_token"]
    except urllib.error.HTTPError as e:  # Google says why, e.g. a deleted key
        raise ApiError(f"Access token: {e.code} {e.read().decode()}") from None


def call(access, method, url, body=None, data=None, content_type="application/json"):
    if body is not None:
        data = json.dumps(body).encode()
    request = urllib.request.Request(url, method=method, data=data,
                                     headers={"Authorization": f"Bearer {access}", "Content-Type": content_type})
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            raw = response.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raise ApiError(f"{method} {url.split(PACKAGE)[-1] or url}: {e.code} {e.read().decode()}") from None


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("screens", help="StoreScreensTest's images, app/build/store-screens")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args(argv[1:])
    wanted = {locale: listing(lang) for locale, lang in LOCALES.items()}

    access = token(json.loads(os.environ["PLAY_SERVICE_ACCOUNT_JSON"]))
    edit = call(access, "POST", f"{API}/edits", {})["id"]
    base = f"{API}/edits/{edit}"
    default = call(access, "GET", f"{base}/details")["defaultLanguage"]
    if default not in LOCALES:
        sys.exit(f"The listing's default language is {default}, none of {', '.join(LOCALES)}")
    have = {each["language"]: each for each in call(access, "GET", f"{base}/listings").get("listings", [])}
    count = 0
    for locale, want in wanted.items():
        changed = text_changes(have.get(locale, {}), want)
        if changed:
            print(f"- {locale}: {', '.join(changed)}" + ("" if locale in have else " (new language)"))
            count += 1
            if args.apply:
                # The whole listing: what the Play Console set beside the texts, such as a video, stays.
                call(access, "PUT", f"{base}/listings/{locale}", {**have.get(locale, {}), "language": locale, **want})
        for kind, paths in images(args.screens, locale, default).items():
            existing = []  # a language new to the listing has no images yet
            if locale in have:
                existing = call(access, "GET", f"{base}/listings/{locale}/{kind}").get("images", [])
            if same_images(existing, paths):
                continue
            print(f"- {locale}: {kind}, {len(paths)} in place of {len(existing)}")
            count += 1
            if args.apply:
                call(access, "DELETE", f"{base}/listings/{locale}/{kind}")
                for path in paths:
                    with open(path, "rb") as f:
                        call(access, "POST", f"{UPLOAD}/edits/{edit}/listings/{locale}/{kind}?uploadType=media",
                             data=f.read(), content_type="image/png")
    if args.apply and count:
        # Not the default, which would cancel a review under way and send it again with these changes.
        commit = f"{base}:commit?changesInReviewBehavior=ERROR_IF_IN_REVIEW"
        try:
            call(access, "POST", commit)
        except ApiError as e:
            if "changesNotSentForReview" not in str(e):
                raise
            # The app's changes wait to be sent for review in the Play Console: these join them there.
            call(access, "POST", commit + "&changesNotSentForReview=true")
    else:
        call(access, "DELETE", base)  # nothing to write
    print(f"\n{count} changes" + ("" if args.apply else " (dry run, nothing written)"))


if __name__ == "__main__":
    try:
        main(sys.argv)
    except ApiError as e:
        sys.exit(str(e))
