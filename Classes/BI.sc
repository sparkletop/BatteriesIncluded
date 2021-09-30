BI {
	classvar <>assetFolder, <samples, <wavetables, <server, handlers, <synthDefs, <specs, <specOrder, <bufferInfo, pluginUGens;

	*initClass {
		server = server ? Server.default;

		Class.initClassTree(Quarks);

		assetFolder = Quarks.all
		.select{ |q| q.name == "BatteriesIncluded"}
		.first.localPath +/+ "Assets";

		pluginUGens = Set[\EnvFollow, \FM7];

		ServerBoot.add({
			var oldDefs, sdFiles, pluginsNotFound;

			// Load samples and wavetables into buffers
			samples = ();
			wavetables = ();
			bufferInfo = ();

			[
				(name: "Samples", dict: this.samples),
				(name: "Wavetables", dict: this.wavetables)
			].do{ |d|
				var bufnums = List.new;
				PathName(assetFolder +/+ d.name).files
				.sort{ |a, b| a.fileName < b.fileName}
				.do{ |file|
					var key = file.asSafeKey;
					d.dict[key] = Buffer.read(server, file.fullPath);
					bufnums.add(d.dict[key].bufnum);
				};
				bufferInfo[d.name.toLower.asSymbol] = Dictionary.newFrom([
					\lo, bufnums.first,
					\hi, bufnums.last
				]);
			};

			server.sync;

			// Load SynthDefs
			oldDefs = SynthDescLib.global.synthDescs.keys;

			PathName(this.assetFolder +/+ "SynthDefs").files
			.select{ |f| f.extension == "scd" }
			.do{ |f| f.fullPath.load };

			// Check for presence of optional UGens (SC3 Plugins)
			if (pluginUGens.isSubsetOf(
				Class.allClasses.collect(_.name).asSet
			), {
				PathName(this.assetFolder +/+ "SynthDefs-sc3plugins").files
				.select{ |f| f.extension == "scd" }
				.do{ |f| f.fullPath.load };
			}, {
				pluginsNotFound = "Couldn't find sc3-plugins. See the help file for BatteriesIncluded for more information.";
			});

			synthDefs = SynthDescLib.global.synthDescs
			.keys.difference(oldDefs);
			server.sync;

			// Report some info after server boot
			fork {
				0.1.wait;
				[
					"BatteriesIncluded has loaded the following resources on the server:",
					"-" + synthDefs.size + "SynthDefs",
					"-" + samples.size + "Buffers with samples",
					"-" + wavetables.size + "Buffers with wavetables",
					"See the BI help file for more information."
				].do(_.postln);
				pluginsNotFound !? (_.warn);
			}
		});

		StartUp.add({
			// Add some common specs (these all work with the SynthDefs included in BI)
			// TODO: Move Specs that are only relevant to one SynthDef to that def's metadata
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
				\decibel, \subDb, \volume, \vol, \subAmp,

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

	/*
	*clock_ { | newClock |
	clock = newClock ? TempoClock.default;
	this.initClass;
	}

	*server_ { | newServer |
	ServerBoot.removeAll(server);
	ServerTree.removeAll(server);
	server = newServer;
	this.initClass;
	}
	*/

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
	}

	*listSpecs {
		specs.keys.do(_.postln);
	}

	*listSamples {
		this.prListBuffers("samples", this.samples);
	}

	*listWavetables {
		this.prListBuffers("wavetables", this.wavetables);
	}

	*listSynthDefs {
		synthDefs.do{ |sd|
			sd.postln;
		}
	}

	*prCheckServer {
		server.serverRunning.not.if({
			"Server is not running".warn;
			"Try running s.boot to get going.".postln;
			^false;
		});
		^true;
	}

	*prListBuffers { |name, dict|
		var random;

		this.prCheckServer.not.if({^nil});

		"These % from the BatteriesIncluded quark are currently loaded into Buffers on the server:".postf(name); "".postln;
		dict.keys.asArray.sort.do{ |key|
			var buf = dict[key],
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

		random = dict.keys.choose;
		(
			"Example: To access the Buffer containing the"
			+ random ++ ", use BI."++name++"[\\"
			++ random ++ "] or BI."++name++"."
			++ random
		).postln;
	}
}

// A small extension of PathName
// Turns a file path into a "sanitized" symbol, for use as a dictionary key
// Non-alphanumeric characters will be replaced with underscores
// Alphabetic characteres will be turned into lower case
+ PathName {
	asSafeKey {
		var key = List.new, prev = $-;
		this.fileNameWithoutExtension.do{ |char|
			if (
				char.isAlphaNum,
				{ key.add(char.toLower) },
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
