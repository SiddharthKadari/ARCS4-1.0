# Makefile

# Directories
BIN_DIR = bin
SRC_DIR = src
LIB_DIR = lib

# Libraries
LIBS = $(LIB_DIR)/twophase.jar:$(LIB_DIR)/jSerialComm-2.11.0.jar

# Main class
MAIN_CLASS = Main

# JAR file
JAR_FILE = $(BIN_DIR)/threephase.jar

# Manifest file
MANIFEST_FILE = META-INF/MANIFEST.MF

# Java files
JAVA_FILES = $(SRC_DIR)/*.java

# Targets
all: run

# Create bin directory
$(BIN_DIR):
	mkdir -p $(BIN_DIR)

# Clean up previous build artifacts
clean:
	rm -rf $(BIN_DIR)/* *.data

# Compile Java files
compile: $(BIN_DIR)
	javac -d $(BIN_DIR) -cp $(LIBS) $(JAVA_FILES)

# Create JAR file
jar: compile
	jar -cfm $(JAR_FILE) $(MANIFEST_FILE) -C $(BIN_DIR) .

# Run the program
run: jar
	java -cp $(BIN_DIR):$(JAR_FILE):$(LIBS) $(MAIN_CLASS)
