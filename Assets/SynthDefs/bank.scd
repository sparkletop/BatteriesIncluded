// Oscillator bank synthesis

SynthDef(\bank, {
	arg freq = 110, pan = 0, amp = 0.1,	outBus = 0,
	detuneStep = 0.1,	subAmp = 0.1,
	drive = 0.1, cutoffOctave = 4,
	lfoRate = 1, lfoDepth = 0.1,
	atk = 0.02, dec = 0.1, sus = 1, rel = 0.3, gate = 1;

	var sig, env, cutoff;

	// Envelope
	env = Env.adsr(atk, dec, sus, rel).kr(2, gate);

	// PinkNoise.kr(1!2).range(0.75,1).lag(0.2);

	// Create an an oscillator bank (i.e., an array of 8 "voices")
	sig = 8.collect{
		arg num;
		var direction, center, deviation, voiceFreq, voiceNum, iphase, lfo;

		// Normalize voice number to range 0..1
		voiceNum = num.linlin(0, 7, 0, 1);

		direction = IRand(0, 1) * 2 - 1;

		center = detuneStep * voiceNum * direction;

		deviation = LFNoise1.kr(1) * detuneStep * 0.1;

		voiceFreq = freq * (center + deviation).midiratio;

		iphase = voiceNum + Rand(-0.01, 0.01);

		lfo = 0.5 + SinOsc.kr(lfoRate * env).bipolar(lfoDepth * 0.1 * env)
		+ PinkNoise.kr.bipolar(lfoDepth * detuneStep * 0.005);

		VarSaw.ar(voiceFreq, iphase.fold(0,1), lfo.clip(0,1)); //, lfo.clip(0,1));
	};

	// Splay the oscillator bank onto a stereo field
	sig = Splay.ar(sig, center: pan);

	// Add a sine oscillator one octave below the main bank
	sig = sig + (SinOsc.ar(freq * 0.5) * subAmp.clip(0.0, 1.0));

	// Add a bit of distortion/glue
	sig = (sig * drive.linexp(0, 1, 1, 10)).tanh;

	// Filter
	cutoff = freq * 2.pow(cutoffOctave);

	//sig = LPF.ar(sig,	cutoff.clip(20, 20000));
	//sig = MoogFF.ar(sig, cutoff.clip(20, 20000), 1);
	sig = BLowPass4.ar(sig, cutoff.clip(20, 20000));

	// Apply the volume envelope
	sig = sig * env;

	sig = Balance2.ar(sig[0], sig[1], pan, amp);

	Out.ar(outBus, sig);
}, (1!10)).add;
