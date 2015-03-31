# feed-media-download

A command-line utility that downloads audio and video files from a given news feed (e.g. RSS, ATOM.)  The utility contains options to only keep a set number of media files in the download directory - deleting the old ones when new ones are available.  It also has a number of options for updating ID3 tags in MP3 files based on values in the news feed.  It is written in Java utilizing Gradle to build.

Why?
----
Plenty of Podcast utilities and GUI applications exist already, why make another one?  I wanted something very simple that ran from a command line.  I wanted to be able to schedule the utility to run whenever I wanted (e.g. via cron or similar) and specify different directories and options for each feed.  Mostly I didn't want to have to install a large OS-specific application, or an app on my devices... I wanted to simply get the audio files and move them where I wanted without requiring application installs.

I use this utility with [BitTorrent Sync](http://www.getsync.com/) and [Dropbox](https://www.dropbox.com) to get podcast audio and video to the devices I want to use.  With the BitTorrent Sync app on my phone, and a cron job configured with the podcasts I am interested in, I get the mp3's (and potentially m3u playlists) delivered to my phone and tablet whenever I connect to WiFi... which means I never use my data plan, and the most current episodes are always available.  I also occasionally listen to podcasts on other computers or devices, and the media files are available to them as well.

Download
--------
The zip file for the most recent distribution is available [from the latest release page](https://github.com/zpalffy/feed-media-download/releases/latest).

Building
--------
- `gradle tasks` will generate a list of tasks that can be performed.  
- `gradle distzip` will create a zip file for distributing the utility.
- `gradle installdist` will create the distribution directory under `build`.

Usage
-----
If you run the utility with `-h` you will get a list of options that may be supplied to the utility, e.g. `feedmedia -h`.

Example Output
--------------
`feedmedia -e 5 -m -d /Media/Audio/Podcast/Java\ Posse -t http://feeds.feedburner.com/javaposse` will produce output similar to:

```
Downloading to /Media/Audio/Podcast/Java Posse/Java Posse 461.mp3, size: 48 MB
Downloading to /Media/Audio/Podcast/Java Posse/Java Posse 460.mp3, size: 21 MB
Downloading to /Media/Audio/Podcast/Java Posse/Java Posse 459.mp3, size: 18 MB
Downloading to /Media/Audio/Podcast/Java Posse/Java Posse 458.mp3, size: 22 MB
Downloading to /Media/Audio/Podcast/Java Posse/Java Posse 457.mp3, size: 21 MB
Wrote playlist file /Media/Audio/Podcast/Java Posse2/all.m3u
```

Note that the destination directory must already exist, this utility will not create directories that do not exist.

History
-------
* **1.0** Initial commit

