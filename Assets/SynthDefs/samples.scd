// Oneshot sample playback (beginning to end)
// Enveloped mono and stereo sample playback
[
	(
		defName: \mSample,
		numChannels: 1,
		panFunction: {
			arg sig, pan = 0;
			Pan2.ar(sig, pan);
		}
	),
	(
		defName: \sSample,
		numChannels: 2,
		panFunction: {
			arg sig, pan = 0;
			Balance2.ar(sig[0], sig[1], pan);
		}
	)
].do{
	arg channelsInfo;
	SynthDef(channelsInfo[\defName], {
		arg amp = 0.1, outBus = 0,
		drive = 0, cutoff = 20000,
		playbackRate = 1, startPosition = 0,
		buffer, loop = 0, t_reset = 1,
		atk = 0.005, sus = 1, rel = 0.2, gate = 1;

		var sig, env;
		env = Env.asr(atk, sus, rel).kr(2, gate);

		sig = PlayBuf.ar(
			numChannels: channelsInfo[\numChannels],
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

		sig = SynthDef.wrap(
			channelsInfo[\panFunction],
			prependArgs: [sig]
		);

		sig = sig * amp;

		Out.ar(outBus, sig);
	},
	metadata: (
		specs: (
			drive: \unipolar.asSpec,
			startPosition: \unipolar.asSpec,
			buffer: ControlSpec(
				minval: BI.bufferInfo.samples[\lo],
				maxval: BI.bufferInfo.samples[\hi],
				step: 1,
				default: BI.bufferInfo.samples[\lo]
			),
			playbackRate: ControlSpec(
				minval: -50,
				maxval: 50,
				default: 1
			),
			loop: ControlSpec(
				minval: 0,
				maxval: 1,
				default: 0
			)
		)
	)
	).add;
};
