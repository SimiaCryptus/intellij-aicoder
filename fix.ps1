$files = Get-ChildItem -Recurse -File | Where-Object {
    $_.Extension -ne '.bat' -and
    $_.Extension -ne '.ps1' -and
    $_.DirectoryName -notlike '*\.git\*' -and
    $_.Extension -ne '.jar'
}

foreach ($file in $files) {
    $content = Get-Content -Raw $file.FullName
    $content = $content -replace '\r\n', "`n"

    Set-Content -Path $file.FullName -Value $content -Encoding UTF8
}
