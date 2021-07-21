BI {
	classvar <specs, <specOrder;

	*initClass {
		StartUp.add({
			specs = Dictionary.newFrom([
				\atk, ControlSpec(0.001, 5, 4, 0, 0.01, "seconds"),
				\dec, ControlSpec(0.001, 5, 4, 0, 0.2, "seconds"),
				\sus, ControlSpec(0, 1, \lin, 0, 1, "level"),
				\rel, ControlSpec(0.001, 5, 2, 0, 1, "seconds"),
				\freq, ControlSpec(20, 20000, \exp, 0, 440, "Hz"),
				\cutoff, \freq.asSpec,
				\cutoffOctave, ControlSpec(1, 8, 0, 0, 3, "octaves"),
				\octave, ControlSpec(1, 8, 0, 0, 3, "octave"),
				\lfoRate, ControlSpec(0.01, 20, 2, 0, 1, "Hz"),
				\lfoFreq, \lfoRate.asSpec,
				\lfoDepth, ControlSpec(0, 1, 2, 0, 0.1),
				\lfoRand, ControlSpec(0, 1, 2, 0, 0.2),
				\outBus, \audiobus.asSpec,
				\out, \audiobus.asSpec,
				\inBus, \audiobus.asSpec,
				\in, \audiobus.asSpec,
				\drive, \unipolar.asSpec,
				\db, ControlSpec(-inf, 0.0, \db, 0, -20, "dB"),
				\subDb, ControlSpec(-inf, 0.0, \db, 0, -20, "dB"),
				\amp, ControlSpec(0, 1, 2, 0, 0.1),
				\detuneStep, ControlSpec(0, 1, 4, 0, 0.01, "semitones"),
				\fmIndex, ControlSpec(1, 100, 4, default: 5),
				\grainDur, ControlSpec(0.001, 0.2, \exp, 0, 0.025, "seconds"),
				\overlap, ControlSpec(0.1, 50, \exp, 0.1, 2),
				\probability, \unipolar.asSpec,
				\pointerRate, ControlSpec(0, 50, 8, 0, 1),
				\transpose, ControlSpec(-24, 24, 0, 1, 0, "semitones"),
				\stereoSpread, \unipolar.asSpec,
				\pointerStartPos, \unipolar.asSpec,
				\startPosition, \unipolar.asSpec,
				\scatter, ControlSpec(0, 5, 6, 0, 0, "seconds"),
				\triggerType, ControlSpec(0, 1, 0, 1, 0),
				\playbackRate, ControlSpec(-4, 4, 0, 0, 1),
				\loop, ControlSpec(0, 1, 0, 1, 0)
			]);

			specOrder = [
				// amplitude
				\amp, \amplitude, \db, \dB, \deciBel,
				\decibel, \subDb, \volume, \vol,

				// oscillator
				\freq, \frequency, \midinote, \note, \pitch, \width,

				// buffer control
				\buf, \buffer, \bufnum, \bufNum, \bufRange,
				\bufMin, \bufMax, \wt, \wavetable, \waveTable,

				// envelope
				\atk, \att, \attack, \dec, \decay, \sus, \sustain,
				\rel, \release, \dur, \duration, \time,

				// lfo
				\lfoFreq, \lfoDepth, \lfo, \lfoRand, \lfoFrequency,
				\lfoRate, \lfoAmp, \lfoDb, \lfodb,

				// detuning
				\detune, \detuneStep,

				// filter/effects settings
				\cutoff, \cutoffOctave, \bandwidth, \rq, \filter, \filterFreq, \cutoffFreq,
				\fFreq, \room, \roomsize, \roomSize, \delay, \delayTime, \drive,

				// routing settings
				\in, \inBus, \mix, \spread, \stereoSpread,
				\pan, \out, \outBus,
			];
		});
	}

	*addSpecsToGlobal {
		specs.keysValuesDo{
			arg key, spec;
			Spec.add(key, spec);
		};
		(specs.size + "specs from BI have been added to the global Spec dictionary.").postln;
		("See BI.specDirectory for a list of the added specs.").postln;
	}

	*specDirectory {
		specs.keys.do(_.postln);
	}
}
