export interface MockUser {
  id: number;
  username: string;
  realName: string;
  role: 'ADMIN' | 'TEACHER' | 'STUDENT';
  avatar: string;
  department: string;
  token: string;
  permissions?: string[];
}

export const MOCK_USERS: Record<'ADMIN' | 'TEACHER' | 'STUDENT', MockUser> = {
  ADMIN: {
    id: 1,
    username: 'admin',
    realName: '平台超级管理员',
    role: 'ADMIN',
    avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
    department: '信息化教学与网络中心',
    token: 'token_mock_admin_2026'
  },
  TEACHER: {
    id: 2,
    username: 'teacher_li',
    realName: '李华教授 (骨干教师)',
    role: 'TEACHER',
    avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
    department: '计算机科学与技术学院',
    token: 'token_mock_teacher_2026'
  },
  STUDENT: {
    id: 3,
    username: 'student_zhang',
    realName: '张明同学 (2024级本科生)',
    role: 'STUDENT',
    avatar: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
    department: '软件工程系',
    token: 'token_mock_student_2026'
  }
};
