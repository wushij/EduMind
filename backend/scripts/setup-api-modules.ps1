# One-time helper: create *-api module dirs and copy contract sources (run from backend/)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function New-ApiModulePom($artifactId, $extraDeps = @()) {
    $dir = Join-Path $root $artifactId
    New-Item -ItemType Directory -Force -Path (Join-Path $dir "src/main/java") | Out-Null
    $depsXml = @"
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
"@
    foreach ($d in $extraDeps) {
        $depsXml += @"

        <dependency>
            <groupId>com.edumind</groupId>
            <artifactId>$d</artifactId>
        </dependency>
"@
    }
    @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.edumind</groupId>
        <artifactId>edu-mind-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>$artifactId</artifactId>
    <name>$artifactId</name>
    <description>跨模块公开 API 契约（无实现）</description>
    <dependencies>
$depsXml
    </dependencies>
</project>
"@ | Set-Content -Encoding UTF8 (Join-Path $dir "pom.xml")
}

function Copy-Rel($fromBase, $relPaths, $toBase) {
    foreach ($rel in $relPaths) {
        $src = Join-Path $fromBase $rel
        $dst = Join-Path $toBase $rel
        $dstDir = Split-Path $dst -Parent
        New-Item -ItemType Directory -Force -Path $dstDir | Out-Null
        Copy-Item -Force $src $dst
    }
}

$courseMod = Join-Path $root "edu-mind-modules/edu-mind-course/src/main/java/com/edumind/course"
$questionMod = Join-Path $root "edu-mind-modules/edu-mind-question/src/main/java/com/edumind/question"
$teachingMod = Join-Path $root "edu-mind-modules/edu-mind-teaching/src/main/java/com/edumind/teaching"
$knowledgeMod = Join-Path $root "edu-mind-modules/edu-mind-knowledge/src/main/java/com/edumind/knowledge"
$resourceMod = Join-Path $root "edu-mind-modules/edu-mind-resource/src/main/java/com/edumind/resource"
$notificationMod = Join-Path $root "edu-mind-modules/edu-mind-notification/src/main/java/com/edumind/notification"

New-ApiModulePom "edu-mind-course-api"
Copy-Rel $courseMod @(
    "api/CourseQueryApi.java",
    "vo/CourseBriefVO.java",
    "vo/course/CourseVO.java",
    "vo/course/CourseDetailVO.java",
    "vo/chapter/ChapterTreeVO.java",
    "vo/knowledge/KnowledgePointVO.java"
) (Join-Path $root "edu-mind-course-api/src/main/java/com/edumind/course")

New-ApiModulePom "edu-mind-question-api"
Copy-Rel $questionMod @(
    "api/QuestionQueryApi.java",
    "api/QuestionCommandApi.java",
    "vo/question/QuestionVO.java",
    "vo/question/QuestionBatchSaveVO.java",
    "dto/question/QuestionBatchCreateDTO.java",
    "dto/question/QuestionCreateDTO.java"
) (Join-Path $root "edu-mind-question-api/src/main/java/com/edumind/question")

New-ApiModulePom "edu-mind-teaching-api" @("edu-mind-question-api")
Copy-Rel $teachingMod @(
    "api/ExamQueryApi.java",
    "api/SubmissionQueryApi.java",
    "vo/exam/ExamVO.java",
    "vo/exam/ExamQuestionVO.java",
    "vo/assignment/AssignmentVO.java",
    "vo/submission/SubmissionStatsVO.java"
) (Join-Path $root "edu-mind-teaching-api/src/main/java/com/edumind/teaching")

New-ApiModulePom "edu-mind-knowledge-api" @("edu-mind-course-api")
Copy-Rel $knowledgeMod @(
    "api/KnowledgeAccessApi.java",
    "api/KnowledgePointQueryApi.java",
    "api/KnowledgeGraphQueryApi.java",
    "api/KnowledgeQueryApi.java",
    "api/ChunkQueryApi.java",
    "vo/knowledge/KnowledgeBaseVO.java",
    "vo/knowledge/KnowledgeDocumentVO.java",
    "vo/knowledge/ChunkVO.java",
    "vo/graph/KnowledgeGraphVO.java"
) (Join-Path $root "edu-mind-knowledge-api/src/main/java/com/edumind/knowledge")

New-ApiModulePom "edu-mind-resource-api"
Copy-Rel $resourceMod @(
    "api/ResourceQueryApi.java",
    "vo/ResourceVO.java"
) (Join-Path $root "edu-mind-resource-api/src/main/java/com/edumind/resource")

New-ApiModulePom "edu-mind-notification-api"
Copy-Rel $notificationMod @(
    "api/NotificationWriteApi.java"
) (Join-Path $root "edu-mind-notification-api/src/main/java/com/edumind/notification")

Write-Host "API module skeletons created."
