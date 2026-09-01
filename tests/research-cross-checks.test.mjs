import test from "node:test";
import assert from "node:assert/strict";

import {
  BUILDER_CROSS_CHECKS_SCHEMA,
  crossChecksConfigurationFromBuilderConfig,
  fetchNativeBuilderCrossChecks,
  renderNativeBuilderCrossChecks,
} from "../web/research-cross-checks.mjs";

function payload() {
  const archive = "c".repeat(64);
  return {
    schema: "tc.sqx-builder-config.v1",
    source_build: "144.2953",
    project: "Builder",
    source_relative_path: "user/projects/Builder/project.cfx",
    archive_sha256: archive,
    internal_entries: ["config.xml", "Build-Task1.xml"],
    cross_checks: {
      schema: BUILDER_CROSS_CHECKS_SCHEMA,
      authority: "native_sqx_read_only",
      source: {
        source_build: "144.2953",
        project: "Builder",
        relative_path: "user/projects/Builder/project.cfx",
        archive_sha256: archive,
        member: "Build-Task1.xml",
      },
      enabled: true,
      producer_configuration: {
        tag: "CrossChecks",
        attributes: { use: "true", futureRoot: "opaque" },
        text: null,
        children: [
          {
            tag: "UnknownCheck",
            attributes: { kind: "producer-owned" },
            text: null,
            children: [
              {
                tag: "FutureSetting",
                attributes: { representation: "native" },
                text: "abc",
                children: [],
              },
            ],
          },
        ],
      },
      semantics: {
        interpreted_by_tradercockpit: false,
        owner: "StrategyQuant X",
        description: "Producer-owned CrossChecks subtree.",
      },
      execution: {
        available: false,
        reason: "native_sqx_builder_owns_cross_check_configuration",
      },
    },
  };
}

test("CrossChecks parser preserves exact unknown native structure without validation inference", () => {
  const crossChecks = crossChecksConfigurationFromBuilderConfig(payload());
  assert.equal(crossChecks.schema, "tc.sqx-builder-cross-checks.v1");
  assert.equal(crossChecks.enabled, true);
  assert.equal(crossChecks.producer_configuration.tag, "CrossChecks");
  assert.deepEqual(crossChecks.producer_configuration.attributes, { use: "true", futureRoot: "opaque" });
  assert.equal(crossChecks.producer_configuration.children[0].tag, "UnknownCheck");
  assert.equal(crossChecks.producer_configuration.children[0].children[0].tag, "FutureSetting");
  assert.equal(crossChecks.producer_configuration.children[0].children[0].text, "abc");
});

test("CrossChecks parser rejects substituted custody, malformed roots, and contradictory use flags", () => {
  const wrongArchive = payload();
  wrongArchive.cross_checks.source.archive_sha256 = "d".repeat(64);
  assert.throws(() => crossChecksConfigurationFromBuilderConfig(wrongArchive), /schema mismatch/);

  const wrongMember = payload();
  wrongMember.cross_checks.source.member = "Other-Task1.xml";
  assert.throws(() => crossChecksConfigurationFromBuilderConfig(wrongMember), /schema mismatch/);

  const wrongRoot = payload();
  wrongRoot.cross_checks.producer_configuration.tag = "Validation";
  assert.throws(() => crossChecksConfigurationFromBuilderConfig(wrongRoot), /root must be CrossChecks/);

  const malformedChild = payload();
  malformedChild.cross_checks.producer_configuration.children[0].attributes.kind = 7;
  assert.throws(() => crossChecksConfigurationFromBuilderConfig(malformedChild), /node mismatch/);

  const contradictory = payload();
  contradictory.cross_checks.enabled = false;
  assert.throws(() => crossChecksConfigurationFromBuilderConfig(contradictory), /use flag mismatch/);
});

test("CrossChecks fetch uses only the canonical Builder configuration API", async () => {
  let requested = "";
  const crossChecks = await fetchNativeBuilderCrossChecks(async (path) => {
    requested = path;
    return { ok: true, status: 200, json: async () => payload() };
  });
  assert.equal(requested, "/api/sqx-builder-config");
  assert.equal(crossChecks.source.member, "Build-Task1.xml");
});

test("CrossChecks renderer exposes producer structure but no verdict, taxonomy, or execution controls", () => {
  const html = renderNativeBuilderCrossChecks(payload().cross_checks);
  assert.match(html, /Exact current SQX CrossChecks structure/);
  assert.match(html, /UnknownCheck/);
  assert.match(html, /FutureSetting/);
  assert.match(html, /not a validation result/i);
  assert.match(html, /does not edit, normalize, classify, execute, score, or judge/);
  assert.doesNotMatch(html, /<button|<input|<select|validation passed|robustness passed|Run validation|Save profile/);
});

test("CrossChecks renderer keeps absent state visible and does not invent defaults", () => {
  const data = payload().cross_checks;
  data.enabled = false;
  data.producer_configuration = null;
  const parsed = crossChecksConfigurationFromBuilderConfig({ ...payload(), cross_checks: data });
  const html = renderNativeBuilderCrossChecks(parsed);
  assert.match(html, /CrossChecks absent/);
  assert.match(html, /does not invent a validation profile or defaults/);
});
