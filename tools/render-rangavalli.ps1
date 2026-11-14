# Render generated vector geometry to transparent, anti-aliased monochrome PNGs.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot = Split-Path $PSScriptRoot -Parent
$art = Get-Content -LiteralPath (Join-Path $taskRoot 'resources/rangavalli/geometry.json') -Raw | ConvertFrom-Json
foreach ($design in $art) {
    if ($design.asset -notmatch '^rangavalli/[a-z_]+\.png$') { throw "Invalid artwork path" }
    $bitmap = [System.Drawing.Bitmap]::new(1024, 1024, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $pen = [System.Drawing.Pen]::new([System.Drawing.Color]::Black, 10)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
        $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
        $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
        foreach ($line in $design.paths) {
            $points = [System.Drawing.PointF[]]@($line | ForEach-Object { [System.Drawing.PointF]::new([single]($_[0]*2), [single]($_[1]*2)) })
            $graphics.DrawLines($pen, $points)
        }
        $destination = Join-Path $taskRoot ('app/src/main/assets/' + $design.asset)
        $bitmap.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $pen.Dispose(); $graphics.Dispose(); $bitmap.Dispose() }
}
Write-Output "Rendered $($art.Count) transparent 1024px PNGs"
