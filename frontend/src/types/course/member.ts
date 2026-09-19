export interface CourseMemberCandidate {
  userId: number;
  username?: string;
  realName?: string;
  avatar?: string;
}

export interface CourseMemberItem {
  id: number;
  courseId: number;
  userId: number;
  username?: string;
  realName?: string;
  avatar?: string;
  memberRole: string;
  progress?: number;
  joinTime?: string;
  status?: string;
}
