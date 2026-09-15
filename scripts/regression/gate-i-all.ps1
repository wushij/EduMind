# =============================================================================
# EduMind V2.0 RC Full Regression & Gate Suite (PowerShell)
# =============================================================================

$ErrorActionPreference = "Continue"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = (Resolve-Path "$ScriptDir/../..").Path
$BackendDir = Join-Path $RootDir "backend"
$FrontendDir = Join-Path $RootDir "frontend"

Write-Host "=============================================================================" -ForegroundColor Cyan
Write-Host " EduMind V2.0 RC - Full Regression Test Suite Starting" -ForegroundColor Cyan
Write-Host " Workspace: $RootDir" -ForegroundColor Gray
Write-Host "=============================================================================" -ForegroundColor Cyan

$Results = [System.Collections.Generic.List[PSCustomObject]]::new()

function Run-Step {
    param(
        [string]$Name,
        [string]$WorkDir,
        [string]$Command,
        [int]$MaxRetries = 0
    )
    Write-Host "`n>>> [RUNNING] $Name ..." -ForegroundColor Yellow
    $startTime = Get-Date
    $attempt = 0
    $exitCode = 1
    while ($attempt -le $MaxRetries) {
        if ($attempt -gt 0) {
            Write-Host ">>> [RETRY #$attempt] $Name (Retrying once)..." -ForegroundColor Yellow
        }
        Push-Location $WorkDir
        try {
            Invoke-Expression $Command
            $exitCode = $LASTEXITCODE
        } catch {
            $exitCode = 1
        } finally {
            Pop-Location
        }
        if ($exitCode -eq 0) {
            break
        }
        $attempt++
    }
    $endTime = Get-Date
    $duration = [math]::Round(($endTime - $startTime).TotalSeconds, 2)

    $status = if ($exitCode -eq 0) { "PASS" } else { "FAIL" }
    $color = if ($exitCode -eq 0) { "Green" } else { "Red" }
    Write-Host ">>> [$status] $Name (Duration: ${duration}s, ExitCode: $exitCode)" -ForegroundColor $color

    $Results.Add([PSCustomObject]@{
        Step = $Name
        Status = $status
        Duration = "${duration}s"
        ExitCode = $exitCode
    })

    return ($exitCode -eq 0)
}

# 1. Backend Clean & Install
Run-Step -Name "1. Backend Clean & Install" -WorkDir $BackendDir -Command "mvn clean install -DskipTests -pl edu-mind-boot -am"

# 2. Gate I1~I11 Integration Tests
Run-Step -Name "Gate I1/I2: TenantQuota & DataIsolation" -WorkDir $BackendDir -Command 'mvn test -pl edu-mind-boot "-Dtest=TenantQuotaIntegrationTest,TenantDataIsolationIntegrationTest"'
Run-Step -Name "Gate I3: NotificationBroadcast" -WorkDir $BackendDir -Command "mvn test -pl edu-mind-boot -Dtest=NotificationBroadcastIntegrationTest"
Run-Step -Name "Gate I4: KnowledgeOcr" -WorkDir $BackendDir -Command "mvn test -pl edu-mind-boot -Dtest=KnowledgeOcrIntegrationTest"
Run-Step -Name "Gate I5/I8: TeachingIntervention" -WorkDir $BackendDir -Command "mvn test -pl edu-mind-boot -Dtest=TeachingInterventionIntegrationTest"
Run-Step -Name "Gate I6: AgentMemory" -WorkDir $BackendDir -Command "mvn test -pl edu-mind-boot -Dtest=AgentMemoryIntegrationTest"
Run-Step -Name "Gate I7: ExportTask" -WorkDir $BackendDir -Command "mvn test -pl edu-mind-boot -Dtest=ExportTaskIntegrationTest"
Run-Step -Name "Gate I9: SecurityKeyKms" -WorkDir $BackendDir -Command "mvn test -pl edu-mind-boot -Dtest=SecurityKeyKmsIntegrationTest"
Run-Step -Name "Gate I10: AiModelKeyKms" -WorkDir $BackendDir -Command "mvn test -pl edu-mind-boot -Dtest=AiModelKeyKmsIntegrationTest"
Run-Step -Name "Gate I11: OperationLog (Round 1)" -WorkDir $BackendDir -Command "mvn test -pl edu-mind-boot -Dtest=OperationLogIntegrationTest" -MaxRetries 1
Run-Step -Name "Gate I11: OperationLog (Round 2 Idempotent)" -WorkDir $BackendDir -Command "mvn test -pl edu-mind-boot -Dtest=OperationLogIntegrationTest" -MaxRetries 1

# 3. Baseline Compatibility
Run-Step -Name "Baseline: GateV10 (V1.0)" -WorkDir $BackendDir -Command 'mvn test -pl edu-mind-boot "-DgateG.integration=true" -Dtest=GateV10IntegrationTest'
Run-Step -Name "Baseline: GateV11 (V1.1)" -WorkDir $BackendDir -Command 'mvn test -pl edu-mind-boot "-DgateH.integration=true" -Dtest=GateV11IntegrationTest'

# 4. Frontend Build
Run-Step -Name "Frontend Build (TypeCheck & Vite)" -WorkDir $FrontendDir -Command "npm run build"

# 5. GA Suite (Wave1/2 + Security + Two-Tenant E2E)
Run-Step -Name "Gate GA-1: TenantLegacy Migration" -WorkDir $BackendDir -Command 'mvn test -pl edu-mind-boot "-Dtest=TenantLegacyWave1IntegrationTest,TenantLegacyMigrationIntegrationTest"'
Run-Step -Name "Gate GA-3: TwoTenant + Security" -WorkDir $BackendDir -Command 'mvn test -pl edu-mind-boot "-Dtest=TwoTenantGaIntegrationTest,TenantSecurityIntegrationTest"'
Run-Step -Name "Gate GA: AiToolManage" -WorkDir $BackendDir -Command "mvn test -pl edu-mind-boot -Dtest=AiToolManageIntegrationTest"

# 6. Summary
Write-Host ""
Write-Host "=============================================================================" -ForegroundColor Cyan
Write-Host " EduMind V2.0 GA Full Regression Summary Report" -ForegroundColor Cyan
Write-Host "=============================================================================" -ForegroundColor Cyan
$Results | Format-Table -AutoSize

$failCount = ($Results | Where-Object { $_.Status -ne "PASS" }).Count
if ($failCount -eq 0) {
    Write-Host "`n[SUCCESS] All tests and builds PASSED! V2.0 GA is ready for sign-off!" -ForegroundColor Green
    exit 0
}

Write-Host "`n[FAILURE] Regression suite failed with $failCount error(s)!" -ForegroundColor Red
exit 1
