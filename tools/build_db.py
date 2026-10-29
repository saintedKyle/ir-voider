#!/usr/bin/env python3
"""Convert Flipper-IRDB TV remotes into raw IR patterns for the Android app.

Encoders are ported from Flipper Zero firmware (lib/infrared/encoder_decoder)
and IRremoteESP8266 timing constants. Output: app/src/main/assets/remotes.json
"""

import json
import os
import re
import sys
from collections import Counter, OrderedDict

ROOT = "data/irdb_all"
OUT_CATEGORIES = OrderedDict([
    ("TVs", "TV"), ("ACs", "Air Conditioner"), ("Projectors", "Projector"),
    ("Fans", "Fan"), ("LED_Lighting", "LED Light"), ("SoundBars", "Soundbar"),
    ("Audio_and_Video_Receivers", "Audio / Receiver"), ("Speakers", "Speaker"),
    ("Streaming_Devices", "Streaming Box"), ("Cable_Boxes", "Cable / Sat Box"),
    ("TV_Tuner", "TV Tuner"), ("DVB-T", "DVB-T Receiver"),
    ("DVD_Players", "DVD Player"), ("Blu-Ray", "Blu-ray Player"),
    ("VCR", "VCR"), ("CD_Players", "CD Player"), ("Cameras", "Camera"),
    ("CCTV", "CCTV"), ("Air_Purifiers", "Air Purifier"), ("Heaters", "Heater"),
    ("Humidifiers", "Humidifier"), ("Vacuum_Cleaners", "Vacuum"),
    ("Monitors", "Monitor"), ("Projectors", "Projector"),
    ("Touchscreen_Displays", "Touchscreen"), ("Whiteboards", "Whiteboard"),
    ("Converters", "Converter"), ("Car_Multimedia", "Car Audio"),
    ("Multimedia", "Multimedia"), ("Consoles", "Console"), ("Toys", "Toy"),
    ("Fireplaces", "Fireplace"), ("Picture_Frames", "Picture Frame"),
    ("Laserdisc", "Laserdisc"), ("MiniDisc", "MiniDisc"),
    ("Miscellaneous", "Misc"),
])
OUT = "app/src/main/assets/remotes.json"

# ---------------------------------------------------------------- protocols


class Timings:
    def __init__(self, pm, ps, m1, s1, m0, s0, freq):
        self.pm = pm
        self.ps = ps
        self.m1 = m1
        self.s1 = s1
        self.m0 = m0
        self.s0 = s0
        self.freq = freq


NEC = Timings(9000, 4500, 560, 1690, 560, 560, 38000)
SAMSUNG = Timings(4500, 4500, 550, 1650, 550, 550, 38000)
SIRC = Timings(2400, 600, 1200, 600, 600, 600, 40000)
KASEIKYO = Timings(3456, 1728, 432, 1296, 432, 432, 38000)
RCA = Timings(4000, 4000, 500, 2000, 500, 1000, 38000)
PIONEER = Timings(8500, 4225, 500, 1500, 500, 500, 40000)
RC5_T = Timings(0, 0, 889, 0, 889, 0, 36000)
RC6_T = Timings(2666, 889, 444, 0, 444, 0, 36000)


def finish_pdwm(t, bits, nbits):
    """LSB-first pulse-distance encoding with preamble and final mark."""
    out = [t.pm, t.ps]
    for i in range(nbits):
        b = (bits >> i) & 1
        out.append(t.m1 if b else t.m0)
        out.append(t.s1 if b else t.s0)
    pwm = t.s1 == t.s0  # spaces equal -> last space is omitted
    if pwm:
        out.pop()
    else:
        out.append(t.m0)
    return out


def rev32(v):
    v &= 0xFFFFFFFF
    r = 0
    for _ in range(32):
        r = (r << 1) | (v & 1)
        v >>= 1
    return r


def rev_bits(v, n):
    r = 0
    for _ in range(n):
        r = (r << 1) | (v & 1)
        v >>= 1
    return r


def nec_pattern(address, command, ext):
    if ext:
        bits = (address & 0xFFFF) | ((command & 0xFFFF) << 16)
        return finish_pdwm(NEC, bits, 32)
    bits = ((address & 0xFF) | ((~address & 0xFF) << 8) |
            ((command & 0xFF) << 16) | ((~command & 0xFF) << 24))
    return finish_pdwm(NEC, bits, 32)


def samsung_pattern(address, command):
    bits = ((address & 0xFF) | ((address & 0xFF) << 8) |
            ((command & 0xFF) << 16) | ((~command & 0xFF) << 24))
    return finish_pdwm(SAMSUNG, bits, 32)


