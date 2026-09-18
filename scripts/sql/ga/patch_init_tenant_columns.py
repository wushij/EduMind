#!/usr/bin/env python3
"""Add tenant_id column to Wave1/Wave2 tables in sql/init.sql for fresh installs."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
init_path = ROOT / "sql" / "init.sql"
content = init_path.read_text(encoding="utf-8")

TABLES = [
    "course_chapter", "course_member", "course_resource", "course_knowledge_point",
    "course_chapter_knowledge_point", "course_lesson_progress",
    "knowledge_document", "knowledge_document_text", "knowledge_document_chunk",
    "knowledge_index_task", "knowledge_chunk_index", "knowledge_ocr_page",
    "teaching_exam", "exam_question", "assignment", "assignment_submission", "submission_answer",
    "ai_message", "ai_memory_item", "ai_memory_feedback", "agent_run",
    "question_bank", "question_option", "question_bank_item", "grading_result", "teaching_resource",
    "agent_step", "agent_tool_call", "prompt_template", "prompt_template_version",
    "sys_user_preference", "statistics_daily_snapshot", "learning_record", "knowledge_mastery",
    "wrong_question_record", "course_statistics", "knowledge_point_relation",
]

TENANT_COL = "    tenant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',\n"

for table in TABLES:
    block_match = re.search(
        rf"CREATE TABLE IF NOT EXISTS {table} \((.*?)\) ENGINE",
        content,
        re.DOTALL,
    )
    if not block_match:
        print(f"SKIP {table} (table not found)")
        continue
    if "tenant_id" in block_match.group(1):
        print(f"SKIP {table} (already has tenant_id)")
        continue

    pattern = rf"(CREATE TABLE IF NOT EXISTS {table} \(\n)(    id[^\n]+\n)"
    content, n = re.subn(pattern, rf"\1\2{TENANT_COL}", content, count=1)
    if not n:
        print(f"FAIL {table} (id line not matched)")
        continue

    pk_pattern = rf"(CREATE TABLE IF NOT EXISTS {table} \([\s\S]*?)(    PRIMARY KEY \(id\))"
    block = re.search(rf"CREATE TABLE IF NOT EXISTS {table} \([\s\S]*?\) ENGINE", content)
    if block and "idx_tenant_id" not in block.group(0):
        content = re.sub(pk_pattern, r"\1    KEY idx_tenant_id (tenant_id),\n\2", content, count=1)
    print(f"Patched {table}")

init_path.write_text(content, encoding="utf-8")
print("Done")
