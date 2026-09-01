import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

import {
  BUILDER_TRADING_OPTIONS_SCHEMA,
  fetchNativeBuilderTradingOptions,
  renderNativeBuilderTradingOptions,
  tradingOptionsConfigurationFromBuilderConfig,
} from "../web/research-trading-options.mjs";

function payload() {
  const archive = "d".repeat(64);
  return {
    schema: "tc.sqx-builder-config.v1",
    source_build: "144.2953",
    project: "Builder",
    source_relative_path: "user/projects/Builder/project.cfx",
    archive_sha256: archive,
    internal_entries: ["config.xml", "Build-Task1.xml"],
    trading_options: {
      schema: BUILDER_TRADING_OPTIONS_SCHEMA,
      authority: "native_sqx_read_only",
      source: {
        source_build: "144.2953",
        project: "Builder",
        relative_path: "user/projects/Builder/project.cfx",
        archive_sha256: archive,
        member: "Build-Task1.xml",
      },
      producer_configuration: {
        tag: "BuildTradingOptions",
        attributes: { futureMode: "opaque" },
        text: null,
        children: [
          {
            tag: "UnknownTradingSetting",
            attributes: { kind: "producer-owned" },
            text: null,
            children: [
              {
                tag: "FutureTradingValue",
                attributes: { representation: "native" },
                text: "17",
                children: [],
              },
            ],
          },
        ],
      },
      semantics: {
        interpreted_by_tradercockpit: false,
        owner: "StrategyQuant X",
        description: "Producer-owned BuildTradingOptions subtree.",
      },
      execution: {
        available: false,
        reason: "native_sqx_builder_owns_trading_options_configuration",
      },
    },
  };
}

test("trading options parser preserves exact unknown native structure without interpretation", () => {
  const tradingOptions = tradingOptionsConfigurationFromBuilderConfig(payload());
  assert.equal(tradingOptions.schema, "tc.sqx-builder-trading-options.v1");
  assert.equal(tradingOptions.producer_configuration.tag, "BuildTradingOptions");
  assert.deepEqual(tradingOptions.producer_configuration.attributes, { futureMode: "opaque" });
  assert.equal(tradingOptions.producer_configuration.children[0].tag, "UnknownTradingSetting");
  assert.equal(tradingOptions.producer_configuration.children[0].children[0].tag, "FutureTradingValue");
  assert.equal(tradingOptions.producer_configuration.children[0].children[0].text, "17");
});

test("trading options parser rejects substituted custody and malformed native roots", () => {
  const wrongArchive = payload();
  wrongArchive.trading_options.source.archive_sha256 = "f".repeat(64);
  assert.throws(() => tradingOptionsConfigurationFromBuilderConfig(wrongArchive), /schema mismatch/);

  const wrongMember = payload();
  wrongMember.trading_options.source.member = "Other-Task1.xml";
  assert.throws(() => tradingOptionsConfigurationFromBuilderConfig(wrongMember), /schema mismatch/);

  const wrongRoot = payload();
  wrongRoot.trading_options.producer_configuration.tag = "TradingOptions";
  assert.throws(() => tradingOptionsConfigurationFromBuilderConfig(wrongRoot), /root must be BuildTradingOptions/);

  const malformedChild = payload();
  malformedChild.trading_options.producer_configuration.children[0].attributes.kind = 7;
  assert.throws(() => tradingOptionsConfigurationFromBuilderConfig(malformedChild), /node mismatch/);
});

test("trading options fetch uses only the canonical Builder configuration API", async () => {
  let requested = "";
  const tradingOptions = await fetchNativeBuilderTradingOptions(async (path) => {
    requested = path;
    return { ok: true, status: 200, json: async () => payload() };
  });
  assert.equal(requested, "/api/sqx-builder-config");
  assert.equal(tradingOptions.source.member, "Build-Task1.xml");
});

test("trading options renderer exposes producer structure but no inferred editor", () => {
  const html = renderNativeBuilderTradingOptions(payload().trading_options);
  assert.match(html, /Exact current SQX BuildTradingOptions structure/);
  assert.match(html, /UnknownTradingSetting/);
  assert.match(html, /FutureTradingValue/);
  assert.match(html, /without assigning order, exit, session, stop, target, timing, numeric, dependency, or other trading semantics/);
  assert.match(html, /does not edit, normalize, classify, calculate, simulate, or execute/);
  assert.doesNotMatch(html, /<button|<input|<select|Stop loss|Profit target|Session editor|Save trading/);
});

test("trading options renderer keeps absent state visible and invents no defaults", () => {
  const data = payload().trading_options;
  data.producer_configuration = null;
  const parsed = tradingOptionsConfigurationFromBuilderConfig({ ...payload(), trading_options: data });
  const html = renderNativeBuilderTradingOptions(parsed);
  assert.match(html, /BuildTradingOptions absent/);
  assert.match(html, /does not invent trading assumptions or defaults/);
});

test("canonical desktop loads the native trading options inspector", async () => {
  const indexSource = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  const matches = indexSource.match(/src="\/research-trading-options\.mjs"/g) || [];
  assert.equal(matches.length, 1);
});
