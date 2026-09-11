export interface Citation {
  id: number;
  docTitle: string;
  page?: number;
  snippet: string;
}

export class CitationFeature {
  /**
   * 解析模型回答中的 [1] [2] 溯源引用标记
   */
  public static parseCitations(text: string, availableCitations: Citation[]): { parsedText: string; matchedCitations: Citation[] } {
    const matched: Citation[] = [];
    const parsedText = text.replace(/\[(\d+)\]/g, (match, p1) => {
      const idx = parseInt(p1, 10);
      const cit = availableCitations.find(c => c.id === idx);
      if (cit && !matched.includes(cit)) {
        matched.push(cit);
      }
      return match;
    });
    return { parsedText, matchedCitations: matched };
  }
}
