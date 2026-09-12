export interface V05Alternative {
  label: string;
  route: string;
}

export interface V05ToolNoticeConfig {
  title: string;
  description: string;
  alternatives: V05Alternative[];
}
