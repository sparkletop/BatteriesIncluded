// Various percussion SynthDefs
// Nothing particularly fancy, just some basic kicks and hats to get started :)

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

