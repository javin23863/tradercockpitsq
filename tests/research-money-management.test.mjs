import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

import {
  BUILDER_MONEY_MANAGEMENT_SCHEMA,
  fetchNativeBuilderMoneyManagement,
  moneyManagementConfigurationFromBuilderConfig,
  renderNativeBuilderMoneyManagement,
} from "../web/research-money-management.mjs";

function payload() {
  const archive = "e".repeat(64);
  return {
    schema: "tc.sqx-builder-config.v1",
    source_build: "144.2953",
    project: "Builder",
    source_relative_path: "user/projects/Builder/project.cfx",
    archive_sha256: archive,
    internal_entries: ["config.xml", "Build-Task1.xml"],
    money_management: {
      schema: BUILDER_MONEY_MANAGEMENT_SCHEMA,
      authority: "native_sqx_read_only",
      source: {
        source_build: "144.2953",
        project: "Builder",
        relative_path: "user/projects/Builder/project.cfx",
        archive_sha256: archive,
        member: "Build-Task1.xml",
      },
      producer_configuration: {
        tag: "MoneyManagement",
        attributes: { futureRoot: "opaque" },
        text: null,
        children: [
          {
            tag: "UnknownSizingModel",
            attributes: { kind: "producer-owned" },
            text: null,
            children: [
              {
                tag: "FutureParameter",
                attributes: { representation: "native" },
                text: "0.73",
                children: [],
              },
            ],
          },
        ],
      },
      semantics: {
        interpreted_by_tradercockpit: false,
        owner: "StrategyQuant X",
        description: "Producer-owned MoneyManagement subtree.",
      },
      execution: {
        available: false,
        reason: "native_sqx_builder_owns_money_management_configuration",
      },
    },
  };
}

test("MoneyManagement parser preserves exact unknown native structure without sizing inference", () => {
  const moneyManagement = moneyManagementConfigurationFromBuilderConfig(payload());
  assert.equal(moneyManagement.schema, "tc.sqx-builder-money-management.v1");
  assert.equal(moneyManagement.producer_configuration.tag, "MoneyManagement");
  assert.deepEqual(moneyManagement.producer_configuration.attributes, { futureRoot: "opaque" });
  assert.equal(moneyManagement.producer_configuration.children[0].tag, "UnknownSizingModel");
  assert.equal(moneyManagement.producer_configuration.children[0].children[0].tag, "FutureParameter");
  assert.equal(moneyManagement.producer_configuration.children[0].children[0].text, "0.73");
});

test("MoneyManagement parser rejects substituted custody and malformed native roots", () => {
  const wrongArchive = payload();
  wrongArchive.money_management.source.archive_sha256 = "f".repeat(64);
  assert.throws(() => moneyManagementConfigurationFromBuilderConfig(wrongArchive), /schema mismatch/);

  const wrongMember = payload();
  wrongMember.money_management.source.member = "Other-Task1.xml";
  assert.throws(() => moneyManagementConfigurationFromBuilderConfig(wrongMember), /schema mismatch/);

  const wrongRoot = payload();
  wrongRoot.money_management.producer_configuration.tag = "Sizing";
  assert.throws(() => moneyManagementConfigurationFromBuilderConfig(wrongRoot), /root must be MoneyManagement/);

  const malformedChild = payload();
  malformedChild.money_management.producer_configuration.children[0].attributes.kind = 7;
  assert.throws(() => moneyManagementConfigurationFromBuilderConfig(malformedChild), /node mismatch/);
});

test("MoneyManagement fetch uses only the canonical Builder configuration API", async () => {
  let requested = "";
  const moneyManagement = await fetchNativeBuilderMoneyManagement(async (path) => {
    requested = path;
    return { ok: true, status: 200, json: async () => payload() };
  });
  assert.equal(requested, "/api/sqx-builder-config");
  assert.equal(moneyManagement.source.member, "Build-Task1.xml");
});

test("MoneyManagement renderer exposes producer structure but no sizing editor or inferred controls", () => {
  const html = renderNativeBuilderMoneyManagement(payload().money_management);
  assert.match(html, /Exact current SQX MoneyManagement structure/);
  assert.match(html, /UnknownSizingModel/);
  assert.match(html, /FutureParameter/);
  assert.match(html, /without assigning a sizing model/);
  assert.match(html, /does not edit, normalize, classify, calculate, simulate, or execute/);
  assert.doesNotMatch(html, /<button|<input|<select|Risk %|Fixed lot|Run sizing|Save sizing/);
});

test("MoneyManagement renderer keeps absent state visible and does not invent defaults", () => {
  const data = payload().money_management;
  data.producer_configuration = null;
  const parsed = moneyManagementConfigurationFromBuilderConfig({ ...payload(), money_management: data });
  const html = renderNativeBuilderMoneyManagement(parsed);
  assert.match(html, /MoneyManagement absent/);
  assert.match(html, /does not invent a sizing model or defaults/);
});

test("canonical desktop loads the native MoneyManagement inspector", async () => {
  const indexSource = await readFile(new URL("../web/index.html", import.meta.url), "utf8");
  const matches = indexSource.match(/src="\/research-money-management\.mjs"/g) || [];
  assert.equal(matches.length, 1);
});
