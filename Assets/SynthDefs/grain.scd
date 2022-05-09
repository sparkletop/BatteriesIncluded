// Produce a single grain (language side triggering of grains)
SynthDef(\grain, {
	arg pan = 0, amp = 0.1, out = 0,
	grainDur = 0.01, buffer, transpose = 0, startPosition = 0;
	var sig, env;

	env = Env.sine(grainDur).ar(2);

	sig = PlayBuf.ar(
		1,
		buffer,
		BufRateScale.ir(buffer) * transpose.midiratio,
		1,
		startPosition * BufFrames.ir(buffer),
		1
	);

	sig = sig * env;

	sig = Pan2.ar(sig, pan, amp);

	Out.ar(out, sig);
}, metadata: (
		specs: (
			detuneCurve: ControlSpec(-20, 20, \lin, default: 4),
		),
		credit: "Anders Eskildsen",
		category: \percussion,
		tags: [\granular],
		description: "A single grain, useful for language-side control of granular synthesis"
	)).add;
