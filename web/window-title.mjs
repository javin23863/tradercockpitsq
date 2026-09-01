function titlePart(value) {
  return String(value || "")
    .replaceAll("-", " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function productWindowTitle(shell) {
  if (!shell || shell.getAttribute("data-product-shell") !== "tradercockpit-desktop") {
    return "TraderCockpit";
  }
  if (shell.getAttribute("data-surface-id") !== "research") return "TraderCockpit";
  const stage = titlePart(shell.getAttribute("data-research-stage-id"));
  const tab = titlePart(shell.getAttribute("data-research-tab-id"));
  const suffix = [stage, tab].filter(Boolean).join(" / ");
  return suffix ? `TraderCockpit — Research / ${suffix}` : "TraderCockpit — Research";
}

export function synchronizeProductWindowTitle(documentLike = globalThis.document) {
  if (!documentLike) return "TraderCockpit";
  const title = productWindowTitle(documentLike.querySelector?.("[data-product-shell]") || null);
  if (documentLike.title !== title) documentLike.title = title;
  return title;
}

if (typeof document !== "undefined") {
  const observer = new MutationObserver(() => synchronizeProductWindowTitle());
  observer.observe(document.documentElement, {
    attributes: true,
    childList: true,
    subtree: true,
    attributeFilter: ["data-product-shell", "data-surface-id", "data-research-stage-id", "data-research-tab-id"],
  });
  synchronizeProductWindowTitle();
}
