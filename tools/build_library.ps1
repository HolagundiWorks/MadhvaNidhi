$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$vendor = Join-Path (Split-Path -Parent $root) "local-artifacts\acharya\upstream-sources"
$out = Join-Path $root "app\src\main\assets\library"

function Get-TitleFromName([string]$stem) {
    $s = $stem -replace "[_-]+", " "
    $s = [regex]::Replace($s, "([a-z])([A-Z])", '$1 $2')
    return ($s -replace "\s+", " ").Trim()
}

function Convert-Tex([string]$raw) {
    $title = ""
    $m = [regex]::Match($raw, "wiki_title:\s*(.+)")
    if ($m.Success) { $title = $m.Groups[1].Value.Trim() }
    $cm = [regex]::Match($raw, "\\(?:chapt|chapter|sect|section)\*?\{([^{}]+)\}")
    if ($cm.Success -and $cm.Groups[1].Value.Trim()) { $title = $cm.Groups[1].Value.Trim() }

    $text = [regex]::Replace($raw, "(?<!\\)%[^\n]*", "")
    $text = [regex]::Replace($text, "%\s*!TeX[^\n]*", "")
    $text = [regex]::Replace($text, "%\s*--meta--[\s\S]*?%?\s*--end-meta--", "")
    $text = [regex]::Replace($text, "\\twolineshloka\*?\{([^{}]*)\}\s*\{([^{}]*)\}", { param($x) ($x.Groups[1].Value.Trim() + "`n" + $x.Groups[2].Value.Trim()) })
    $text = [regex]::Replace($text, "\\threelineshloka\*?\{([^{}]*)\}\s*\{([^{}]*)\}\s*\{([^{}]*)\}", { param($x) ($x.Groups[1].Value.Trim() + "`n" + $x.Groups[2].Value.Trim() + "`n" + $x.Groups[3].Value.Trim()) })
    $text = [regex]::Replace($text, "\\fourlineshloka\*?\{([^{}]*)\}\s*\{([^{}]*)\}\s*\{([^{}]*)\}\s*\{([^{}]*)\}", { param($x) ($x.Groups[1].Value.Trim() + "`n" + $x.Groups[2].Value.Trim() + "`n" + $x.Groups[3].Value.Trim() + "`n" + $x.Groups[4].Value.Trim()) })
    $text = $text.Replace("~", " ").Replace("\\", "`n").Replace("\,", " ").Replace("\-", "").Replace("\&", "&")
    for ($i = 0; $i -lt 4; $i++) {
        $text = [regex]::Replace($text, "\\[A-Za-z]+\*?\{([^{}]*)\}", '$1')
    }
    $text = [regex]::Replace($text, "\\[A-Za-z]+\*?", "")
    $text = $text.Replace("{", "").Replace("}", "")
    $lines = New-Object System.Collections.Generic.List[string]
    $blank = $false
    foreach ($line in $text -split "`r?`n") {
        $line = ([regex]::Replace($line, "[ \t]+", " ")).Trim()
        if (-not $line) {
            if (-not $blank) { $lines.Add("") }
            $blank = $true
            continue
        }
        $blank = $false
        $lines.Add($line)
    }
    $body = ($lines -join "`n").Trim()
    return @{ Title = $title; Body = $body }
}

function Collect-Lib($src, $outName, $sectionFromParent) {
    $dest = Join-Path $out $outName
    New-Item -ItemType Directory -Force -Path $dest | Out-Null
    $items = @()
    $used = @{}
    $skip = "kindle|learn|preamble|frontmatter|shloka\.sty|nAmA\.tex|nAmAvalis|compact"
    Get-ChildItem -Path $src -Recurse -Filter *.tex | Sort-Object FullName | ForEach-Object {
        $rel = $_.FullName.Substring($src.Length)
        if ($_.Name -match $skip -or $rel -match $skip) { return }
        $raw = [System.IO.File]::ReadAllText($_.FullName, [System.Text.Encoding]::UTF8)
        $conv = Convert-Tex $raw
        if ($conv.Body.Length -lt 40) { return }
        $ident = ($_.BaseName -replace "[^A-Za-z0-9._-]+", "_").Trim("_")
        if (-not $ident) { $ident = "item" }
        if ($used.ContainsKey($ident)) { $ident = $ident + "_" + $_.Directory.Name }
        $used[$ident] = $true
        $title = if ($conv.Title) { $conv.Title } else { Get-TitleFromName $_.BaseName }
        $section = if ($sectionFromParent) { $_.Directory.Name } else { $outName }
        [System.IO.File]::WriteAllText((Join-Path $dest "$ident.txt"), $conv.Body, [System.Text.UTF8Encoding]::new($false))
        $words = $conv.Body -split "\s+" | Select-Object -First 12
        $preview = ($words -join " ")
        $items += [ordered]@{ id = $ident; title = $title; section = $section; preview = $preview }
    }
    $index = [ordered]@{ source = (Split-Path $src -Leaf); items = $items }
    $json = $index | ConvertTo-Json -Depth 6 -Compress
    [System.IO.File]::WriteAllText((Join-Path $dest "index.json"), $json, [System.Text.UTF8Encoding]::new($false))
    Write-Host "$outName : $($items.Count) texts"
}

New-Item -ItemType Directory -Force -Path $out | Out-Null
Collect-Lib (Join-Path $vendor "vedamantra-book\mantras") "mantras" $false
Collect-Lib (Join-Path $vendor "puja-vidhanam") "puja" $true
Collect-Lib (Join-Path $vendor "stotra-sangrahah\stotras") "stotra" $true
Collect-Lib (Join-Path $vendor "namavali-manjari") "namavali" $true
[System.IO.File]::WriteAllText(
    (Join-Path $out "SOURCE.txt"),
    "Texts forked from Stotra Samhita (https://github.com/stotrasamhita)`nvedamantra-book / puja-vidhanam / stotra-sangrahah / namavali-manjari`nConverted from XeLaTeX for on-device reading.`n",
    [System.Text.UTF8Encoding]::new($false)
)
