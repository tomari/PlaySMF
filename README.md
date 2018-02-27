# PlaySMF (Plays SMF files after proper reset sequence)

[![CircleCI](https://circleci.com/gh/tomari/PlaySMF.svg?style=svg)](https://circleci.com/gh/tomari/PlaySMF)

PlaySMF is a Standard MIDI File (*.mid) player that appropriately resets the
synthesizer before playing the file. This program is written in Java.

## Introduction

Even though there are several programs that can play MIDI files using external
sound modules, few resets them appropriately before playing a MIDI file.
Unfortunately not all MIDI files have reset sequences embedded, so they sound
different depending on whatever the state that the sound module was left with.
Moreover there are several pre-GM synthesizers that require special reset
sequences. To provide consistent sound, this program appropriately resets your
synthesizer before playing a MIDI file.

Additionally this program supports the OpenJDK's software synthesizer by
aceepting the option to load the SoundFont file (*.sf2). With appropriate
soundfont the software synth sounds somewhat acceptable.

## Supported Resetting Methods
  
* `gm` General MIDI reset sequence.
* `gs` Roland GS sequence. Also see sc88.
* `xg` Yamaha XG sequence. Sets MU100 to MU100 Basic mode. Also see mu100.
* `doc` Yamaha Disk Orchestra/Clavinova.
* `sc88` SC-88/SC-88Pro Mode1 reset. This is the preferred reset sequence for these models, over gs.
* `mu100` Resets Yamaha MU100 to MU100 Native mode.
* `mt32` Roland MT-32/CM-32. Writes a byte to the Reset All register.
* `fb01` Yamaha FB-01 FM synthesizer. Loads ROM config 17 as a reset. After all this synth is unlikely to work well with GM sequences.

## Examples

As this is a Java application you'll probably want to have a small shell script
like this:

```
#!/bin/sh
java -jar /path/to/PlaySMF.jar "$@"
```

Save it as PlaySMF.
If you want to see the list of available devices use -l:

```
% PlaySMF -l
Dev 0 OpenJDK Gervill 1.0
    Software MIDI Synthesizer
    Source: 0 Sink: Unlimited
Dev 2 ALSA (http://www.alsa-project.org) UX16 [hw:2,0,0] 3.7.10-1.16-desktop
    UX16, USB MIDI, UX16
    Source: 0 Sink: Unlimited
Dev 3 Oracle Corporation Real Time Sequencer Version 1.0
    Software sequencer
    Source: Unlimited Sink: Unlimited
```

In this case device 0 is a software synth bundled with the Java runtime.
[Yamaha_UX16] Device 2 is the YAMAHA UX16 USB-MIDI adapter. Device 3 is a
pseudo sequencer device. To inhibit filtering devices that do not act as Sinks,
specify -la instead of -l.
To play MIDI files using the synthesizer connected to the UX16 adapter:

```
% PlaySMF -p 2 -r sc88 song1.mid song2.mid
```

Here sc88 is specified as the reset sequence. See Supported_Resetting_Methods
for what can be specified here. If you omit the -r option no reset sequence is
sent.

When the software synthesizer is to be used, you can (and should) specify a 
soundfont using the -s option:

```
% PlaySMF -s /usr/share/sounds/sf2/8MBGMSFX.SF2 song1.mid ...
```

All options are:

```
% PlaySMF [-p dev] [-r gm|gs|sc88|xg|mu100|doc|mt32|fb01] [-s soundfont.sf2] [-l|-la] 1.mid 2.mid
3.mid ...
```

Nov-06-2013 H.Tomari
Jul-08-2017 H.Tomari markdownify
