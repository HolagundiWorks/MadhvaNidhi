# Verify bundled catalogue references and transparent PNG artwork.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot = Split-Path $PSScriptRoot -Parent
$catalogue = Get-Content (Join-Path $taskRoot 'app/src/main/assets/rangavalli/index.json') -Raw | ConvertFrom-Json
$sources = Get-Content (Join-Path $taskRoot 'app/src/main/assets/rangavalli/sources.json') -Raw | ConvertFrom-Json
$ids = @{}
foreach ($item in $catalogue.items) {
    if ($ids.ContainsKey($item.id)) { throw "Duplicate artwork ID" }; $ids[$item.id]=$true
    if ($item.asset -notmatch '^rangavalli/[a-z_]+\.png$') { throw "Invalid asset path" }
    if ($item.source_id -notin $sources.sources.id) { throw "Unknown source" }
    if ($item.steps_kn.Count -lt 3) { throw "Incomplete drawing guide" }
    $bitmap = [System.Drawing.Bitmap]::FromFile((Join-Path $taskRoot ('app/src/main/assets/'+$item.asset)))
    try {
        if ($bitmap.Width -ne 1024 -or $bitmap.Height -ne 1024) { throw "Unexpected artwork dimensions" }
        if ($bitmap.GetPixel(0,0).A -ne 0 -or $bitmap.GetPixel(1023,1023).A -ne 0) { throw "Artwork background is not transparent" }
        $ink=0; $transparent=0
        for ($y=0;$y -lt 1024;$y+=8) { for($x=0;$x -lt 1024;$x+=8) {
            $pixel=$bitmap.GetPixel($x,$y)
            if ($pixel.A -eq 0) { $transparent++ }
            else { if ($pixel.R -ne 0 -or $pixel.G -ne 0 -or $pixel.B -ne 0) { throw "Non-monochrome ink" }; $ink++ }
        }}
        if ($ink -lt 50 -or $transparent -lt 1000) { throw "Empty or opaque artwork" }
    } finally { $bitmap.Dispose() }
}
Write-Output "PASS: $($ids.Count) unique 1024px transparent monochrome PNGs; source links and guides present"