def sirc_pattern(address, command):
    bits = (command & 0x7F) | ((address & 0x1F) << 7)
    return finish_pdwm(SIRC, bits, 12)


def kaseikyo_pattern(address, command):
    addr = address & 0x3FFFFFF
    cmd = command & 0x3FF
    vid = (addr >> 8) & 0xFFFF
    genre1 = (addr >> 4) & 0xF
    genre2 = addr & 0xF
    ident = (addr >> 24) & 0x3
    d = [0] * 6
    d[0] = vid & 0xFF
    d[1] = vid >> 8
    vp = d[0] ^ d[1]
    vp = (vp & 0xF) ^ (vp >> 4)
    d[2] = (vp & 0xF) | (genre1 << 4)
    d[3] = (genre2 & 0xF) | ((cmd & 0xF) << 4)
    d[4] = (ident << 6) | (cmd >> 4)
    d[5] = d[2] ^ d[3] ^ d[4]
    bits = 0
    for i, byte in enumerate(d):
        bits |= byte << (8 * i)
    return finish_pdwm(KASEIKYO, bits, 48)


def rca_pattern(address, command):
    a = address & 0xF
    c = command & 0xFF
    bits = a | (c << 4) | ((~a & 0xF) << 12) | ((~c & 0xFF) << 16)
    return finish_pdwm(RCA, bits, 24)


def pioneer_pattern(address, command):
    a = address & 0xFF
    c = command & 0xFF
    d = [a, ~a & 0xFF, c, ~c & 0xFF, 0]
    bits = 0
    for i, byte in enumerate(d):
        bits |= byte << (8 * i)
    return finish_pdwm(PIONEER, bits, 33)


def rc5_pattern(address, command):
    """IRremoteESP8266 sendRC5 with toggle=0 (14 on-air bits)."""
    t = 889
    out = [t]  # leading space is skipped on first send
    out.append(t)  # mark
    # field/second start bit = 1 for RC5
    out += [t, t]  # space, mark
    data = ((address & 0x1F) << 6) | (command & 0x3F)  # 12 bits incl toggle=0
    for mask in range(1 << 11, 0, -1):
        if data & mask:
            out += [t, t]  # space then mark
        else:
            out += [t, t]  # mark then space
    # The first value is a leading space; drop it. Alternation handled below.
    return _levels_to_pattern(_rc5_levels(data, t))


def _rc5_levels(data, t):
    """Return (level,duration) list; level True=mark."""
    events = []
    # first start bit: space, mark
    events += [(False, t), (True, t)]
    # second start bit: 1 -> space, mark
    events += [(False, t), (True, t)]
    for i in range(11, -1, -1):
        if (data >> i) & 1:
            events += [(False, t), (True, t)]
        else:
            events += [(True, t), (False, t)]
    return events


def rc6_pattern(address, command):
    t = 444
    events = [(True, 2666), (False, 889)]
    events += [(True, t), (False, t)]  # start bit 1
    # 20 data bits, MSB first: mode(3)=0, toggle=0, address(8), command(8)
    data = ((address & 0xFF) << 8) | (command & 0xFF)
    for i in range(19, -1, -1):
        bit_time = 2 * t if i == 16 else t  # 4th sent bit = toggle
        if (data >> i) & 1:
            events += [(True, bit_time), (False, bit_time)]
        else:
            events += [(False, bit_time), (True, bit_time)]
    return _levels_to_pattern(events)


def _levels_to_pattern(events):
    """Merge consecutive same-level events, drop leading space, add gap."""
    merged = []
    for lvl, dur in events:
        if merged and merged[-1][0] == lvl:
            merged[-1] = (lvl, merged[-1][1] + dur)
        else:
            merged.append((lvl, dur))
    if merged and not merged[0][0]:
        merged.pop(0)
    pat = [int(dur) for _, dur in merged]
    if merged and not merged[-1][0]:
        pat[-1] += 30000  # extend trailing off into the gap
    else:
        pat.append(30000)
    return pat


PROTOCOLS = {
    "NEC": lambda a, c: nec_pattern(a, c, False),
    "NECext": lambda a, c: nec_pattern(a, c, True),
    "NEC42": lambda a, c: None,
    "NEC42ext": lambda a, c: None,
    "Samsung32": samsung_pattern,
    "SIRC": sirc_pattern,
    "SIRC15": sirc_pattern,
    "SIRC20": sirc_pattern,
    "Kaseikyo": kaseikyo_pattern,
    "RCA": rca_pattern,
    "Pioneer": pioneer_pattern,
    "RC5": rc5_pattern,
    "RC6": rc6_pattern,
}

