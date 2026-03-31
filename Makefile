ifeq ($(OS),Windows_NT)
	GRADLE_EXECUTABLE = gradlew.cmd
else
	GRADLE_EXECUTABLE = ./gradlew
endif

.PHONY: all
all: clean flatbuffers build

.PHONY: build
build:
	${GRADLE_EXECUTABLE} jar

.PHONY: flatbuffers
flatbuffers:
	${GRADLE_EXECUTABLE} createFlatBuffers

.PHONY: clean
clean:
	${GRADLE_EXECUTABLE} clean
