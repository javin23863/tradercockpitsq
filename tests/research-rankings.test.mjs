import test from "node:test";
import assert from "node:assert/strict";

import {
  BUILDER_RANKINGS_SCHEMA,
  fetchNativeBuilderRankings,
  rankingsConfigurationFromBuilderConfig,
  renderNativeBuilderRankings,
} from "../web/research-rankings.mjs";

function payload() {
  const archive = "a".repeat(64);
  return {
    schema: "tc.sqx-builder-config.v1",
    source_build: "144.2953",
    project: "Builder",
    source_relative_path: "user/projects/Builder/project.cfx",
    archive_sha256: archive,
    internal_entries: ["config.xml", "Build-Task1.xml"],
    rankings: {
      schema: BUILDER_RANKINGS_SCHEMA,
      authority: "native_sqx_read_only",
      source: {
        source_build: "144.2953",
        project: "Builder",
        relative_path: "user/projects/Builder/project.cfx",
        archive_sha256: archive,
        member: "Build-Task1.xml",
      },
      producer_configuration: {
        tag: "Rankings",
        attributes: { futureRoot: "opaque" },
        text: null,
        children: [
          {
            tag: "MaxStrategies",
            attributes: {},
            text: "321",
            children: [],
          },
          {
            tag: "StopCondition",
            attributes: { type: "90m", futureFlag: "native" },
            text: null,
            children: [
              {
                tag: "FutureStopField",
                attributes: { representation: "producer-owned" },
                text: "native-value",
                children: [],
              },
            ],
          },
          {
            tag: "UnknownRankingNode",
            attributes: { mode: "opaque" },
            text: "producer-owned",
            children: [],
          },
        ],
      },
      semantics: {
        interpreted_by_tradercockpit: false,
        owner: "StrategyQuant X",
        description: "Producer-owned Rankings subtree.",
      },
      execution: {
        available: false,
        reason: "native_sqx_builder_owns_ranking_configuration",
      },
    },
  };
}

test("Rankings parser preserves exact unknown native structure without semantic inference", () => {
  const rankings = rankingsConfigurationFromBuilderConfig(payload());
  assert.equal(rankings.schema, "tc.sqx-builder-rankings.v1");
  assert.equal(rankings.producer_configuration.tag, "Rankings");
  assert.equal(rankings.producer_configuration.children[0].tag, "MaxStrategies");
  assert.equal(rankings.producer_configuration.children[0].text, "321");
  assert.equal(rankings.producer_configuration.children[1].tag, "StopCondition");
  assert.deepEqual(rankings.producer_configuration.children[1].attributes, { type: "90m", futureFlag: "native" });
  assert.equal(rankings.producer_configuration.children[1].children[0].tag, "FutureStopField");
  assert.equal(rankings.producer_configuration.children[2].tag, "UnknownRankingNode");
});

test("Rankings parser rejects substituted source/archive identity and malformed roots", () => {
  const wrongArchive = payload();
  wrongArchive.rankings.source.archive_sha256 = "b".repeat(64);
  assert.throws(() => rankingsConfigurationFromBuilderConfig(wrongArchive), /schema mismatch/);

  const wrongMember = payload();
  wrongMember.rankings.source.member = "Other-Task1.xml";
  assert.throws(() => rankingsConfigurationFromBuilderConfig(wrongMember), /schema mismatch/);

  const wrongRoot = payload();
  wrongRoot.rankings.producer_configuration.tag = "Fitness";
  assert.throws(() => rankingsConfigurationFromBuilderConfig(wrongRoot), /root must be Rankings/);

  const malformedChild = payload();
  malformedChild.rankings.producer_configuration.children[1].attributes.futureFlag = 7;
  assert.throws(() => rankingsConfigurationFromBuilderConfig(malformedChild), /node mismatch/);
});

test("Rankings fetch uses only the canonical Builder configuration API", async () => {
  let requested = "";
  const rankings = await fetchNativeBuilderRankings(async (path) => {
    requested = path;
    return { ok: true, status: 200, json: async () => payload() };
  });
  assert.equal(requested, "/api/sqx-builder-config");
  assert.equal(rankings.source.member, "Build-Task1.xml");
});

test("Rankings renderer exposes producer structure but no ranking editor or inferred controls", () => {
  const html = renderNativeBuilderRankings(payload().rankings);
  assert.match(html, /Exact current SQX Rankings structure/);
  assert.match(html, /MaxStrategies/);
  assert.match(html, /StopCondition/);
  assert.match(html, /FutureStopField/);
  assert.match(html, /without assigning a fitness objective/);
  assert.match(html, /does not edit, normalize, score, rank, select, or execute/);
  assert.doesNotMatch(html, /<button|<input|<select|Run ranking|Save ranking|Objective:|Direction:/);
});

test("Rankings renderer fails visible when native Rankings are absent", () => {
  const data = payload().rankings;
  data.producer_configuration = null;
  const html = renderNativeBuilderRankings(data);
  assert.match(html, /Rankings absent/);
  assert.match(html, /does not invent defaults/);
});
