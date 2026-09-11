export class LearningRecommendationFeature {
  public static calculateMasteryScore(correctCount: number, totalCount: number): number {
    if (!totalCount) return 0;
    return Math.round((correctCount / totalCount) * 100);
  }
}
