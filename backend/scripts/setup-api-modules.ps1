# Sync / bootstrap edu-mind-api contract modules (run from repo: backend/scripts/)
# Canonical layout: backend/edu-mind-api/edu-mind-*-api/
# Business modules keep only com.edumind.<domain>.api.impl.*
param(
    [switch]$FromBusinessModules,
    [switch]$PruneBusinessContracts,
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$apiRoot = Join-Path $root "edu-mind-api"
$modulesRoot = Join-Path $root "edu-mind-modules"

# artifactId -> domain folder under com/edumind
$DomainModules = @(
    @{ Artifact = "edu-mind-system-api";       Domain = "system";       ExtraDeps = @(); Validation = $false }
    @{ Artifact = "edu-mind-course-api";       Domain = "course";       ExtraDeps = @(); Validation = $false }
    @{ Artifact = "edu-mind-question-api";     Domain = "question";     ExtraDeps = @(); Validation = $true }
    @{ Artifact = "edu-mind-resource-api";     Domain = "resource";     ExtraDeps = @(); Validation = $false }
    @{ Artifact = "edu-mind-notification-api"; Domain = "notification"; ExtraDeps = @(); Validation = $false }
    @{ Artifact = "edu-mind-statistics-api";   Domain = "statistics";   ExtraDeps = @(); Validation = $false }
    @{ Artifact = "edu-mind-knowledge-api";    Domain = "knowledge";     ExtraDeps = @("edu-mind-course-api"); Validation = $false }
    @{ Artifact = "edu-mind-teaching-api";     Domain = "teaching";      ExtraDeps = @("edu-mind-question-api"); Validation = $false }
    @{ Artifact = "edu-mind-ai-api";           Domain = "ai";            ExtraDeps = @("edu-mind-question-api"); Validation = $true }
)

# Relative paths under com/edumind/<domain>/ (manifest = current edu-mind-api tree)
$ContractManifest = @{
    "system" = @(
        "api/OrganizationQueryApi.java", "api/SecurityKeyQueryApi.java", "api/TenantDataScope.java",
        "api/TenantDataScopeApi.java", "api/TenantQueryApi.java", "api/TenantQuotaApi.java",
        "api/UserPreferenceQueryApi.java", "api/UserQueryApi.java",
        "vo/tenant/MemberOrgBriefVO.java", "vo/tenant/OrganizationBriefVO.java",
        "vo/tenant/TenantBriefVO.java", "vo/tenant/TenantQuotaVO.java", "vo/user/UserBriefVO.java"
    )
    "course" = @(
        "api/CourseAccessApi.java", "api/CourseQueryApi.java", "vo/CourseBriefVO.java",
        "vo/chapter/ChapterTreeVO.java", "vo/course/CourseDetailVO.java", "vo/course/CourseVO.java",
        "vo/knowledge/KnowledgePointVO.java", "vo/overview/CourseAnnouncementVO.java",
        "vo/overview/CourseCapabilityTagVO.java", "vo/overview/CourseInstructorCardVO.java",
        "vo/overview/CourseObjectiveVO.java", "vo/overview/CourseOverviewVO.java"
    )
    "question" = @(
        "api/QuestionCommandApi.java", "api/QuestionQueryApi.java",
        "dto/question/QuestionBatchCreateDTO.java", "dto/question/QuestionCreateDTO.java",
        "vo/question/QuestionBatchSaveVO.java", "vo/question/QuestionVO.java"
    )
    "resource" = @("api/ResourceQueryApi.java", "vo/ResourceVO.java")
    "notification" = @("api/NotificationWriteApi.java")
    "statistics" = @("api/KnowledgeMasteryQueryApi.java", "api/RecommendationQueryApi.java")
    "knowledge" = @(
        "api/ChunkQueryApi.java", "api/KnowledgeAccessApi.java", "api/KnowledgeGraphQueryApi.java",
        "api/KnowledgePointQueryApi.java", "api/KnowledgeQueryApi.java",
        "vo/graph/KnowledgeGraphVO.java", "vo/knowledge/ChunkVO.java",
        "vo/knowledge/KnowledgeBaseVO.java", "vo/knowledge/KnowledgeDocumentVO.java"
    )
    "teaching" = @(
        "api/ExamQueryApi.java", "api/SubmissionQueryApi.java",
        "vo/assignment/AssignmentVO.java", "vo/exam/ExamQuestionVO.java", "vo/exam/ExamVO.java",
        "vo/submission/SubmissionStatsVO.java"
    )
    "ai" = @(
        "api/AiAuditQueryApi.java", "api/AiChatApi.java", "api/AiGradingApi.java", "api/AiQueryApi.java",
        "api/QuestionGenerateApi.java", "api/embedding/EmbeddingApi.java", "api/graph/GraphRelationSuggestApi.java",
        "dto/QuestionGenerateDTO.java", "dto/SubjectiveGradingDTO.java",
        "vo/SubjectiveGradingVO.java", "vo/audit/AiUsageSummaryVO.java"
    )
}

function New-ApiModulePomIfMissing($artifactId, $extraDeps, $validation) {
    $dir = Join-Path $apiRoot $artifactId
    $pomPath = Join-Path $dir "pom.xml"
    if (Test-Path $pomPath) {
        return
    }
    New-Item -ItemType Directory -Force -Path (Join-Path $dir "src/main/java") | Out-Null
    $depsXml = @"
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
"@
    if ($validation) {
        $depsXml += @"

        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
"@
    }
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
        <artifactId>edu-mind-api</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>$artifactId</artifactId>
    <name>$artifactId</name>
    <description>跨模块公开 API 契约（无 Spring 实现）</description>
    <dependencies>
$depsXml
    </dependencies>
</project>
"@ | Set-Content -Encoding UTF8 $pomPath
    Write-Host "Created $pomPath"
}

function Copy-ContractRel($srcBase, $rel, $dstBase) {
    $src = Join-Path $srcBase $rel
    if (-not (Test-Path $src)) {
        Write-Warning "Missing source: $src"
        return
    }
    $dst = Join-Path $dstBase $rel
    $dstDir = Split-Path $dst -Parent
    if ($WhatIf) {
        Write-Host "[WhatIf] Copy $src -> $dst"
        return
    }
    New-Item -ItemType Directory -Force -Path $dstDir | Out-Null
    Copy-Item -Force $src $dst
}

function Sync-DomainContracts($domain, $artifactId) {
    $relPaths = $ContractManifest[$domain]
    if (-not $relPaths) {
        throw "No manifest for domain $domain"
    }
    $apiJavaBase = Join-Path $apiRoot "$artifactId/src/main/java/com/edumind/$domain"
    $bizJavaBase = Join-Path $modulesRoot "edu-mind-$domain/src/main/java/com/edumind/$domain"

    foreach ($rel in $relPaths) {
        if ($FromBusinessModules) {
            Copy-ContractRel $bizJavaBase $rel $apiJavaBase
            if ($PruneBusinessContracts) {
                $bizFile = Join-Path $bizJavaBase $rel
                if ((Test-Path $bizFile) -and -not $WhatIf) {
                    Remove-Item -Force $bizFile
                    Write-Host "Removed business duplicate: $bizFile"
                }
            }
        } else {
            # Ensure api tree exists; copy from business only when api file missing
            $apiFile = Join-Path $apiJavaBase $rel
            if (-not (Test-Path $apiFile)) {
                Copy-ContractRel $bizJavaBase $rel $apiJavaBase
            }
        }
    }
}

foreach ($m in $DomainModules) {
    New-ApiModulePomIfMissing $m.Artifact $m.ExtraDeps $m.Validation
    Sync-DomainContracts $m.Domain $m.Artifact
}

$verify = Join-Path $PSScriptRoot "verify-api-modules.ps1"
if (Test-Path $verify) {
    & $verify
}

Write-Host "API module sync finished. Run: mvn -pl edu-mind-api -am compile"
