export interface AIModel {
  id: string;
  name: string;
  provider: string;
  contextLength?: number;
  temperature?: number;
  enabled: boolean;
}
