$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$docs = Join-Path $root "docs"
$problems = @()

# 1. Every relative Markdown link must resolve. Directory links require a README.
Get-ChildItem $docs -Recurse -Filter *.md | ForEach-Object {
    $source = $_
    $content = Get-Content $source.FullName -Raw -Encoding UTF8
    [regex]::Matches($content, '\[[^\]]+\]\((?!https?://|mailto:|#)([^)]+)\)') | ForEach-Object {
        $target = $_.Groups[1].Value.Split('#')[0].Trim().Trim('<', '>')
        if (-not $target) { return }
        $resolved = Join-Path $source.DirectoryName $target
        if (-not (Test-Path -LiteralPath $resolved)) {
            $problems += "broken link: $($source.FullName) -> $target"
        } elseif ((Get-Item -LiteralPath $resolved).PSIsContainer -and -not (Test-Path -LiteralPath (Join-Path $resolved "README.md"))) {
            $problems += "link to directory without README.md: $($source.FullName) -> $target"
        }
    }
}

# 2. Every Markdown document must be reachable from docs/README.md or a linked subdirectory README.
$index = Join-Path $docs "README.md"
$linked = [System.Collections.Generic.HashSet[string]]::new()
function Add-Links([string]$file, [string]$prefix) {
    $content = Get-Content $file -Raw -Encoding UTF8
    [regex]::Matches($content, '\]\(([^)]+)\)') | ForEach-Object {
        $target = $_.Groups[1].Value.Split('#')[0].Trim().Trim('<', '>') -replace '^\./', ''
        if ($target -like '*.md' -and $target -notlike '../*') {
            [void]$linked.Add((($prefix + $target) -replace '\\', '/'))
        }
    }
}
Add-Links $index ""
foreach ($entry in @($linked)) {
    if ($entry -notlike '*/README.md') { continue }
    $subIndex = Join-Path $docs $entry
    if (Test-Path -LiteralPath $subIndex) {
        Add-Links $subIndex ($entry -replace 'README\.md$', '')
    }
}
Get-ChildItem $docs -Recurse -Filter *.md | ForEach-Object {
    $relative = ($_.FullName.Substring($docs.Length + 1)) -replace '\\', '/'
    if ($relative -ne 'README.md' -and -not $linked.Contains($relative)) {
        $problems += "not reachable from docs/README.md: $relative"
    }
}

# 3. Stage status vocabulary and report/roadmap/index consistency.
$legal = 'NOT_STARTED|IN_PROGRESS|COMPLETED|BLOCKED'
Get-ChildItem $docs -Recurse -Filter *.md | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $candidates = [regex]::Matches($content, '状态[：:]\s*`([A-Z][A-Z_]+)`') | ForEach-Object { $_.Groups[1].Value }
    foreach ($value in ($candidates | Select-Object -Unique)) {
        if ($value -notmatch "^($legal)$") {
            $problems += "illegal stage status '$value' in $($_.FullName)"
        }
    }
}

$stageDirectory = Join-Path $docs "stages"
$statusPattern = '`?(COMPLETED|IN_PROGRESS|BLOCKED|NOT_STARTED)`?'
$roadmap = Get-Content (Join-Path $stageDirectory "stage-roadmap.md") -Raw -Encoding UTF8
$stageIndex = Get-Content (Join-Path $stageDirectory "README.md") -Raw -Encoding UTF8
Get-ChildItem $stageDirectory -Filter 'stage-*-report.md' | ForEach-Object {
    if ($_.Name -notmatch '^stage-(\d+)-report\.md$') { return }
    $number = [int]$Matches[1]
    $head = (Get-Content $_.FullName -TotalCount 8 -Encoding UTF8) -join "`n"
    if ($head -notmatch ('状态[：:]\s*' + $statusPattern)) { return }
    $declared = $Matches[1]
    $roadmapRow = [regex]::Match($roadmap, "^\|\s*$number\s*\|.*$", 'Multiline')
    $indexRow = [regex]::Match($stageIndex, "^.*\(stage-$number-report\.md\).*$", 'Multiline')
    foreach ($pair in @(@{ Name = 'roadmap'; Row = $roadmapRow }, @{ Name = 'index'; Row = $indexRow })) {
        if (-not $pair.Row.Success) { continue }
        $found = [regex]::Match($pair.Row.Value, $statusPattern)
        if ($found.Success -and $found.Groups[1].Value -ne $declared) {
            $problems += "stage $number status mismatch: report=$declared but $($pair.Name)=$($found.Groups[1].Value)"
        }
    }
}

if ($problems) {
    $problems | ForEach-Object { Write-Error $_ -ErrorAction Continue }
    throw "Documentation validation failed with $($problems.Count) problem(s)"
}

[ordered]@{
    passed = $true
    checkedAt = (Get-Date).ToUniversalTime().ToString('o')
    markdownFiles = (Get-ChildItem $docs -Recurse -Filter *.md).Count
    indexedDocuments = $linked.Count
} | ConvertTo-Json
