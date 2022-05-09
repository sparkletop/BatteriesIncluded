// Produce a cloud of grains (server side triggering of grains)
[
	(
		defName: \cloud, // unpitched
		grainParams: {
			arg overlap, grainDur = 0.025, loop = 1;
			var triggerFreq = overlap * grainDur.reciprocal;
			[triggerFreq, grainDur, loop];
		}
	),
	(
		defName: \pitchCloud, // pitched
		grainParams: {
			arg overlap, freq = 440;
			var grainDur = overlap * freq.reciprocal;
			[freq, grainDur, 1];
		}
	)
].do{
	arg variant;
	SynthDef(variant[\defName], {
		arg pan = 0, amp = 0.1, overlap = 5,
		pointerRate = 1, scatter = 0, probability = 1,
		transpose = 0, detuneStep = 0.01, stereoSpread = 0.05,
		buffer = BI.monoSamples.guit_e_fifths,
		bufferRange = 0, pointerStartPos = 0,
		triggerType = 0, t_resetPointer = 1,
		out = 0,	atk = 0.01, sus = 1, rel = 0.1, gate = 1;

		var sig, env, triggerFreq, trigger, params, atEnd,
		pointerPosition, panPosition, buf, grainDur, loop,
		ampCompensation, numFrames, spread, playbackRate;


		// Grain parameters
		params = SynthDef.wrap(
			variant[\grainParams],
			[0.2],
			[overlap]
		);
		triggerFreq = params[0];
		grainDur = params[1];
		loop = params[2];

		// Grain trigger
		trigger = Select.ar(
			triggerType.clip(0, 1),
			[	Impulse.ar(triggerFreq), Dust.ar(triggerFreq)	]
		);

		// Trigger skipping
		trigger = CoinGate.ar(probability.clip(0, 1), trigger);

		// Buffer selection
		buf = TIRand.ar(buffer, buffer + bufferRange, trigger);

		// Pointer position
		numFrames = BufFrames.kr(buf);
		pointerPosition = (
			Phasor.ar(
				trig: t_resetPointer,
				rate: pointerRate * BufRateScale.kr(buf),
				start: 0,
				end: numFrames,
				resetPos: pointerStartPos.clip(0, 1) * numFrames
			)
		);

		// Determine whether we are at the end of the buffer
		atEnd = Select.kr(loop, [
			InRange.kr(
				pointerPosition,
				numFrames - (
					rel * pointerRate * SampleRate.ir
					* BufRateScale.kr(buf)
				),
				numFrames
			) * 1.neg + 1,
			1
		]);

		// Main cloud envelope
		env = Env.asr(atk, sus, rel).kr(2, gate * atEnd);

		// Add randomness to pointer position
		pointerPosition = (
			pointerPosition
			+ TRand.ar(0,
				scatter * SampleRate.ir * BufRateScale.kr(buf),
				trigger
			)
		) * numFrames.reciprocal;

		// Stereo position
		spread = stereoSpread.clip(0, 1);
		panPosition = TRand.ar(
			pan - spread,
			pan + spread,
			trigger)
		.fold(-1, 1);

		// Playback rate
		playbackRate = BufRateScale.kr(buf)
		* transpose.midiratio
		* TRand.ar(detuneStep.neg, detuneStep).midiratio;

		// Grain generation
		sig = GrainBuf.ar(
			2,
			trigger,
			grainDur,
			buf,
			playbackRate,
			pointerPosition,
			2,
			panPosition
		);

		// Amplitude compensation for overlapping grains
		ampCompensation =	overlap.reciprocal.linlin(0, 1, 0.25, 1)
		+ grainDur.lincurve(0.01, 0.1, 5, 0, -6);

		sig = sig * env * amp * ampCompensation;

		sig = Limiter.ar(sig);

		Out.ar(out, sig);
	}, (0.2!9),
	metadata: (
		credit: "Anders Eskildsen",
		tags: [\granular],
		description: "Granular synthesis for atmospheres and textures, grains are triggered server side",
		specs: (
			buffer: ControlSpec(
				minval: BI.prBufInfo(\mSamples).lo,
				maxval: BI.prBufInfo(\mSamples).hi,
				step: 1,
				default: BI.monoSamples.guit_e_fifths
			)
		)
	)
	).add;
};
