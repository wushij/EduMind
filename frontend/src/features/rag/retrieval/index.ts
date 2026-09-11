export interface RetrievalQuery {
  query: string;
  knowledgeBaseId?: number;
  topK?: number;
  scoreThreshold?: number;
}

export interface RetrievalResultItem {
  id: string;
  content: string;
  score: number;
  sourceDocName: string;
  chunkIndex: number;
}

export class RAGRetrievalFeature {
  public static filterResults(items: RetrievalResultItem[], threshold = 0.65): RetrievalResultItem[] {
    return items.filter(i => i.score >= threshold).sort((a, b) => b.score - a.score);
  }
}
