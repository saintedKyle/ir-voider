"""Render the app's Surface.java shading math in Pillow to eyeball depth without a device.

Mirrors lighten/darken/lift/gloss/bottom-shade exactly, so the swatch is the same arithmetic the
APK runs; only the system elevation shadow is approximated. Not a screenshot.
"""

from PIL import Image, ImageDraw, ImageFilter

W, H = 1180, 700
S = 3  # supersample for smooth corners


def ch(c, i):
    return (c >> (16 - 8 * i)) & 0xFF


def alpha(c):
    return (c >> 24) & 0xFF


def rgb(c):
    return (ch(c, 0), ch(c, 1), ch(c, 2))


def mix(frm, to, t):
    out = alpha(frm) << 24
    for i in range(3):
        a, b = ch(frm, i), ch(to, i)
        out |= int(a + (b - a) * t + 0.5) << (16 - 8 * i)
    return out


def lighten(c, t):
    return mix(c, 0xFFFFFFFF, t)


def darken(c, t):
    return mix(c, 0xFF000000, t)


def lift(base):
    lum = (0.2126 * ch(base, 0) + 0.7152 * ch(base, 1) + 0.0722 * ch(base, 2)) / 255.0
    return [lighten(base, 0.06 + 0.16 * lum), base, darken(base, 0.34)]


def color_at(stops, y, y0, y1):
    if y1 <= y0:
        return stops[1]
    f = min(1.0, max(0.0, (y - y0) / (y1 - y0)))
    if f <= 0.5:
        return mix(stops[0], stops[1], f / 0.5)
    return mix(stops[1], stops[2], (f - 0.5) / 0.5)


def box_mask(size, box, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([int(v * S) for v in box], radius=int(radius * S), fill=255)
    return m


def plate(img, box, radius, stops, rim, gloss, bottom_shade=0x2E000000):
    x0, y0, x1, y1 = [int(v * S) for v in box]
    r, edge = int(radius * S), int(1 * S)

    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.rounded_rectangle([x0, y0, x1, y1], radius=r, fill=rgb(rim) + (255,))
    for y in range(y0 + edge, y1 - edge):
        c = color_at(stops, y, y0 + edge, y1 - edge - 1)
        d.line([(x0 + edge, y), (x1 - edge, y)], fill=rgb(c) + (255,))

    inner = box_mask(img.size, (x0 + edge, y0 + edge, x1 - edge, y1 - edge), max(0, radius - 1))
    for argb, top in ((gloss, True), (bottom_shade, False)):
        if not argb:
            continue
        g = Image.new("RGBA", img.size, (0, 0, 0, 0))
        gd = ImageDraw.Draw(g)
        span = y1 - edge - (y0 + edge)
        for y in range(y0 + edge, y1 - edge):
            f = (y - (y0 + edge)) / max(1, span)
            t = f if top else 1.0 - f
            a = int(alpha(argb) * max(0.0, 1.0 - 2.0 * t))
            if a > 0:
                gd.line([(x0 + edge, y), (x1 - edge, y)], fill=rgb(argb) + (a,))
        g.putalpha(Image.composite(g.split()[3], Image.new("L", img.size, 0), inner))
        layer = Image.alpha_composite(layer, g)

    img.alpha_composite(layer)


def shadow(img, box, radius, blur, strength):
    pad = int(blur * S)
    s = Image.new("RGBA", img.size, (0, 0, 0, 0))
    shifted = [box[0], box[1] + blur * 0.8, box[2], box[3] + blur * 0.8]
    ImageDraw.Draw(s).rounded_rectangle([int(v * S) for v in shifted],
                                        radius=int(radius * S), fill=(0, 0, 0, int(255 * strength)))
    img.alpha_composite(s.filter(ImageFilter.GaussianBlur(pad)))


def text(img, xy, s, color):
    ImageDraw.Draw(img).text([int(v * S) for v in xy], s, fill=color)


BG = 0xFF0A0908
CARD = 0xFF14110F
BTN = 0xFF1A1715
STROKE = 0xFF2C2824
ACCENT = 0xFFE8753D

img = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 255))
d = ImageDraw.Draw(img)
for y in range(H * S):
    d.line([(0, y), (W * S, y)],
           fill=rgb(mix(BG, 0xFF120E0B, y / (H * S))) + (255,))

