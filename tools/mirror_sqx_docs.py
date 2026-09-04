#!/usr/bin/env python3
"""Mirror the official StrategyQuant X documentation into docs/sqx-official-docs/ as Markdown.

Developer/documentation tool only; never imported by product code.

Usage:
    python tools/mirror_sqx_docs.py [docs/sqx-official-docs]

Requires network access to strategyquant.com and the optional packages
``beautifulsoup4``, ``lxml`` and ``markdownify`` (not part of the product dependencies):

    pip install beautifulsoup4 lxml markdownify

The tool rewrites every page under the output folder plus ``README.md`` and ``manifest.json``.
It does not touch ``SQX_PROGRAM_GUIDE.md`` or ``digests/``; review those by hand after a refresh.
"""
import concurrent.futures as cf
import datetime as dt
import hashlib
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

from bs4 import BeautifulSoup
from markdownify import MarkdownConverter

OUT = Path(sys.argv[1] if len(sys.argv) > 1 else "/workspace/docs/sqx-official-docs")
BASE = "https://strategyquant.com"
SEED = f"{BASE}/doc/strategyquant/introduction/"
PRODUCTS = {
    "strategyquant": "StrategyQuant X",
    "cli-command-line": "CLI (command line)",
    "programming-for-sq": "Programming for StrategyQuant X",
    "quantdatamanager": "QuantDataManager",
}
ALLOWED = re.compile(r"^https://strategyquant\.com/doc/(" + "|".join(map(re.escape, PRODUCTS)) + r")/[^?#]*$")
UA = {"User-Agent": "Mozilla/5.0 (X11; Linux x86_64) TraderCockpit-docs-mirror/1.0"}
FETCHED = dt.date.today().isoformat()


def fetch(url, tries=4):
    for i in range(tries):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
                return r.read().decode("utf-8", "ignore"), r.geturl()
        except Exception as e:  # noqa: BLE001
            if i == tries - 1:
                raise
            time.sleep(2 * (i + 1))


def norm(url):
    url = url.split("#")[0].split("?")[0]
    if not url.endswith("/"):
        url += "/"
    return url


def slug_of(url):
    seg = urllib.parse.unquote(norm(url).rstrip("/").rsplit("/", 1)[-1])
    seg = re.sub(r"[^a-z0-9]+", "-", seg.lower()).strip("-")
    return seg or "index"


def product_of(url):
    m = re.match(r"https://strategyquant\.com/doc/([^/]+)/", url)
    return m.group(1) if m else None


class Conv(MarkdownConverter):
    def convert_img(self, el, text, parent_tags):
        src = el.get("data-src") or el.get("src") or ""
        if src.startswith("data:"):
            return ""
        src = urllib.parse.urljoin(BASE, src)
        alt = (el.get("alt") or "").strip().replace("\n", " ")
        return f"![{alt}]({src})"

    def convert_pre(self, el, text, parent_tags):
        code = el.get_text()
        return f"\n```\n{code.rstrip()}\n```\n"


def parse_nav(html):
    """Return ordered tree: [(product_key, product_name, [(section_title, [(page_title, url)])])]."""
    soup = BeautifulSoup(html, "lxml")
    tree = []
    for item in soup.select("ul.sidebar-nav__list > li.sidebar-nav-item"):
        link = item.select_one("a.sidebar-nav-item__link")
        if not link:
            continue
        prod_url = norm(link.get("href", ""))
        key = product_of(prod_url)
        if key not in PRODUCTS:
            continue
        sections = []
        for wrap in item.select("div.sidebar-nav-item__wrap"):
            title_el = wrap.select_one(".sidebar-nav-item__title a")
            title = title_el.get_text(" ", strip=True) if title_el else "Untitled"
            pages = []
            for a in wrap.select("ul.ul-arrow li a"):
                href = a.get("href", "")
                if ALLOWED.match(href.split("#")[0].split("?")[0]):
                    pages.append((a.get_text(" ", strip=True), norm(href)))
            sections.append((title, pages))
        tree.append((key, PRODUCTS[key], sections))
    return tree


