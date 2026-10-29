#!/usr/bin/env python3
"""Generate illustrative README preview images for IR Voider.

These are design previews rendered from the app's palette/layout, not device
screenshots. Requires Pillow and Windows Segoe UI fonts.
"""

import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1080, 2400
BG_TOP = (5, 7, 13)
BG_MID = (11, 18, 32)
BG_BOT = (17, 28, 49)
CARD = (18, 26, 43)
CARD2 = (22, 33, 58)
STROKE = (38, 52, 79)
BTN = (27, 39, 64)
ACCENT = (34, 211, 238)
ACCENT2 = (99, 102, 241)
TEXT = (248, 250, 252)
MUTED = (148, 163, 184)
RED1 = (251, 113, 133)
RED2 = (225, 29, 72)
GREEN1 = (52, 211, 153)
GREEN2 = (5, 150, 105)
DARK = (4, 20, 27)

F_BLACK = "C:/Windows/Fonts/segoeuib.ttf"
F_SEMI = "C:/Windows/Fonts/seguisb.ttf"
F_REG = "C:/Windows/Fonts/segoeui.ttf"
F_SYM = "C:/Windows/Fonts/seguisym.ttf"
ICON = "app/src/main/res/drawable/ic_launcher.png"


def font(path, size):
    return ImageFont.truetype(path, size)


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


def card(draw, box, radius=48, fill=CARD, outline=STROKE):
    draw.rounded_rectangle([box[0] + 4, box[1] + 6, box[2] + 4, box[3] + 6],
                           radius=radius, fill=(7, 11, 20))
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=2)


def text(draw, xy, value, f, fill=TEXT, anchor="la"):
    draw.text(xy, value, font=f, fill=fill, anchor=anchor)


def tracked(draw, xy, value, f, fill, spacing=6):
    x, y = xy
    for ch in value:
        draw.text((x, y), ch, font=f, fill=fill, anchor="la")
        x += draw.textlength(ch, font=f) + spacing
    return x


def button(draw, box, label, fill, color=TEXT, radius=32, f=None):
    draw.rounded_rectangle(box, radius=radius, fill=fill)
    f = f or font(F_SEMI, 32)
    cx = (box[0] + box[2]) / 2
    cy = (box[1] + box[3]) / 2
    draw.text((cx, cy), label, font=f, fill=color, anchor="mm")


def gradient_button(img, draw, box, label, colors, color, radius=36, size=36):
    paste_gradient(img, box, colors, radius)
    f = font(F_BLACK, size)
    draw.text(((box[0] + box[2]) / 2, (box[1] + box[3]) / 2), label,
              font=f, fill=color, anchor="mm")


def status_bar(draw):
    f = font(F_SEMI, 28)
    draw.text((44, 34), "9:41", font=f, fill=TEXT, anchor="la")
    # signal bars
    x = 880
    for i, hgt in enumerate((10, 16, 22, 28)):
        draw.rounded_rectangle([x + i * 12, 56 - hgt, x + i * 12 + 7, 56],
                               radius=2, fill=TEXT)
    # wifi arcs
    draw.arc([952, 30, 1000, 70], start=200, end=340, fill=TEXT, width=4)
    draw.arc([962, 40, 990, 66], start=200, end=340, fill=TEXT, width=4)
    draw.ellipse([973, 53, 979, 59], fill=TEXT)
    # battery
    draw.rounded_rectangle([1012, 34, 1054, 58], radius=5, outline=TEXT, width=3)
    draw.rounded_rectangle([1015, 37, 1043, 55], radius=3, fill=TEXT)
    draw.rectangle([1055, 42, 1059, 50], fill=TEXT)


def logo(size, radius):
    icon = Image.open(ICON).convert("RGBA").resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1],
                                           radius=radius, fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(icon, (0, 0), mask)
    return out


def chip(draw, x, y, label, f, fill, color, outline, pad_x=18, pad_y=8):
    w = draw.textlength(label, font=f) + pad_x * 2
    draw.rounded_rectangle([x, y, x + w, y + f.size + pad_y * 2], radius=40,
                           fill=fill, outline=outline, width=2)
    draw.text((x + pad_x, y + pad_y), label, font=f, fill=color, anchor="la")
    return x + w


