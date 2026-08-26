param(
    [string]$OutputDirectory = "family-growth-android/app/src/main/res/raw"
)

$ErrorActionPreference = "Stop"
$ffmpeg = (Get-Command ffmpeg -ErrorAction Stop).Source
$resolvedOutput = Join-Path (Get-Location) $OutputDirectory
New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null

$font = "C\:/Windows/Fonts/msyh.ttc"
$base = "color=c=0xFFFDF7:s=640x360:r=15:d=18"
$label = "drawtext=fontfile='$font':fontsize=32:fontcolor=0x40534D:x=(w-text_w)/2"

function New-LessonVideo {
    param([string]$Name, [string]$Filter)
    $target = Join-Path $resolvedOutput "$Name.mp4"
    & $ffmpeg -hide_banner -loglevel error -y -f lavfi -i $base -vf $Filter -an -c:v libx264 -profile:v baseline -level 3.0 -preset slow -crf 27 -pix_fmt yuv420p -movflags +faststart $target
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $Name" }
}

$colors = "$label`:text='颜色花园':y=28,$label`:text='红色':y=292:enable='between(t,0,6)',drawtext=fontfile='$font':text='●':fontsize=190:fontcolor=0xD96B61:x=(w-text_w)/2:y=74:enable='between(t,0,6)',$label`:text='黄色':y=292:enable='between(t,6,12)',drawtext=fontfile='$font':text='●':fontsize=190:fontcolor=0xE8B04A:x=(w-text_w)/2:y=74:enable='between(t,6,12)',$label`:text='蓝色':y=292:enable='between(t,12,18)',drawtext=fontfile='$font':text='●':fontsize=190:fontcolor=0x6C9DC7:x=(w-text_w)/2:y=74:enable='between(t,12,18)'"
New-LessonVideo "lesson_color_garden" $colors

$count = "$label`:text='一起数到五':y=28,drawtext=fontfile='$font':text='1':fontsize=130:fontcolor=0x2F6D5A:x=(w-text_w)/2:y=88:enable='between(t,0,3.6)',drawtext=fontfile='$font':text='●':fontsize=48:fontcolor=0xE8B04A:x=(w-text_w)/2:y=252:enable='between(t,0,3.6)',drawtext=fontfile='$font':text='2':fontsize=130:fontcolor=0x2F6D5A:x=(w-text_w)/2:y=88:enable='between(t,3.6,7.2)',drawtext=fontfile='$font':text='● ●':fontsize=48:fontcolor=0xE8B04A:x=(w-text_w)/2:y=252:enable='between(t,3.6,7.2)',drawtext=fontfile='$font':text='3':fontsize=130:fontcolor=0x2F6D5A:x=(w-text_w)/2:y=88:enable='between(t,7.2,10.8)',drawtext=fontfile='$font':text='● ● ●':fontsize=48:fontcolor=0xE8B04A:x=(w-text_w)/2:y=252:enable='between(t,7.2,10.8)',drawtext=fontfile='$font':text='4':fontsize=130:fontcolor=0x2F6D5A:x=(w-text_w)/2:y=88:enable='between(t,10.8,14.4)',drawtext=fontfile='$font':text='● ● ● ●':fontsize=48:fontcolor=0xE8B04A:x=(w-text_w)/2:y=252:enable='between(t,10.8,14.4)',drawtext=fontfile='$font':text='5':fontsize=130:fontcolor=0x2F6D5A:x=(w-text_w)/2:y=88:enable='between(t,14.4,18)',drawtext=fontfile='$font':text='● ● ● ● ●':fontsize=48:fontcolor=0xE8B04A:x=(w-text_w)/2:y=252:enable='between(t,14.4,18)'"
New-LessonVideo "lesson_count_to_five" $count

$shapes = "$label`:text='形状找到家':y=28,$label`:text='圆形':y=292:enable='between(t,0,6)',drawtext=fontfile='$font':text='○':fontsize=190:fontcolor=0x6C9DC7:x=(w-text_w)/2:y=74:enable='between(t,0,6)',$label`:text='方形':y=292:enable='between(t,6,12)',drawtext=fontfile='$font':text='□':fontsize=190:fontcolor=0x2F6D5A:x=(w-text_w)/2:y=74:enable='between(t,6,12)',$label`:text='三角形':y=292:enable='between(t,12,18)',drawtext=fontfile='$font':text='△':fontsize=190:fontcolor=0xC96B5A:x=(w-text_w)/2:y=74:enable='between(t,12,18)'"
New-LessonVideo "lesson_shape_home" $shapes

Get-ChildItem -LiteralPath $resolvedOutput -Filter "lesson_*.mp4" | Select-Object Name, Length
