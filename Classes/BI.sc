BI {
	classvar <specs, <specOrder, assetFolder, server, <samples, <wavetables;

	*initClass {

		server = Server.default;

		StartUp.add({

			assetFolder = Quarks.all
			.select{ |q| q.name == "BatteriesIncluded"}
			.first.localPath +/+ "Assets";

			ServerBoot.add({
				// SynthDefs
				var synthDefs;
				PathName(assetFolder +/+ "SynthDefs").files
				.select{ |f| f.extension == "scd" }
				.do{ |f,n| f.fullPath.load; synthDefs = n };

				samples = ();
				wavetables = ();

				// Load samples and wavetables into buffers

				PathName(assetFolder +/+ "Samples").files
				.sort{ |a,b| a.fileName < b.fileName}
				.do{ |file|
					this.samples[file.asSafeKey] = Buffer.read(server, file.fullPath)
				};

				PathName(assetFolder +/+ "Wavetables").files
				.sort{ |a,b| a.fileName < b.fileName}
				.do{ |file| this.wavetables[file.asSafeKey] = Buffer.read(server, file.fullPath)
				};

				server.sync;

				// Report a bit of information to user after loading stuff on boot
				fork {
					0.2.wait;
					[
						"BatteriesIncluded has loaded the following resources on the server:",
						"-" + synthDefs + "SynthDefs",
						"-" + samples.size + "Buffers with samples",
						"-" + wavetables.size + "Buffers with wavetables",
						"See the help file for BatteriesIncluded for more information."
					].do(_.postln);
				}
			});
		});


		// Add some common specs (work with the SynthDefs included in BI)
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

	*clearBuffers {
		server.serverRunning.not.if({
			"Server is not running".error;
			^nil;
		});
		samples.notNil.if({samples.values.do(_.free)});
		wavetables.notNil.if({wavetables.values.do(_.free)});

		server.sync;

		samples = ();
		wavetables = ();
		^nil;
	}

	*addSpecsToGlobalLibrary {
		specs.keysValuesDo{
			arg key, spec;
			Spec.add(key, spec);
		};
		(specs.size + "specs from BI have been added to the global Spec dictionary.").postln;
		("See BI.specDirectory for a list of the added specs.").postln;
	}

	*listSpecs {
		specs.keys.do(_.postln);
	}

	*listSamples {
		var r;
		"Samples from the BatteriesIncluded quark loaded on the server:".postln;
		samples.keys.asArray.sort.do{ |key|
			var buf = this.samples[key],
			channels = buf.numChannels
			.switch(1, "mono", 2, "stereo") ?
			(buf.numChannels.asString + "channels");
			(
				"\\" ++ key ++ ":"
				+ buf.duration.round(0.001).asString ++ "s,"
				+ channels ++ ","
				+ "bufnum" + buf.bufnum
			).postln;
		};
		r = samples.keys.choose;
		("Example: To access the Buffer containing the sample" + r ++ ", use BI.samples[\\" ++ r ++ "] or BI.samples." ++ r).postln;
	}

	*listWavetables {
		"Here will be listed the wavetables that come with BatteriesIncluded :-)".postln;
	}

	*prCheckServer {
		server.serverRunning.not.if({
			"Server is not running".warn;
			^nil;
		});
	}
}

// A small extension of PathName - turns a file path into a "sanitized" symbol, for use as a dictionary key
// Substrings composed of non-alphanumeric characters will be replaced with underscores
+ PathName {
	asSafeKey {
		var key = List.new, prev = $-;
		this.fileNameWithoutExtension.do{ |char|
			if (
				char.isAlphaNum,
				{ key.add(char) },
				{
					if (
						prev.isAlphaNum,
						{ key.add($_) }
					)
				}
			);
			prev = char;
		};
		^key.join("").asSymbol;
	}
}
