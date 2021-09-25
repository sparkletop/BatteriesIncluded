// Various percussion SynthDefs
// Nothing particularly fancy, just some basics to get started :)

SynthDef(\kick, {
	arg pan = 0, amp = 0.1, outBus = 0,
	rel = 0.2, cutoff = 2000;
	var sig = [
		// Click
		BPF.ar(WhiteNoise.ar, 300, 0.5)
		* Env.perc(0.001, 0.02).ar,
		// Body
		SinOsc.ar(XLine.ar(350, 60, 0.045))
		* Env.perc(0.0001, rel, curve: \lin).delay(0.005).ar(2)
	].sum.tanh;

	sig = LPF.ar(sig, cutoff);

	sig = Pan2.ar(sig, pan, amp);
	Out.ar(outBus, sig);
}, metadata: (
	credit: "Anders Eskildsen",
	category: \drums,
	tags: [\percussion, \kick],
	comments: "Inspired by Nathan Ho, see http://sccode.org/1-57g"
)).add;

SynthDef(\snare,{
	arg pan = 0, amp = 0.1, outBus = 0,
	body = -3, harmonics = 0, click = 0, highNoise = -6, lowNoise = -5;
	var sig = [
		// main body
		SinOsc.ar([180, 330]).sum
		* Env.perc(0.03, 0.2).ar
		* body.dbamp,

		// harmonics
		LFTri.ar([286, 335] * Env.new([1, 1.5, 1], [0.01, 0.09], \sine).ar).sum
		* Env.perc(0.01, 0.1).ar * 2.dbamp
		* harmonics.dbamp,

		// initial click
		LPF.ar(HPF.ar(WhiteNoise.ar, 300), 8000)
		* Env.linen(0.001, 0.01, 0.001).ar
		* click.dbamp,

		// high noise (snare coils)
		HPF.ar(BPeakEQ.ar(WhiteNoise.ar, 4000, 0.5, 3), 300)
		* Env.perc(0.05, 0.2).delay(0.01).ar(2)
		* highNoise.dbamp,

		// low noise
		LPF.ar(HPF.ar(WhiteNoise.ar, 230), 500)
		* Env.perc(0.1,0.1).ar
		* lowNoise.dbamp
	].sum;

	// distortion/compression
	sig = (sig * 1.5).tanh;

	Out.ar(outBus, Pan2.ar(sig, pan, amp));
}, metadata: (
	credit: "Anders Eskildsen",
	category: \drums,
	tags: [\percussion, \snare],
	comments: "Inspired by Nathan Ho (http://sccode.org/1-57g) and Gordon Reid (https://www.soundonsound.com/techniques/practical-snare-drum-synthesis).",
	specs: (
		body: \db.asSpec,
		harmonics: \db.asSpec,
		click: \db.asSpec,
		highNoise: \db.asSpec,
		lowNoise: \db.asSpec;
	)
)).add;

SynthDef(\hihat, {
	arg pan = 0, amp = 0.1, outBus = 0,
	rel = 0.1, cutoff = 4000;
	var env, sig;

	env = Env.perc(0.01, rel * Rand(0.9, 1.0)).kr(2);

	cutoff = cutoff.clip(20, 20000);

	sig = WhiteNoise.ar + Pulse.ar(cutoff * 2 * env, 0.1).bipolar(amp * 0.2);

	sig = RHPF.ar(sig, cutoff, 0.4);

	sig = BPF.ar(sig, cutoff * [1, 1.5, 2.2, 2.78], 0.25).sum.tanh;

	sig = sig * 0.2 * env;

	sig = Pan2.ar(sig, pan, amp);
	Out.ar(outBus, sig);
}, metadata: (
	credit: "Anders Eskildsen",
	category: \drums,
	tags: [\percussion, \hihat]
)).add;

