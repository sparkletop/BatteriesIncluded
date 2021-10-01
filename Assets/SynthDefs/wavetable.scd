// Wavetable synthesis

SynthDef(\wavetable, {
	arg freq = 440, pan = 0, amp = 0.1, outBus = 0,
	cutoffOctave = 4,	buffer = BI.prBufInfo(\wavetables).lo,
	atk = 0.1, sus = 1, rel = 0.8, gate = 1;

	var sig, env;

	env = Env.asr(atk, sus, rel, \sin).kr(2, gate)
	* PinkNoise.ar(1!2).range(0.5, 1).lag(0.01);

	// In order to use the wavetables, we use a generic wavetable lookup oscillator - Osc
	// Osc needs to know which buffer contains the desired wavetable
	sig = Osc.ar(
		bufnum: buffer,
		freq: freq
	);

	// Wavetable synthesis can generate a wide variety of timbres, including very harsh and noisy ones
	// To allow for a bit of timbral control, we add a low pass filter
	sig = LPF.ar(
		sig,
		(freq * 2.pow(cutoffOctave)).clip(20, 20000)
	);

	sig = Pan2.ar(sig, pan, amp) * env;
	Out.ar(outBus, sig);
}, (1!5),

metadata: (
	specs: (
		freq: BI.specs[\freq],
		pan: BI.specs[\pan],
		amp: BI.specs[\amp],
		outBus: BI.specs[\outBus],
		cutoffOctave: BI.specs[\cutoffOctave],
		buffer: ControlSpec(
			minval: BI.prBufInfo(\wavetables).lo,
			maxval: BI.prBufInfo(\wavetables).hi,
			step: 1,
			default: BI.prBufInfo(\wavetables).lo
		),
		atk: BI.specs[\atk],
		sus: BI.specs[\sus],
		rel: BI.specs[\rel]
	)
)
).add;
