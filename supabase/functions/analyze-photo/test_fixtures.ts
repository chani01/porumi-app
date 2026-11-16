// 테스트 공용 픽스처 (배포 코드에서 import 하지 않음)
import type { AnalysisOutput } from "./analysis.ts";

export function sampleOutput(overrides: Partial<AnalysisOutput> = {}): AnalysisOutput {
  return {
    is_pet: true,
    species: "DOG",
    face_visible: true,
    primary_emotion: "HAPPY",
    scores: { HAPPY: 0.6, RELAXED: 0.2, EXCITED: 0.1, CURIOUS: 0.1, SLEEPY: 0, ANXIOUS: 0, ANNOYED: 0 },
    cues: ["입을 벌리고 혀가 보여요", "눈이 부드러워요"],
    summary: "기분이 아주 좋아 보여요!",
    ...overrides,
  };
}
