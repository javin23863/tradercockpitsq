// Shared read of the exact native Builder configuration (`/api/sqx-builder-config`),
// validated through the existing per-subtree parsers. Node helpers only read native tags,
// attributes and text; no semantics are assigned here.

import { searchConfigurationFromBuilderConfig, specificationFromBuilderConfig } from "./research-specification.mjs";
import { blocksConfigurationFromBuilderConfig } from "./research-blocks.mjs";
import { rankingsConfigurationFromBuilderConfig } from "./research-rankings.mjs";
import { crossChecksConfigurationFromBuilderConfig } from "./research-cross-checks.mjs";
import { moneyManagementConfigurationFromBuilderConfig } from "./research-money-management.mjs";

const SQX_BUILDER_CONFIG_API_PATH = "/api/sqx-builder-config";

export async function fetchBuilderConfigPayload(fetchImpl = globalThis.fetch) {
  if (typeof fetchImpl !== "function") throw new Error("Native Builder configuration fetch is unavailable");
  const response = await fetchImpl(SQX_BUILDER_CONFIG_API_PATH, { headers: { accept: "application/json" } });
  let payload = null;
  try { payload = await response.json(); } catch { payload = null; }
  if (!response?.ok) throw new Error(payload?.detail || `Native Builder configuration request failed: ${response?.status ?? "unknown"}`);
  return payload;
}

export async function fetchNativeBuilderView(fetchImpl = globalThis.fetch) {
  const payload = await fetchBuilderConfigPayload(fetchImpl);
  return Object.freeze({
    payload,
    specification: specificationFromBuilderConfig(payload),
    search: searchConfigurationFromBuilderConfig(payload),
    blocks: blocksConfigurationFromBuilderConfig(payload),
    rankings: rankingsConfigurationFromBuilderConfig(payload),
    crossChecks: crossChecksConfigurationFromBuilderConfig(payload),
    moneyManagement: moneyManagementConfigurationFromBuilderConfig(payload),
    charts: Array.isArray(payload.charts) ? payload.charts : [],
    instruments: Array.isArray(payload.instruments) ? payload.instruments : [],
  });
}

export function childNode(node, tag) {
  return node?.children?.find((child) => child.tag === tag) || null;
}

export function childNodes(node, tag) {
  return (node?.children || []).filter((child) => child.tag === tag);
}

export function nodeText(node, tag, fallback = null) {
  const child = childNode(node, tag);
  if (!child) return fallback;
  return child.text === null || child.text === undefined ? fallback : String(child.text);
}

export function nodeAttribute(node, tag, attribute, fallback = null) {
  const child = childNode(node, tag);
  if (!child) return fallback;
  return child.attributes?.[attribute] ?? fallback;
}

// Flatten every `Block` element under the Blocks subtree with its native section/category.
export function nativeBlocks(blocks) {
  const root = blocks?.producer_configuration;
  if (!root) return [];
  const rows = [];
  for (const section of root.children) {
    for (const node of section.children) {
      if (node.tag !== "Block") continue;
      rows.push({
        section: section.tag,
        key: node.attributes.key || "",
        category: node.attributes.category || section.tag,
        enabled: node.attributes.use === "true",
        weight: node.attributes.weight ?? null,
        attributes: node.attributes,
        node,
      });
    }
  }
  return rows;
}

// Acceptance conditions of the form Left-Side/Column-Value <Comparator> Right-Side/Numeric-Value.
export function nativeConditions(conditionsNode) {
  return childNodes(conditionsNode, "Condition").map((condition) => {
    const left = childNode(condition, "Left-Side");
    const column = childNode(left, "Column-Value");
    const right = childNode(condition, "Right-Side");
    const numeric = childNode(right, "Numeric-Value");
    return {
      enabled: condition.attributes.use === "true",
      column: column?.attributes?.column || column?.attributes?.class || left?.attributes?.valueType || "—",
      comparator: nodeAttribute(condition, "Comparator", "value", "—"),
      value: numeric?.attributes?.value ?? nodeText(right, "Numeric-Value", "—"),
    };
  });
}
