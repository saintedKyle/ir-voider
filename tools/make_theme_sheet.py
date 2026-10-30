#!/usr/bin/env python3
"""Render the ten orange-family themes as a contact sheet.

Re-implements the shading arithmetic from Surface.java / Palette.java so the sheet
matches what the app draws. Illustrative design preview, not a device screenshot.
"""

import os
from PIL import Image, ImageDraw, ImageFont

F_BLACK = "C:/Windows/Fonts/segoeuib.ttf"
F_SEMI = "C:/Windows/Fonts/seguisb.ttf"
F_REG = "C:/Windows/Fonts/segoeui.ttf"

# name, bg, bgBottom, card, btn, stroke, accent, accent2, text, muted, warn, ok, onAccent
PRESETS = [
    ("Ember Signal", (10, 9, 8), (18, 14, 11), (20, 17, 15), (26, 23, 21), (44, 40, 36),
     (232, 117, 61), (176, 125, 82), (233, 224, 212), (139, 129, 118), (224, 107, 85),
     (183, 196, 138), (10, 9, 8)),
    ("Void Amber", (9, 8, 7), (18, 15, 10), (20, 18, 14), (27, 24, 19), (46, 42, 33),
     (232, 163, 61), (181, 132, 67), (233, 226, 208), (139, 131, 112), (224, 138, 85),
     (174, 196, 138), (10, 9, 8)),
    ("Terrace Green", (7, 11, 8), (12, 19, 14), (14, 21, 16), (20, 29, 23), (36, 53, 40),
     (95, 201, 122), (62, 158, 98), (220, 233, 220), (117, 137, 122), (224, 112, 90),
     (155, 212, 143), (7, 19, 11)),
    ("Teal Current", (6, 11, 12), (10, 18, 20), (13, 21, 24), (19, 30, 34), (33, 54, 59),
     (49, 199, 184), (42, 158, 151), (216, 233, 232), (112, 137, 140), (224, 112, 90),
     (143, 212, 160), (4, 16, 15)),
    ("Cobalt Pulse", (7, 9, 17), (11, 16, 32), (14, 19, 34), (20, 27, 46), (36, 48, 79),
     (76, 141, 246), (127, 168, 232), (221, 228, 245), (119, 131, 160), (224, 107, 107),
     (143, 212, 143), (6, 10, 20)),
    ("Ultraviolet", (10, 7, 19), (17, 12, 31), (20, 15, 36), (27, 21, 48), (46, 36, 80),
     (164, 124, 245), (126, 91, 208), (230, 223, 247), (133, 124, 160), (224, 107, 143),
     (155, 212, 143), (11, 7, 22)),
    ("Rose Signal", (14, 7, 11), (23, 11, 18), (26, 14, 21), (35, 19, 28), (59, 32, 48),
     (242, 100, 155), (194, 90, 134), (237, 220, 227), (154, 122, 135), (224, 96, 90),
     (155, 212, 143), (20, 4, 9)),
    ("Smoke Signal", (10, 10, 11), (18, 18, 20), (21, 21, 23), (28, 28, 31), (47, 47, 52),
     (195, 201, 210), (140, 147, 160), (231, 232, 234), (132, 134, 140), (224, 107, 85),
     (155, 212, 143), (10, 10, 11)),
    ("OLED Void", (0, 0, 0), (10, 7, 5), (14, 12, 10), (21, 18, 16), (38, 34, 32),
     (232, 117, 61), (176, 125, 82), (233, 224, 212), (126, 118, 109), (224, 107, 85),
     (183, 196, 138), (0, 0, 0)),
    ("Bone Frost", (231, 235, 242), (216, 222, 233), (241, 244, 249), (223, 229, 239),
     (192, 200, 214), (47, 111, 208), (93, 139, 216), (20, 24, 33), (94, 102, 116),
     (194, 58, 46), (63, 122, 58), (244, 247, 252)),
]