# ---------------------------------------------------------------- .ir parsing


def parse_ir(path):
    entries = []
    cur = None
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            s = line.strip()
            if not s or s.startswith("#"):
                continue
            if s.startswith("name:"):
                if cur:
                    entries.append(cur)
                cur = {"name": s.split(":", 1)[1].strip()}
                continue
            if ":" in s and cur is not None:
                k, v = s.split(":", 1)
                k = k.strip().lower()
                v = v.strip()
                if k == "data":
                    cur[k] = [int(x) for x in v.split() if re.fullmatch(r"-?\d+", x)]
                else:
                    cur[k] = v
    if cur:
        entries.append(cur)
    return entries


def hexval(s):
    if s is None:
        return 0
    parts = s.replace("-", " ").split()
    try:
        raw = bytes(int(p, 16) for p in parts)
    except ValueError:
        return 0
    return int.from_bytes(raw, "little")


BUTTON_ALIASES = {
    "power": "power", "power_toggle": "power", "power_on": "power",
    "on": "power", "turn_on": "power", "off": "power_off",
    "power_off": "power_off", "turn_off": "power_off",
    "vol_up": "vol_up", "volume_up": "vol_up", "vol_+": "vol_up", "vol+": "vol_up",
    "vol_dn": "vol_down", "vol_down": "vol_down", "volume_down": "vol_down",
    "vol_-": "vol_down", "vol-": "vol_down",
    "mute": "mute", "mute_toggle": "mute",
    "ch_next": "ch_up", "ch_up": "ch_up", "channel_up": "ch_up", "ch+": "ch_up",
    "ch_prev": "ch_down", "ch_down": "ch_down", "channel_down": "ch_down", "ch-": "ch_down",
    "input": "input", "source": "input", "tv_source": "input", "input_source": "input",
    "up": "up", "arrow_up": "up", "down": "down", "arrow_down": "down",
    "left": "left", "arrow_left": "left", "right": "right", "arrow_right": "right",
    "ok": "ok", "enter": "ok", "select": "ok", "center": "ok",
    "back": "back", "return": "back", "exit": "exit", "close": "exit",
    "home": "home", "smart_hub": "home", "menu_home": "home",
    "menu": "menu", "settings": "menu",
    "info": "info", "information": "info", "guide": "guide", "epg": "guide",
    "play": "play_pause", "pause": "play_pause", "play_pause": "play_pause",
    "stop": "stop", "rewind": "rewind", "rw": "rewind",
    "fast_forward": "forward", "ff": "forward", "forward": "forward",
    "prev": "prev", "previous": "prev", "next": "next", "record": "record",
    "subtitle": "subtitle", "subtitles": "subtitle", "cc": "subtitle",
    "audio": "audio", "audio_track": "audio", "language": "audio",
    "aspect": "aspect", "ratio": "aspect", "picture_size": "aspect",
    "last": "last", "last_channel": "last", "channel_return": "last",
    "fav": "fav", "favorite": "fav", "favourite": "fav",
    "apps": "apps", "netflix": "netflix",
    "temp_up": "temp_up", "temp_plus": "temp_up", "temp+": "temp_up",
    "temp_increase": "temp_up", "increase_temp": "temp_up",
    "temp_down": "temp_down", "temp_minus": "temp_down", "temp-": "temp_down",
    "temp_decrease": "temp_down", "decrease_temp": "temp_down",
    "mode": "mode", "operation_mode": "mode",
    "fan": "fan", "fan_speed": "speed", "fan_speed_up": "speed",
    "swing": "swing", "swing_v": "swing", "swing_h": "swing",
    "swing_toggle": "swing", "vertical_swing": "swing", "horizontal_swing": "swing",
    "speed": "speed", "speed_up": "speed",
    "light": "light", "lamp": "light", "display": "light", "led": "light",
    "timer": "timer", "sleep_timer": "timer", "off_timer": "timer", "on_timer": "timer",
    "sleep": "sleep",
    "brightness_up": "brightness_up", "bright_up": "brightness_up",
    "brightness+": "brightness_up",
    "brightness_down": "brightness_down", "bright_down": "brightness_down",
    "brightness-": "brightness_down",
    "color": "color", "colour": "color", "color_temp": "color",
    "red": "red", "green": "green", "yellow": "yellow", "blue": "blue",
    "zoom_in": "zoom_in", "zoom+": "zoom_in",
    "zoom_out": "zoom_out", "zoom-": "zoom_out",
    "dash": "dash", "-": "dash", "dot": "dot", ".": "dot", "period": "dot",
}

