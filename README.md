# project
file compressor and decompressor

JavaFX File Compressor/Decompressor
This is a complete, runnable desktop application built with JavaFX that provides a graphical user interface to compress (zip) and decompress (unzip) files and folders.
The core logic uses the java.util.zip package.
Project Deliverables
1.	Source Code: All source code is included in the src directory, structured as a standard Maven project.
2.	Executable File: You can create an executable .jar file from this Maven project using the command mvn package. The resulting .jar will be in the target directory
Features
•	Compress Multiple Files: Select one or more files to add to a new .zip archive.
•	Compress Folder: Select an entire directory to compress into a .zip archive, preserving the folder structure.
•	Extract ZIP: Select a .zip file and a destination directory to extract its contents.
•	Progress Bar: A progress bar shows the status of the compression/extraction based on the number of files processed.
•	Logging: A text area logs all actions, including which files are added/extracted and the final statistics.


🛑 How to Run This Project 🛑
You'll need Java (JDK 11 or newer) and Maven installed.
1.	Open a terminal or command prompt.
2.	Navigate to the root directory of this project (where the pom.xml file is).
3.	Run the following command:
4.	mvn compile exec:java

Maven will download all the necessary dependencies (JavaFX) and then launch the application window.
Project Structure
.
├── pom.xml                 # Maven configuration (dependencies)
├── README.md               # This file
└── src
    └── main
        ├── java
        │   └── com
        │       └── filezipper
        │           ├── App.java          # Main JavaFX application (UI)
        │           └── ZipService.java   # Core zipping/unzipping logic
        └── resources
            └── com
                └── filezipper
                    └── style.css       # Styles for the UI


