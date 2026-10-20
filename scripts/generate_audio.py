"""Generate original 30-second PCM chord samples without external recordings."""
import math
import struct
import wave
from pathlib import Path
output = Path(__file__).resolve().parents[1] / 'app/src/main/assets'
for name, notes in [('mint', [220, 277.18, 329.63]), ('slate', [196, 246.94, 293.66]), ('lavender', [174.61, 220, 261.63])]:
    with wave.open(str(output / (name + '.wav')), 'wb') as wav:
        wav.setparams((1, 2, 22050, 0, 'NONE', 'not compressed'))
        data = bytearray()
        for i in range(30 * 22050):
            t = i / 22050
            envelope = min(t / 1.5, 1, (30 - t) / 2)
            pulse = .65 + .35 * math.sin(2 * math.pi * .15 * t)
            value = sum(math.sin(2 * math.pi * f * t) * .11 for f in notes) * envelope * pulse
            data.extend(struct.pack('<h', int(value * 32767)))
        wav.writeframes(data)