for _d in range(10):
    BUTTON_ALIASES[str(_d)] = "num_%d" % _d
    BUTTON_ALIASES["num_%d" % _d] = "num_%d" % _d
    BUTTON_ALIASES["digit_%d" % _d] = "num_%d" % _d
    BUTTON_ALIASES["number_%d" % _d] = "num_%d" % _d

KEEP = ["power", "power_off", "vol_up", "vol_down", "mute", "ch_up", "ch_down",
        "input", "up", "down", "left", "right", "ok", "back", "home", "menu",
        "exit", "info", "guide", "play_pause", "stop", "rewind", "forward",
        "prev", "next", "record", "temp_up", "temp_down", "mode", "fan", "swing",
        "speed", "light", "timer", "sleep", "brightness_up", "brightness_down",
        "color", "red", "green", "yellow", "blue", "zoom_in", "zoom_out",
        "subtitle", "audio", "aspect", "last", "fav", "apps", "netflix",
        "num_0", "num_1", "num_2", "num_3", "num_4", "num_5", "num_6", "num_7",
        "num_8", "num_9", "dash", "dot"]


def normalize(name):
    key = re.sub(r"[^a-z0-9_+\-]", "", name.strip().lower().replace(" ", "_"))
    return BUTTON_ALIASES.get(key)


def to_pattern(entry):
    if entry.get("type") == "raw":
        data = entry.get("data") or []
        if len(data) < 4:
            return None
        freq = int(float(entry.get("frequency", 38000)))
        pat = [int(x) for x in data if x > 0]
        if not pat or max(pat) > 400000:
            return None
        if len(pat) % 2 == 1:
            pat.append(30000)
        else:
            pat[-1] = max(pat[-1], 30000)
        return freq, pat
    proto = entry.get("protocol")
    fn = PROTOCOLS.get(proto)
    if not fn:
        return None
    addr = hexval(entry.get("address"))
    cmd = hexval(entry.get("command"))
    try:
        pat = fn(addr, cmd)
    except Exception:
        return None
    if not pat:
        return None
    freq = {
        "RC5": 36000, "RC6": 36000, "SIRC": 40000, "SIRC15": 40000,
        "SIRC20": 40000, "Pioneer": 40000,
    }.get(proto, 38000)
    pat = list(pat)
    if len(pat) % 2 == 1:  # ends on a mark -> add trailing off gap
        pat.append(30000)
    return freq, pat


def main():
    categories = []
    grand_total = 0
    skipped = Counter()
    for folder, display in OUT_CATEGORIES.items():
        cpath = os.path.join(ROOT, folder)
        if not os.path.isdir(cpath):
            continue
        brands = OrderedDict()
        count = 0
        for brand in sorted(os.listdir(cpath)):
            bpath = os.path.join(cpath, brand)
            if not os.path.isdir(bpath):
                continue
            remotes = []
            brand_sigs = {}
            for fn in sorted(os.listdir(bpath)):
                if not fn.endswith(".ir"):
                    continue
                path = os.path.join(bpath, fn)
                buttons = {}
                for e in parse_ir(path):
                    key = normalize(e.get("name", ""))
                    if not key or key in buttons:
                        continue
                    res = to_pattern(e)
                    if not res:
                        skipped[e.get("protocol", "raw")] += 1
                        continue
                    buttons[key] = [res[0], res[1]]
                if "power" not in buttons:
                    continue
                ordered = {k: buttons[k] for k in KEEP if k in buttons}
                sig = tuple((k, v[0], tuple(v[1])) for k, v in ordered.items())
                prev = brand_sigs.get(sig)
                if prev is not None:
                    if len(ordered) > len(prev["buttons"]):
                        prev["buttons"] = ordered
                        prev["name"] = fn[:-3]
                    continue
                remote = {"name": fn[:-3], "buttons": ordered}
                brand_sigs[sig] = remote
                remotes.append(remote)
                count += 1
            if remotes:
                brands[brand] = remotes
        if not brands:
            continue
        categories.append({
            "name": display,
            "brands": [{"name": b, "remotes": rs} for b, rs in brands.items()],
        })
        grand_total += count
        print(f"{display:<22} brands={len(brands):<4} remotes={count}")

    data = {
        "source": "Flipper-IRDB (https://github.com/Lucaslhm/Flipper-IRDB)",
        "categories": categories,
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(data, f, separators=(",", ":"))
    size = os.path.getsize(OUT)
    print(f"categories={len(categories)} remotes={grand_total} bytes={size/1e6:.2f}MB")
    print("skipped protocols:", skipped.most_common(12))


if __name__ == "__main__":
    main()
