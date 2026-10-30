#!/usr/bin/env python3
"""Generate illustrative README images for IR Voider v1.4.

These are design previews built from the app's palette, copy and control layout,
not device screenshots. Requires Pillow and Windows Segoe UI fonts.
"""

import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1080, 2400
BG = (10, 9, 8)
CARD = (20, 17, 15)
CARD2 = (26, 22, 19)
STROKE = (44, 40, 36)
BTN = (26, 22, 19)
ACCENT = (232, 117, 61)
CYAN = (176, 125, 82)
TEXT = (233, 224, 212)
MUTED = (139, 129, 118)
RED = (224, 107, 85)
GREEN = (183, 196, 138)
DARK = (10, 9, 8)

F_BLACK = "C:/Windows/Fonts/segoeuib.ttf"
F_SEMI = "C:/Windows/Fonts/seguisb.ttf"
F_REG = "C:/Windows/Fonts/segoeui.ttf"
F_SYM = "C:/Windows/Fonts/seguisym.ttf"
ICON = "app/src/main/res/drawable/ic_launcher.png"


def font(path, size):
    return ImageFont.truetype(path, size)


def glow_bg(w, h):
    img = Image.new("RGB", (w, h), BG)
    glow = Image.new("RGB", (w, h), (0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([-220, -260, 680, 560], fill=(66, 32, 18))
    gd.ellipse([w - 760, h - 580, w + 300, h + 240], fill=(40, 27, 17))
    glow = glow.filter(ImageFilter.GaussianBlur(150))
    return Image.blend(img, Image.composite(glow, img, glow.convert("L")), 0.30)


def vgrad(size, top, mid, bottom):
    w, h = size
    img = Image.new("RGB", (1, h))
    for y in range(h):
        t = y / max(1, h - 1)
        if t < 0.5:
            a, b, k = top, mid, t * 2
        else:
            a, b, k = mid, bottom, (t - 0.5) * 2
        img.putpixel((0, y), tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3)))
    return img.resize((w, h))


def hgrad(size, left, right):
    w, h = size
    img = Image.new("RGB", (w, 1))
    for x in range(w):
        t = x / max(1, w - 1)
        img.putpixel((x, 0), tuple(int(left[i] + (right[i] - left[i]) * t) for i in range(3)))
    return img.resize((w, h))


def paste_gradient(base, box, colors, radius, outline=None, width=2):
    x, y, x2, y2 = box
    w, h = x2 - x, y2 - y
    g = hgrad((w, h), colors[0], colors[1]) if len(colors) == 2 else vgrad((w, h), *colors)
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w - 1, h - 1], radius=radius, fill=255)
    base.paste(g, (x, y), mask)
    if outline:
        ImageDraw.Draw(base).rounded_rectangle(box, radius=radius, outline=outline, width=width)


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def _lift(base):
    lum = (0.2126 * base[0] + 0.7152 * base[1] + 0.0722 * base[2]) / 255
    return (_mix(base, (255, 255, 255), 0.06 + 0.16 * lum), base, _mix(base, (0, 0, 0), 0.34))


