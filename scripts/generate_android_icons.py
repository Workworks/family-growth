"""Generate deterministic Android launcher assets from the approved transparent master."""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from PIL import Image, ImageDraw


BACKGROUND = "#EDF6F0"
PREVIEW_BACKGROUND = "#F7F3E8"
MASTER_SIZE = 1024
ADAPTIVE_MARK_SIZE = 620
LEGACY_MARK_SIZE = 720
LEGACY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def fit_mark(source: Image.Image, canvas_size: int, mark_size: int) -> Image.Image:
    rgba = source.convert("RGBA")
    alpha = rgba.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError("source has no visible pixels")
    mark = rgba.crop(bounds)
    scale = min(mark_size / mark.width, mark_size / mark.height)
    target = (
        max(1, round(mark.width * scale)),
        max(1, round(mark.height * scale)),
    )
    mark = mark.resize(target, Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    position = ((canvas_size - mark.width) // 2, (canvas_size - mark.height) // 2)
    canvas.alpha_composite(mark, position)
    return canvas


def legacy_icon(mark: Image.Image, size: int, round_icon: bool) -> Image.Image:
    canvas = Image.new("RGBA", (MASTER_SIZE, MASTER_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    if round_icon:
        draw.ellipse((0, 0, MASTER_SIZE - 1, MASTER_SIZE - 1), fill=BACKGROUND)
    else:
        draw.rounded_rectangle(
            (0, 0, MASTER_SIZE - 1, MASTER_SIZE - 1),
            radius=220,
            fill=BACKGROUND,
        )
    canvas.alpha_composite(mark)
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def preview(mark: Image.Image, output: Path) -> None:
    size = 320
    gap = 48
    canvas = Image.new("RGB", (gap * 4 + size * 3, size + gap * 2), PREVIEW_BACKGROUND)
    positions = [gap, gap * 2 + size, gap * 3 + size * 2]
    for index, x in enumerate(positions):
        tile = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        shape = ImageDraw.Draw(tile)
        if index == 0:
            shape.rounded_rectangle((0, 0, size - 1, size - 1), radius=70, fill=BACKGROUND)
        elif index == 1:
            shape.ellipse((0, 0, size - 1, size - 1), fill=BACKGROUND)
        else:
            shape.rounded_rectangle((0, 0, size - 1, size - 1), radius=120, fill=BACKGROUND)
        tile.alpha_composite(mark.resize((size, size), Image.Resampling.LANCZOS))
        canvas.paste(tile, (x, gap), tile)
    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output, optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--res", required=True, type=Path)
    parser.add_argument("--branding", required=True, type=Path)
    parser.add_argument("--preview", required=True, type=Path)
    args = parser.parse_args()

    source = Image.open(args.source)
    args.branding.mkdir(parents=True, exist_ok=True)
    source_copy = args.branding / "family-growth-icon-generated-source.png"
    if args.source.resolve() != source_copy.resolve():
        shutil.copy2(args.source, source_copy)

    adaptive = fit_mark(source, MASTER_SIZE, ADAPTIVE_MARK_SIZE)
    adaptive_path = args.branding / "family-growth-icon-foreground-1024.png"
    adaptive.save(adaptive_path, optimize=True)

    legacy_mark = fit_mark(source, MASTER_SIZE, LEGACY_MARK_SIZE)
    drawable_dir = args.res / "drawable-xxxhdpi"
    drawable_dir.mkdir(parents=True, exist_ok=True)
    adaptive.resize((432, 432), Image.Resampling.LANCZOS).save(
        drawable_dir / "ic_launcher_foreground.png",
        optimize=True,
    )

    for density, size in LEGACY_SIZES.items():
        directory = args.res / f"mipmap-{density}"
        directory.mkdir(parents=True, exist_ok=True)
        legacy_icon(legacy_mark, size, False).save(directory / "ic_launcher.png", optimize=True)
        legacy_icon(legacy_mark, size, True).save(directory / "ic_launcher_round.png", optimize=True)

    preview(adaptive, args.preview)


if __name__ == "__main__":
    main()
