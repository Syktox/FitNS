param(
    [string]$BaseUrl = 'https://pi.pufferfish-lenok.ts.net/',
    [switch]$IncludeMealImageAnalysis
)

$ErrorActionPreference = 'Stop'

function Get-EnvValue([string]$Name) {
    $line = Get-Content '.env' | Where-Object { $_ -match "^\s*$Name\s*=" } | Select-Object -First 1
    if (-not $line) { throw "$Name is missing from .env." }
    $value = ($line -replace "^\s*$Name\s*=\s*", '').Trim().Trim('"').Trim("'")
    if ([string]::IsNullOrWhiteSpace($value)) { throw "$Name is empty in .env." }
    return $value
}

$BaseUrl = $BaseUrl.TrimEnd('/')
$apiKey = Get-EnvValue 'N8N_API_KEY'
$webhookToken = Get-EnvValue 'FITNS_WEBHOOK_TOKEN'
$headers = @{ 'X-N8N-API-KEY' = $apiKey; 'Content-Type' = 'application/json' }

function Invoke-N8n([string]$Method, [string]$Path, $Body = $null) {
    $params = @{
        Uri = "$BaseUrl/api/v1$Path"
        Headers = $headers
        Method = $Method
        TimeoutSec = 30
    }
    if ($null -ne $Body) {
        $params.Body = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 30 -Compress }
    }
    return Invoke-RestMethod @params
}

function Ensure-DataTable([string]$Name) {
    $table = @(Invoke-N8n 'GET' '/data-tables?limit=100').data | Where-Object { $_.name -eq $Name } | Select-Object -First 1
    if ($table) { return $table }

    return Invoke-N8n 'POST' '/data-tables' @{
        name = $Name
        columns = @(
            @{ name = 'entityId'; type = 'string' },
            @{ name = 'operation'; type = 'string' },
            @{ name = 'idempotencyKey'; type = 'string' },
            @{ name = 'generatedAt'; type = 'number' },
            @{ name = 'receivedAt'; type = 'number' },
            @{ name = 'payloadJson'; type = 'string' }
        )
    }
}

function Ensure-HeaderCredential([string]$Name, [string]$HeaderName, [string]$HeaderValue) {
    $credential = @(Invoke-N8n 'GET' '/credentials?limit=100').data | Where-Object { $_.name -eq $Name } | Select-Object -First 1
    if ($credential) { return $credential }

    return Invoke-N8n 'POST' '/credentials' @{
        name = $Name
        type = 'httpHeaderAuth'
        data = @{ name = $HeaderName; value = $HeaderValue }
    }
}

function Get-Credential([string]$Name, [string]$Type) {
    $credential = @(Invoke-N8n 'GET' '/credentials?limit=100').data | Where-Object { $_.name -eq $Name -and $_.type -eq $Type } | Select-Object -First 1
    if (-not $credential) { throw "The required n8n credential '$Name' ($Type) does not exist." }
    return $credential
}

function Ensure-Workflow([string]$Definition) {
    $workflow = $Definition | ConvertFrom-Json
    $existing = @(Invoke-N8n 'GET' '/workflows?limit=100').data | Where-Object { $_.name -eq $workflow.name } | Select-Object -First 1
    if ($existing) {
        $created = Invoke-N8n 'PUT' ("/workflows/{0}" -f $existing.id) $Definition
    } else {
        $created = Invoke-N8n 'POST' '/workflows' $Definition
    }
    return Invoke-N8n 'POST' ("/workflows/{0}/activate" -f $created.id)
}

$nutritionTable = Ensure-DataTable 'fitns_nutrition_sync'
$workoutTable = Ensure-DataTable 'fitns_workout_sync'
$bodyWeightTable = Ensure-DataTable 'fitns_body_weight_sync'
$profileTable = Ensure-DataTable 'fitns_profile_sync'
$webhookCredential = Ensure-HeaderCredential 'FitNS Webhook Bearer Token' 'Authorization' ("Bearer {0}" -f $webhookToken)
$dataTableCredential = Ensure-HeaderCredential 'FitNS Data Table API' 'X-N8N-API-KEY' $apiKey
$openAiCredential = Get-Credential 'OpenAI account' 'openAiApi'