def raised(draw, box, base, radius, outline=None, gloss=0.10, width=2, sink=False):
    """Light-from-above fill used by app/src/main/java/com/kylem/remote/Surface.java.

    sink=True flips the lighting so the plate reads as a groove instead of a raised pad.
    """
    x0, y0, x1, y1 = [int(v) for v in box]
    stops = _lift(base)
    if sink:
        stops = (_mix(base, (0, 0, 0), 0.35), base, _mix(base, (255, 255, 255), 0.09))
    r = max(1, min(int(radius), (y1 - y0) // 2, (x1 - x0) // 2))
    for y in range(y0, y1):
        t = (y - y0) / max(1, y1 - y0 - 1)
        c = _mix(stops[0], stops[1], t * 2) if t < 0.5 else _mix(stops[1], stops[2], (t - 0.5) * 2)
        if sink:
            c = _mix(c, (0, 0, 0), gloss * max(0.0, 1 - 2 * t))
            c = _mix(c, (255, 255, 255), 0.10 * max(0.0, 2 * t - 1))
        else:
            c = _mix(c, (255, 255, 255), gloss * max(0.0, 1 - 2 * t))
            c = _mix(c, (0, 0, 0), 0.18 * max(0.0, 2 * t - 1))
        dy = max(0, y0 + r - 1 - y, y - (y1 - r))
        inset = int(r - (r * r - dy * dy) ** 0.5) if dy else 0
        draw.line([(x0 + inset, y), (x1 - inset - 1, y)], fill=c)
    if outline:
        draw.rounded_rectangle(box, radius=int(radius), outline=outline, width=width)


def card(draw, box, radius=28, fill=CARD, outline=STROKE):
    draw.rounded_rectangle([box[0] + 4, box[1] + 6, box[2] + 4, box[3] + 6],
                           radius=radius, fill=(4, 4, 4))
    raised(draw, box, fill, radius, outline, gloss=0.06)


def text(draw, xy, value, f, fill=TEXT, anchor="la"):
    draw.text(xy, value, font=f, fill=fill, anchor=anchor)


def tracked(draw, xy, value, f, fill, spacing=6):
    x, y = xy
    for ch in value:
        draw.text((x, y), ch, font=f, fill=fill, anchor="la")
        x += draw.textlength(ch, font=f) + spacing
    return x


def button(draw, box, label, fill=BTN, color=TEXT, radius=20, f=None, outline=STROKE):
    raised(draw, box, fill, radius, outline, gloss=0.10)
    f = f or font(F_REG, 30)
    cx = (box[0] + box[2]) / 2
    cy = (box[1] + box[3]) / 2
    draw.text((cx, cy), label, font=f, fill=color, anchor="mm")


def highlight_button(draw, box, label, size=40, radius=22):
    raised(draw, box, ACCENT, radius, _mix(ACCENT, (0, 0, 0), 0.42), gloss=0.16)
    draw.text(((box[0] + box[2]) / 2, (box[1] + box[3]) / 2), label,
              font=font(F_SEMI, size), fill=DARK, anchor="mm")


def pill(draw, x, y, label, f, color=CYAN, fill=CARD, outline=STROKE, pad_x=14, pad_y=5):
    w = draw.textlength(label, font=f) + pad_x * 2
    h = f.size + pad_y * 2 + 3
    raised(draw, [x, y, x + w, y + h], fill, h / 2, outline, gloss=0.07, width=1)
    draw.text((x + pad_x, y + pad_y), label, font=f, fill=color, anchor="la")
    return w


def status_bar(draw):
    f = font(F_SEMI, 28)
    draw.text((44, 34), "9:41", font=f, fill=TEXT, anchor="la")
    x = 880
    for i, hgt in enumerate((10, 16, 22, 28)):
        draw.rounded_rectangle([x + i * 12, 56 - hgt, x + i * 12 + 7, 56],
                               radius=2, fill=TEXT)
    draw.arc([952, 30, 1000, 70], start=200, end=340, fill=TEXT, width=4)
    draw.arc([962, 40, 990, 66], start=200, end=340, fill=TEXT, width=4)
    draw.ellipse([973, 53, 979, 59], fill=TEXT)
    draw.rounded_rectangle([1012, 34, 1054, 58], radius=5, outline=TEXT, width=3)
    draw.rounded_rectangle([1015, 37, 1043, 55], radius=3, fill=TEXT)
    draw.rectangle([1055, 42, 1059, 50], fill=TEXT)


def home_bar(draw, dark=False):
    color = (70, 70, 74) if not dark else (30, 30, 30)
    draw.rounded_rectangle([470, 2368, 610, 2375], radius=4, fill=color)


def logo(size, radius):
    icon = Image.open(ICON).convert("RGBA").resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1],
                                           radius=radius, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(icon, (0, 0), mask)
    return out


def top_bar(img, d, category):
    raised(d, [40, 96, 148, 204], BTN, 28, STROKE)
    for i in range(3):
        d.rounded_rectangle([66, 126 + i * 20, 122, 132 + i * 20], radius=3, fill=TEXT)
    icon = logo(84, 24)
    img.paste(icon, (170, 108), icon)
    text(d, (278, 92), "IR Voider", font(F_BLACK, 54), TEXT)
    pill(d, 280, 160, category, font(F_SEMI, 22), TEXT)
    raised(d, [940, 96, 1040, 196], BTN, 28, STROKE)
    d.text((990, 146), "\u2699", font=font(F_SYM, 40), fill=TEXT, anchor="mm")


def now_card(img, d, name, meta, note):
    card(d, (40, 246, 1040, 552))
    tracked(d, (76, 286), "NOW USING", font(F_SEMI, 24), TEXT, 5)
    pill(d, 846, 276, "IR READY", font(F_SEMI, 21), (216, 163, 106),
         (43, 28, 18), (106, 74, 47), 14, 4)
    text(d, (76, 336), name, font(F_BLACK, 50), TEXT)
    text(d, (76, 408), meta, font(F_REG, 27), MUTED)
    text(d, (76, 456), note, font(F_REG, 23), MUTED)
    text(d, (76, 496), "Power toggles; saved devices are one tap away.",
         font(F_REG, 22), MUTED)


def page_dots(d, x, y, count=6, current=1):
    parts = []
    for i in range(count):
        parts.append("\u25CF" if i == current - 1 else "\u25CB")
    s = "  ".join(parts) + f"    {current}/{count}"
    d.text((x, y), s, font=font(F_SEMI, 26), fill=TEXT, anchor="ra")


def page_row(d, y, label, x=76, w=928):
    button(d, (x, y, x + 92, y + 50), "\u2039", BTN, TEXT, 18,
           font(F_SEMI, 30))
    button(d, (x + w - 92, y, x + w, y + 50), "\u203A", BTN, TEXT, 18,
           font(F_SEMI, 30))
    d.text((x + w / 2, y + 25), label, font=font(F_SEMI, 22),
           fill=MUTED, anchor="mm")


def field(d, y, label, value, x=76, w=928, search=False):
    raised(d, [x, y, x + w, y + 80], CARD2, 22, STROKE, gloss=0.12, width=2, sink=True)
    d.text((x + 28, y + 26), label, font=font(F_SEMI, 24), fill=MUTED, anchor="la")
    d.text((x + 162, y + 24), value, font=font(F_SEMI, 27), fill=TEXT, anchor="la")
    if search:
        button(d, (x + w - 132, y + 10, x + w - 12, y + 70), "Search",
               BTN, TEXT, 16, font(F_SEMI, 20))
    else:
        d.text((x + w - 48, y + 40), "\u2304", font=font(F_SYM, 32),
               fill=MUTED, anchor="mm")


def remote_screen():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(d)
    top_bar(img, d, "TV  \u00B7  345 remotes")
    now_card(img, d, "Living Room TV", "TV  \u00B7  TCL \u2014 TCL_65C635K",
             "Tap the menu to change category, or the gear to name and save devices.")

    card(d, (40, 582, 1040, 1230))
    tracked(d, (76, 618), "CONTROLS", font(F_SEMI, 21), MUTED, 5)
    highlight_button(d, (76, 656, 1004, 768), "POWER", 40)
    d.text((76, 802), "BASICS", font=font(F_SEMI, 24), fill=TEXT, anchor="la")
    page_dots(d, 1004, 796, 6, 1)

    labels = [["Vol +", "Vol \u2212", "Mute"], ["CH +", "CH \u2212", "Input"]]
    for r, row in enumerate(labels):
        y = 844 + r * 104
        for c, label in enumerate(row):
            x = 76 + c * 312
            button(d, (x, y, x + 300, y + 92), label, BTN, TEXT, 20,
                   font(F_SEMI, 30))

    page_row(d, 1066, "Swipe left or right for more buttons")
    d.text((76, 1132), "BASICS  \u00B7  NAVIGATION  \u00B7  MEDIA  \u00B7  NUMBERS  \u00B7  COLORS  \u00B7  EXTRAS",
           font=font(F_REG, 19), fill=MUTED, anchor="la")
    d.text((76, 1174), "Swipe pages \u2014 only the buttons this remote has.",
           font=font(F_REG, 21), fill=MUTED, anchor="la")

    card(d, (40, 1268, 1040, 2070))
    text(d, (76, 1304), "Find a working code", font(F_BLACK, 44), TEXT)
    field(d, 1368, "Brand", "TCL", search=True)
    field(d, 1462, "Model", "TCL 65C635K")
    text(d, (76, 1570), "Point the phone's top edge at the device. The scan sends one",
         font(F_REG, 22), MUTED)
    text(d, (76, 1602), "power code at a time; stop when the device reacts.",
         font(F_REG, 22), MUTED)

    d.rounded_rectangle([76, 1652, 110, 1686], radius=8, outline=TEXT, width=3)
    text(d, (128, 1654), "Search every brand in this category", font(F_REG, 25), TEXT)

    d.text((76, 1722), "Speed", font=font(F_REG, 25), fill=MUTED, anchor="la")
    speeds = ["1 s", "1.5 s", "2 s", "3 s"]
    for i, label in enumerate(speeds):
        x = 190 + i * 194
        active = i == 2
        button(d, (x, 1706, x + 170, 1764), label,
               ACCENT if active else BTN,
               DARK if active else MUTED, 18, font(F_SEMI, 24))

    button(d, (76, 1800, 1004, 1900), "Find code", ACCENT, DARK, 22,
           font(F_SEMI, 34))
    d.text((76, 1934), "Scanner keeps the last five codes it tried.",
           font=font(F_REG, 22), fill=TEXT, anchor="la")
    d.text((76, 1972), "It worked saves the match; Previous replays the last code.",
           font=font(F_REG, 21), fill=MUTED, anchor="la")

    d.text((76, 2100), "Codes: Flipper-IRDB (CC0). IR only \u2014 no network permission.",
           font=font(F_REG, 20), fill=(100, 100, 104), anchor="la")
    home_bar(d)
    return img


def scanner_screen():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(d)

    tracked(d, (44, 104), "SCANNING CODES", font(F_SEMI, 20), MUTED, 5)
    text(d, (44, 148), "Point at the device", font(F_BLACK, 76), TEXT)
    text(d, (44, 232), "Air Conditioner  \u00B7  92 codes", font(F_REG, 30), MUTED)

    raised(d, [44, 302, 1036, 314], (12, 10, 9), 7, STROKE, gloss=0.14, width=1, sink=True)
    raised(d, [44, 302, 44 + int(992 * 0.13), 314], ACCENT, 7, None, gloss=0.16)
    text(d, (44, 334), "12 / 92", font(F_REG, 26), MUTED)
    text(d, (44, 382), "Haier \u2014 HPD10XCM-LW", font(F_BLACK, 62), TEXT)
    text(d, (44, 454), "Watch the device. When it reacts, tap It worked.",
         font(F_REG, 24), MUTED)

    card(d, (44, 524, 1036, 1000))
    tracked(d, (76, 550), "LAST 5 SCANNED", font(F_SEMI, 18), MUTED, 4)
    history = [
        ("12. Haier \u2014 HPD10XCM-LW", TEXT),
        ("11. Haier \u2014 HWE08XCR-L", MUTED),
        ("10. Haier \u2014 AS18GD3HRA", MUTED),
        ("9. TCL \u2014 65C635K", MUTED),
        ("8. LG \u2014 AKB75095308", MUTED),
    ]
    for i, (line, color) in enumerate(history):
        d.text((76, 592 + i * 46), line, font=font(F_REG, 28), fill=color, anchor="la")
    d.text((76, 854), "Pause stops sending. Previous replays; Skip 5 jumps ahead.",
           font=font(F_REG, 21), fill=(100, 100, 104), anchor="la")

    highlight_button(d, (44, 1838, 1036, 2010), "It worked", 44)
    row = ["Previous", "Pause", "Skip 5"]
    for i, label in enumerate(row):
        x = 44 + i * 330
        button(d, (x, 2038, x + 316, 2162), label, BTN, TEXT, 20,
               font(F_SEMI, 32))
    button(d, (44, 2190, 1036, 2322), "Stop", (26, 10, 14), RED, 22,
           font(F_SEMI, 32), outline=(67, 25, 32))
    text(d, (44, 2340), "Pause to stop sending. Previous replays; Skip 5 jumps ahead.",
         font(F_REG, 20), (100, 100, 104))
    home_bar(d)
    return img


def ac_screen():
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    status_bar(d)
    top_bar(img, d, "Air Conditioner  \u00B7  93 remotes")
    now_card(img, d, "Bedroom AC", "Air Conditioner  \u00B7  Haier \u2014 HPD10XCM-LW",
             "Tap the menu to change category, or the gear to name and save devices.")

    card(d, (40, 582, 1040, 1370))
    tracked(d, (76, 618), "CONTROLS", font(F_SEMI, 21), MUTED, 5)
    highlight_button(d, (76, 656, 1004, 768), "POWER", 40)
    button(d, (76, 786, 532, 846), "POWER \u00D72", BTN, TEXT, 18, font(F_SEMI, 23))
    button(d, (548, 786, 1004, 846), "POWER OFF", BTN, RED, 18, font(F_SEMI, 23))
    text(d, (76, 870), "Some ACs use separate On/Off codes; POWER \u00D72 helps",
         font(F_REG, 21), MUTED)
    text(d, (76, 900), "units that ignore a single frame.", font(F_REG, 21), MUTED)

    d.text((76, 952), "BASICS", font=font(F_SEMI, 24), fill=TEXT, anchor="la")
    page_dots(d, 1004, 946, 4, 1)
    labels = [
        ["Temp +", "Temp \u2212", "Mode"],
        ["Fan", "Swing", "Light"],
        ["Timer", "Sleep", None],
    ]
    for r, row in enumerate(labels):
        y = 994 + r * 100
        for c, label in enumerate(row):
            if label is None:
                continue
            x = 76 + c * 312
            button(d, (x, y, x + 300, y + 88), label, BTN, TEXT, 20,
                   font(F_SEMI, 29))
    page_row(d, 1298, "Buttons appear only when the profile has them")

    card(d, (40, 1388, 1040, 1678))
    tracked(d, (76, 1422), "AIRCON NOTES", font(F_SEMI, 20), TEXT, 5)
    text(d, (76, 1466), "Codes are included per button. Temperature/mode may set a",
         font(F_REG, 21), MUTED)
    text(d, (76, 1498), "captured state, not a step.", font(F_REG, 21), MUTED)
    text(d, (76, 1544), "If your model is missing, its protocol is not in the database.",
         font(F_REG, 21), MUTED)
    text(d, (76, 1584), "Try POWER \u00D72 or POWER OFF for units that miss a frame.",
         font(F_REG, 21), MUTED)

    card(d, (40, 1716, 1040, 2070))
    text(d, (76, 1752), "Find a working code", font(F_BLACK, 39), TEXT)
    text(d, (76, 1812), "Same scanner as TV: Last 5, Previous and Pause.",
         font(F_REG, 22), MUTED)
    button(d, (76, 1866, 1004, 1966), "Find code", ACCENT, DARK, 22,
           font(F_SEMI, 34))
    text(d, (76, 1998), "It worked saves the match to your AC.",
         font(F_REG, 21), (100, 100, 104))
    text(d, (76, 2140), "Codes: Flipper-IRDB (CC0). IR only \u2014 no network permission.",
         font(F_REG, 20), (100, 100, 104))
    home_bar(d)
    return img


def draw_banner():
    w, h = 1600, 560
    img = glow_bg(w, h)
    icon = logo(150, 42)
    img.paste(icon, (96, 190), icon)
    d = ImageDraw.Draw(img)
    d.text((292, 170), "IR Voider", font=font(F_BLACK, 104), fill=TEXT)
    d.text((300, 292), "The offline IR remote for Android phones.",
           font=font(F_REG, 34), fill=MUTED)
    d.text((300, 348), "39 categories  \u00B7  1,502 remotes  \u00B7  scanner history  \u00B7  no internet",
           font=font(F_SEMI, 27), fill=CYAN)
    d.text((300, 400), "Phone only  \u00B7  Android 6.0+  \u00B7  built-in IR blaster required",
           font=font(F_REG, 24), fill=MUTED)
    chips = ["SCANNER + LAST 5", "SWIPEABLE PAGES", "AIRCON CONTROLS", "IR OVERRIDE"]
    y = 170
    for label in chips:
        draw_chip = font(F_SEMI, 21)
        wpx = d.textlength(label, font=draw_chip) + 32
        d.rounded_rectangle([1130, y, 1130 + wpx, y + 46], radius=23,
                            fill=(10, 10, 10), outline=STROKE, width=2)
        d.ellipse([1148, y + 18, 1158, y + 28], fill=CYAN)
        d.text((1170, y + 12), label, font=draw_chip, fill=TEXT)
        y += 62
    d.text((1504, 500), "made by poging kyle", font=font(F_REG, 22),
           fill=(100, 100, 104), anchor="ra")
    return img


def draw_social():
    w, h = 1280, 640
    img = glow_bg(w, h)
    icon = logo(138, 38)
    img.paste(icon, (84, 250), icon)
    d = ImageDraw.Draw(img)
    d.text((258, 196), "IR Voider", font=font(F_BLACK, 92), fill=TEXT)
    d.text((264, 312), "The offline IR remote for Android phones.",
           font=font(F_REG, 30), fill=MUTED)
    d.text((264, 362), "39 categories  \u00B7  1,502 remotes  \u00B7  scanner + AC pages  \u00B7  no internet",
           font=font(F_SEMI, 24), fill=CYAN)
    d.text((264, 406), "Phone only  \u00B7  Android 6.0+  \u00B7  built-in IR blaster",
           font=font(F_REG, 22), fill=MUTED)
    d.text((1196, 582), "made by poging kyle", font=font(F_REG, 20),
           fill=(100, 100, 104), anchor="ra")
    return img


def draw_feature():
    w, h = 1024, 500
    img = glow_bg(w, h)
    icon = logo(112, 30)
    img.paste(icon, (64, 194), icon)
    d = ImageDraw.Draw(img)
    d.text((204, 150), "IR Voider", font=font(F_BLACK, 72), fill=TEXT)
    d.text((208, 250), "The offline IR remote for Android phones.",
           font=font(F_REG, 24), fill=MUTED)
    d.text((208, 292), "39 categories  \u00B7  1,502 remotes  \u00B7  scanner + AC pages  \u00B7  no internet",
           font=font(F_SEMI, 18), fill=CYAN)
    d.text((208, 330), "Phone only  \u00B7  Android 6.0+  \u00B7  built-in IR blaster",
           font=font(F_REG, 17), fill=MUTED)
    d.text((960, 460), "made by poging kyle", font=font(F_REG, 16),
           fill=(100, 100, 104), anchor="ra")
    return img


def phone_frame(screen):
    sw, sh = 720, 1600
    scr = screen.resize((sw, sh), Image.LANCZOS)
    mask = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, sw - 1, sh - 1], radius=44, fill=255)
    frame = Image.new("RGBA", (sw + 28, sh + 28), (0, 0, 0, 0))
    fd = ImageDraw.Draw(frame)
    fd.rounded_rectangle([0, 0, sw + 27, sh + 27], radius=56, fill=(16, 13, 11, 255),
                         outline=(44, 40, 36, 255), width=3)
    frame.paste(scr, (14, 14), mask)
    fd.ellipse([sw // 2 + 4, 28, sw // 2 + 24, 48], fill=(0, 0, 0, 255))
    return frame


def draw_composite(remote, scanner, ac):
    w, h = 2600, 1920
    img = glow_bg(w, h)
    d = ImageDraw.Draw(img)
    d.text((90, 70), "IR Voider \u2014 UI preview", font=font(F_BLACK, 58), fill=TEXT)
    d.text((92, 146), "v1.4  \u00B7  illustrative layout from the app's palette and controls.",
           font=font(F_REG, 28), fill=MUTED)

    screens = [remote, scanner, ac]
    labels = ["Remote", "Scanner", "Air conditioner"]
    xs = [80, 940, 1800]
    for x, scr, label in zip(xs, screens, labels):
        frame = phone_frame(scr)
        img.paste(frame, (x, 220), frame)
        d.text((x + 374, 1868), label, font=font(F_SEMI, 30),
               fill=TEXT, anchor="mm")
    d.text((1300, 1900), "Actual buttons depend on the selected remote profile.",
           font=font(F_REG, 20), fill=(100, 100, 104), anchor="mm")
    return img


def main():
    os.makedirs("docs/images", exist_ok=True)
    os.makedirs("docs/play", exist_ok=True)
    remote = remote_screen()
    scanner = scanner_screen()
    ac = ac_screen()
    banner = draw_banner()
    composite = draw_composite(remote, scanner, ac)
    social = draw_social()
    feature = draw_feature()

    for old in ("home.png", "more.png", "devices.png"):
        path = os.path.join("docs/images", old)
        if os.path.exists(path):
            os.remove(path)

    banner.save("docs/images/banner.png")
    composite.save("docs/images/preview.png")
    social.save("docs/images/social-preview.png")
    feature.save("docs/play/feature-graphic.png")
    print("Wrote docs/images/banner.png, preview.png, social-preview.png")
    print("Wrote docs/play/feature-graphic.png")


if __name__ == "__main__":
    main()
