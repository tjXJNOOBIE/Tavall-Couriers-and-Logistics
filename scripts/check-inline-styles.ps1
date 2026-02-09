$matches = Get-ChildItem -Recurse -Filter *.html -Path spring-webview/main/resources/templates |
    Select-String -Pattern 'style="'

if ($matches) {
    Write-Error "Inline styles detected in templates. Move styles into CSS files."
    $matches | ForEach-Object { Write-Error ("{0}:{1}:{2}" -f $_.Path, $_.LineNumber, $_.Line.Trim()) }
    exit 1
}

Write-Output "No inline styles detected."
