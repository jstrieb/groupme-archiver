# GroupMe Archiver

This project aims to offer a simple interface for downloading the messages and media from GroupMe chats for archival and offline analysis.


## Screenshot(s)

![Main Screenshot](https://github.com/jstrieb/GroupMe-Archiver/blob/master/doc/screenshots/main.png?raw=true)


## Quick Start

1. Download the appropriate version for your computer from the links below or the [releases page](https://github.com/jstrieb/GroupMe-Archiver/releases)
2. Run the program
3. Click the `Help > Get GroupMe acecss token` menu button from the menu bar at the top of the window
4. Log into GroupMe at the link that will open in the browser
5. After you are redirected from the login screen, copy the API key found after the `=` in the URL. Ignore that it says `Not Found`. The API key may be called an "access token" or something similar, but each term refers to the same thing.
6. Paste the API key into the appropriate text box in the program
7. Click a group you want to archive and configure how to download it
8. Configure the number of threads to use. Generally, it is best to use between 4 and 8 for maximum efficiency, depending on your computer.
9. Press `Begin Archiving` in the bottom right corner and enjoy


## Download

- Download the latest `jar` version [here](https://github.com/jstrieb/GroupMe-Archiver/releases/download/v1.0/GroupMeArchiver-v1.0-jar.zip). This version should work on most operating systems. (7MB)
- Alternatively, download a package for Windows with all dependencies preinstalled [here](https://github.com/jstrieb/GroupMe-Archiver/releases/download/v1.0/GroupMeArchiver-v1.0-windows.zip). This version is considerably larger, but will probably work better on Windows computers. (70MB)
- For more release information and old versions, see the [releases page](https://github.com/jstrieb/GroupMe-Archiver/releases)

This code has not been tested beyond use on a Windows 10 computer with Java 8; your mileage may vary.


## Compiling

GroupMe Archiver depends on `JavaFX` and the `Jackson` JSON parsing library, and it is built with `ant`. I originally created the project in NetBeans, and it is probably easiest to clone the repository with `git clone`. Then, import the project into NetBeans and run it.


## TODO

- Support plaintext and CSV message export
- Allow for downloading media and messages within a specific date range and support for updating existing archives
- Support listing more than 499 groups
- Add icon, description, and other metadata
- Build versions for various operating systems and updated versions of Java/JavaFX