# BEFORE: v1.4 flat fills
text(img, (40, 22), "BEFORE  flat fill", "#8B8176")
plate(img, (40, 54, 500, 290), 28, [CARD, CARD, CARD], STROKE, 0, 0)
text(img, (58, 70), "TV / Samsung", "#E9E0D4")
plate(img, (58, 104, 250, 158), 20, [ACCENT, ACCENT, ACCENT], 0, 0, 0)
plate(img, (262, 104, 480, 158), 20, [BTN, BTN, BTN], STROKE, 0, 0)
plate(img, (58, 170, 250, 224), 20, [BTN, BTN, BTN], STROKE, 0, 0)
plate(img, (262, 170, 480, 224), 20, [BTN, BTN, BTN], STROKE, 0, 0)

# AFTER: lit from above
text(img, (620, 22), "AFTER  lit top rim, body gradient, contact shade, shadow", "#8B8176")
shadow(img, (620, 54, 1080, 290), 28, 5, 0.5)
plate(img, (620, 54, 1080, 290), 28, lift(CARD), STROKE, 0x18FFFFFF)
text(img, (638, 70), "TV / Samsung", "#E9E0D4")
for bx, by, ex, ey, base, rim, gloss in [
    (638, 104, 830, 158, ACCENT, darken(ACCENT, 0.42), 0x3CFFFFFF),
    (842, 104, 1060, 158, BTN, lighten(STROKE, 0.22), 0x24FFFFFF),
    (638, 170, 830, 224, BTN, lighten(STROKE, 0.22), 0x24FFFFFF),
    (842, 170, 1060, 224, BTN, lighten(STROKE, 0.22), 0x24FFFFFF),
]:
    shadow(img, (bx, by, ex, ey), 20, 2.5, 0.55)
    plate(img, (bx, by, ex, ey), 20, lift(base), rim, gloss)

# segmented control + recessed progress track
shadow(img, (40, 330, 1080, 470), 28, 5, 0.5)
plate(img, (40, 330, 1080, 470), 28, lift(CARD), STROKE, 0x18FFFFFF)
text(img, (58, 344), "Scan speed", "#8B8176")
for i, x in enumerate([58, 300, 542, 784]):
    active = i == 1
    if active:
        shadow(img, (x, 380, x + 226, 428), 12, 2, 0.6)
    plate(img, (x, 380, x + 226, 428), 12,
          lift(ACCENT if active else darken(BTN, 0.35)),
          darken(ACCENT, 0.42) if active else STROKE,
          0x38FFFFFF if active else 0x14FFFFFF)

track = [darken(0xFF0C0A09, 0.35), 0xFF0C0A09, lighten(0xFF0C0A09, 0.09)]
plate(img, (58, 438, 1060, 450), 6, track, STROKE, 0, 0)
shadow(img, (58, 438, 640, 450), 6, 1.5, 0.7)
plate(img, (58, 438, 640, 450), 6, lift(ACCENT), darken(ACCENT, 0.40), 0x40FFFFFF)

# CTA
shadow(img, (40, 500, 1080, 600), 22, 6, 0.6)
plate(img, (40, 500, 1080, 600), 22, lift(ACCENT), darken(ACCENT, 0.42), 0x3CFFFFFF)
text(img, (510, 534), "POWER", "#0A0908")

out = img.convert("RGB").resize((W, H), Image.LANCZOS)
out.save("internal-docs/depth-preview.png")
print("ember", [hex(c & 0xFFFFFF) for c in lift(ACCENT)], "rim", hex(darken(ACCENT, .42) & 0xFFFFFF))
print("key  ", [hex(c & 0xFFFFFF) for c in lift(BTN)])
print("card ", [hex(c & 0xFFFFFF) for c in lift(CARD)])