$healthWorkflow = @'
{
  "name": "FitNS - Health",
  "nodes": [
    {
      "parameters": { "httpMethod": "GET", "path": "health", "responseMode": "lastNode", "authentication": "headerAuth", "options": {} },
      "id": "0d76425b-4f12-454a-bb57-2d0cabdbab70",
      "name": "Health Webhook",
      "type": "n8n-nodes-base.webhook",
      "typeVersion": 2.1,
      "position": [0, 0],
      "webhookId": "0d76425b-4f12-454a-bb57-2d0cabdbab70",
      "credentials": { "httpHeaderAuth": { "id": "__WEBHOOK_CREDENTIAL_ID__", "name": "FitNS Webhook Bearer Token" } }
    },
    {
      "parameters": { "mode": "runOnceForAllItems", "jsCode": "return [{ json: { status: 'ok', service: 'fitns' } }];" },
      "id": "3a3bc765-1295-4698-a901-e213672f9d15",
      "name": "Health Response",
      "type": "n8n-nodes-base.code",
      "typeVersion": 2,
      "position": [280, 0]
    }
  ],
  "connections": { "Health Webhook": { "main": [[{ "node": "Health Response", "type": "main", "index": 0 }]] } },
  "settings": { "executionOrder": "v1" }
}
'@.Replace('__WEBHOOK_CREDENTIAL_ID__', $webhookCredential.id)

$barcodeWorkflow = @'
{
  "name": "FitNS - Barcode Lookup",
  "nodes": [
    {
      "parameters": { "httpMethod": "POST", "path": "food/barcode", "responseMode": "lastNode", "authentication": "headerAuth", "options": {} },
      "id": "11111111-1111-4111-8111-111111111111",
      "name": "Barcode Webhook",
      "type": "n8n-nodes-base.webhook",
      "typeVersion": 2.1,
      "position": [0, 0],
      "webhookId": "11111111-1111-4111-8111-111111111111",
      "credentials": { "httpHeaderAuth": { "id": "__WEBHOOK_CREDENTIAL_ID__", "name": "FitNS Webhook Bearer Token" } }
    },
    {
      "parameters": { "url": "={{ 'https://world.openfoodfacts.org/api/v2/product/' + encodeURIComponent($json.body.barcode) + '.json' }}", "options": {} },
      "id": "22222222-2222-4222-8222-222222222222",
      "name": "Open Food Facts",
      "type": "n8n-nodes-base.httpRequest",
      "typeVersion": 4.2,
      "position": [280, 0]
    },
    {
      "parameters": { "mode": "runOnceForEachItem", "jsCode": "const request = $node['Barcode Webhook'].json; const product = $json.product ?? {}; const nutrients = product.nutriments ?? {}; const number = (value) => Number.isFinite(Number(value)) ? Number(value) : 0; const barcode = product.code ?? request.body?.barcode ?? null; if ($json.status !== 1 || !product.product_name) return { json: { found: false, product: null } }; return { json: { found: true, product: { barcode, name: product.product_name, brand: product.brands ?? null, servingSizeGrams: product.serving_quantity ? number(product.serving_quantity) : null, nutritionPer100g: { caloriesKcal: number(nutrients['energy-kcal_100g'] ?? nutrients.energy_kcal_100g), proteinGrams: number(nutrients.proteins_100g), carbohydratesGrams: number(nutrients.carbohydrates_100g), sugarGrams: number(nutrients.sugars_100g), fatGrams: number(nutrients.fat_100g), saturatedFatGrams: number(nutrients['saturated-fat_100g']), fiberGrams: number(nutrients.fiber_100g), saltGrams: number(nutrients.salt_100g) } } } };" },
      "id": "33333333-3333-4333-8333-333333333333",
      "name": "Map Barcode Result",
      "type": "n8n-nodes-base.code",
      "typeVersion": 2,
      "position": [560, 0]
    }
  ],
  "connections": {
    "Barcode Webhook": { "main": [[{ "node": "Open Food Facts", "type": "main", "index": 0 }]] },
    "Open Food Facts": { "main": [[{ "node": "Map Barcode Result", "type": "main", "index": 0 }]] }
  },
  "settings": { "executionOrder": "v1" }
}
'@.Replace('__WEBHOOK_CREDENTIAL_ID__', $webhookCredential.id)