WHITE = (255, 255, 255)
BLACK = (0, 0, 0)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t + 0.5) for i in range(3))


def lumin(c):
    return (0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]) / 255


def lift(base):
    l = lumin(base)
    under = 0.34 - (l - 0.7) if l > 0.7 else 0.34
    return (mix(base, WHITE, 0.06 + 0.16 * l), base, mix(base, BLACK, under))


def recess(base):
    return (mix(base, BLACK, 0.35), base, mix(base, WHITE, 0.09))


def raised(draw, box, base, radius, rim, gloss, width=2):
    x0, y0, x1, y1 = [int(v) for v in box]
    stops = lift(base)
    for y in range(y0, y1):
        t = (y - y0) / max(1, y1 - y0 - 1)
        c = mix(stops[0], stops[1], t * 2) if t < 0.5 else mix(stops[1], stops[2], (t - 0.5) * 2)
        c = mix(c, WHITE, gloss * max(0.0, 1 - 2 * t))
        c = mix(c, BLACK, 0.18 * max(0.0, 2 * t - 1))
        r = max(1, min(int(radius), (y1 - y0) // 2, (x1 - x0) // 2))
        dy = max(0, y0 + r - 1 - y, y - (y1 - r))
        inset = int(r - (r * r - dy * dy) ** 0.5) if dy else 0
        draw.line([(x0 + inset, y), (x1 - inset - 1, y)], fill=c)
    draw.rounded_rectangle(box, radius=int(radius), outline=rim, width=width)


def sunken(draw, box, base, radius, rim, width=2):
    x0, y0, x1, y1 = [int(v) for v in box]
    stops = recess(base)
    for y in range(y0, y1):
        t = (y - y0) / max(1, y1 - y0 - 1)
        c = mix(stops[0], stops[1], t * 2) if t < 0.5 else mix(stops[1], stops[2], (t - 0.5) * 2)
        draw.line([(x0, y), (x1 - 1, y)], fill=c)
    draw.rounded_rectangle(box, radius=int(radius), outline=rim, width=width)


def tile(img, draw, x, y, w, h, p):
    (name, bg, bgb, card, btn, stroke, accent, accent2, text, muted, warn, ok, on_accent) = p
    light = lumin(text) < 0.5
    gloss = lambda s: s * 0.55 if light else s
    rim = mix(stroke, BLACK, 0.18) if light else mix(stroke, WHITE, 0.22)
    active = mix(card, accent, 0.10)

    # phone body: vertical bg ramp, then the app chrome
    body = Image.new("RGB", (w, h))
    bd = ImageDraw.Draw(body)
    for i in range(h):
        t = i / max(1, h - 1)
        bd.line([(0, i), (w, i)], fill=mix(bg, bgb, t))
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w - 1, h - 1], radius=26, fill=255)
    img.paste(body, (x, y), mask)
    draw.rounded_rectangle([x, y, x + w - 1, y + h - 1], radius=26, outline=mix(bg, text, 0.16), width=2)

    pad = 16
    # title + IR chip
    draw.text((x + pad, y + 22), "IR Voider", font=font(F_BLACK, 26), fill=text)
    chip_w = 92
    chip = [x + w - pad - chip_w, y + 22, x + w - pad, y + 50]
    raised(draw, chip, mix(card, accent, 0.18), 14, mix(card, accent, 0.45), gloss(0.12), 1)
    draw.text(((chip[0] + chip[2]) / 2, (chip[1] + chip[3]) / 2), "IR READY",
              font=font(F_SEMI, 13), fill=mix(accent, text, 0.35), anchor="mm")

    # category chips
    cw = (w - pad * 2 - 8) // 2
    for i, label in enumerate(("TV", "Aircon")):
        cx = x + pad + i * (cw + 8)
        on = i == 0
        raised(draw, [cx, y + 64, cx + cw, y + 104],
               active if on else card, 14, accent if on else stroke,
               gloss(0.20 if on else 0.09), 1)
        draw.text((cx + 10, y + 74), label, font=font(F_SEMI, 15),
                  fill=accent if on else text)
        draw.text((cx + 10, y + 88), "214 codes", font=font(F_REG, 11), fill=muted)

    # controls card
    cbox = [x + pad, y + 118, x + w - pad, y + 324]
    raised(draw, cbox, card, 22, stroke, gloss(0.09), 2)
    raised(draw, [cbox[0] + 14, cbox[1] + 14, cbox[2] - 14, cbox[1] + 62],
           accent, 18, mix(accent, BLACK, 0.42), gloss(0.24), 1)
    draw.text(((cbox[0] + cbox[2]) / 2, cbox[1] + 38), "POWER", font=font(F_SEMI, 22),
              fill=on_accent, anchor="mm")
    bw = (cbox[2] - cbox[0] - 28 - 16) // 4
    for i, label in enumerate(("Vol +", "Vol −", "CH +", "CH −")):
        bx = cbox[0] + 14 + i * (bw + 4)
        raised(draw, [bx, cbox[1] + 74, bx + bw, cbox[1] + 110], btn, 14,
               rim, gloss(0.14), 1)
        draw.text(((bx + bx + bw) / 2, cbox[1] + 92), label, font=font(F_REG, 14),
                  fill=text, anchor="mm")

    # scanner progress
    track = mix(card, BLACK, 0.55)
    sunken(draw, [cbox[0] + 14, cbox[1] + 124, cbox[2] - 14, cbox[1] + 140],
           track, 8, stroke, 1)
    fill_w = int((cbox[2] - cbox[0] - 28) * 0.62)
    raised(draw, [cbox[0] + 14, cbox[1] + 124, cbox[0] + 14 + fill_w, cbox[1] + 140],
           accent, 8, mix(accent, BLACK, 0.40), gloss(0.25), 1)
    draw.text((cbox[0] + 14, cbox[1] + 148), "Scanning 62 / 100", font=font(F_REG, 12),
              fill=muted)

    # warning + confirm accents
    raised(draw, [cbox[0] + 14, cbox[3] - 34, cbox[2] - 14, cbox[3] - 6],
           mix(card, warn, 0.22), 12, rim, gloss(0.14), 1)
    draw.text(((cbox[0] + cbox[2]) / 2, cbox[3] - 20), "Stop", font=font(F_REG, 15),
              fill=warn, anchor="mm")

    # theme name under the screen
    draw.text((x, y + h + 12), name, font=font(F_SEMI, 20), fill=(214, 206, 194))
    draw.text((x, y + h + 40), "accent " + "#%02X%02X%02X" % accent, font=font(F_REG, 14),
              fill=(150, 142, 132))


def font(path, size):
    return ImageFont.truetype(path, size)


def main():
    tw, th = 300, 344
    gx, gy = 3, 4
    gap_x, gap_y = 34, 84
    margin = 44
    header = 96
    W = margin * 2 + gx * tw + (gx - 1) * gap_x
    H = margin * 2 + header + gy * th + (gy - 1) * gap_y + 66
    img = Image.new("RGB", (W, H), (8, 7, 6))
    draw = ImageDraw.Draw(img)
    draw.text((margin, margin), "IR Voider \u2014 ten themes",
              font=font(F_BLACK, 40), fill=(233, 224, 212))
    draw.text((margin, margin + 48),
              "Each preset re-tints its own neutrals; only Bone Frost inverts to light.",
              font=font(F_REG, 20), fill=(139, 129, 118))
    for i, p in enumerate(PRESETS):
        col, row = i % gx, i // gx
        x = margin + col * (tw + gap_x)
        y = margin + header + row * (th + gap_y)
        tile(img, draw, x, y, tw, th, p)
    out = "docs/images/themes.png"
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)
    print(out, img.size)


if __name__ == "__main__":
    main()
