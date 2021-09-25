// A SynthDef that showcases (mis)use of envelopes
SynthDef(\envy, {
	arg freq = 440, amp = 0.1, outBus = 0, gate = 1,
	grainAmp = 0.3, atk = 2, rel = 4;
	var env, sig, grains;

	// Define the main envelope
	env = Env.asr(
		attackTime: atk,
		sustainLevel: 1,
		releaseTime: rel,
		curve: -2
	).kr(
		doneAction: 2,
		gate: gate
	);

	// Source: Detune and waveform width modulated by envelope
	sig = VarSaw.ar(
		freq: freq * env.curverange(
			lo: [-0.05, -0.5, 0.05],
			hi: [0, -0.01, 0.005]
		).midiratio,
		width: env.range(0.5, 0.9)
	);

	// Filter: Cutoff and resonance modulated by envelope
	sig = MoogFF.ar(
		in: sig,
		freq: freq * env.curverange(12, 36, -10).midiratio,
		gain: env.range(1, 3.2)
	);

	// Granular sprinkle: Various parameters modulated by envelope
	grains = GrainIn.ar(
		numChannels: 2,
		trigger: Impulse.ar(env.curverange(4, 15, 10)),
		dur: 0.05,
		in: PitchShift.ar(
			in: sig.sum,
			windowSize: 0.05,
			pitchRatio: env.curverange(0.5, 3, -10)
			* LFNoise1.kr(5).bipolar(env * 0.25).midiratio
		),
		pan: LFNoise0.kr(env*100 + 10).bipolar(env)
	) * grainAmp * env;

	sig = Splay.ar(sig) + grains;
	sig = sig * env * amp;
	Out.ar(outBus, sig);
}).add;