$mealAnalysisWorkflow = @'
{
  "name": "FitNS - Meal Image Analysis",
  "nodes": [
    {
      "parameters": { "httpMethod": "POST", "path": "food/analyze-image", "responseMode": "lastNode", "authentication": "headerAuth", "options": {} },
      "id": "12121212-1212-4121-8121-121212121212",
      "name": "Meal Analysis Webhook",
      "type": "n8n-nodes-base.webhook",
      "typeVersion": 2.1,
      "position": [0, 0],
      "webhookId": "12121212-1212-4121-8121-121212121212",
      "credentials": { "httpHeaderAuth": { "id": "__WEBHOOK_CREDENTIAL_ID__", "name": "FitNS Webhook Bearer Token" } }
    },
    {
      "parameters": { "mode": "runOnceForEachItem", "jsCode": "if ($json.body?.consentGranted !== true) throw new Error('Image-analysis consent is required.'); if (!$json.body?.imageBase64) throw new Error('imageBase64 is required.'); return { json: $json };" },
      "id": "13131313-1313-4131-8131-131313131313",
      "name": "Validate Image Request",
      "type": "n8n-nodes-base.code",
      "typeVersion": 2,
      "position": [260, 0]
    },
    {
      "parameters": { "method": "POST", "url": "https://api.openai.com/v1/chat/completions", "authentication": "predefinedCredentialType", "nodeCredentialType": "openAiApi", "sendBody": true, "specifyBody": "json", "jsonBody": "={{ { model: 'gpt-4o', response_format: { type: 'json_schema', json_schema: { name: 'meal_analysis', strict: true, schema: { type: 'object', additionalProperties: false, properties: { items: { type: 'array', items: { type: 'object', additionalProperties: false, properties: { name: { type: 'string' }, estimatedGrams: { type: 'number' }, confidence: { type: 'number' }, nutrition: { type: 'object', additionalProperties: false, properties: { caloriesKcal: { type: 'number' }, proteinGrams: { type: 'number' }, carbohydratesGrams: { type: 'number' }, sugarGrams: { type: 'number' }, fatGrams: { type: 'number' }, saturatedFatGrams: { type: 'number' }, fiberGrams: { type: 'number' }, saltGrams: { type: 'number' } }, required: ['caloriesKcal', 'proteinGrams', 'carbohydratesGrams', 'sugarGrams', 'fatGrams', 'saturatedFatGrams', 'fiberGrams', 'saltGrams'] } }, required: ['name', 'estimatedGrams', 'confidence', 'nutrition'] } }, total: { type: 'object', additionalProperties: false, properties: { caloriesKcal: { type: 'number' }, proteinGrams: { type: 'number' }, carbohydratesGrams: { type: 'number' }, sugarGrams: { type: 'number' }, fatGrams: { type: 'number' }, saturatedFatGrams: { type: 'number' }, fiberGrams: { type: 'number' }, saltGrams: { type: 'number' } }, required: ['caloriesKcal', 'proteinGrams', 'carbohydratesGrams', 'sugarGrams', 'fatGrams', 'saturatedFatGrams', 'fiberGrams', 'saltGrams'] }, disclaimer: { type: 'string' } }, required: ['items', 'total', 'disclaimer'] } } }, messages: [ { role: 'system', content: 'Estimate meal nutrition from the provided photo. Treat all values as estimates. Identify visible food items only. Return an empty items array when no meal can be identified.' }, { role: 'user', content: [ { type: 'text', text: 'Analyze this meal photo. Return nutrition totals and a concise German disclaimer.' }, { type: 'image_url', image_url: { url: 'data:image/jpeg;base64,' + $node['Validate Image Request'].json.body.imageBase64, detail: 'low' } } ] } ] } }}", "options": {} },
      "id": "14141414-1414-4141-8141-141414141414",
      "name": "OpenAI Meal Vision",
      "type": "n8n-nodes-base.httpRequest",
      "typeVersion": 4.2,
      "position": [540, 0],
      "credentials": { "openAiApi": { "id": "__OPENAI_CREDENTIAL_ID__", "name": "OpenAI account" } }
    },
    {
      "parameters": { "mode": "runOnceForEachItem", "jsCode": "const content = $json.choices?.[0]?.message?.content; if (!content) throw new Error('OpenAI returned no analysis.'); return { json: JSON.parse(content) };" },
      "id": "15151515-1515-4151-8151-151515151515",
      "name": "Map Meal Analysis",
      "type": "n8n-nodes-base.code",
      "typeVersion": 2,
      "position": [820, 0]
    }
  ],
  "connections": {
    "Meal Analysis Webhook": { "main": [[{ "node": "Validate Image Request", "type": "main", "index": 0 }]] },
    "Validate Image Request": { "main": [[{ "node": "OpenAI Meal Vision", "type": "main", "index": 0 }]] },
    "OpenAI Meal Vision": { "main": [[{ "node": "Map Meal Analysis", "type": "main", "index": 0 }]] }
  },
  "settings": { "executionOrder": "v1" }
}
'@.Replace('__WEBHOOK_CREDENTIAL_ID__', $webhookCredential.id).Replace('__OPENAI_CREDENTIAL_ID__', $openAiCredential.id)

