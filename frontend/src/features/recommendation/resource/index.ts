export class ResourceRecommendationFeature {
  public static matchResourcesByKnowledgePoint(kpId: number, allResources: any[]): any[] {
    return allResources.filter(r => r.knowledgePointId === kpId || (r.tags && r.tags.includes(kpId)));
  }
}