def draw_home():
    img = Image.new("RGB", (W, H), BG_TOP)
    img.paste(vgrad((W, H), BG_TOP, BG_MID, BG_BOT), (0, 0))
    d = ImageDraw.Draw(img)
    status_bar(d)

    # top bar
    d.rounded_rectangle([40, 96, 148, 204], radius=28, fill=BTN)
    for i in range(3):
        d.rounded_rectangle([66, 126 + i * 20, 122, 132 + i * 20], radius=3, fill=TEXT)
    img.paste(logo(84, 24), (170, 108), logo(84, 24))
    text(d, (278, 92), "IR Voider", font(F_BLACK, 54), TEXT)
    f_small = font(F_SEMI, 25)
    text(d, (280, 164), "TV", f_small, ACCENT)
    text(d, (316, 164), "  ·  345 codes  ·  ≡ for more", f_small, MUTED)
    d.rounded_rectangle([940, 96, 1040, 196], radius=28, fill=BTN)
    d.text((990, 146), "\u2699", font=font(F_SYM, 40), fill=TEXT, anchor="mm")

    # now card
    card(d, (40, 246, 1040, 586))
    tracked(d, (76, 286), "NOW USING", font(F_SEMI, 23), ACCENT, 4)
    chip(d, 858, 278, "IR READY", font(F_SEMI, 21), (12, 46, 36),
         (110, 231, 183), (20, 83, 45), 16, 7)
    text(d, (76, 336), "Living Room TV", font(F_BLACK, 50), TEXT)
    text(d, (76, 410), "TV  ·  TCL — TCL_65C635K", font(F_REG, 28), MUTED)
    text(d, (76, 466), "Tap ≡ to change category, or the gear to name and save devices.",
         font(F_REG, 24), MUTED)
    text(d, (76, 520), "Power is a toggle — the same button turns it on and off.",
         font(F_REG, 24), MUTED)

    # controls card
    card(d, (40, 626, 1040, 1656))
    tracked(d, (76, 664), "CONTROLS", font(F_SEMI, 22), MUTED, 4)
    gradient_button(img, d, (76, 706, 1004, 818), "POWER",
                    (RED1, RED2), (255, 255, 255), 38, 42)

    rows = [
        ["Vol +", "Vol −", "Mute"],
        ["CH +", "CH −", "Input"],
        [None, "▲", None],
        ["‹", "OK", "›"],
        [None, "▼", None],
        ["Back", "Home", "Menu"],
        ["Play/Pause", None, None],
    ]
    y = 848
    for row in rows:
        for i, label in enumerate(row):
            if label is None:
                continue
            x = 76 + i * 312
            w = 300
            if row[0] is None and row[2] is None:
                x = 390
                w = 300
            button(d, (x, y, x + w, y + 92), label, BTN, TEXT, 30,
                   font(F_SEMI, 31))
        y += 104
    text(d, (76, 1600), "Hold Vol or CH to repeat.", font(F_REG, 23), MUTED)

    # find card
    card(d, (40, 1690, 1040, 2320))
    text(d, (76, 1726), "Find a working code", font(F_BLACK, 39), TEXT)
    # brand/model fields
    for i, (label, value) in enumerate((("Brand", "TCL"), ("Model", "TCL 65C635K"))):
        yy = 1794 + i * 96
        d.rounded_rectangle([76, yy, 1004, yy + 80], radius=26, fill=CARD2,
                            outline=STROKE, width=2)
        text(d, (104, yy + 25), label, font(F_SEMI, 25), MUTED)
        text(d, (230, yy + 24), value, font(F_SEMI, 28), TEXT)
        d.text((960, yy + 40), "⌄", font=font(F_SYM, 30), fill=MUTED, anchor="mm")
    text(d, (76, 1990), "Point the phone's top edge at the device. One code at a time.",
         font(F_REG, 23), MUTED)
    # checkbox
    d.rounded_rectangle([76, 2040, 110, 2074], radius=8, outline=ACCENT, width=3)
    d.line([83, 2057, 92, 2066, 105, 2047], fill=ACCENT, width=4)
    text(d, (128, 2042), "Search every brand in this category", font(F_REG, 26), TEXT)
    # speed
    text(d, (76, 2110), "Speed", font(F_REG, 26), MUTED)
    labels = ["1.5 s", "2 s", "3 s"]
    for i, lab in enumerate(labels):
        x = 200 + i * 150
        active = i == 1
        d.rounded_rectangle([x, 2100, x + 136, 2160], radius=20,
                            fill=ACCENT if active else BTN)
        d.text((x + 68, 2130), lab, font=font(F_SEMI, 28),
               fill=DARK if active else MUTED, anchor="mm")
    gradient_button(img, d, (76, 2190, 1004, 2288), "Find code",
                    ((34, 211, 238), (59, 130, 246)), DARK, 36, 36)

    text(d, (540, 2360), "made by poging kyle", font(F_REG, 23), MUTED, "mm")
    return img


