import test from "node:test";
import assert from "node:assert/strict";

import {
  BUILDER_BLOCKS_SCHEMA,
  blocksConfigurationFromBuilderConfig,
  fetchNativeBuilderBlocks,
  renderNativeBuilderBlocks,
} from "../web/research-blocks.mjs";

function payload() {
  const archive = "a".repeat(64);
  return {
    schema: "tc.sqx-builder-config.v1",
    source_build: "144.2953",
    project: "Builder",
    source_relative_path: "user/projects/Builder/project.cfx",
    archive_sha256: archive,
    internal_entries: ["config.xml", "Build-Task1.xml"],
    blocks: {
      schema: BUILDER_BLOCKS_SCHEMA,
      authority: "native_sqx_read_only",
      source: {
        source_build: "144.2953",
        project: "Builder",
        relative_path: "user/projects/Builder/project.cfx",
        archive_sha256: archive,
        member: "Build-Task1.xml",
      },
      producer_configuration: {
        tag: "Blocks",
        attributes: { use: "true" },
        text: null,
        children: [
          {
            tag: "UnknownNativeFamily",
            attributes: { mode: "opaque", weight: "7" },
            text: null,
            children: [
              {
                tag: "FutureParameterShape",
                attributes: { representation: "producer-owned" },
                text: "native-value",
                children: [],
              },
            ],
          },
        ],
      },
      semantics: {
        interpreted_by_tradercockpit: false,
        owner: "StrategyQuant X",
        description: "Producer-owned Blocks subtree.",
      },
      execution: {
        available: false,
        reason: "native_sqx_builder_owns_block_configuration",
      },
    },
  };
}

test("Blocks parser preserves exact unknown native structure without taxonomy inference", () => {
  const blocks = blocksConfigurationFromBuilderConfig(payload());
  assert.equal(blocks.schema, "tc.sqx-builder-blocks.v1");
  assert.equal(blocks.producer_configuration.tag, "Blocks");
  assert.equal(blocks.producer_configuration.children[0].tag, "UnknownNativeFamily");
  assert.deepEqual(blocks.producer_configuration.children[0].attributes, { mode: "opaque", weight: "7" });
  assert.equal(blocks.producer_configuration.children[0].children[0].tag, "FutureParameterShape");
  assert.equal(blocks.producer_configuration.children[0].children[0].text, "native-value");
});

test("Blocks parser rejects substituted source/archive identity and malformed roots", () => {
  const wrongArchive = payload();
  wrongArchive.blocks.source.archive_sha256 = "b".repeat(64);
  assert.throws(() => blocksConfigurationFromBuilderConfig(wrongArchive), /schema mismatch/);

  const wrongMember = payload();
  wrongMember.blocks.source.member = "Other-Task1.xml";
  assert.throws(() => blocksConfigurationFromBuilderConfig(wrongMember), /schema mismatch/);

  const wrongRoot = payload();
  wrongRoot.blocks.producer_configuration.tag = "Indicators";
  assert.throws(() => blocksConfigurationFromBuilderConfig(wrongRoot), /root must be Blocks/);

  const malformedChild = payload();
  malformedChild.blocks.producer_configuration.children[0].attributes.weight = 7;
  assert.throws(() => blocksConfigurationFromBuilderConfig(malformedChild), /node mismatch/);
});

test("Blocks fetch uses only the canonical Builder configuration API", async () => {
  let requested = "";
  const blocks = await fetchNativeBuilderBlocks(async (path) => {
    requested = path;
    return { ok: true, status: 200, json: async () => payload() };
  });
  assert.equal(requested, "/api/sqx-builder-config");
  assert.equal(blocks.source.member, "Build-Task1.xml");
});

test("Blocks renderer exposes producer structure but no editor or semantic classification", () => {
  const html = renderNativeBuilderBlocks(payload().blocks);
  assert.match(html, /Exact current SQX Blocks structure/);
  assert.match(html, /UnknownNativeFamily/);
  assert.match(html, /FutureParameterShape/);
  assert.match(html, /producer-owned/);
  assert.match(html, /does not edit, classify, normalize, or execute/);
  assert.doesNotMatch(html, /<button|<input|<select|Run blocks|Save blocks|Indicator family|Signal family/);
});

test("Blocks renderer fails visible when native Blocks are absent", () => {
  const data = payload().blocks;
  data.producer_configuration = null;
  const html = renderNativeBuilderBlocks(data);
  assert.match(html, /Blocks absent/);
  assert.match(html, /does not expose a Blocks subtree/);
});
