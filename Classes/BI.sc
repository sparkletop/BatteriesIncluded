BI {
	classvar <>assetFolder, <server, <synthDefs, <specs, <specOrder, pluginUGens, buffers;

	*initClass {
		server = server ? Server.default;

		Class.initClassTree(Quarks);

		assetFolder = Quarks.all
		.select{ |q| q.name == "BatteriesIncluded" }
		.first.localPath +/+ "Assets";

		pluginUGens = Set[\EnvFollow, \FM7];

		ServerBoot.add({
			var sdFiles, pluginsNotFound, loadSynthDefFolder;

			buffers = (
				\mSamples: (
					name: "mono samples",
					folder: "monoSamples"
				),
				\sSamples: (
					name: "stereo samples",
					folder: "stereoSamples"
				),
				\wavetables: (
					name: "wavetables",
					folder: "wavetables"
				)
			);

			server.bind{


				// Load samples and wavetables into buffers
				buffers.keysValuesDo{ |key, collection|
					var bufnums = List.new;

					collection.bufs = ();

					PathName(assetFolder +/+ collection.folder).files
					.sort{ |a, b| a.fileName < b.fileName }
					.do{ |file|
						var bufKey = file.asSafeKey;

						collection.bufs[bufKey] = Buffer.read(
							server, file.fullPath
						);

						bufnums.add(collection.bufs[bufKey].bufnum);
					};

					collection.minmax = (
						lo: bufnums.first,
						hi: bufnums.last
					);
				};

				server.sync;

				// Load SynthDefs

				synthDefs.notNil.if({
					synthDefs.keys.do{|defName|
						SynthDescLib.global.removeAt(defName)
					};
				});

				synthDefs = ();

				loadSynthDefFolder = { |folderName|
					PathName(this.assetFolder +/+ folderName).files
					.select{ |file| file.extension == "scd" }
					.do{ |file|
						var oldDefs, newDefs;
						oldDefs = SynthDescLib.global.synthDescs.keys;
						file.fullPath.load;
						newDefs = SynthDescLib.global.synthDescs.keys.difference(oldDefs);
						newDefs.do{ |def| synthDefs[def] = file }
					};
				};

				// Load SynthDefs which are compatible with standard lib
				loadSynthDefFolder.("SynthDefs");

				// Check for presence of SC3 Plugins
				if (pluginUGens.isSubsetOf(
					Class.allClasses.collect(_.name).asSet
				), {
					loadSynthDefFolder.("SynthDefs-sc3plugins")
				}, {
					pluginsNotFound = "Couldn't find sc3-plugins. See the help file for BatteriesIncluded for more information.";
				});

				server.sync;

				// Report some info after server boot
				fork {
					0.1.wait;
					[
						"BatteriesIncluded has loaded the following resources on the server:",
						"-" + synthDefs.size + "SynthDefs",
						"-" + buffers.mSamples.bufs.size + "Buffers with mono samples",
						"-" + buffers.sSamples.bufs.size + "Buffers with stereo samples",
						"-" + buffers.wavetables.bufs.size + "Buffers with wavetables",
						"See the BI help file for more information."
					].do(_.postln);
					pluginsNotFound !? (_.warn);
				}
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
				\detuneStep, ControlSpec(0, 0.5, 4, 0, 0.01, "semitones"),
				\startPosition, \unipolar.asSpec,
				\grainDur, ControlSpec(0.001, 0.2, \exp, 0, 0.025, "seconds"),
				\overlap, ControlSpec(0.1, 50, \exp, 0.1, 2),
				\probability, \unipolar.asSpec,
				\pointerRate, ControlSpec(0, 50, 8, 0, 1),
				\transpose, ControlSpec(-24, 24, 0, 1, 0, "semitones"),
				\stereoSpread, \unipolar.asSpec,
				\pointerStartPos, \unipolar.asSpec,
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
				\detune, \detuneCent, \detuneStep,

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

	*prBufInfo { |key|
		^buffers[key].minmax;
	}

	*monoSamples {
		^buffers.mSamples.bufs;
	}

	*stereoSamples {
		^buffers.sSamples.bufs;
	}

	*wavetables {
		^buffers.wavetables.bufs;
	}

	*listStereoSamples {
		this.prCheckServer.not.if({^nil});
		this.prListBuffers(buffers.sSamples);
	}

	*listMonoSamples {
		this.prCheckServer.not.if({^nil});
		this.prListBuffers(buffers.mSamples);
	}

	*listWavetables {
		this.prCheckServer.not.if({^nil});
		this.prListBuffers(buffers.wavetables);
	}

	*prCheckServer {
		server.serverRunning.not.if({
			"Server is not running".warn;
			"Try running s.boot to get going.".postln;
			^false;
		});
		^true;
	}

	*prListBuffers { |collection|
		var keys;
		this.prCheckServer.not.if({^nil});

		"These % from the BatteriesIncluded quark are currently loaded into Buffers on the server:".postf(collection.name); "".postln;

		keys = collection.bufs.keys.asArray.sort;
		keys.do{ |key|
			(
				key ++ ":" +
				collection.bufs[key].duration
				.round(0.001).asString ++ "s"
			).postln;
		};

		"See the BI help file for further information about how to access the Buffers.".postln;
	}

	*listSynthDefs {
		this.prCheckServer.not.if({^nil});
		synthDefs.keys.do{ |sd|
			sd.postln;
		}
	}

	*hackSynthDef { |synthDefName|
		this.prCheckServer.not.if({^nil});
		try {
			^File.readAllString(
				synthDefs[synthDefName].fullPath
			).newTextWindow;
		} { |error|
			("Could not find" + synthDefName + "file.").error;
			"Only SynthDefs that come with BatteriesIncluded are hackable like this. Are you sure the SynthDef you are hacking ".postln;
			^nil;
		};

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
