SynthDefGui {
	classvar <all;
	classvar bufnames = #[\buf, \buffer, \bufnum, \bufNum];

	var defName, args, server,
	plotBuffers, values, synthDesc, topMenu, player,
	group, synth, specs, keys, <window, grid,
	gridContainer, sustainTime = 1,
	snippetWindow, maxBufDisplay = 50, canvas,
	looping = true, isPlaying = false, playBtn, hackBtn,
	timeSpec, startArgs, freeing = false;

	*initClass {
		all = IdentityDictionary.new;
	}

	*new {
		arg defName = \default, args,
		server = Server.default,
		plotBuffers = true;

		all[defName].free;

		^super.newCopyArgs(
			defName, args, server,
			plotBuffers
		).init;
	}

	init {
		var btnFont = Font("Helvetica", 10);

		synthDesc = SynthDescLib.global.synthDescs[defName];

		if ( synthDesc.isNil, {
			(
				"SynthDef" + defName + "could not be found. Did you .add it first?"
			).error;
			^nil;
		});

		startArgs = args !? ( _.asDict ) ?? { Dictionary.new };

		startArgs.select(_.isKindOf(Buffer))
		.keysValuesDo{ | key, buffer |
			startArgs[key] = buffer.bufnum;
		};

		// Warnings for common errors
		if (server.serverRunning.not,
			{
				"Audio server is not running".warn;
			}
		);

		if (synthDesc.canFreeSynth.not,
			{
				(
					"Synths based on SynthDef"
					+ defName
					+ "cannot free themselves (see the Done help file)"
				).warn;
			}
		);

		// Dictionary to hold all current values
		values = Dictionary(synthDesc.controls.size);

		synthDesc.controls.do{ |control|
			values.put(control.name,
				startArgs[control.name] ? control.defaultValue
			);
		};

		// Get spec list from SynthDesc metadata
		specs = synthDesc.specs.deepCopy.asDict;

		// Guestimate remaining specs based on ControlName and default value
		// If you need to control a gate with the GUI, use a different argument name than \gate
		synthDesc.controls.reject{ |control|
			specs.includesKey(control.name)	|| [\gate, \t_gate].includes(control.name)
		}.do{ |control|
			var spec, default;
			default = control.defaultValue;

			if (bufnames.includes(control.name),
				{
					// Buffer specification
					var tempBuf, numBufs;
					tempBuf = Buffer.alloc(server, 1);
					numBufs = tempBuf.bufnum; tempBuf.free;
					if (numBufs == 0, {
						(
							"SynthDef" + defName
							+ "has a \\buffer or similar argument, but"
							+ "no Buffers are currently allocated on"
							+ server.asString
						).warn;
						spec = ControlSpec(
							0, 10, step: 1,
							default: 0, units: "!"
						);
					}, {
						spec = ControlSpec(
							0, numBufs-1,
							step: 1, default: 0
						);
					});
				},
				{
					spec = BI.specs[control.name] ? control.name.asSpec ?? {
						var min, max;
						if (default == 0,
							{ min = 0.0; max = 1.0; },
							{ min = default/2; max = default*2; }
						);
						(
							"Could not find a suitable Spec for the \\"
							++ control.name
							++ " argument in the \\"
							++ defName
							++ " SynthDef.\n"
							++ "Using a ControlSpec with range "
							++ min ++ "-" ++ max ++ "."
						).postln;

						ControlSpec(
							min,
							max,
							default: default,
							units: "-"
						);
					};
				}
			);
			specs.put(control.name, spec);
		};

		grid = GridLayout();

		keys = specs.keys.asArray.sort{ | keyA, keyB |
			(BI.specOrder.indexOf(keyA) ? -1) < (BI.specOrder.indexOf(keyB) ? -1);
		};

		keys.do{ | name, rowIndex |
			var slider, valueBox, unitString, spec, default, setter;

			// name.asString.find("freq", true);

			spec = specs[name];

			default = values[name];

			// label
			grid.add(
				StaticText()
				.string_("\\" ++ name)
				.font_(Font("Helvetica", 12)),
				rowIndex,
				0,
				\right
			);

			// value display
			valueBox = NumberBox()
			.value_(values[name])
			.action_({ | box |
				slider.valueAction_(spec.unmap(box.value))
			})
			.step_(0.001)
			.maxDecimals_(3)
			.minDecimals_(0)
			.align_(\center)
			.font_(Font("Helvetica", 12));

			setter = { | val |
				slider.valueAction_(
					spec.unmap(
						spec.constrain(val)
					)
				)
			};

			// slider
			slider = Slider()
			.orientation_(\horizontal)
			.action_({ | slider |
				values[name] = spec.map(slider.value);
				valueBox.value_(values[name]);
				group.set(name, values[name]);
			})
			.valueAction_(spec.unmap(values[name]));

			grid.add(
				slider,
				rowIndex,
				1
			);

			grid.add(
				valueBox,
				rowIndex,
				2
			);

			// units label
			unitString = spec.units;
			(unitString.size == 0).if({unitString = "-"});
			grid.add(StaticText()
				.string_(unitString)
				.font_(Font("Helvetica", 9)),
				rowIndex,	3, \left
			);

			// reset/buffer selection button
			if (
				bufnames.includes(name),
				{
					grid.add(
						Button()
						.states_([[
							"Select...",
							Color.black,
							Color.green
						]])
						.action_({
							this.prSelectBuffer(name, setter);
						}),
						rowIndex, 4
					)
				},
				{
					grid.add(
						Button()
						.string_("Reset")
						.action_({
							slider.valueAction_(
								spec.unmap(default)
							)
						}),
						rowIndex, 4
					);

				}
			);
		};

		// playback
		group = Group(server);

		player = Routine{
			looping.not.if(
				{
					this.prNewSynth(true);
				},
				{
					loop {
						this.prNewSynth(false);
						sustainTime.wait;
						synth.isPlaying.if({synth.set(\gate, 0)});
						sustainTime.wait;
					}
			})
		};

		playBtn = Button()
		.states_([
			["Play", Color.black, Color.green],
			["Stop", Color.white, Color.red]
		])
		.font_(btnFont)
		.action_({ | button |
			switch (button.value,
				0, {
					this.stop;
				},
				1, {
					this.play;
				}
			);
		});

		timeSpec = ControlSpec(0.1, 4, 2, 0.01, 1, "seconds");

		hackBtn = Button()
		.string_("Hack this SynthDef")
		.font_(btnFont)
		.action_({ BI.hackSynthDef(defName); });

		if (BI.synthDefs[defName].isNil, {hackBtn.enabled_(false)});

		topMenu = HLayout(
			[HLayout(
				[CheckBox()
					.string_("Loop")
					.value_(looping)
					.font_(btnFont)
					.action_({ | checked |
						looping = checked.value;
						isPlaying.if({ this.restart; });
				}), 'stretch', 1],
				[Slider()
					.orientation_(\horizontal)
					.value_(timeSpec.unmap(sustainTime))
					.action_({ | slider |
						sustainTime = timeSpec.map(slider.value);
				}), 'stretch', 2]
			), 'stretch', 2],

			[playBtn, 'stretch', 3],

			[hackBtn, 'stretch', 2],

			[Button()
				.string_("Open as Synth")
				.font_(btnFont)
				.action_({
					this.prShowSettingsWindow(
						this.prAsSynthString
					);
			}), 'stretch', 2],

			[Button()
				.string_("Open as Pbind")
				.font_(btnFont)
				.action_({
					this.prShowSettingsWindow(
						this.prAsPbindString
					);
			}), 'stretch', 2]
		);

		4.do{| i | grid.setColumnStretch(i, 1)};
		grid.setColumnStretch(1, 20);
		grid.setMinColumnWidth(1, 150);
		grid.setMinColumnWidth(2, 45);

		// Place grid in a scrollable view
		gridContainer = ScrollView()
		.hasHorizontalScroller_(false)
		.canvas_(
			View().layout_(grid)
		);

		window = Window(
			"SynthDefGui: \\" ++ defName,
			Rect(
				Window.availableBounds.width * 0.6,
				0,
				(Window.availableBounds.width * 0.4).clip(470, 700),
				specs.size * 30 + 90
			)
			.center_(Window.availableBounds.center)
			.left_(Window.availableBounds.width * 0.5)
		)
		.layout_(
			VLayout(
				HLayout(
					StaticText()
					.string_("SynthDef:")
					.font_(Font("Helvetica", 15))
					.align_(\left),
					StaticText()
					.string_("\\" ++ defName)
					.font_(Font("Helvetica", 15, true))
					.align_(\left),
				).setStretch(1, 10),
				topMenu,
				gridContainer
			)
		)
		.onClose_({ freeing.not.if { this.free } })
		.alwaysOnTop_(true);

		window.front;

		all[defName] = this;

		^this;
	}

	prNewSynth {
		arg stopRoutineOnFree = false;
		var bundle = server.makeBundle(false, {
			synth = Synth(defName, values.asPairs, group)
			.onFree{
				if(stopRoutineOnFree, {
					defer { playBtn.valueAction_(0) }
				});
			};
			NodeWatcher.register(synth);
		});
		server.listSendBundle(nil, bundle);
	}

	play {
		player.reset.play;
		isPlaying = true;
	}

	restart {
		defer {
			fork {
				this.stop;
				0.05.wait;
				this.play;
			}
		}
	}

	stop {
		player.stop;
		synth.isPlaying.if {
			group.set(\gate, 0);
		};
		synthDesc.canFreeSynth.not.if{
			group.freeAllMsg;
		};
		isPlaying = false;
	}

	prValuesAsSortedKeyValuePairs {
		^keys.collect{ | key |
			"\t\\" ++ key ++ ", "	++ values[key].round(0.001) ++ ",\n"
		}.join("");
	}

	prAsPbindString {
		var string = "(\nPbind(\n\t\\instrument, \\" ++ defName ++ ",\n";
		string = string ++ this.prValuesAsSortedKeyValuePairs;
		string = string ++ ").play;\n)";
		^string;
	}

	prAsSynthString {
		^(
			"(\nSynth(\\" ++ defName ++ ", [\n"
			++ this.prValuesAsSortedKeyValuePairs
			++ "]);\n)"
		);
	}

	prShowSettingsWindow { | text |
		snippetWindow !? {snippetWindow.close};
		snippetWindow = Window.new(\Settings)
		.layout_(VLayout(
			TextView()
			.string_(text)
			.font_(Font(size: 20))
		)).front;
	}

	prSelectBuffer { | key, setter |
		var bufSelectorWindow, data, plot, scroll, canvas,
		bufList = GridLayout(),
		bufNums = (specs[key].minval .. specs[key].maxval);

		if (bufNums.size > maxBufDisplay, {
			"Restricting number of displayed buffers.".warn;
			bufNums = bufNums[0..maxBufDisplay];
		});

		bufNums.do{ | bufnum, rowIndex |
			var plot, graph,
			buf = Buffer.cachedBufferAt(server, bufnum),
			fileName = buf.path !? { |p| PathName(p).fileName } ?? "-";

			bufList.add(
				VLayout(
					StaticText()
					.string_("Buffer number:" + buf.bufnum)
					.font_(Font("Helvetica", 16, true))
					.align_(\left),
					StaticText()
					.string_(fileName)
					.font_(Font("Helvetica", 12))
					.align_(\left),
					StaticText()
					.string_(
						[
							"Samplerate:" + buf.sampleRate,
							"Channels:" + buf.numChannels,
							"Sample frames:" + buf.numFrames,
							"Duration:" + buf.duration.round(0.001) + "seconds"
						].join("\n");
					)
					.font_(Font("Helvetica", 10))
					.align_(\left)
				),
				rowIndex,
				0
			);

			if (plotBuffers, {
				graph = UserView(bounds: Rect(0, 0, 200, 120));
				plot = Plotter(parent: graph);

				plot.plotMode = \bars;

				buf.loadToFloatArray(
					action: { |data|
						defer {
							plot.setValue(
								[data.resamp1(500).postln],
								separately: true,
								minval: -1,
								maxval: 1
							);
							plot.setProperties(
								\plotColor, [Color.blue],
								\backgroundColor, Color(1, 1, 1, 0.5),
								\gridOnX, false,
								\gridOnY, false,
								\labelX, "",
								\labelY, ""
							);
						};
				});

				bufList.add(graph, rowIndex, 1);
			});

			bufList.add(
				VLayout(
					Button()
					.string_("Choose")
					.action_({
						setter.value(buf.bufnum);
						scroll.close;
					}),
					Button()
					.string_("Play")
					.action_({
						buf.play
					})
				), rowIndex, 2
			);

			bufList.setColumnStretch(1, 5);
			bufList.setMinColumnWidth(1, 200);
			bufList.setMinColumnWidth(0, 150);
			bufList.vSpacing_(10);
		};

		scroll = ScrollView(
			bounds:	Rect(50, 50, 500, 500)
			.center_(Window.availableBounds.center)
		);

		canvas = View();
		canvas.layout = bufList;
		scroll.canvas = canvas;
		scroll.front;
	}

	free {
		freeing = true;
		this.stop;
		group.free;
		this.window.close;
		all.removeAt(defName);
	}
}


+ SynthDef {

	gui {
		arg args,	server = Server.default,
		plotBuffers = true;

		^SynthDefGui(this.name, args, server, plotBuffers);
	}

}
