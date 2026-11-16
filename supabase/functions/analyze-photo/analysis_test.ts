import { assert, assertAlmostEquals, assertEquals, assertFalse } from "@std/assert";
import {
  AnalysisSchema,
  buildAnalysisRow,
  EMOTIONS,
  isAnalyzable,
  isOwnImagePath,
  MAX_CUES,
  normalizeScores,
  toBase64,
} from "./analysis.ts";
import { sampleOutput } from "./test_fixtures.ts";

const UID = "11111111-1111-1111-1111-111111111111";

Deno.test("isOwnImagePath: 본인 폴더만 허용, 경로 조작 거부", () => {
  assert(isOwnImagePath(`${UID}/a.jpg`, UID));
  assertFalse(isOwnImagePath(undefined, UID));
  assertFalse(isOwnImagePath("", UID));
  assertFalse(isOwnImagePath("22222222-2222-2222-2222-222222222222/a.jpg", UID));
  assertFalse(isOwnImagePath(`${UID}/../other/a.jpg`, UID));
  // uid 접두사만 같은 다른 폴더
  assertFalse(isOwnImagePath(`${UID}x/a.jpg`, UID));
});

Deno.test("normalizeScores: 합이 1이 되게 정규화", () => {
  const s = normalizeScores({ HAPPY: 2, RELAXED: 2, EXCITED: 0, CURIOUS: 0, SLEEPY: 0, ANXIOUS: 0, ANNOYED: 0 });
  // 1 초과 값은 1로 자른 뒤 정규화
  assertEquals(s.HAPPY, 0.5);
  assertEquals(s.RELAXED, 0.5);
  const total = Object.values(normalizeScores(sampleOutput().scores)).reduce((a, b) => a + b, 0);
  assertAlmostEquals(total, 1, 0.005);
});

Deno.test("normalizeScores: 음수·NaN·Infinity 는 0, 전부 0이면 그대로", () => {
  const s = normalizeScores({ HAPPY: -1, RELAXED: NaN, EXCITED: Infinity, CURIOUS: 0, SLEEPY: 0, ANXIOUS: 0, ANNOYED: 0 });
  assertEquals(Object.values(s).every((v) => v === 0), true);
});

Deno.test("isAnalyzable: 반려동물 얼굴이 보일 때만 true", () => {
  assert(isAnalyzable(sampleOutput()));
  assert(isAnalyzable(sampleOutput({ species: "CAT" })));
  assertFalse(isAnalyzable(sampleOutput({ is_pet: false })));
  assertFalse(isAnalyzable(sampleOutput({ face_visible: false })));
  assertFalse(isAnalyzable(sampleOutput({ species: "OTHER" })));
});

Deno.test("buildAnalysisRow: 정규화 점수의 주 감정을 confidence 로, cues 최대 4개", () => {
  const now = new Date("2026-11-16T00:00:00.000Z");
  const row = buildAnalysisRow(
    sampleOutput({ cues: ["1", "2", "3", "4", "5", "6"] }),
    { pet_id: "pet-1" },
    `${UID}/a.jpg`,
    now,
  );
  assertEquals(row.pet_id, "pet-1");
  assertEquals(row.primary_emotion, "HAPPY");
  assertEquals(row.confidence, row.scores.HAPPY);
  assertEquals(row.cues.length, MAX_CUES);
  assertEquals(row.taken_at, now.toISOString());
  // DB CHECK: species 는 DOG/CAT 만
  assert(row.species === "DOG" || row.species === "CAT");
});

Deno.test("buildAnalysisRow: taken_at·pet_id 전달값 우선, 없으면 null/now", () => {
  const row = buildAnalysisRow(sampleOutput(), { taken_at: "2026-01-01T00:00:00Z" }, `${UID}/a.jpg`);
  assertEquals(row.taken_at, "2026-01-01T00:00:00Z");
  assertEquals(row.pet_id, null);
});

Deno.test("AnalysisSchema: scores 키가 EMOTIONS 와 같은 순서·구성", () => {
  assertEquals(Object.keys(AnalysisSchema.shape.scores.shape), [...EMOTIONS]);
});

Deno.test("toBase64: 큰 입력(청크 경계 넘음)도 표준 base64 와 같다", () => {
  const bytes = new Uint8Array(0x8000 * 2 + 123).map((_, i) => i % 256);
  const expected = btoa(Array.from(bytes, (b) => String.fromCharCode(b)).join(""));
  assertEquals(toBase64(bytes), expected);
});
