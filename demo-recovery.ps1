# ============================================================
# MAPREDUCE DEMO RECOVERY TOOLKIT
# ============================================================

$global:clientSecret = "oE67FIe0ih7T5VVD0zk5baNGZ3ViQuhA"

Write-Host "=== MAPREDUCE DEMO RECOVERY TOOLKIT ===" -ForegroundColor Cyan

# === CHECK EVERYTHING ===
function Check-Status {
    Write-Host "`n[STATUS CHECK]" -ForegroundColor Yellow
    kubectl get pods -n mapreduce-system
    kubectl get pods -n mapreduce-workers
    kubectl get pods -n mapreduce-infra
}

# === FIX 1: Restart port-forwards ===
function Start-PortForwards {
    Write-Host "`n[STARTING PORT-FORWARDS]" -ForegroundColor Yellow
    $POD = kubectl get pod -n mapreduce-system -l app=manager-service -o jsonpath='{.items[0].metadata.name}'
    
    # Manager port-forward with auto-restart loop
    Start-Process powershell -ArgumentList "-NoExit", "-Command", `
        "while(`$true) { kubectl port-forward -n mapreduce-system pod/$POD 9081:8091; Start-Sleep -Seconds 2 }"
    
    # Keycloak port-forward with auto-restart loop
    Start-Process powershell -ArgumentList "-NoExit", "-Command", `
        "while(`$true) { kubectl port-forward svc/keycloak -n mapreduce-infra 9082:8080; Start-Sleep -Seconds 2 }"
    
    Write-Host "Port-forwards started with auto-restart in new terminals" -ForegroundColor Green
}

# === FIX 2: Re-register workers ===
function Fix-Workers {
    Write-Host "`n[RE-REGISTERING WORKERS]" -ForegroundColor Yellow
    kubectl rollout restart deployment/worker-service -n mapreduce-workers
    Write-Host "Workers restarting..." -ForegroundColor Green
    Start-Sleep -Seconds 10
    kubectl get pods -n mapreduce-workers
}

# === FIX 3: Get fresh token ===
function Get-Token {
    Write-Host "`n[GETTING FRESH TOKEN]" -ForegroundColor Yellow
    $response = Invoke-WebRequest -Uri "http://localhost:9082/realms/mapreduce/protocol/openid-connect/token" `
        -Method POST -ContentType "application/x-www-form-urlencoded" `
        -Body "client_id=mapreduce-engine&client_secret=$global:clientSecret&grant_type=client_credentials" `
        -UseBasicParsing
    $global:token = ($response.Content | ConvertFrom-Json).access_token
    Write-Host "Token obtained: $($global:token.Substring(0,20))..." -ForegroundColor Green
}

# === FIX 4: Fix Keycloak (when client is lost after pod restart) ===
function Fix-Keycloak {
    param($newSecret)
    Write-Host "`n[FIXING KEYCLOAK]" -ForegroundColor Yellow

    # Update global secret
    $global:clientSecret = $newSecret

    # Update K8s secret
    kubectl delete secret mapreduce-auth-secret -n mapreduce-system 2>$null
    kubectl create secret generic mapreduce-auth-secret `
        --from-literal=client-secret=$newSecret `
        -n mapreduce-system

    # Restart manager to pick up new secret
    kubectl rollout restart deployment/manager-service -n mapreduce-system
    Write-Host "Waiting for manager to restart..." -ForegroundColor Yellow
    Start-Sleep -Seconds 20

    # Re-export realm to ConfigMap so it survives next restart
    $adminResp = Invoke-WebRequest -Uri "http://localhost:9082/realms/master/protocol/openid-connect/token" `
        -Method POST -ContentType "application/x-www-form-urlencoded" `
        -Body "client_id=admin-cli&username=admin&password=admin1234&grant_type=password" `
        -UseBasicParsing
    $adminToken = ($adminResp.Content | ConvertFrom-Json).access_token

    $export = Invoke-WebRequest -Uri "http://localhost:9082/admin/realms/mapreduce" `
        -Headers @{Authorization = "Bearer $adminToken"} -UseBasicParsing
    $export.Content | Out-File -FilePath "k8s/infra/mapreduce-realm.json" -Encoding UTF8

    kubectl delete configmap keycloak-realm-config -n mapreduce-infra 2>$null
    kubectl create configmap keycloak-realm-config `
        --from-file=mapreduce-realm.json=k8s/infra/mapreduce-realm.json `
        -n mapreduce-infra
    kubectl rollout restart deployment/keycloak -n mapreduce-infra

    Write-Host "Keycloak fixed! New secret saved to ConfigMap." -ForegroundColor Green
    Write-Host "Run Get-Token to verify." -ForegroundColor Green
}