def extract(html, url):
    soup = BeautifulSoup(html, "lxml")
    h1 = soup.select_one(".documentation h1, h1.documentation__title, h1")
    title = h1.get_text(" ", strip=True) if h1 else slug_of(url)
    body = soup.select_one("section.sanitized-content") or soup.select_one(".documentation__content")
    if body is None:
        return title, "", []
    for junk in body.select("script, style, noscript, iframe, form, .wpd-, #comments, .sharedaddy"):
        junk.decompose()
    for a in body.select("a[href]"):
        a["href"] = urllib.parse.urljoin(url, a["href"])
    links = [norm(a["href"]) for a in body.select("a[href]") if ALLOWED.match(a["href"].split("#")[0].split("?")[0])]
    # crumbs
    crumbs = [c.get_text(" ", strip=True) for c in soup.select("nav.breadcrumbs a, nav.breadcrumbs li")]
    md = Conv(heading_style="ATX", bullets="-", strip=["span"]).convert_soup(body)
    md = re.sub(r"(?m)^#{1,6}\s*$\n?", "", md)
    md = re.sub(r"\n{3,}", "\n\n", md).strip()
    return title, md, links


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    intro_html, _ = fetch(SEED)
    tree = [n for n in parse_nav(intro_html) if n[2]]
    have = {k for k, _n, _s in tree}
    landing = {
        "cli-command-line": f"{BASE}/doc/cli-command-line/introduction-to-cli/",
        "programming-for-sq": f"{BASE}/doc/programming-for-sq/introduction-2/",
        "quantdatamanager": f"{BASE}/doc/quantdatamanager/introduction-to-qdm/",
    }
    for key in PRODUCTS:
        if key in have:
            continue
        html, _ = fetch(landing[key])
        for node in parse_nav(html):
            if node[0] == key and node[2] and node[0] not in have:
                tree.append(node)
                have.add(key)
    nav_pages = {}  # url -> (product, section_index, section_title, page_title)
    for key, _pname, sections in tree:
        for si, (stitle, pages) in enumerate(sections, 1):
            for ptitle, purl in pages:
                nav_pages.setdefault(purl, (key, si, stitle, ptitle))
    print(f"nav pages: {len(nav_pages)}", file=sys.stderr)

    # sitemap seeds
    seeds = set(nav_pages)
    try:
        sm, _ = fetch(f"{BASE}/doc-sitemap.xml")
        for loc in re.findall(r"<loc>(.*?)</loc>", sm):
            if ALLOWED.match(loc):
                seeds.add(norm(loc))
    except Exception as e:  # noqa: BLE001
        print("sitemap failed", e, file=sys.stderr)

    pages = {}  # url -> dict
    queue = list(seeds)
    seen = set(queue)

    def work(url):
        try:
            html, final = fetch(url)
        except Exception as e:  # noqa: BLE001
            return url, None, str(e)
        title, md, links = extract(html, url)
        return url, {"title": title, "md": md, "links": links, "final": norm(final)}, None

    while queue:
        batch, queue = queue[:8], queue[8:]
        with cf.ThreadPoolExecutor(max_workers=4) as ex:
            for url, rec, err in ex.map(work, batch):
                if err:
                    print("ERR", url, err, file=sys.stderr)
                    pages[url] = {"title": slug_of(url), "md": "", "links": [], "error": err}
                    continue
                if url.endswith("/feed/") or not rec["md"]:
                    if not rec["md"]:
                        print("EMPTY", url, file=sys.stderr)
                pages[url] = rec
                for l in rec["links"]:
                    if l.endswith("/feed/"):
                        continue
                    if l not in seen:
                        seen.add(l)
                        queue.append(l)
        time.sleep(0.2)
        print(f"fetched {len(pages)} queued {len(queue)}", file=sys.stderr)

    # assign paths
    sec_dirs = {}
    for key, _pname, sections in tree:
        for si, (stitle, _pages) in enumerate(sections, 1):
            sec_dirs[(key, si)] = f"{si:02d}-{re.sub(r'[^a-z0-9]+', '-', stitle.lower()).strip('-')}"
    path_of = {}
    for url, rec in pages.items():
        if url.endswith("/feed/") or rec.get("error") or not rec["md"]:
            continue
        key = product_of(url)
        if url in nav_pages:
            k, si, _st, _pt = nav_pages[url]
            d = f"{k}/{sec_dirs[(k, si)]}"
        else:
            d = f"{key}/zz-unlisted"
        path_of[url] = f"{d}/{slug_of(url)}.md"

    # write
    manifest = []
    for url, rel in sorted(path_of.items(), key=lambda kv: kv[1]):
        rec = pages[url]
        md = rec["md"]
        # rewrite internal links to local relative paths
        def repl(m):
            target = norm(m.group(2))
            if target in path_of:
                relp = os.path.relpath(path_of[target], str(Path(rel).parent))
                return f"{m.group(1)}({relp})"
            return m.group(0)
        md = re.sub(r"(\[[^\]]*\])\((https://strategyquant\.com/doc/[^)\s]+)\)", repl, md)
        key = product_of(url)
        crumb = PRODUCTS[key]
        if url in nav_pages:
            crumb += " › " + nav_pages[url][2]
        header = (
            f"# {rec['title']}\n\n"
            f"- Source: <{url}>\n"
            f"- Section: {crumb}\n"
            f"- Fetched: {FETCHED}\n\n---\n\n"
        )
        content = header + md + "\n"
        p = OUT / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(content, encoding="utf-8")
        manifest.append({
            "url": url,
            "path": rel,
            "title": rec["title"],
            "sha256": hashlib.sha256(content.encode("utf-8")).hexdigest(),
            "fetched": FETCHED,
        })

    (OUT / "manifest.json").write_text(json.dumps({"fetched": FETCHED, "pages": manifest}, indent=2), encoding="utf-8")
    json.dump({"tree": tree, "nav_pages": nav_pages, "path_of": path_of}, open("/tmp/sqx_nav.json", "w"), indent=1)

    # index
    lines = [
        "# StrategyQuant X official documentation mirror",
        "",
        f"Markdown mirror of <https://strategyquant.com/doc/> fetched {FETCHED}. Reference material for agents and reviewers; "
        "not a roadmap, not a runtime dependency, and not a substitute for exercising the installed producer. "
        "Page order follows the official documentation sidebar. Images stay as remote links to strategyquant.com.",
        "",
        "Start with `SQX_PROGRAM_GUIDE.md` (synthesized operating guide with a concept-to-TraderCockpit map). "
        "`digests/` holds section digests that cite these pages. `manifest.json` records the exact source URL and SHA-256 per page. "
        "Refresh with `python tools/mirror_sqx_docs.py`.",
        "",
    ]
    for key, pname, sections in tree:
        lines += [f"## {pname}", ""]
        for si, (stitle, spages) in enumerate(sections, 1):
            lines += [f"### {stitle}", ""]
            for ptitle, purl in spages:
                if purl in path_of:
                    lines.append(f"- [{ptitle}]({path_of[purl]})")
                else:
                    lines.append(f"- {ptitle} — <{purl}> (not mirrored)")
            lines.append("")
        unlisted = sorted((p for u, p in path_of.items() if product_of(u) == key and u not in nav_pages))
        if unlisted:
            lines += ["### Pages linked from the documentation but not in the sidebar", ""]
            for p in unlisted:
                lines.append(f"- [{pages[[u for u, pp in path_of.items() if pp == p][0]]['title']}]({p})")
            lines.append("")
    (OUT / "README.md").write_text("\n".join(lines), encoding="utf-8")
    print(f"wrote {len(manifest)} pages", file=sys.stderr)


if __name__ == "__main__":
    main()