function New-SyncWorkflow([string]$Name, [string]$Path, [string]$PayloadProperty, [string]$TableId, [string]$WebhookId, [string]$PrepareId, [string]$RequestId, [string]$ResponseId) {
    $definition = @'
{
  "name": "__NAME__",
  "nodes": [
    {
      "parameters": { "httpMethod": "POST", "path": "__PATH__", "responseMode": "lastNode", "authentication": "headerAuth", "options": {} },
      "id": "__WEBHOOK_ID__",
      "name": "Sync Webhook",
      "type": "n8n-nodes-base.webhook",
      "typeVersion": 2.1,
      "position": [0, 0],
      "webhookId": "__WEBHOOK_ID__",
      "credentials": { "httpHeaderAuth": { "id": "__WEBHOOK_CREDENTIAL_ID__", "name": "FitNS Webhook Bearer Token" } }
    },
    {
      "parameters": { "mode": "runOnceForEachItem", "jsCode": "const payload = $json.body ?? {}; const entry = payload.__PAYLOAD_PROPERTY__; if (!entry?.id) throw new Error('FitNS payload is missing __PAYLOAD_PROPERTY__.id'); return { json: { entityId: entry.id, operation: String(payload.operation ?? 'upsert'), idempotencyKey: String($json.headers?.['idempotency-key'] ?? ''), generatedAt: Number(payload.generatedAt ?? Date.now()), receivedAt: Date.now(), payloadJson: JSON.stringify(payload) } };" },
      "id": "__PREPARE_ID__",
      "name": "Prepare Sync Row",
      "type": "n8n-nodes-base.code",
      "typeVersion": 2,
      "position": [280, 0]
    },
    {
      "parameters": { "method": "POST", "url": "__BASE_URL__/api/v1/data-tables/__TABLE_ID__/rows/upsert", "authentication": "genericCredentialType", "genericAuthType": "httpHeaderAuth", "sendBody": true, "specifyBody": "json", "jsonBody": "={{ { filter: { type: 'and', filters: [{ columnName: 'entityId', condition: 'eq', value: $json.entityId }] }, data: { entityId: $json.entityId, operation: $json.operation, idempotencyKey: $json.idempotencyKey, generatedAt: $json.generatedAt, receivedAt: $json.receivedAt, payloadJson: $json.payloadJson }, returnData: false, dryRun: false } }}", "options": {} },
      "id": "__REQUEST_ID__",
      "name": "Upsert Sync Row",
      "type": "n8n-nodes-base.httpRequest",
      "typeVersion": 4.2,
      "position": [560, 0],
      "credentials": { "httpHeaderAuth": { "id": "__DATA_TABLE_CREDENTIAL_ID__", "name": "FitNS Data Table API" } }
    },
    {
      "parameters": { "mode": "runOnceForAllItems", "jsCode": "return [{ json: { accepted: true } }];" },
      "id": "__RESPONSE_ID__",
      "name": "Sync Response",
      "type": "n8n-nodes-base.code",
      "typeVersion": 2,
      "position": [840, 0]
    }
  ],
  "connections": {
    "Sync Webhook": { "main": [[{ "node": "Prepare Sync Row", "type": "main", "index": 0 }]] },
    "Prepare Sync Row": { "main": [[{ "node": "Upsert Sync Row", "type": "main", "index": 0 }]] },
    "Upsert Sync Row": { "main": [[{ "node": "Sync Response", "type": "main", "index": 0 }]] }
  },
  "settings": { "executionOrder": "v1" }
}
'@

    return $definition.Replace('__NAME__', $Name).Replace('__PATH__', $Path).Replace('__PAYLOAD_PROPERTY__', $PayloadProperty).Replace('__TABLE_ID__', $TableId).Replace('__BASE_URL__', $BaseUrl).Replace('__WEBHOOK_ID__', $WebhookId).Replace('__PREPARE_ID__', $PrepareId).Replace('__REQUEST_ID__', $RequestId).Replace('__RESPONSE_ID__', $ResponseId).Replace('__WEBHOOK_CREDENTIAL_ID__', $webhookCredential.id).Replace('__DATA_TABLE_CREDENTIAL_ID__', $dataTableCredential.id)
}