# === FIX 5: Full recovery (nuclear option) ===
function Full-Recovery {
    Write-Host "`n[FULL RECOVERY]" -ForegroundColor Red
    kubectl rollout restart deployment/manager-service -n mapreduce-system
    kubectl rollout restart deployment/worker-service -n mapreduce-workers
    Write-Host "Waiting 30 seconds for pods to restart..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
    kubectl get pods -n mapreduce-system
    kubectl get pods -n mapreduce-workers
    Start-PortForwards
    Get-Token
}

# === SUBMIT JOB ===
function Submit-Job {
    param($name, $inputFile, $output, $mappers=2, $reducers=2)
    Get-Token
    $t = $global:token
    Write-Host "Using token: $($t.Substring(0,20))..." -ForegroundColor Gray
    $body = "{`"name`":`"$name`",`"inputPath`":`"$inputFile`",`"outputPath`":`"$output`",`"numMappers`":$mappers,`"numReducers`":$reducers}"
    try {
        $job = Invoke-WebRequest -Uri "http://localhost:9081/api/v1/jobs" `
            -Method POST -ContentType "application/json" `
            -Headers @{Authorization = "Bearer $t"} `
            -Body $body -UseBasicParsing
        $global:jobId = ($job.Content | ConvertFrom-Json).id
        Write-Host "Job submitted: $global:jobId" -ForegroundColor Green
    } catch {
        Write-Host "Error: $_" -ForegroundColor Red
    }
}

# === POLL JOB ===
function Watch-Job {
    param($jobId)
    if (-not $jobId) { $jobId = $global:jobId }
    Get-Token
    while($true) {
        try {
            $s = (Invoke-WebRequest -Uri "http://localhost:9081/api/v1/jobs/$jobId" `
                -Headers @{Authorization = "Bearer $global:token"} -UseBasicParsing).Content |
                ConvertFrom-Json | Select-Object -ExpandProperty status
            Write-Host "$(Get-Date -Format 'HH:mm:ss') - $s" -ForegroundColor Cyan
            if($s -eq "COMPLETED") { Write-Host "JOB COMPLETED!" -ForegroundColor Green; break }
            if($s -eq "FAILED")    { Write-Host "JOB FAILED!"    -ForegroundColor Red;   break }
        } catch {
            Write-Host "Refreshing token..." -ForegroundColor Yellow
            Get-Token
        }
        Start-Sleep -Seconds 3
    }
}

# === GET RESULT ===
function Get-Result {
    param($jobId)
    if (-not $jobId) { $jobId = $global:jobId }
    Get-Token
    $result = (Invoke-WebRequest -Uri "http://localhost:9081/api/v1/jobs/$jobId/result" `
        -Headers @{Authorization = "Bearer $global:token"} -UseBasicParsing).Content
    Write-Host $result -ForegroundColor Green
}

# === WATCH MANAGER LOGS ===
function Watch-Logs {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", `
        "kubectl logs -n mapreduce-system deployment/manager-service -f | Select-String 'c\.m\.m\.'"
}

Write-Host @"

AVAILABLE COMMANDS:
  Check-Status                      - check all pods
  Start-PortForwards                - restart port-forwards in new terminals
  Fix-Workers                       - restart worker pods (fixes unknown worker)
  Get-Token                         - get fresh Keycloak token
  Fix-Keycloak -newSecret 'SECRET'  - recreate client after Keycloak restart
  Full-Recovery                     - restart everything
  Submit-Job                        - submit a job
  Watch-Job                         - poll job status
  Get-Result                        - get job result
  Watch-Logs                        - open manager logs in new terminal

DEMO FLOW:
  Check-Status
  Watch-Logs
  Submit-Job -name 'demo-wordcount' -inputFile 'word-count-test.txt' -output 'output/demo-1'
  Watch-Job
  Get-Result

FAULT TOLERANCE DEMO:
  Submit-Job -name 'demo-fault' -inputFile 'word-count-test.txt' -output 'output/demo-fault' -mappers 4
  kubectl delete pod -n mapreduce-workers -l app=worker-service --wait=false
  Watch-Job

IF KEYCLOAK LOSES CLIENT (invalid_client error):
  1. Open http://localhost:8082 -> create mapreduce-engine client
  2. Fix-Keycloak -newSecret 'THE_NEW_SECRET'

"@ -ForegroundColor White
