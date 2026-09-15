import fs from 'fs';
const style = fs.readFileSync('src/views/question/exams/ExamDetail.vue', 'utf8')
  .match(/<style scoped lang="scss">([\s\S]*)<\/style>/)[1];
const extract = (startMarker, endMarker) => {
  const start = style.indexOf(startMarker);
  const end = endMarker ? style.indexOf(endMarker, start) : style.length;
  return style.slice(start, end);
};
const hero = extract('  .top-nav-bar', '  .main-content-layout');
const paper = extract('    .paper-display-area', '    .paper-sidebar-area');
const sidebar = extract('    .paper-sidebar-area', '  }\n\n  .export-dialog-body');
fs.writeFileSync('scripts/_hero.scss', hero);
fs.writeFileSync('scripts/_paper.scss', paper);
fs.writeFileSync('scripts/_sidebar.scss', sidebar);
console.log('ok', hero.length, paper.length, sidebar.length);
