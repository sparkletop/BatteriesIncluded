// Enveloped mono and stereo sample playback

SynthDef(\mSample, {
	arg amp = 0.1, out = 0, pan = 0,
	drive = 0, cutoff = 20000,
	playbackRate = 1, startPosition = 0,
	buffer = BI.prBufInfo(\mSamples).lo,
	loop = 0, t_reset = 1,
	atk = 0.005, sus = 1, rel = 0.2, gate = 1;

	var sig, env;
	env = Env.asr(atk, sus, rel).kr(2, gate);

	sig = PlayBuf.ar(
		numChannels: 1,
		bufnum: buffer,
		rate: playbackRate * BufRateScale.kr(buffer),
		trigger: t_reset,
		startPos: startPosition.linlin(0, 1, 0, BufFrames.kr(buffer) - 2),
		loop: loop
	);

	sig = (sig * drive.linexp(0, 1, 1, 100)).tanh;

	sig = LPF.ar(
		sig,
		cutoff.clip(20, 20000)
	);

	sig = sig * env;

	sig = Pan2.ar(sig, pan, amp);

	Out.ar(out, sig);

},
metadata: (
	credit: "Anders Eskildsen",
	tags: [\sampler],
	description: "Enveloped sample player for mono samples",
	specs: (
		drive: \unipolar.asSpec,
		startPosition: \unipolar.asSpec,
		playbackRate: ControlSpec(
			minval: -50,
			maxval: 50,
			default: 1
		),
		loop: ControlSpec(
			minval: 0,
			maxval: 1,
			step: 1,
			default: 0,
			units: "on/off"
		),
		buffer: ControlSpec(
			minval: BI.prBufInfo(\mSamples).lo,
			maxval: BI.prBufInfo(\mSamples).hi,
			step: 1,
			default: BI.prBufInfo(\mSamples).lo
		)
	)
)).add;

SynthDef(\sSample, {
	arg amp = 0.1, out = 0, pan = 0,
	drive = 0, cutoff = 20000,
	playbackRate = 1, startPosition = 0,
	buffer = BI.prBufInfo(\sSamples).lo,
	loop = 0, t_reset = 1,
	atk = 0.005, sus = 1, rel = 0.2, gate = 1;

	var sig, env;
	env = Env.asr(atk, sus, rel).kr(2, gate);

	sig = PlayBuf.ar(
		numChannels: 2,
		bufnum: buffer,
		rate: playbackRate * BufRateScale.kr(buffer),
		trigger: t_reset,
		startPos: startPosition.linlin(0, 1, 0, BufFrames.kr(buffer) - 2),
		loop: loop
	);

	sig = (sig * drive.linexp(0, 1, 1, 100)).tanh;

	sig = LPF.ar(
		sig,
		cutoff.clip(20, 20000)
	);

	sig = sig * env;

	sig = Balance2.ar(sig[0], sig[1], pan, amp);

	Out.ar(out, sig);

},
metadata: (
	credit: "Anders Eskildsen",
	tags: [\sampler],
	description: "Enveloped sample player for stereo samples",
	specs: (
		drive: \unipolar.asSpec,
		startPosition: \unipolar.asSpec,
		playbackRate: ControlSpec(
			minval: -50,
			maxval: 50,
			default: 1
		),
		loop: ControlSpec(
			minval: 0,
			maxval: 1,
			step: 1,
			default: 0,
			units: "on/off"
		),
		buffer: ControlSpec(
			minval: BI.prBufInfo(\sSamples).lo,
			maxval: BI.prBufInfo(\sSamples).hi,
			step: 1,
			default: BI.prBufInfo(\sSamples).lo
		)
	)
)).add;
