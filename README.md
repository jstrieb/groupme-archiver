# GroupMe Archiver

This project aims to offer a simple interface for downloading the messages and media from GroupMe chats for archival and offline analysis.


## Screenshot(s)

![Main Screenshot](https://github.com/jstrieb/GroupMe-Archiver/blob/master/doc/screenshots/main.png?raw=true)


## Quick Start

1. Download the appropriate version for your computer below
2. Run the GroupMe Archiver
3. Use the `Help > Get GroupMe acecss token` menu button
4. Log into GroupMe at the link that will open in the browser
5. Copy the API key found after the `=` in the URL it redirects to -- ignore that it says `Not Found`
6. Paste the API key into the appropriate text box in the program
7. Click a group you want to archive and configure how to download it
8. Press `Begin Archiving` in the bottom right corner


## Download

- Download the latest `jar` version [here](https://github.com/jstrieb/GroupMe-Archiver/releases/download/v1.0/GroupMeArchiver-v1.0-jar.zip). This version should work on most operating systems. (7MB)
- Alternatively, download a package for Windows with all dependencies preinstalled [here](https://github.com/jstrieb/GroupMe-Archiver/releases/download/v1.0/GroupMeArchiver-v1.0-windows.zip). This version is considerably larger, but will probably work better on Windows computers. (70MB)
- For more release information and old versions, see the [releases page](https://github.com/jstrieb/GroupMe-Archiver/releases)

This code has not been tested beyond use on a Windows 10 computer with Java 8; your mileage may vary.


## Compiling

GroupMe Archiver depends on `JavaFX` and the `Jackson` JSON parsing library, and it is built with `ant`. I originally created the project in NetBeans, and it is probably easiest to clone the repository with `git clone`. Then, import the project into NetBeans and run it.


## TODO

- Support plaintext and CSV message export
- Allow for only downloading media and messages within a date range
- Support listing more than 499 groups
