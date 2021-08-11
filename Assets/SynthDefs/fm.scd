// FM synthesis

SynthDef(\fm, {
	arg freq = 440, pan = 0, amp = 0.1, outBus = 0,
	fmIndex = 5, modulatorNoise = 0.1,
	atk = 0.01, dec = 0.2, sus = 0.8, rel = 0.3, gate = 1;
	var env, sig, modulator, carrier;

	env = Env.adsr(atk, dec, sus, rel).kr(2, gate)
	* PinkNoise.kr(1!2).range(0.5,1).lag(0.005);

	// First, we create the modulator signal
	modulator = SinOsc.ar(freq * 2) * freq * fmIndex;

	// Add a bit of noise to the modulator,
	modulator = modulator + PinkNoise.ar.bipolar(
		freq * env * 0.1 * modulatorNoise.clip(0, 1) ! 2
	);

	// Create the carrier signal
	carrier = SinOsc.ar(freq + modulator);

	sig = carrier * env;

	sig = Balance2.ar(sig[0], sig[1], pan, amp);
	Out.ar(outBus, sig);
}, (1!8),
metadata: (
	specs:
	(
		\modulatorNoise: ControlSpec(0, 1, step: 0.01),
		\fmIndex: ControlSpec(1, 200, 2, 0.001)
	)
)
).add;