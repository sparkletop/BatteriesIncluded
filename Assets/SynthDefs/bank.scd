// Oscillator bank synthesis

SynthDef(\bank, {
	arg freq = 220, pan = 0, amp = 0.1,	outBus = 0,
	detuneCent = 10,	subAmp = 0.1, spread = 0.75,
	drive = 0.1, cutoffOctave = 4, rq = 1,
	atk = 0.02, dec = 0.1, sus = 0.9, rel = 0.3, gate = 1;

	var sig, env, cutoff;

	// Envelope
	env = Env.adsr(atk, dec, sus, rel).kr(2, gate);

	// Create an an oscillator bank (i.e., an array of 6 "voices")
	sig = 4.collect{
		arg num;
		VarSaw.ar(
			// Detuning: Spread 4 voices between +- detuneCent cents deviation from fundamental
			freq: freq * (
				num.linlin(0, 3, -1, 1) * 0.01 *
				LFNoise1.kr(1).unipolar(detuneCent).clip(0, 50)
			).midiratio,
			width: 0);
	};

	// Splay the oscillator bank onto a stereo field
	sig = Splay.ar(sig, spread, center: pan);

	// Add a sine oscillator one octave below the main bank
	sig = sig + (SinOsc.ar(freq * 0.5) * subAmp.clip(0.0, 1.0));

	// Add a bit of distortion/glue
	sig = (sig * drive.linexp(0, 1, 1, 50)).tanh;

	// Filter
	sig = BLowPass.ar(sig,
		(freq * 2.pow(cutoffOctave)).clip(20, 20000),
		rq.clip(0.001, 1)
	);

	// Apply the volume envelope
	sig = sig * env;

	sig = Balance2.ar(sig[0], sig[1], pan, amp);

	Out.ar(outBus, sig);
}, (1!10), metadata: (
	specs: (
		detuneCent: ControlSpec(0, 50, 0, 1, 10, "cent"),
		spread: \unipolar.asSpec,
		subAmp: \amp.asSpec,
		rq: ControlSpec(0.001, 1, \exp, 0, 1, "recip Q")
	)
)).add;