def draw_more(home):
    base = home.copy()
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 165))
    base = Image.alpha_composite(base.convert("RGBA"), overlay).convert("RGB")
    d = ImageDraw.Draw(base)
    # sheet extends below screen
    d.rounded_rectangle([0, 170, 1080, 2600], radius=64, fill=CARD,
                        outline=STROKE, width=2)
    d.rounded_rectangle([478, 204, 602, 214], radius=5, fill=(51, 65, 85))
    text(d, (56, 242), "More", font(F_BLACK, 64), TEXT)
    text(d, (58, 326), "Pick a device type, manage devices, fetch remotes.",
         font(F_REG, 26), MUTED)
    tracked(d, (58, 398), "CATEGORIES", font(F_SEMI, 22), ACCENT, 4)

    cats = [
        ("TV", "345 codes", True), ("Air Conditioner", "93 codes", False),
        ("Projector", "99 codes", False), ("Fan", "118 codes", False),
        ("LED Light", "110 codes", False), ("Soundbar", "94 codes", False),
        ("Audio / Receiver", "82 codes", False), ("Streaming Box", "15 codes", False),
        ("Cable / Sat Box", "43 codes", False), ("Blu-ray Player", "53 codes", False),
        ("DVD Player", "20 codes", False), ("Camera", "1 code", False),
        ("Air Purifier", "17 codes", False), ("Heater", "31 codes", False),
        ("Humidifier", "9 codes", False), ("Monitor", "7 codes", False),
    ]
    y = 446
    for i, (name, count, active) in enumerate(cats):
        x = 50 + (i % 2) * 510
        yy = y + (i // 2) * 132
        d.rounded_rectangle([x, yy, x + 470, yy + 112], radius=26,
                            fill=(11, 46, 58) if active else CARD2,
                            outline=ACCENT if active else STROKE, width=2)
        d.text((x + 24, yy + 22), name, font=font(F_SEMI, 25),
               fill=ACCENT if active else TEXT)
        d.text((x + 24, yy + 64), count, font=font(F_REG, 21), fill=MUTED)

    tracked(d, (58, 1520), "TOOLS", font(F_SEMI, 22), ACCENT, 4)
    tools = [
        ("Devices", "Name and switch saved devices"),
        ("Fetch original remote", "Import a captured .ir file"),
        ("IR & scan settings", "Boost and scan speed"),
        ("About", "Sources, limits and how it works"),
    ]
    y = 1564
    for title, sub in tools:
        d.rounded_rectangle([50, y, 1030, y + 104], radius=26, fill=CARD2,
                            outline=STROKE, width=2)
        d.text((78, y + 24), title, font=font(F_SEMI, 27), fill=TEXT)
        d.text((78, y + 63), sub, font=font(F_REG, 21), fill=MUTED)
        d.text((985, y + 52), "\u203A", font=font(F_REG, 40), fill=MUTED, anchor="mm")
        y += 118
    button(d, (50, 2050, 1030, 2144), "Close", BTN, TEXT, 28, font(F_SEMI, 28))
    text(d, (540, 2310), "made by poging kyle", font(F_REG, 23), MUTED, "mm")
    return base


def draw_devices():
    img = Image.new("RGB", (W, H), BG_TOP)
    img.paste(vgrad((W, H), BG_TOP, BG_MID, BG_BOT), (0, 0))
    d = ImageDraw.Draw(img)
    status_bar(d)
    text(d, (44, 96), "IR Voider", font(F_BLACK, 54), TEXT)
    d.rounded_rectangle([884, 96, 1036, 176], radius=28, fill=ACCENT)
    d.text((960, 136), "Done", font=font(F_SEMI, 28), fill=DARK, anchor="mm")
    text(d, (44, 196), "Devices & settings — name each remote, switch devices, tune IR.",
         font(F_REG, 24), MUTED)

    card(d, (40, 250, 1040, 780))
    tracked(d, (72, 286), "YOUR DEVICES", font(F_SEMI, 22), ACCENT, 4)
    devices = [
        ("Living Room TV", "TV  ·  TCL — TCL_65C635K"),
        ("Bedroom AC", "Aircon  ·  Generic"),
    ]
    y = 340
    for name, meta in devices:
        text(d, (72, y), name, font(F_SEMI, 32), TEXT)
        text(d, (72, y + 46), meta, font(F_REG, 22), MUTED)
        for i, (lab, col, tc) in enumerate((("Open", (14, 116, 144), TEXT),
                                           ("Rename", BTN, TEXT),
                                           ("Delete", (127, 29, 29), TEXT))):
            x = 72 + i * 146
            button(d, (x, y + 82, x + 134, y + 122), lab, col, tc, 18,
                   font(F_SEMI, 20))
        y += 190
    d.line([72, 700, 1008, 700], fill=STROKE, width=2)

    card(d, (40, 818, 1040, 1430))
    tracked(d, (72, 854), "ADD CURRENT REMOTE", font(F_SEMI, 22), ACCENT, 4)
    text(d, (72, 896), "Current: TCL — TCL_65C635K", font(F_REG, 25), TEXT)
    d.rounded_rectangle([72, 942, 1008, 1024], radius=22, fill=CARD2,
                        outline=STROKE, width=2)
    text(d, (96, 968), "Living Room TV", font(F_REG, 26), TEXT)
    text(d, (72, 1050), "Type", font(F_REG, 24), MUTED)
    text(d, (200, 1050), "TV", font(F_SEMI, 25), TEXT)
    button(d, (72, 1096, 1008, 1184), "Save as device", ACCENT, DARK, 26,
           font(F_SEMI, 28))
    text(d, (72, 1210), "Type is a label. Aircon/LED still use TV-style codes;",
         font(F_REG, 21), MUTED)
    text(d, (72, 1240), "full aircon state frames are not included.",
         font(F_REG, 21), MUTED)

    card(d, (40, 1468, 1040, 1815))
    tracked(d, (72, 1504), "IR & SCAN", font(F_SEMI, 22), ACCENT, 4)
    rows = [("IR boost", "Strong — 2 repeats"), ("Scan speed", "Normal — 2 s")]
    y = 1554
    for left, right in rows:
        text(d, (72, y), left, font(F_REG, 25), MUTED)
        text(d, (380, y), right, font(F_SEMI, 25), TEXT)
        y += 72
    text(d, (72, 1716), "Repeats volume/channel presses. Power, OK and navigation",
         font(F_REG, 21), MUTED)
    text(d, (72, 1746), "are sent once so they cannot double-fire.",
         font(F_REG, 21), MUTED)

    card(d, (40, 1855, 1040, 2190))
    tracked(d, (72, 1895), "LEARN FROM ORIGINAL REMOTE", font(F_SEMI, 22), ACCENT, 4)
    text(d, (72, 1939), "This phone only transmits IR. Use an IR receiver",
         font(F_REG, 23), MUTED)
    text(d, (72, 1973), "with an ESP32 to capture and import raw .ir codes.",
         font(F_REG, 23), MUTED)

    text(d, (540, 2340), "made by poging kyle", font(F_REG, 23), MUTED, "mm")
    return img


def draw_banner():
    w, h = 1600, 560
    img = Image.new("RGB", (w, h), BG_TOP)
    img.paste(vgrad((w, h), BG_TOP, BG_MID, BG_BOT), (0, 0))
    glow = Image.new("RGB", (w, h), (0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([-180, -220, 620, 500], fill=(8, 70, 90))
    gd.ellipse([980, 120, 1760, 820], fill=(44, 36, 110))
    glow = glow.filter(ImageFilter.GaussianBlur(140))
    img = Image.blend(img, Image.blend(img, glow, 0.0), 0.0)
    # screen blend
    img = Image.blend(img, Image.composite(glow, img, glow.convert("L")), 0.35)

    icon = logo(150, 42)
    img.paste(icon, (96, 190), icon)
    d = ImageDraw.Draw(img)
    d.text((292, 170), "IR Voider", font=font(F_BLACK, 104), fill=TEXT)
    d.text((300, 292), "The offline IR remote for Android phones.",
           font=font(F_REG, 34), fill=MUTED)
    d.text((300, 348), "33 device categories  ·  1,421 remotes  ·  no internet",
           font=font(F_SEMI, 27), fill=ACCENT)
    d.text((300, 400), "Phone only  ·  Android 6.0+  ·  built-in IR blaster required",
           font=font(F_REG, 24), fill=MUTED)
    d.text((1504, 500), "made by poging kyle", font=font(F_REG, 22),
           fill=(100, 116, 139), anchor="ra")
    return img


def draw_social():
    w, h = 1280, 640
    img = Image.new("RGB", (w, h), BG_TOP)
    img.paste(vgrad((w, h), BG_TOP, BG_MID, BG_BOT), (0, 0))
    glow = Image.new("RGB", (w, h), (0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([-140, -160, 520, 460], fill=(8, 70, 90))
    gd.ellipse([820, 120, 1440, 760], fill=(44, 36, 110))
    glow = glow.filter(ImageFilter.GaussianBlur(120))
    img = Image.blend(img, Image.composite(glow, img, glow.convert("L")), 0.35)

    icon = logo(138, 38)
    img.paste(icon, (84, 250), icon)
    d = ImageDraw.Draw(img)
    d.text((258, 196), "IR Voider", font=font(F_BLACK, 92), fill=TEXT)
    d.text((264, 312), "The offline IR remote for Android phones.",
           font=font(F_REG, 30), fill=MUTED)
    d.text((264, 362), "33 device categories  ·  1,421 remotes  ·  no internet",
           font=font(F_SEMI, 24), fill=ACCENT)
    d.text((264, 406), "Phone only  ·  Android 6.0+  ·  built-in IR blaster",
           font=font(F_REG, 22), fill=MUTED)
    d.text((1196, 582), "made by poging kyle", font=font(F_REG, 20),
           fill=(100, 116, 139), anchor="ra")
    return img


def phone_frame(screen):
    sw, sh = 720, 1600
    scr = screen.resize((sw, sh), Image.LANCZOS)
    mask = Image.new("L", (sw, sh), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, sw - 1, sh - 1], radius=44, fill=255)
    frame = Image.new("RGBA", (sw + 28, sh + 28), (0, 0, 0, 0))
    fd = ImageDraw.Draw(frame)
    fd.rounded_rectangle([0, 0, sw + 27, sh + 27], radius=56, fill=(10, 15, 24, 255),
                         outline=(42, 58, 85, 255), width=3)
    frame.paste(scr, (14, 14), mask)
    # punch-hole camera
    fd.ellipse([sw // 2 + 4, 28, sw // 2 + 24, 48], fill=(5, 7, 13, 255))
    return frame


def draw_composite(home, more, devices):
    w, h = 2600, 1920
    img = Image.new("RGB", (w, h), BG_TOP)
    img.paste(vgrad((w, h), BG_TOP, BG_MID, BG_BOT), (0, 0))
    d = ImageDraw.Draw(img)
    d.text((90, 70), "IR Voider — UI preview", font=font(F_BLACK, 58), fill=TEXT)
    d.text((92, 146), "Illustrative preview of the v1.0 layout.",
           font=font(F_REG, 28), fill=MUTED)

    screens = [home, more, devices]
    labels = ["Remote", "More", "Devices"]
    xs = [80, 940, 1800]
    for x, scr, label in zip(xs, screens, labels):
        frame = phone_frame(scr)
        img.paste(frame, (x, 220), frame)
        d.text((x + 374, 1850), label, font=font(F_SEMI, 30),
               fill=ACCENT, anchor="mm")
    d.text((1300, 1886), "UI preview generated from the app's palette and layout",
           font=font(F_REG, 20), fill=(100, 116, 139), anchor="mm")
    return img


def main():
    os.makedirs("docs/images", exist_ok=True)
    home = draw_home()
    more = draw_more(home)
    devices = draw_devices()
    banner = draw_banner()
    composite = draw_composite(home, more, devices)
    social = draw_social()
    home.save("docs/images/home.png")
    more.save("docs/images/more.png")
    devices.save("docs/images/devices.png")
    banner.save("docs/images/banner.png")
    composite.save("docs/images/preview.png")
    social.save("docs/images/social-preview.png")
    print("Wrote docs/images/{banner,home,more,devices,preview,social-preview}.png")


if __name__ == "__main__":
    main()