$nutritionWorkflow = New-SyncWorkflow 'FitNS - Nutrition Sync' 'nutrition/sync' 'foodEntry' $nutritionTable.id '44444444-4444-4444-8444-444444444444' '55555555-5555-4555-8555-555555555555' '66666666-6666-4666-8666-666666666666' '77777777-7777-4777-8777-777777777777'
$workoutWorkflow = New-SyncWorkflow 'FitNS - Workout Sync' 'workout/sync' 'workout' $workoutTable.id '88888888-8888-4888-8888-888888888888' '99999999-9999-4999-8999-999999999999' 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa' 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb'
$bodyWeightWorkflow = New-SyncWorkflow 'FitNS - Body Weight Sync' 'body-weight/sync' 'bodyWeightEntry' $bodyWeightTable.id 'cccccccc-cccc-4ccc-8ccc-cccccccccccc' 'dddddddd-dddd-4ddd-8ddd-dddddddddddd' 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee' 'ffffffff-ffff-4fff-8fff-ffffffffffff'
$profileWorkflow = New-SyncWorkflow 'FitNS - Profile Sync' 'profile/sync' 'profile' $profileTable.id '16161616-1616-4161-8161-161616161616' '17171717-1717-4171-8171-171717171717' '18181818-1818-4181-8181-181818181818' '19191919-1919-4191-8191-191919191919'

$activeWorkflows = @(
    Ensure-Workflow $healthWorkflow
    Ensure-Workflow $barcodeWorkflow
    Ensure-Workflow $nutritionWorkflow
    Ensure-Workflow $workoutWorkflow
    Ensure-Workflow $bodyWeightWorkflow
    Ensure-Workflow $profileWorkflow
)
if ($IncludeMealImageAnalysis) {
    $activeWorkflows += Ensure-Workflow $mealAnalysisWorkflow
}

$activeWorkflows | Select-Object name, id, active | Format-Table -AutoSize
